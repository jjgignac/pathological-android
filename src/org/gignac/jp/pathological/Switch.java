package org.gignac.jp.pathological;

class Switch extends TunnelTile
{
	private int curdir;
	private int otherdir;
	private static int[] switch_images = {
		R.drawable.switch_01, R.drawable.switch_01,
		R.drawable.switch_02, R.drawable.switch_03,
		R.drawable.switch_10, R.drawable.switch_01,
		R.drawable.switch_12, R.drawable.switch_13,
		R.drawable.switch_20, R.drawable.switch_21,
		R.drawable.switch_01, R.drawable.switch_23,
		R.drawable.switch_30, R.drawable.switch_31,
		R.drawable.switch_32
	};

	public Switch(Board board, int paths, int dir1, int dir2)
	{
		super(board, paths);
		curdir = dir1;
		otherdir = dir2;
		board.sc.cache(switch_images);
	}

	private void switch_()
	{
		int t = curdir;
		curdir = otherdir;
		otherdir = t;
		board.gr.play_sound( board.gr.switched);
	}

	public void draw_fore(Blitter surface)
	{
		surface.blit( switch_images[curdir*4+otherdir],
			pos.left, pos.top);
	}

	public void affect_marble(Board board, Marble marble, int x, int y)
	{
		if( x == tile_size/2 && y == tile_size/2) {
			marble.direction = curdir;
			switch_();
		}
	}
}
