package org.gignac.jp.pathological;

public class TunnelTile extends Tile
{
	private static final int[] tunnel_images = {
		R.drawable.tunnel_0, R.drawable.tunnel_1, R.drawable.tunnel_2,
		R.drawable.tunnel_3, R.drawable.tunnel_4, R.drawable.tunnel_5,
		R.drawable.tunnel_6, R.drawable.tunnel_7, R.drawable.tunnel_8,
		R.drawable.tunnel_9, R.drawable.tunnel_10, R.drawable.tunnel_11,
		R.drawable.tunnel_12, R.drawable.tunnel_13, R.drawable.tunnel_14,
		R.drawable.tunnel_15
	};

	public TunnelTile( Board board, int paths) {
		super(board,paths);
		Sprite.cache(tunnel_images);
	}

	@Override
	public void draw_back( Blitter b) {
		super.draw_back(b);
		b.blit(tunnel_images[paths], pos.left, pos.top);
	}
}
