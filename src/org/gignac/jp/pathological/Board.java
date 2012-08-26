package org.gignac.jp.pathological;
import java.io.*;
import java.util.*;
import android.graphics.*;
import android.util.*;
import android.content.res.*;

class Board implements Paintable
{
	public static final int INCOMPLETE = 0;
	public static final int COMPLETE = 1;
	public static final int LAUNCH_TIMEOUT = -1;
	public static final int BOARD_TIMEOUT = -2;
	private static final String default_colors = "2346";
	private static final String default_stoplight = "643";
	private static final int default_launch_timer = 6;
	private static final int default_board_timer = 30;
	public static final int vert_tiles = 6;
	public static final int horiz_tiles = 8;
	public static final int board_width = horiz_tiles * Tile.tile_size;
	public static final int board_height = vert_tiles * Tile.tile_size;
	public static final int screen_width = board_width + Marble.marble_size;
	public static final int screen_height = board_height + Marble.marble_size;
	public static final int timer_width = Marble.marble_size*3/4;
	public GameResources gr;
	public Trigger trigger;
	public Stoplight stoplight;
	public Vector<Marble> marbles;
	public Tile[][] tiles;
	private int[] launch_queue;
	private int board_state;
	private boolean paused;
	public String name;
	public int live_marbles_limit;
	private int launch_timeout;
	private int launch_timeout_start;
	private int board_timer;
	private int board_timeout;
	private int board_timeout_start;
	public String colors;
	private Board self;
	public int launch_timer;
	private Marble[] marblesCopy = new Marble[20];
	private HashMap<Integer,Point> down;
	private int launch_queue_offset;
	private Bitmap liveCounter;
	private Canvas liveCounterCanvas;
	private final Paint paint = new Paint();
	private float scale = 0f;
	private float offsetx;
	private Bitmap bg;
	private BitmapBlitter bgBlitter;
	private Runnable onPainted;
	public SpriteCache sc;

