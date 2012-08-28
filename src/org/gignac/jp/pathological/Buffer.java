package org.gignac.jp.pathological;

class Buffer extends TunnelTile
{
	private int marble;
	private Marble entering;

	public Buffer(Board board, int paths, int color) {
		super(board, paths);
		marble = color;
		entering = null;
		board.sc.cache(R.drawable.buffer);
		board.sc.cache(R.drawable.buffer_top);
	}

	public void draw_back(Blitter surface) {
		super.draw_back(surface);
		int color = marble;
		if( color >= 0) {
			surface.blit( Marble.marble_images[color],
				pos.left + (tile_size-Marble.marble_size)/2,
				pos.top + (tile_size-Marble.marble_size)/2);
		} else {
			surface.blit( R.drawable.buffer, pos.left, pos.top);
		}
	}

	protected void draw_cap(Blitter surface) {
		surface.blit( R.drawable.buffer_top, pos.left, pos.top);
	}

	public void affect_marble(Board board, Marble marble, int x, int y)
	{
		GameResources gr = board.gr;

		// Watch for marbles entering
		if((x+Marble.marble_size == tile_size/2 && marble.direction == 1) ||
		   (x-Marble.marble_size == tile_size/2 && marble.direction == 3) ||
		   (y+Marble.marble_size == tile_size/2 && marble.direction == 2) ||
		   (y-Marble.marble_size == tile_size/2 && marble.direction == 0))
		{
			if( entering != null) {
				// Bump the marble that is currently entering
				Marble newmarble = entering;
				newmarble.left = pos.left + (tile_size-Marble.marble_size)/2;
				newmarble.top = pos.top + (tile_size-Marble.marble_size)/2;
				newmarble.direction = marble.direction;

				gr.play_sound( gr.ping);

				// Let the base class affect the marble
				super.affect_marble(board, newmarble,
					tile_size/2, tile_size/2);
			} else if( this.marble >= 0) {
				// Bump the marble that is currently caught
				Marble newmarble = new Marble(gr, this.marble,
					pos.left + tile_size/2, pos.top + tile_size/2,
					marble.direction);

				board.activateMarble( newmarble);

				gr.play_sound( gr.ping);

				// Let the base class affect the marble
				super.affect_marble(board, newmarble,
					tile_size/2, tile_size/2);

				this.marble = -1;

				invalidate();
			}

			// Remember which marble is on its way in
			entering = marble;
		} else if( x == tile_size/2 && y == tile_size/2) {
			// Catch this marble
			this.marble = marble.color;
			board.deactivateMarble( marble);
			entering = null;

			invalidate();
		}
	}
}
