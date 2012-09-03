package org.gignac.jp.pathological;

class Shredder extends TunnelTile
{
	public Shredder(Board board, int paths) {
		super(board, paths);
		board.sc.cache(R.drawable.shredder);
	}

	protected void draw_cap(Blitter surface) {
		surface.blit( R.drawable.shredder, left, top);
	}

	public void affect_marble(Board board, Marble marble, int x, int y)
	{
		if( x == tile_size/2 && y == tile_size/2) {
			board.deactivateMarble( marble);
			board.gr.play_sound( GameResources.shredder);
		}
	}
}

