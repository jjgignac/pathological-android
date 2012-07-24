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
		b.blit( gr.marble_images[color],
			pos.left, pos.top, marble_size, marble_size);
	}
}
