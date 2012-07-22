package org.gignac.jp.pathological;
import android.graphics.*;

class Wheel extends Tile {
	public int[] marbles;
	public int spinpos;
	private Wheel self;

	public Wheel(int paths, int cx, int cy) {
		super(paths, cx, cy); // Call base class intializer
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
	public boolean draw_back(Bitmap surface) {
		if( self.drawn) return false;

		super.draw_back(surface);

		if( self.spinpos != 0) {
			surface.blit( self.blank_images[self.completed], self.rect.topleft)
			for(int i=0; i<4; ++i) {
				holecenter = holecenters[self.spinpos][i];
				surface.blit( self.moving_holes[self.completed],
					(holecenter[0]-Marble.marble_size/2+self.rect.left,
					holecenter[1]-Marble.marble_size/2+self.rect.top));
			}
		} else {
			surface.blit( self.images[self.completed], self.rect.topleft);
		}

		for( int i=0; i < 4; ++i) {
			color = self.marbles[i];
			if( color >= 0) {
				holecenter = holecenters[self.spinpos][i];
				surface.blit( Marble.images[color],
					(holecenter[0]-Marble.marble_size/2+self.rect.left,
					holecenter[1]-Marble.marble_size/2+self.rect.top));
			}
		}

		return true;
	}

	@Override
	public void update( Board board) {
		if( self.spinpos > 0) {
			self.spinpos -= 1;
			self.drawn = 0;
		}
	}

	@Override
	public void click( Board board, int posx, int posy, int tile_x, int tile_y)
	{
		// Ignore all clicks while rotating
		if( self.spinpos != 0) return

		b1, b2, b3 = pygame.mouse.get_pressed();
		if( b3) {
			// First, make sure that no marbles are currently entering
			for( i in self.marbles)
				if( i == -1 || i == -2) return;

			// Start the wheel spinning
			self.spinpos = wheel_steps - 1;
			play_sound( wheel_turn);

			// Reposition the marbles
			t = self.marbles[0];
			self.marbles[0] = self.marbles[1];
			self.marbles[1] = self.marbles[2];
			self.marbles[2] = self.marbles[3];
			self.marbles[3] = t;

			self.drawn = 0;

		} else if( b1) {
			// Determine which hole is being clicked
			for( int i=0; i<4; ++i) {
				// If there is no marble here, skip it
				if( self.marbles[i] < 0) continue;

				holecenter = holecenters[0][i];
				rect = pygame.Rect( 0, 0, marble_size, marble_size);
				rect.center = holecenter;
				if( rect.collidepoint( posx, posy)) {

					// Determine the neighboring tile
					Tile neighbor = board.tiles[ (tile_y + dirs[i][1]) %
						vert_tiles][ (tile_x + dirs[i][0]) % horiz_tiles];

					if (
						// Disallow marbles to go off the top of the board
						(tile_y == 0 && i==0) ||

						// If there is no way out here, skip it
						((self.paths & (1 << i)) == 0) ||

						// If the neighbor is a wheel that is either turning
						// or has a marble already in the hole, disallow
						// the ejection
						(neighbor instanceof Wheel &&
						(neighbor.spinpos ||
						neighbor.marbles[i^2] != -3))
						)
						play_sound( incorrect);
					else {
						// If the neighbor is a wheel, apply a special lock
						if( neighbor instanceof Wheel)
							neighbor.marbles[i^2] = -2;
						else if( board.marbles.length >= board.live_marbles_limit) {
							// Impose the live marbles limit
							play_sound( incorrect);
							break;
						}

						// Eject the marble
						board.marbles.append(
							Marble( self.marbles[i],
							(holecenter[0]+self.rect.left,
							holecenter[1]+self.rect.top),
							i));
						self.marbles[i] = -3;
						play_sound( marble_release);
						self.drawn = 0;
					}

					break;
				}
			}
		}
	}

	@Override
	public void affect_marble(Board board, Marble marble, int rposx, int rposy)
	{
		// Watch for marbles entering
		if( rposx+Marble.marble_size/2 == wheel_margin ||
			rposx-Marble.marble_size/2 == tile_size - wheel_margin ||
			rposy+Marble.marble_size/2 == wheel_margin ||
			rposy-Marble.marble_size/2 == tile_size - wheel_margin) {
			if( self.spinpos || self.marbles[marble.direction^2] >= -1) {
				// Reject the marble
				marble.direction = marble.direction ^ 2;
				play_sound( ping);
			} else
				self.marbles[marble.direction^2] = -1;
		}

		for( holecenter in holecenters[0]) {
			if( rpos == holecenter) {
				// Accept the marble
				board.marbles.remove( marble);
				self.marbles[marble.direction^2] = marble.color;

				self.drawn = 0;

				break;
			}
		}
	}

	public void complete(Board board) {
		// Complete the wheel
		for( int i=0; i<4; ++i) self.marbles[i] = -3;
		if( self.completed) board.game.increase_score( 10);
		else board.game.increase_score( 50);
		self.completed = 1;
		play_sound( wheel_completed);
		self.drawn = 0;
	}

	public boolean maybe_complete(Board board) {
		if( self.spinpos > 0) return false;

		// Is there a trigger?
		if((board.trigger != null) &&
		   (board.trigger.marbles != None)) {
			// Compare against the trigger
			for( int i=0; i<4; ++i) {
				if( self.marbles[i] != board.trigger.marbles[i] &&
					self.marbles[i] != 8) return false;
			}
			self.complete( board);
			board.trigger.complete( board);
			return true;
		}

		// Do we have four the same color?
		color = 8;
		for( c in self.marbles) {
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

		self.complete( board);
		return true;
	}
}

