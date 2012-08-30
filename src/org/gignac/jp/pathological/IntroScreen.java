package org.gignac.jp.pathological;

public class IntroScreen
{
	private static final int[] pyramid = {
		R.drawable.marble_2, R.drawable.marble_6,
		R.drawable.marble_3, R.drawable.marble_4,
		R.drawable.marble_4, R.drawable.marble_3,
		R.drawable.marble_2, R.drawable.marble_6,
		R.drawable.marble_4, R.drawable.marble_6
	};

	public static void setup(SpriteCache sc) {
		sc.cache(R.drawable.intro_bg);
		sc.cache(R.drawable.pathological_logo);
		sc.cache(R.drawable.pipe_br);
		sc.cache(R.drawable.pipe_v);
		sc.cache(R.drawable.pipe_d);
		sc.cache(R.drawable.wheel);
		sc.cache(R.drawable.wheel_dark);
		for(int i : pyramid) sc.cache(i);
	}

	public static void draw_back(GameResources gr, Blitter b)
	{
		b.blit(R.drawable.intro_bg,0,0,b.getWidth(),b.getHeight());
	}

	public static void draw_fore(GameResources gr, Blitter b)
	{
		int logo_w = 343;
		int logo_h = 55;
		b.blit(R.drawable.pathological_logo,
			(b.getWidth()-logo_w)/2,
			Marble.marble_size);

		int pipe_w = 30;
		int pipe_x = (Tile.tile_size-30)/2;
		int pipe_y = b.getHeight()-Tile.tile_size*5/2;
		
		b.blit(R.drawable.pipe_v,pipe_x,0,pipe_w,pipe_y+2);
		b.blit(R.drawable.pipe_d,pipe_x,pipe_y);
		
		pipe_x = b.getWidth() - pipe_w*6/5;
		pipe_y = b.getHeight() - pipe_w;
		int pipe_br_w = 90;
		b.blit(R.drawable.pipe_v,pipe_x,0,pipe_w,pipe_y+2);
		b.blit(R.drawable.pipe_br,pipe_x+pipe_w-pipe_br_w,pipe_y);

		b.blit(R.drawable.wheel,0,b.getHeight()-2*Tile.tile_size);
		b.blit(R.drawable.marble_2,
			gr.holecenters_x[0][0]-Marble.marble_size/2,
			b.getHeight()-2*Tile.tile_size +
				gr.holecenters_y[0][0]-Marble.marble_size/2);
		b.blit(R.drawable.wheel_dark,0,b.getHeight()-Tile.tile_size);
		b.blit(R.drawable.wheel,Tile.tile_size*9/4,
			b.getHeight()-Tile.tile_size);
		
		// Pyramid
		int p = 0;
		int x = b.getWidth()-Tile.tile_size*4;
		int y = b.getHeight()-Marble.marble_size;
		int dy = (int)Math.round(Marble.marble_size*Math.sqrt(3.0)/2.0);
		for(int j=0; j<4; ++j)
			for(int i=0; i<4-j; ++i,++p)
				b.blit(pyramid[p],
					x+(i+i+j)*Marble.marble_size/2,
					y-dy*j);

		b.blit(R.drawable.marble_3,x+Marble.marble_size*5,y);
	}
}
