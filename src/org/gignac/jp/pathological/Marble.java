package org.gignac.jp.pathological;

class Marble {
	public static final int marble_size = 28;
	public static final int marble_speed = 4;
	public static final int dx[] = {0,1,0,-1};
	public static final int dy[] = {-1,0,1,0};
	public int color;
	public int left, top;
	public int direction;
	public static final int[] marble_images = {
		R.drawable.marble_0, R.drawable.marble_1,
		R.drawable.marble_2, R.drawable.marble_3,
		R.drawable.marble_4, R.drawable.marble_5,
		R.drawable.marble_6, R.drawable.marble_7,
		R.drawable.marble_8
	};
	public static final int[] marble_images_cb = {
		R.drawable.marble_0_cb, R.drawable.marble_1_cb,
		R.drawable.marble_2_cb, R.drawable.marble_3_cb,
		R.drawable.marble_4_cb, R.drawable.marble_5_cb,
		R.drawable.marble_6_cb, R.drawable.marble_7_cb,
		R.drawable.marble_8_cb
	};
	
	public Marble(GameResources gr, int color, int cx, int cy, int direction) {
		this.color = color;
		this.left = cx-marble_size/2;
		this.top = cy-marble_size/2;
		this.direction = direction;
	}

	public void update(Board board) {
		left += marble_speed * dx[direction];
		top += marble_speed * dy[direction];
		board.affect_marble(this);
	}

	public void draw(Blitter b) {
		b.blit( marble_images[color],
			left, top, marble_size, marble_size);
	}
}
