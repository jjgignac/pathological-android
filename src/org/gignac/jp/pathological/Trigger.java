package org.gignac.jp.pathological;
import android.graphics.*;

class Trigger extends Tile
{
	public static final int trigger_time = 30; // 30 seconds
	public String marbles;
	private int countdown;
	private static Bitmap image;

	public Trigger(GameResources gr, String colors) {
		super(gr,0); // Call base class intializer
		this.marbles = null;
		this.setup( colors);
	}

	private void setup(String colors) {
		this.countdown = 0;
		this.marbles = ""+
			colors.charAt(gr.random.nextInt()%colors.length()) +
			colors.charAt(gr.random.nextInt()%colors.length()) +
			colors.charAt(gr.random.nextInt()%colors.length()) +
			colors.charAt(gr.random.nextInt()%colors.length());
	}

	@Override
	public void update(Board board) {
		if( countdown > 0) {
			countdown -= 1;
			if( countdown == 0) {
				setup( board.colors);
				gr.play_sound( gr.trigger_setup);
			}
		}
	}

	@Override
	public boolean draw_back(Blitter b) {
		super.draw_back(b);
		image = gr.cache(image, R.drawable.trigger);
		b.blit( image, pos.left, pos.top, tile_size, tile_size);
		if( marbles != null) {
			for(int i=0; i<4; ++i) {
				b.blit( gr.marble_images[marbles.charAt(i)-'0'],
					gr.holecenters_x[0][i]+pos.left-Marble.marble_size/2,
					gr.holecenters_y[0][i]+pos.top-Marble.marble_size/2,
					Marble.marble_size, Marble.marble_size);
			}
		}
		return true;
	}

	public void complete(Board board) {
		marbles = null;
		countdown = trigger_time * Board.frames_per_sec;
		board.game.increase_score( 50);
	}
}
