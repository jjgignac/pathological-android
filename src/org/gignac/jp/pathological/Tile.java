package org.gignac.jp.pathological;
import android.graphics.*;

class Tile
{
	public static final int tile_size = 92;
	public int paths;
	public Rect pos;  // Only left & top are maintained
	public boolean completed;
	protected GameResources gr;
	
	public Tile( GameResources gr, int paths, int cx, int cy) {
		this.gr = gr;
		this.paths = paths;
		this.pos = new Rect(cx - tile_size/2, cy - tile_size/2, 0, 0);
		this.completed = false;
	}
	
	public Tile( GameResources gr, int paths) {
		this(gr,paths,0,0);
	}

	public boolean draw_back(Canvas c) {
		pos.right = pos.left + tile_size;
		pos.bottom = pos.top + tile_size;
		if(paths > 0)
			c.drawBitmap( gr.plain_tiles[paths], null, pos, null);
		return true;
	}

	public void update( Board board) {}

	public boolean draw_fore( Canvas c) {
		return false;
	}

	public void click( Board board, int posx, int posy, int tile_x, int tile_y) {}

	public void affect_marble( Board board, Marble marble, int rposx, int rposy)
	{
		if(rposx == tile_size/2 && rposy == tile_size/2) {
			if((paths & (1 << marble.direction)) != 0) return;

			// Figure out the new direction
			int t = paths - (1 << (marble.direction^2));
			if(t == 1) marble.direction = 0;
			else if(t == 2) marble.direction = 1;
			else if(t == 4) marble.direction = 2;
			else if(t == 8) marble.direction = 3;
			else marble.direction = marble.direction ^ 2;
		}
	}
}
