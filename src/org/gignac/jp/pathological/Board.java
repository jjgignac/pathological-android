package org.gignac.jp.pathological;
import java.io.*;
import java.util.*;
import android.graphics.*;
import android.util.*;

class Board
{
	public static final int frames_per_sec = 100;
	private static final String default_colors = "2346";
	private static final String default_stoplight = "643";
	private static final int default_launch_timer = 6;
	private static final int default_board_timer = 30;
	public static final int vert_tiles = 6;
	public static final int horiz_tiles = 8;
	public static final int info_height = 20;
	public static final int launch_timer_posx = 0;
	public static final int launch_timer_posy = info_height;
	public static final int board_width = horiz_tiles * Tile.tile_size;
	public static final int board_height = vert_tiles * Tile.tile_size;
	public static final int timer_width = 36;
	public static final int timer_margin = 4;
	public static final int timer_height = board_height + Marble.marble_size;
	public static final int board_posx = timer_width;
	public static final int board_posy = info_height + Marble.marble_size;
	public Game game;
	private GameResources gr;
	public Trigger trigger;
	public Stoplight stoplight;
	private int posx, posy;
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
	private int launch_timer_height;
	public int launch_timer;
	private Paint paint;
	private Marble[] marblesCopy = new Marble[20];
	private int downx, downy;

	public Board(Game game, GameResources gr, int posx, int posy)
	{
		self = this;
		this.game = game;
		this.gr = gr;
		this.posx = posx;
		this.posy = posy;
		this.marbles = new Vector<Marble>();
		this.trigger = null;
		this.stoplight = null;
		this.launch_queue = new int[vert_tiles * Tile.tile_size / Marble.marble_size + 2];
		this.board_complete = 0;
		this.paused = false;
		this.live_marbles_limit = 10;
		this.launch_timeout = -1;
		this.board_timeout = -1;
		this.colors = default_colors;
		this.launch_timer_height = -1;

		set_launch_timer( default_launch_timer);
		set_board_timer( default_board_timer);

		// Create the board array
		tiles = new Tile[vert_tiles][];
		for( int j=0; j < vert_tiles; ++j)
			tiles[j] = new Tile[horiz_tiles];

		// Load the level
		try {
			_load( game.level % game.numlevels);
		} catch(IOException e) {}

		// Fill up the launch queue
		for( int i=0; i < launch_queue.length; ++i) {
			launch_queue[i] = colors.charAt((int)(Math.random()*colors.length()))-'0';
		}

		// Create The Background
		Bitmap b = Bitmap.createBitmap(
			Game.screen_width, Game.screen_height, Bitmap.Config.ARGB_8888);
		
		Canvas c = new Canvas(b);
		this.paint = new Paint();
		paint.setColor(0xffc8c8c8);
		c.drawPaint(paint);    // Color of Info Bar

		// Draw the Backdrop
		Bitmap backdrop = BitmapFactory.decodeResource(game.getResources(), R.drawable.backdrop);
		c.drawBitmap( backdrop, null,
			new Rect(board_posx, board_posy,
				board_posx + horiz_tiles * Tile.tile_size,-
				board_posy + vert_tiles * Tile.tile_size), null);

		// Draw the launcher
		Bitmap launcher_background = BitmapFactory.decodeResource(
			game.getResources(), R.drawable.launcher);
		c.drawBitmap( launcher_background, null,
			new Rect(board_posx, board_posy - Marble.marble_size,
				board_posx + horiz_tiles * Tile.tile_size,
				board_posy), null);
		Bitmap launcher_v = BitmapFactory.decodeResource(
			game.getResources(), R.drawable.launcher_v);
		c.drawBitmap( launcher_v, null,
			new Rect(board_posx+horiz_tiles*Tile.tile_size, board_posy,
				board_posx+horiz_tiles*Tile.tile_size+Marble.marble_size,
				board_posy+vert_tiles*Tile.tile_size), null);
		Bitmap launcher_entrance = BitmapFactory.decodeResource(
			game.getResources(), R.drawable.entrance);
		for(int i=0; i < horiz_tiles; ++i)
			if((self.tiles[0][i].paths & 1) == 1)
				c.drawBitmap( launcher_entrance, null,
					new Rect( board_posx+Tile.tile_size*i,
						board_posy-Marble.marble_size,
						board_posx+Tile.tile_size*(i+1),
						board_posy), null);
		Bitmap launcher_corner = BitmapFactory.decodeResource(
			game.getResources(), R.drawable.launcher_corner);
		c.drawBitmap( launcher_corner, null,
			new Rect( board_posx+horiz_tiles*Tile.tile_size-
				(Tile.tile_size-Marble.marble_size)/2,
				board_posy - Marble.marble_size,
				board_posx+horiz_tiles*Tile.tile_size+Marble.marble_size,
				board_posy), null);

		Sprite.cache( R.drawable.backdrop, b);
		Sprite.cache( Marble.marble_images);
	}

