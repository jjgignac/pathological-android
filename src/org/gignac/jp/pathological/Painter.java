package org.gignac.jp.pathological;

class Painter extends TunnelTile
{
	private int color;
	private static int[] painter_images = {
		R.drawable.painter_0, R.drawable.painter_1,
		R.drawable.painter_2, R.drawable.painter_3,
		R.drawable.painter_4, R.drawable.painter_5,
		R.drawable.painter_6, R.drawable.painter_7
	};
	private static int[] painter_images_cb = {
		R.drawable.painter_0_cb, R.drawable.painter_1_cb,
		R.drawable.painter_2_cb, R.drawable.painter_3_cb,
		R.drawable.painter_4_cb, R.drawable.painter_5_cb,
		R.drawable.painter_6_cb, R.drawable.painter_7_cb
	};
	
	public Painter(GameResources gr, int paths, int color)
	{
		super(gr, paths);
		Sprite.cache(painter_images);
		Sprite.cache(painter_images_cb);
		this.color = color;
	}

	@Override
	public void draw_fore(Blitter surface) {
		surface.blit( painter_images[color], pos.left, pos.top);
	}

	@Override
	public void affect_marble(Board board, Marble marble, int x, int y)
	{
		super.affect_marble( board, marble, x, y);
		if( x == tile_size/2 && y == tile_size/2) {
			if( marble.color != color) {
				// Change the color
				marble.color = color;
				gr.play_sound( gr.change_color);
			}
		}
	}
}
