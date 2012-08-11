package org.gignac.jp.pathological;

class Filter extends TunnelTile
{
	private int color;
	private static int[] filter_images = {
		R.drawable.filter_0, R.drawable.filter_1,
		R.drawable.filter_2, R.drawable.filter_3,
		R.drawable.filter_4, R.drawable.filter_5,
		R.drawable.filter_6, R.drawable.filter_7
	};
	private static int[] filter_images_cb = {
		R.drawable.filter_0_cb, R.drawable.filter_1_cb,
		R.drawable.filter_2_cb, R.drawable.filter_3_cb,
		R.drawable.filter_4_cb, R.drawable.filter_5_cb,
		R.drawable.filter_6_cb, R.drawable.filter_7_cb
	};
	
	public Filter(Board board, int paths, int color) {
		super(board, paths);
		Sprite.cache(filter_images);
		Sprite.cache(filter_images_cb);
		this.color = color;
	}

	public void draw_fore(Blitter surface) {
		surface.blit( filter_images[color], pos.left, pos.top);
	}

	public void affect_marble(Board board, Marble marble, int x, int y)
	{
		GameResources gr = board.gr;
		if( x == tile_size/2 && y == tile_size/2) {
			// If the color is wrong, bounce the marble
			if( marble.color != color && marble.color != 8) {
				marble.direction = marble.direction ^ 2;
				gr.play_sound( gr.ping);
			} else {
				super.affect_marble( board, marble, x, y);
				gr.play_sound( gr.filter_admit);
			}
		}
	}
}