	public Board(GameResources gr, SpriteCache sc,
		int level, Runnable onPainted)
	{
		self = this;
		this.gr = gr;
		this.marbles = new Vector<Marble>();
		this.trigger = null;
		this.stoplight = null;
		this.launch_queue = new int[screen_height * 3 / Marble.marble_size];
		this.board_state = INCOMPLETE;
		this.paused = false;
		this.live_marbles_limit = 10;
		this.launch_timeout = -1;
		this.board_timeout = -1;
		this.colors = default_colors;
		this.onPainted = onPainted;
		this.sc = sc;

		down = new HashMap<Integer,Point>();

		paint.setAntiAlias(true);
		paint.setColor(0xfff0f0f0);
		paint.setTextSize(Marble.marble_size*4/5);
		paint.setTypeface(Typeface.DEFAULT_BOLD);

		// Seed the randomness based on the level number and
		// the current time.  Only use the time accurate to
		// the ten-minute interval.  This will discourage players
		// from reloading levels repeatedly in order to get
		// their choice of marbles/trigger/etc.
		gr.random.setSeed((System.currentTimeMillis()/600000)*1000+level);

		set_launch_timer( default_launch_timer);
		set_board_timer( default_board_timer);

		// Create the board array
		tiles = new Tile[vert_tiles][];
		for( int j=0; j < vert_tiles; ++j)
			tiles[j] = new Tile[horiz_tiles];

		// prepare the entrance image
		Bitmap entrance = Bitmap.createBitmap( Tile.tile_size/2,
			Tile.tile_size/2, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(entrance);
		c.drawBitmap( gr.loadBitmap(R.drawable.path_14), null,
			new Rect(-Tile.tile_size/4, -Tile.tile_size/4,
				Tile.tile_size*3/4, Tile.tile_size*3/4), null);

		// Prepare the live marbles counter image
		liveCounter = Bitmap.createBitmap(
			Marble.marble_size * 5, Marble.marble_size,
			Bitmap.Config.ARGB_8888);
		liveCounterCanvas = new Canvas(liveCounter);

		sc.cache( R.drawable.backdrop);
		sc.cache( R.drawable.launcher_corner);
		sc.cache( 0x100000001l, entrance);
		sc.cache( Marble.marble_images);
		sc.cache( 0x100000002l, liveCounter);

		// Load the level
		try {
			_load(gr,  level);
		} catch(IOException e) {}

		// Fill up the launch queue
		for( int i=0; i < launch_queue.length; ++i) {
			launch_queue[i] = colors.charAt(gr.random.nextInt(colors.length()))-'0';
		}
	}

	private void draw_backdrop(Blitter b) {
		Rect v = b.getVisibleArea();
		b.blit( R.drawable.backdrop,
			0, 0, v.right, v.bottom);
	}

	private void draw_back(Blitter b)
	{
		// Black-out the right edge of the backdrop
		b.fill( 0xff000000, screen_width - Marble.marble_size*3/4,
			0, Marble.marble_size*3/4+timer_width, screen_height);

		// Draw the launcher
		b.blit( R.drawable.path_10,
			Marble.marble_size, (Marble.marble_size-Tile.tile_size)/2,
			board_width - Marble.marble_size, Tile.tile_size);
		b.blit( R.drawable.path_2,
			(Marble.marble_size-Tile.tile_size)/2,
			(Marble.marble_size-Tile.tile_size)/2);
		b.blit( R.drawable.launcher_corner,
			board_width-(Tile.tile_size-Marble.marble_size)/2, 0);
		b.blit(R.drawable.path_5,
			   board_width+(Marble.marble_size-Tile.tile_size)/2,
			   Marble.marble_size-1, Tile.tile_size,
			   board_height*3);
		
		for( Tile[] row : self.tiles)
			for( Tile tile : row)
				tile.draw_back(b);
	}

	private void draw_mid( Blitter b)
	{
		Rect v = b.getVisibleArea();
		int fullHeight = (int)Math.ceil(v.bottom/scale);

		// Draw the launch timer
		int timerColor = 0x40404040;
		float timeLeft = (float)launch_timeout / Game.frames_per_sec;
		if( timeLeft < 3.5f) {
			// Make the timer flash to indicate that time
			// is running out.
			float s = (float)Math.sin(timeLeft*5);
			int phase = Math.round(s*s*191);
			timerColor = 0x40404040 + (phase<<16) + ((phase*2/3)<<24);
		}
		int x = (launch_timeout*board_width+launch_timeout_start/2) /
			launch_timeout_start;
		b.fill(timerColor, x, 0,
			board_width - x, Marble.marble_size);

		// Draw the live marble counter
		b.blit(0x100000002l, Marble.marble_size/2, 0);

		// Draw the board timer
		timerColor = 0xff000080;
		timeLeft = (float)board_timeout / Game.frames_per_sec;
		if( timeLeft < 60f && board_timeout*2 < board_timeout_start) {
			// Make the timer flash to indicate that time
			// is running out.
			float s = (float)Math.sin(timeLeft*3);
			int phase = Math.round(s*s*255);
			timerColor = 0xff000000 | phase | (255-phase)<<16;
		}
		int timer_height = fullHeight;
		int y = (board_timeout*timer_height+board_timeout_start/2) /
			board_timeout_start;
		b.fill(0xff000000, screen_width+3,
			0, timer_width-3, timer_height-y);
		b.fill(timerColor, screen_width+3,
			timer_height-y, timer_width-3, y);

		// Draw the marble queue
		for(int i=0; i < self.launch_queue.length; ++i)
			b.blit(Marble.marble_images[self.launch_queue[i]],
				board_width, launch_queue_offset + i * Marble.marble_size);
	}

	private void draw_fore( Blitter b) {
		for(Tile[] row : self.tiles)
			for(Tile tile : row)
				tile.draw_fore(b);
	}

	public synchronized int update()
	{
		// Return INCOMPLETE even if the board is complete.
		// This ensures that the end of level signal is only
		// sent once.
		if(paused || board_state != INCOMPLETE) return INCOMPLETE;

		// Animate the marbles
		marblesCopy = marbles.toArray(marblesCopy);
		for(Marble marble : marblesCopy) {
			if( marble == null) break;
			marble.update(this);
		}

		// Animate the tiles
		for(Tile[] row : self.tiles)
			for(Tile tile : row)
				tile.update(this);

		// Complete any wheels, if appropriate
		boolean try_again = true;
		while(try_again) {
			try_again = false;
			for(Tile[] row : self.tiles)
				for(Tile tile : row)
					if( tile instanceof Wheel)
						try_again |= ((Wheel)tile).maybe_complete(this);
		}

		// Check if the board is complete
		board_state = COMPLETE;
		for(Tile[] row : self.tiles)
			for(Tile tile : row)
				if(tile instanceof Wheel)
					if(!((Wheel)tile).completed)
						board_state = INCOMPLETE;

		// Decrement the launch timer
		if(board_state == INCOMPLETE && launch_timeout > 0) {
			launch_timeout -= 1;
			if(launch_timeout == 0) board_state = LAUNCH_TIMEOUT;
		}

		// Decrement the board timer
		if( board_state == INCOMPLETE && board_timeout > 0) {
			board_timeout -= 1;
			if(board_timeout == 0) board_state = BOARD_TIMEOUT;
		}

		// Animate the launch queue
		if( launch_queue_offset > 0)
			--launch_queue_offset;

		return board_state;
	}

	private void refresh_bg_cache()
	{
		// Refresh the background
		boolean dirty = false;
		for( Tile[] row : self.tiles) {
			for( Tile tile : row) {
				if( tile.dirty) {
					tile.draw_back(bgBlitter);
					tile.dirty = false;
					dirty = true;
				}
			}
		}
		if(dirty) sc.cache(0x500000000l,bg);
	}

	private void cache_background(int w,int h)
	{
		int px = Board.screen_width + Board.timer_width;
		int py = Board.screen_height;
		if( w * py > h * px) {
			w = (py * w + h/2) / h;
			h = py;
		} else {
			h = (px * h + w/2) / w;
			w = px;
		}

		if( bgBlitter != null) {
			Rect v = bgBlitter.getVisibleArea();
			if( v.right == w && v.bottom == h) return;
		}

		bg = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
		bgBlitter = new BitmapBlitter(sc, bg);
		draw_backdrop(bgBlitter);
		bgBlitter.transform(1f,w-px,0f);
		draw_back(bgBlitter);
		sc.cache(0x500000000l,bg);
	}

	public synchronized void paint(Blitter b)
	{
		int px = Board.screen_width + Board.timer_width;
		Rect r = b.getVisibleArea();
		float width = r.right - r.left;
		float height = r.bottom - r.top;
		scale = width * Board.screen_height < height * px ?
			width / px : height / Board.screen_height;
		offsetx = width - px*scale;
		
		// Draw the background
		cache_background(r.right, r.bottom);
		refresh_bg_cache();
		b.blit(0x500000000l,0,0,r.right,r.bottom);

		b.transform( scale, offsetx, 0.0f);

		// Draw the middle
		self.draw_mid(b);

		// Draw all of the marbles
		for(Marble marble : self.marbles)
			marble.draw(b);

		// Draw the foreground
		self.draw_fore(b);

		// Trigger the update step		
		if(onPainted != null) onPainted.run();
	}

	public void set_tile( int x, int y, Tile tile) {
		self.tiles[y][x] = tile;
		tile.setxy(x,y);

		// If it's a trigger, keep track of it
		if( tile instanceof Trigger)
			self.trigger = (Trigger)tile;

		// If it's a stoplight, keep track of it
		if( tile instanceof Stoplight)
			self.stoplight = (Stoplight)tile;
	}

	public void set_launch_timer( int passes) {
		self.launch_timer = passes;
		self.launch_timeout_start = (Marble.marble_size +
			(horiz_tiles * Tile.tile_size - Marble.marble_size)
				* passes) / Marble.marble_speed;
	}

	public void set_board_timer(int seconds) {
		self.board_timer = seconds;
		self.board_timeout_start = seconds * Game.frames_per_sec;
		self.board_timeout = self.board_timeout_start;
	}

	public void activateMarble( Marble m) {
		marbles.add(m);
		updateLiveCounter();
	}

	public void deactivateMarble( Marble m) {
		marbles.remove(m);
		updateLiveCounter();
	}

	private void updateLiveCounter() {
		liveCounterCanvas.drawColor(0,PorterDuff.Mode.CLEAR);
		int live = marbles.size();
		if( live > live_marbles_limit) live = live_marbles_limit;
		String s = live+" / "+live_marbles_limit;
		liveCounterCanvas.drawText( s, 0, Marble.marble_size*4/5, paint);
		sc.cache( 0x100000002l, liveCounter);
	}

	public void launch_marble() {
		activateMarble( new Marble(
			gr, self.launch_queue[0],
			board_width+Marble.marble_size/2,
			Marble.marble_size/2, 3));
		for( int i=0; i < launch_queue.length-1; ++i)
			launch_queue[i] = launch_queue[i+1];
		self.launch_queue[launch_queue.length-1] =
			colors.charAt(gr.random.nextInt(colors.length()))-'0';
		self.launch_timeout = self.launch_timeout_start;
		launch_queue_offset = Marble.marble_size;
	}

	public void affect_marble( Marble marble)
	{
		int cx = marble.pos.left + Marble.marble_size/2;
		int cy = marble.pos.top - Marble.marble_size/2;

		// Bounce marbles off of the top
		if( cy == Marble.marble_size/2) {
			marble.direction = 2;
			return;
		}

		int effective_cx = cx + Marble.marble_size/2 * Marble.dx[marble.direction];
		int effective_cy = cy + Marble.marble_size/2 * Marble.dy[marble.direction];

		if( cy < 0) {
			if(cx == Marble.marble_size/2) {
				marble.direction = 1;
				return;
			}
			if( cx == Tile.tile_size * horiz_tiles - Marble.marble_size/2
				&& marble.direction == 1) {
				marble.direction = 3;
				return;
			}

			// The special case of new marbles at the top
			effective_cx = cx;
			effective_cy = cy + Marble.marble_size;
		}
		
		int tile_x = effective_cx / Tile.tile_size;
		int tile_y = effective_cy / Tile.tile_size;
		int tile_xr = cx - tile_x * Tile.tile_size;
		int tile_yr = cy - tile_y * Tile.tile_size;

		if( tile_x >= horiz_tiles) return;

		Tile tile = self.tiles[tile_y][tile_x];

		if( cy < 0 && marble.direction != 2) {
			// The special case of new marbles at the top
			if( tile_xr == Tile.tile_size / 2 && ((tile.paths & 1) == 1)) {
				if( tile instanceof Wheel) {
					Wheel w = (Wheel)tile;
					if( w.spinpos > 0 || w.marbles[0] != -3) return;
					w.marbles[0] = -2;
					marble.direction = 2;
					this.launch_marble();
				} else if( this.marbles.size() < self.live_marbles_limit) {
					marble.direction = 2;
					this.launch_marble();
				}
			}
		} else
			tile.affect_marble( this, marble, tile_xr, tile_yr);
	}

	private Tile whichTile(int posx, int posy) {
		// Determine which tile the pointer is in
		int tile_x = posx / Tile.tile_size;
		int tile_y = (posy - Marble.marble_size) / Tile.tile_size;
		if( tile_x >= 0 && tile_x < horiz_tiles &&
			tile_y >= 0 && tile_y < vert_tiles) {
			return self.tiles[tile_y][tile_x];
		}
		return null;
	}

	public synchronized void downEvent(int pointerId, float x, float y)
	{
		if(paused || board_state != INCOMPLETE) return;
		if(scale == 0f) return;
		int posx = Math.round((x - offsetx) / scale);
		int posy = Math.round(y / scale);
		Point pos = down.get(pointerId);
		if( pos == null) {
			down.put(pointerId,new Point(posx,posy));
		} else {
			pos.x = posx;
			pos.y = posy;
		}
	}

	public synchronized void upEvent(int pointerId, float x, float y)
	{
		if(paused || board_state != INCOMPLETE) return;
		if(scale == 0f) return;
		int posx = Math.round((x - offsetx) / scale);
		int posy = Math.round(y / scale);
		final Point dpos = down.get(pointerId);
		if(dpos == null) return;
		final int downx = dpos.x, downy = dpos.y;
		Tile downtile = whichTile(downx,downy);
		if(downtile == null) return;
		int downtile_x = downx / Tile.tile_size;
		int downtile_y = (downy - Marble.marble_size) / Tile.tile_size;
		int tile_xr = downx-(downtile_x*Tile.tile_size);
		int tile_yr = downx-Marble.marble_size-(downtile_y*Tile.tile_size);
		int dx = posx - downx;
		int dy = posy - downy;
		int dx2 = dx*dx;
		int dy2 = dy*dy;
		if(dx2 + dy2 <= Marble.marble_size * Marble.marble_size) {
			downtile.click(this, tile_xr, tile_yr);
		} else {
			int dir = (dx2>dy2)?(dx>0?1:3):(dy>0?2:0);
			downtile.flick(this, tile_xr, tile_yr, dir);
		}
	}

	public boolean _load(GameResources gr, int level)
		throws IOException
	{
		BufferedReader f = new BufferedReader( new InputStreamReader(
			gr.openRawResource( R.raw.all_boards)));

		// Skip the previous levels
		int j = 0;
		while( j < vert_tiles * level) {
			String line = f.readLine();
			if( line==null) {
				f.close();
				return false;
			}
			if( line.isEmpty()) continue;
			if( line.charAt(0) == '|') j += 1;
		}

		Vector<Tile> teleporters = new Vector<Tile>();
		String teleporter_names = "";
		String stoplight = default_stoplight;

		int numwheels = 0;
		int boardtimer = -1;

		j = 0;
		while( j < vert_tiles) {
			String line = f.readLine();
			if(line.isEmpty()) continue;

			if( line.charAt(0) != '|') {
				if( line.startsWith("name="))
					self.name = line.substring(5);
				else if( line.startsWith("maxmarbles="))
					self.live_marbles_limit = Integer.parseInt(line.substring(11));
				else if( line.startsWith("launchtimer="))
					self.set_launch_timer( Integer.parseInt(line.substring(12)));
				else if( line.startsWith("boardtimer="))
					boardtimer = Integer.parseInt(line.substring(11));
				else if( line.startsWith("colors=")) {
					self.colors = "";
					for( char c : line.substring(7).toCharArray()) {
						if( c >= '0' && c <= '7') {
							self.colors = self.colors + c;
							self.colors = self.colors + c;
							self.colors = self.colors + c;
						} else if( c == '8') {
							// Crazy marbles are one-third as common
							self.colors = self.colors + c;
						}
					}
				} else if( line.startsWith("stoplight=")) {
					stoplight = "";
					for(char c : line.substring(10).toCharArray())
						if( c >= '0' && c <= '7')
							stoplight = stoplight + c;
				}

				continue;
			}

			for( int i=0; i < horiz_tiles; ++i) {
				char type = line.charAt(i*4+1);
				char paths = line.charAt(i*4+2);
				int pathsint = paths-'0';
				if( paths == ' ') pathsint = 0;
				else if( paths >= 'a') pathsint = paths-'a'+10;
				char color = line.charAt(i*4+3);
				int colorint = 0;
				if( color == ' ') colorint = 0;
				else if( color >= 'a') colorint = color-'a'+10;
				else if( color >= '0' && color <= '9') colorint = color-'0';
				else colorint = 0;

				Tile tile = null;
				if( type == 'O') {
					tile = new Wheel(this, pathsint);
					numwheels += 1;
				} else if( type == '%') tile = new Trigger(this, self.colors);
				else if( type == '!') tile = new Stoplight(this, stoplight);
				else if( type == '&') tile = new Painter(this, pathsint, colorint);
				else if( type == '#') tile = new Filter(this, pathsint, colorint);
				else if( type == '@') {
					if( color == ' ') tile = new Buffer(this, pathsint, -1);
					else tile = new Buffer(this, pathsint, colorint);
				}
				else if( type == ' ' ||
					(type >= '0' && type <= '8')) tile = new Tile(this, pathsint);
				else if( type == 'X') tile = new Shredder(this, pathsint);
				else if( type == '*') tile = new Replicator(this, pathsint, colorint);
				else if( type == '^') {
					if( color == ' ') tile = new Director(this, pathsint, 0);
					else if( color == '>') tile = new Switch(this, pathsint, 0, 1);
					else if( color == 'v') tile = new Switch(this, pathsint, 0, 2);
					else if( color == '<') tile = new Switch(this, pathsint, 0, 3);
				} else if( type == '>') {
					if( color == ' ') tile = new Director(this, pathsint, 1);
					else if( color == '^') tile = new Switch(this, pathsint, 1, 0);
					else if( color == 'v') tile = new Switch(this, pathsint, 1, 2);
					else if( color == '<') tile = new Switch(this, pathsint, 1, 3);
				} else if( type == 'v') {
					if( color == ' ') tile = new Director(this, pathsint, 2);
					else if( color == '^') tile = new Switch(this, pathsint, 2, 0);
					else if( color == '>') tile = new Switch(this, pathsint, 2, 1);
					else if( color == '<') tile = new Switch(this, pathsint, 2, 3);
				} else if( type == '<') {
					if( color == ' ') tile = new Director(this,pathsint, 3);
					else if( color == '^') tile = new Switch(this, pathsint, 3, 0);
					else if( color == '>') tile = new Switch(this, pathsint, 3, 1);
					else if( color == 'v') tile = new Switch(this, pathsint, 3, 2);
				}
				else if( type == '=') {
					if( teleporter_names.indexOf(color) >= 0) {
						Tile other = teleporters.get( teleporter_names.indexOf(color));
						tile = new Teleporter( this, pathsint, (Teleporter)other);
					} else {
						tile = new Teleporter( this, pathsint, null);
						teleporters.addElement( tile);
						teleporter_names = teleporter_names + color;
					}
				}

				this.set_tile( i, j, tile);

				if( type >= '0' && type <= '8') {
					int direction;
					if( color == '^') direction = 0;
					else if( color == '>') direction = 1;
					else if( color == 'v') direction = 2;
					else direction = 3;
					activateMarble( new Marble(gr, type-'0',
						tile.pos.left + Tile.tile_size/2,
						tile.pos.top + Tile.tile_size/2,
						direction));
				}
			}

			j += 1;
		}
		if( boardtimer < 0) boardtimer = default_board_timer * numwheels;
		this.set_board_timer( boardtimer);
		f.close();
		return true;
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}
}

