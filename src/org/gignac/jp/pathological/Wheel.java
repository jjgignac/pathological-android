package org.gignac.jp.pathological;
import android.graphics.*;

class Wheel extends Tile {
	public int[] marbles;
	public int spinpos;
	private Wheel self;
	private boolean completed;
	private Bitmap moving_hole;
	private Bitmap moving_hole_dark;
	private Bitmap blank_wheel;
	private Bitmap blank_wheel_dark;
	private Bitmap wheel;
	private Bitmap wheel_dark;
	
	public Wheel( GameResources gr, int paths) {
		super(gr, paths); // Call base class intializer
		self = this;
		self.spinpos = 0;
		self.completed = false;
		self.marbles = new int[4];
		self.marbles[0] = -3;
		self.marbles[1] = -3;
		self.marbles[2] = -3;
		self.marbles[3] = -3;
	}

	@Override
	public boolean draw_back(Blitter b)
	{
		moving_hole = gr.cache(moving_hole, R.drawable.moving_hole);
		moving_hole_dark = gr.cache(moving_hole_dark, R.drawable.moving_hole);
		blank_wheel = gr.cache(blank_wheel, R.drawable.blank_wheel);
		blank_wheel_dark = gr.cache(blank_wheel_dark, R.drawable.blank_wheel_dark);
		wheel = gr.cache(wheel, R.drawable.wheel);
		wheel_dark = gr.cache(wheel_dark, R.drawable.wheel_dark);
		
		super.draw_back(b);

		if( self.spinpos != 0) {
			b.blit( completed ? blank_wheel_dark : blank_wheel,
				pos.left, pos.top, tile_size, tile_size);
			for(int i=0; i<4; ++i) {
				int holecenter_x = gr.holecenters_x[self.spinpos][i];
				int holecenter_y = gr.holecenters_y[self.spinpos][i];
				b.blit( completed ? moving_hole_dark : moving_hole,
					holecenter_x-Marble.marble_size/2+pos.left,
					holecenter_y-Marble.marble_size/2+pos.top);
			}
		} else {
			b.blit( completed ? wheel_dark : wheel, pos.left, pos.top);
		}

		for( int i=0; i < 4; ++i) {
			int color = self.marbles[i];
			if( color >= 0) {
				int holecenter_x = gr.holecenters_x[self.spinpos][i];
				int holecenter_y = gr.holecenters_y[self.spinpos][i];
				b.blit( gr.marble_images[color],
					holecenter_x-Marble.marble_size/2+pos.left,
					holecenter_y-Marble.marble_size/2+pos.top);
			}
		}

		return true;
	}

	@Override
	public void update( Board board) {
		if( self.spinpos > 0) {
			self.spinpos -= 1;
		}
	}

	@Override
	public void click( Board board, int posx, int posy, int tile_x, int tile_y)
	{
		// Ignore all clicks while rotating
		if( spinpos != 0) return;

		// First, make sure that no marbles are currently entering
		for( int i=0; i < 4; ++i)
			if( marbles[i] == -1 || marbles[i] == -2) return;

		// Start the wheel spinning
		spinpos = gr.wheel_steps - 1;
		gr.play_sound( gr.wheel_turn);

		// Reposition the marbles
		int t = self.marbles[0];
		self.marbles[0] = self.marbles[1];
		self.marbles[1] = self.marbles[2];
		self.marbles[2] = self.marbles[3];
		self.marbles[3] = t;
	}
	
	@Override
	public void flick( Board board, int posx, int posy,
		int tile_x, int tile_y, int dir)
	{
		// Ignore all flicks while rotating
		if( spinpos != 0) return;
		eject( dir, board, tile_x, tile_y);
	}

	private void eject(int i, Board board, int tile_x, int tile_y)
	{
		// Determine the neighboring tile
		Tile neighbor = board.tiles[ (tile_y + Marble.dy[i]) %
			Board.vert_tiles][ (tile_x + Marble.dx[i]) % Board.horiz_tiles];

		if ( marbles[i] < 0 ||
			// Disallow marbles to go off the top of the board
			(tile_y == 0 && i==0) ||

			// If there is no way out here, skip it
			((self.paths & (1 << i)) == 0) ||

			// If the neighbor is a wheel that is either turning
			// or has a marble already in the hole, disallow
			// the ejection
			(neighbor instanceof Wheel &&
			(((Wheel)neighbor).spinpos != 0 ||
			 ((Wheel)neighbor).marbles[i^2] != -3))
			)
			gr.play_sound( gr.incorrect);
		else {
			// If the neighbor is a wheel, apply a special lock
			if( neighbor instanceof Wheel)
				((Wheel)neighbor).marbles[i^2] = -2;
			else if( board.marbles.size() >= board.live_marbles_limit) {
				// Impose the live marbles limit
				gr.play_sound( gr.incorrect);
				return;
			}

			// Eject the marble
			board.marbles.addElement(
				new Marble( gr, self.marbles[i],
					gr.holecenters_x[0][i]+pos.left,
					gr.holecenters_y[0][i]+pos.top,
					i));
			self.marbles[i] = -3;
			gr.play_sound( gr.marble_release);
		}
	}

	@Override
	public void affect_marble(Board board, Marble marble, int rposx, int rposy)
	{
		// Watch for marbles entering
		if( rposx+Marble.marble_size/2 == gr.wheel_margin ||
			rposx-Marble.marble_size/2 == tile_size - gr.wheel_margin ||
			rposy+Marble.marble_size/2 == gr.wheel_margin ||
			rposy-Marble.marble_size/2 == tile_size - gr.wheel_margin) {
			if( spinpos != 0 || marbles[marble.direction^2] >= -1) {
				// Reject the marble
				marble.direction = marble.direction ^ 2;
				gr.play_sound( gr.ping);
			} else
				self.marbles[marble.direction^2] = -1;
		}

		for( int i=0; i<4; ++i) {
			if( rposx == gr.holecenters_x[0][i] &&
				rposy == gr.holecenters_y[0][i]) {
				// Accept the marble
				board.marbles.remove( marble);
				self.marbles[marble.direction^2] = marble.color;
				break;
			}
		}
	}

	public void complete(Board board) {
		// Complete the wheel
		for( int i=0; i<4; ++i) self.marbles[i] = -3;
		if( completed) board.game.increase_score( 10);
		else board.game.increase_score( 50);
		completed = true;
		gr.play_sound( gr.wheel_completed);
	}

	public boolean maybe_complete(Board board) {
		if( self.spinpos > 0) return false;

		// Is there a trigger?
		if(board.trigger != null &&
		   board.trigger.marbles != null) {
			// Compare against the trigger
			for( int i=0; i<4; ++i) {
				if( self.marbles[i] != board.trigger.marbles.charAt(i)-'0' &&
					self.marbles[i] != 8) return false;
			}
			self.complete( board);
			board.trigger.complete( board);
			return true;
		}

		// Do we have four the same color?
		int color = 8;
		for( int i=0; i<4; ++i) {
			int c = marbles[i];
			if( c < 0) return false;
			if( color==8) color=c;
			else if( c==8) c=color;
			else if( c != color) return false;
		}

		// Is there a stoplight?
		if((board.stoplight != null) &&
		   (board.stoplight.current < 3)) {
			// Compare against the stoplight
			if( color != 8 &&
				color != board.stoplight.marbles[board.stoplight.current])
				return false;
			else
				board.stoplight.complete( board);
		}

		complete( board);
		return true;
	}
}

