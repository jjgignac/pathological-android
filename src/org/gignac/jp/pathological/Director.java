package org.gignac.jp.pathological;

class Director extends TunnelTile
{
	private int direction;
	private static final int[] director_images = {
		R.drawable.director_0, R.drawable.director_1,
		R.drawable.director_2, R.drawable.director_3
	};

	public Director(Board board, int paths, int direction)
	{
		super(board, paths);
		this.direction = direction;
		board.sc.cache(director_images);
	}

	@Override
	public void draw_fore(Blitter surface) {
		surface.blit( director_images[direction], pos.left, pos.top);
	}

	@Override
	public void affect_marble(Board board, Marble marble, int x, int y) {
		if(x == tile_size/2 && y == tile_size/2) {
			marble.direction = direction;
			board.gr.play_sound( board.gr.direct_marble);
		}
	}
}

