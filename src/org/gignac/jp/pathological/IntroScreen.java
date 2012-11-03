package org.gignac.jp.pathological;

public class IntroScreen
{
	private static final int[] pyramid = {
		2, 6, 3, 4, 4, 3, 2, 6, 4, 6
	};

	public static void setup(SpriteCache sc) {
		sc.cache(R.drawable.misc);
	}

	public static void draw_back(GameResources gr, Blitter b)
	{
		b.blit(R.drawable.misc,0,768,512,256,0,0,b.getWidth(),b.getHeight()+1);
	}

	public static void draw_fore(GameResources gr, Blitter b)
	{
	    // Draw the logo
		b.blit(R.drawable.misc, 0, 93, 342, 55,
			(b.getWidth()-342)/2, Marble.marble_size);

		int pipe_w = 30;
		int pipe_x = (Tile.tile_size-30)/2;
		int pipe_y = b.getHeight()-227;

		// Draw the pipe on the left
		b.blit(R.drawable.misc,391,663,30,1,pipe_x,0,pipe_w,pipe_y+2);
		b.blit(R.drawable.misc,391,665,30,10,pipe_x,pipe_y);

		// Draw the pipe on the right		
		pipe_x = b.getWidth() - pipe_w*6/5;
		pipe_y = b.getHeight() - pipe_w;
		int pipe_br_w = 90;
		b.blit(R.drawable.misc,391,663,30,1,pipe_x,0,pipe_w,pipe_y+2);
		b.blit(R.drawable.misc,92,662,90,30,pipe_x+pipe_w-pipe_br_w,pipe_y);

		// Top-left wheel
		b.blit(R.drawable.misc,277,1,90,90,0,b.getHeight()-180);

		// Marble in top-left wheel
		b.blit(R.drawable.misc,56,357,28,28,
			gr.holecenters_x[0][0]-Marble.marble_size/2,
			b.getHeight()-181 +
				gr.holecenters_y[0][0]-Marble.marble_size/2);

		// Bottom-left wheel
		b.blit(R.drawable.misc,185,1,90,90,0,b.getHeight()-90);

		// Other bottom wheels
		b.blit(R.drawable.misc,277,1,90,90,200,b.getHeight()-90);
		b.blit(R.drawable.misc,422,662,90,90,290,b.getHeight()-90);

		// Pyramid
		int p = 0;
		int x = b.getWidth()-Tile.tile_size*4;
		int y = b.getHeight()-Marble.marble_size;
		int dy = (int)Math.round(Marble.marble_size*Math.sqrt(3.0)/2.0);
		for(int j=0; j<4; ++j)
			for(int i=0; i<4-j; ++i,++p)
				b.blit(R.drawable.misc,
				    28*pyramid[p], 357, 28, 28,
					x+(i+i+j)*Marble.marble_size/2,
					y-dy*j);

		// Stationary green marble
		b.blit(R.drawable.misc,84,357,28,28,x+Marble.marble_size*5,y);

		// Moving blue marble
        b.blit(R.drawable.misc,93,693,49,28,x+Marble.marble_size*8,y);
	}
}