	public void draw_back(Blitter b) {
		int height;
		
		// Draw the background
		b.blit(R.drawable.backdrop, 0, 0);

		// Draw the launch timer
		if(self.launch_timer_height == -1) {
			height = timer_height;
//			paint.setColor(0xff000000);
//			c.drawRect( launch_timer_posx, launch_timer_posy,
//				launch_timer_posx + timer_width,
//				launch_timer_posy + timer_height, paint);
//			paint.setColor(0xff0028ff);
//			c.drawRect( launch_timer_posx+timer_margin,
//				launch_timer_posy+timer_height-height,
//				launch_timer_posx+timer_width-timer_margin,
//				launch_timer_posy+timer_height, paint);
		} else {
			height = timer_height*self.launch_timeout/self.launch_timeout_start;
			if( height < self.launch_timer_height) {
//				paint.setColor(0xff000000);
//				c.drawRect( launch_timer_posx + timer_margin,
//					launch_timer_posy + timer_height - self.launch_timer_height,
//					launch_timer_posx + timer_width - timer_margin,
//					launch_timer_posy + timer_height - height, paint);
			}
		}
		this.launch_timer_height = height;
/*
		self.screen.blit( self.launch_timer_text, self.launch_timer_text_rect);
*/
/*
		// Draw the score
		String text = "Score: "+("00000000"+self.game.score)[-8:];
		text = info_font.render( text, 1, (0,0,0));
		rect = text.get_rect();
		rect.left = self.score_pos;
		self.screen.blit( text, rect);

		// Draw the board timer
		time_remaining = (self.board_timeout+frames_per_sec-1)/frames_per_sec;
		text = time_remaining/60+":"+("00"+(time_remaining%60))[-2:];
		text = info_font.render( text, 1, (0,0,0));
		rect = text.get_rect();
		rect.left = self.board_timer_pos;
		self.screen.blit( text, rect);

		// Draw the lives counter
		right_edge = self.board_timer_pos - 32;
		for(int i=0; i < self.game.lives - 1; ++i) {
			rect = self.life_marble.get_rect();
			rect.centery = info_height / 2;
			rect.right = right_edge;
			self.screen.blit( self.life_marble, rect);
			right_edge -= rect.width + 4;
		}

		// Draw the live marbles
		int num_marbles = self.marbles.length;
		if( num_marbles > self.live_marbles_limit)
			num_marbles = self.live_marbles_limit;
		text = ""+num_marbles+"/"+self.live_marbles_limit;
		text = active_marbles_font.render( text, 1, (40,40,40));
		rect = text.get_rect();
		rect.left = self.pos[0] + 8;
		rect.centery = self.pos[1] - marble_size / 2;
		rect.width += 100;
		self.screen.set_clip( rect);
		self.screen.blit( self.background, (0,0));
		self.screen.set_clip();
		self.screen.blit( text, rect);
*/
		for( Tile[] row : self.tiles)
			for( Tile tile : row)
				tile.draw_back(b);

		for(int i=0; i < self.launch_queue.length; ++i)
			b.blit(Marble.marble_images[self.launch_queue[i]],
				self.posx + horiz_tiles * Tile.tile_size,
				self.posy + i * Marble.marble_size - Marble.marble_size);
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
	}
	
	public void paint(Blitter b)
	{
		// Draw the background
		self.draw_back(b);

		// Draw all of the marbles
		for(Marble marble : self.marbles)
			marble.draw(b);

		// Draw the foreground
		self.draw_fore(b);
	}

	public void set_tile( int x, int y, Tile tile) {
		self.tiles[y][x] = tile;
		tile.pos.left = self.posx + Tile.tile_size * x;
		tile.pos.top = self.posy + Tile.tile_size * y;

		//tile.x = x;
		//tile.y = y;

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
		self.launch_timer_height = -1;
	}

	public void set_board_timer(int seconds) {
		self.board_timer = seconds;
		self.board_timeout_start = seconds * frames_per_sec;
		self.board_timeout = self.board_timeout_start;
	}

	public void launch_marble() {
		self.marbles.insertElementAt( new Marble(
			gr, self.launch_queue[0],
			self.posx+Tile.tile_size*horiz_tiles+Marble.marble_size/2,
			self.posy-Marble.marble_size/2, 3), 0);
		for( int i=0; i < launch_queue.length-1; ++i)
			launch_queue[i] = launch_queue[i+1];
		self.launch_queue[launch_queue.length-1] =
			colors.charAt(gr.random.nextInt(colors.length()))-'0';
		self.launch_timeout = self.launch_timeout_start;
		self.launch_timer_height = -1;
	}

