package org.gignac.jp.pathological;

class Shredder extends TunnelTile
{
	public Shredder(GameResources gr, int paths) {
		super(gr, paths);
		Sprite.cache(R.drawable.shredder);
	}

	public void draw_fore(Blitter surface) {
		surface.blit( R.drawable.shredder, pos.left, pos.top);
	}

	public void affect_marble(Board board, Marble marble, int x, int y)
	{
		if( x == tile_size/2 && y == tile_size/2) {
			board.deactivateMarble( marble);
			gr.play_sound( gr.shredder);
		}
	}
}

