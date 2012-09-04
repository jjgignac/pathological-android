package org.gignac.jp.pathological;

class Filter extends TunnelTile
{
	private int color;
	
	public Filter(Board board, int paths, int color) {
		super(board, paths);
		board.sc.cache(R.drawable.misc);
		this.color = color;
	}

	protected void draw_cap(Blitter surface) {
		surface.blit( R.drawable.misc,
            152+color*38, 204, 38, 38, left+27, top+27);
	}

	public void affect_marble(Board board, Marble marble, int x, int y)
	{
		GameResources gr = board.gr;
		if( x == tile_size/2 && y == tile_size/2) {
			// If the color is wrong, bounce the marble
			if( marble.color != color && marble.color != 8) {
				marble.direction = marble.direction ^ 2;
				gr.play_sound( GameResources.ping);
			} else {
				super.affect_marble( board, marble, x, y);
				gr.play_sound( GameResources.filter_admit);
			}
		}
	}
}