	public void affect_marble( Marble marble)
	{
		int cx = marble.pos.left + Marble.marble_size/2 - self.posx;
		int cy = marble.pos.top + Marble.marble_size/2 - self.posy;

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
		int tile_x = (posx - this.posx) / Tile.tile_size;
		int tile_y = (posy - this.posy) / Tile.tile_size;
		if( tile_x >= 0 && tile_x < horiz_tiles &&
			tile_y >= 0 && tile_y < vert_tiles) {
			return self.tiles[tile_y][tile_x];
		}
		return null;
	}

	public void downEvent(int posx, int posy)
	{
		downx = posx;
		downy = posy;
	}

	public void upEvent(int posx, int posy)
	{
		Tile downtile = whichTile(downx,downy);
		if(downtile == null) return;
		int downtile_x = (downx - this.posx) / Tile.tile_size;
		int downtile_y = (downy - this.posy) / Tile.tile_size;
		int tile_xr = downx-this.posx-(downtile_x*Tile.tile_size);
		int tile_yr = downx-this.posy-(downtile_y*Tile.tile_size);
		int dx = posx - downx;
		int dy = posy - downy;
		int dx2 = dx*dx;
		int dy2 = dy*dy;
		if(dx2 + dy2 <= Marble.marble_size * Marble.marble_size) {
			downtile.click(this, tile_xr, tile_yr,
				downtile_x, downtile_y);
		} else {
			int dir = (dx2>dy2)?(dx>0?1:3):(dy>0?2:0);
			downtile.flick(this, tile_xr, tile_yr,
				downtile_x, downtile_y, dir);
		}
	}

	public boolean _load(int level)
		throws IOException
	{
		BufferedReader f = new BufferedReader( new InputStreamReader(
			game.getResources().openRawResource( R.raw.all_boards)));

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
				if( line.substring(0,5).equals("name="))
					self.name = line.substring(5);
				else if( line.substring(0,11).equals("maxmarbles="))
					self.live_marbles_limit = Integer.parseInt(line.substring(11));
				else if( line.substring(0,12).equals("launchtimer="))
					self.set_launch_timer( Integer.parseInt(line.substring(12)));
				else if( line.substring(0,11).equals("boardtimer="))
					boardtimer = Integer.parseInt(line.substring(11));
				else if( line.substring(0,7).equals("colors=")) {
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
				} else if( line.substring(0,10).equals("stoplight=")) {
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
//				else if( type == '@') {
//					if( color == ' ') tile = new Buffer(gr, pathsint);
//					else tile = new Buffer(gr, pathsint, colorint);
//				}
				else if( type == ' ' ||
					(type >= '0' && type <= '8')) tile = new Tile(gr, pathsint);
//				else if( type == 'X') tile = new Shredder(gr, pathsint);
//				else if( type == '*') tile = new Replicator(gr, pathsint, colorint);
				else if( type == '^') {
					if( color == ' ') tile = new Director(gr, pathsint, 0);
//					else if( color == '>') tile = new Switch(gr, pathsint, 0, 1);
//					else if( color == 'v') tile = new Switch(gr, pathsint, 0, 2);
//					else if( color == '<') tile = new Switch(gr, pathsint, 0, 3);
				} else if( type == '>') {
					if( color == ' ') tile = new Director(gr, pathsint, 1);
//					else if( color == '^') tile = new Switch(gr, pathsint, 1, 0);
//					else if( color == 'v') tile = new Switch(gr, pathsint, 1, 2);
//					else if( color == '<') tile = new Switch(gr, pathsint, 1, 3);
				} else if( type == 'v') {
					if( color == ' ') tile = new Director(gr, pathsint, 2);
//					else if( color == '^') tile = new Switch(gr, pathsint, 2, 0);
//					else if( color == '>') tile = new Switch(gr, pathsint, 2, 1);
//					else if( color == '<') tile = new Switch(gr, pathsint, 2, 3);
				} else if( type == '<') {
					if( color == ' ') tile = new Director(gr,pathsint, 3);
//					else if( color == '^') tile = new Switch(gr, pathsint, 3, 0);
//					else if( color == '>') tile = new Switch(gr, pathsint, 3, 1);
//					else if( color == 'v') tile = new Switch(gr, pathsint, 3, 2);
				}
//				else if( type == '=') {
//					if( teleporter_names.indexOf(color) >= 0) {
//						Tile other = teleporters.get(teleporter_names.indexOf(color));
//						tile = new Teleporter( gr, pathsint, other);
//					} else {
//						tile = new Teleporter( gr, pathsint);
//						teleporters.addElement( tile);
//						teleporter_names = teleporter_names + color;
//					}
//				}

				this.set_tile( i, j, tile);

				if( type >= '0' && type <= '8') {
					int direction;
					if( color == '^') direction = 0;
					else if( color == '>') direction = 1;
					else if( color == 'v') direction = 2;
					else direction = 3;
					marbles.addElement(
						new Marble(gr, type-'0',
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

