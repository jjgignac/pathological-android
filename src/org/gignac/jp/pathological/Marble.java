package org.gignac.jp.pathological;

class Marble {
	public static final int marble_size = 28;
	public static final int marble_speed = 4;
	public static final int dx[] = {0,1,0,-1};
	public static final int dy[] = {-1,0,1,0};
	public int color;
	public int left, top;
	public int direction;
	
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
		b.blit( R.drawable.misc, 28*color, 357, 28, 28,
			left, top, marble_size, marble_size);
	}
}
