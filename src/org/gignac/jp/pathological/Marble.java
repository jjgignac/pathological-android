package org.gignac.jp.pathological;
import android.graphics.*;

class Marble {
	public static final int marble_size = 28;
	public static final int marble_speed = 2;
	public static final int dx[] = {0,1,0,-1};
	public static final int dy[] = {-1,0,1,0};
	public int color;
	public Rect pos;  // Only left & top are kept up-to-date
	public int direction;
	private GameResources gr;
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
		this.gr = gr;
		this.color = color;
		this.pos = new Rect(
			cx-marble_size/2, cy-marble_size/2, 0, 0);
		this.direction = direction;
	}

	public void update(Board board) {
		pos.left += marble_speed * dx[direction];
		pos.top += marble_speed * dy[direction];
		board.affect_marble(this);
	}

	public void draw(Blitter b) {
		pos.right = pos.left + marble_size;
		pos.bottom = pos.top + marble_size;
		b.blit( marble_images[color],
			pos.left, pos.top, marble_size, marble_size);
	}
}
