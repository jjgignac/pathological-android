package org.gignac.jp.pathological;
import android.graphics.*;

class Stoplight extends Tile {
	public int current;
	public int[] marbles;
	private static Bitmap image;
	private static final int stoplight_marble_size = 28;

	public Stoplight(GameResources gr, String colors) {
		super(gr, 0); // Call base class intializer
		marbles = new int[3];
		for(int i=0; i<3; ++i)
			marbles[i] = colors.charAt(i)-'0';
		current = 0;
	}

	@Override
	public boolean draw_back( Canvas c) {
		if(drawn) return false;
		super.draw_back(c);
		image = gr.cache(image, R.drawable.stoplight);
		c.drawBitmap( image, null, pos, null);
		for(int i=current; i < 3; ++i) {
			gr.blit( c, gr.marble_images[marbles[i]],
				pos.left + Tile.tile_size/2 - 14,
				pos.top + 3 + (29*i),
				stoplight_marble_size,
				stoplight_marble_size);
		}
		return true;
	}

	public void complete( Board board) {
		for( int i=0; i<3; ++i) {
			if( marbles[i] >= 0) {
				marbles[i] = -1;
				break;
			}
		}
		current += 1;
		drawn = false;
		board.game.increase_score( 20);
	}
}
