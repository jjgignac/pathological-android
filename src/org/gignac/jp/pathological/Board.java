package org.gignac.jp.pathological;
import java.io.*;
import java.util.*;
import android.graphics.*;
import android.util.*;
import android.content.res.*;

class Board implements Paintable
{
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
	private GameResources gr;
	public Trigger trigger;
	public Stoplight stoplight;
	public Vector<Marble> marbles;
	public Tile[][] tiles;
	private int[] launch_queue;
	private int board_complete;
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

	public Board(GameResources gr, int level)
	{
		self = this;
		this.gr = gr;
		this.marbles = new Vector<Marble>();
		this.trigger = null;
		this.stoplight = null;
		this.launch_queue = new int[screen_height * 3 / Marble.marble_size];
		this.board_complete = 0;
		this.paused = false;
		this.live_marbles_limit = 10;
		this.launch_timeout = -1;
		this.board_timeout = -1;
		this.colors = default_colors;

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

		// Fill up the launch queue
		for( int i=0; i < launch_queue.length; ++i) {
			launch_queue[i] = colors.charAt(gr.random.nextInt(colors.length()))-'0';
		}

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

		Sprite.cache( R.drawable.backdrop);
		Sprite.cache( R.drawable.launcher_corner);
		Sprite.cache( R.drawable.entrance, entrance);
		Sprite.cache( Marble.marble_images);
		Sprite.cache( R.drawable.blank_bg_tile, liveCounter);

		// Load the level
		try {
			_load(gr,  level);
		} catch(IOException e) {}
	}

	public void draw_back(Blitter b) {
		// Draw the backdrop
		Rect v = b.getVisibleArea();
		int fullHeight = (int)Math.ceil(v.bottom/scale);
		int leftMargin = Math.round(offsetx/scale);
		b.blit( R.drawable.backdrop, -leftMargin, 0,
			screen_width - Marble.marble_size*3/4 + leftMargin,
			fullHeight);

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

		b.blit(R.drawable.blank_bg_tile, Marble.marble_size/2, 0);

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

		for(int i=0; i < self.launch_queue.length; ++i)
			b.blit(Marble.marble_images[self.launch_queue[i]],
				board_width, launch_queue_offset + i * Marble.marble_size);
	}

	public void draw_fore( Blitter b) {
		for(Tile[] row : self.tiles)
			for(Tile tile : row)
				tile.draw_fore(b);
	}

	public void update() {
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
		self.board_complete = 1;
		for(Tile[] row : self.tiles)
			for(Tile tile : row)
				if(tile instanceof Wheel)
					if(!tile.completed) self.board_complete = 0;

		// Decrement the launch timer
		if(self.launch_timeout > 0) {
			self.launch_timeout -= 1;
			if(self.launch_timeout == 0) self.board_complete = -1;
		}

		// Decrement the board timer
		if( self.board_timeout > 0) {
			self.board_timeout -= 1;
			if(self.board_timeout == 0) self.board_complete = -2;
		}

		// Animate the launch queue
		if( launch_queue_offset > 0)
			--launch_queue_offset;
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
		b.transform( scale, offsetx, 0.0f);
		
		// Draw the background
		self.draw_back(b);

		// Draw all of the marbles
		for(Marble marble : self.marbles)
			marble.draw(b);

		// Draw the foreground
		self.draw_fore(b);

		// Trigger the update step		
		notify();
	}

	public void set_tile( int x, int y, Tile tile) {
		self.tiles[y][x] = tile;
		tile.pos.left = Tile.tile_size * x;
		tile.pos.top = Marble.marble_size + Tile.tile_size * y;
		tile.tile_x = x;
		tile.tile_y = y;

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
		Sprite.cache( R.drawable.blank_bg_tile, liveCounter);
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

	public void downEvent(int pointerId, float x, float y)
	{
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

	public void upEvent(int pointerId, float x, float y)
	{
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
					tile = new Wheel( gr, pathsint);
					numwheels += 1;
				} else if( type == '%') tile = new Trigger(gr, self.colors);
				else if( type == '!') tile = new Stoplight(gr, stoplight);
				else if( type == '&') tile = new Painter(gr, pathsint, colorint);
				else if( type == '#') tile = new Filter(gr, pathsint, colorint);
				else if( type == '@') {
					if( color == ' ') tile = new Buffer(gr, pathsint, -1);
					else tile = new Buffer(gr, pathsint, colorint);
				}
				else if( type == ' ' ||
					(type >= '0' && type <= '8')) tile = new Tile(gr, pathsint);
				else if( type == 'X') tile = new Shredder(gr, pathsint);
				else if( type == '*') tile = new Replicator(gr, pathsint, colorint);
				else if( type == '^') {
					if( color == ' ') tile = new Director(gr, pathsint, 0);
					else if( color == '>') tile = new Switch(gr, pathsint, 0, 1);
					else if( color == 'v') tile = new Switch(gr, pathsint, 0, 2);
					else if( color == '<') tile = new Switch(gr, pathsint, 0, 3);
				} else if( type == '>') {
					if( color == ' ') tile = new Director(gr, pathsint, 1);
					else if( color == '^') tile = new Switch(gr, pathsint, 1, 0);
					else if( color == 'v') tile = new Switch(gr, pathsint, 1, 2);
					else if( color == '<') tile = new Switch(gr, pathsint, 1, 3);
				} else if( type == 'v') {
					if( color == ' ') tile = new Director(gr, pathsint, 2);
					else if( color == '^') tile = new Switch(gr, pathsint, 2, 0);
					else if( color == '>') tile = new Switch(gr, pathsint, 2, 1);
					else if( color == '<') tile = new Switch(gr, pathsint, 2, 3);
				} else if( type == '<') {
					if( color == ' ') tile = new Director(gr,pathsint, 3);
					else if( color == '^') tile = new Switch(gr, pathsint, 3, 0);
					else if( color == '>') tile = new Switch(gr, pathsint, 3, 1);
					else if( color == 'v') tile = new Switch(gr, pathsint, 3, 2);
				}
				else if( type == '=') {
					if( teleporter_names.indexOf(color) >= 0) {
						Tile other = teleporters.get( teleporter_names.indexOf(color));
						tile = new Teleporter( gr, pathsint, (Teleporter)other);
					} else {
						tile = new Teleporter( gr, pathsint, null);
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

	// Return values for this function:
	// -4: User closed the application window
	// -3: User aborted the level
	// -2: Board timer expired
	// -1: Launch timer expired
	//  1: Level completed successfully
	//  2: User requested a skip to the next level
	//  3: User requested a skip to the previous level
	public int play_level()
	{
		// Perform the first render
		this.update();

		// Play the start sound
		//play_sound( levelbegin)

		// Launch the first marble
		self.launch_marble();

		// Do the first update
//		pygame.display.update();

		// Play the end sound
		if( self.board_complete > 0)
			gr.play_sound( gr.levelfinish);
		else
			gr.play_sound( gr.die);

		return self.board_complete;
	}
}

