package org.gignac.jp.pathological;

class Teleporter extends TunnelTile
{
	private int image;
	private Teleporter other;

	public Teleporter(Board board, int paths, Teleporter other)
	{
		super(board, paths);
		image = ((paths & 5) == 0) ? 418 : 456;
		if( other != null) connect( other);
		board.sc.cache(R.drawable.misc);
	}

	protected void draw_cap(Blitter surface) {
		surface.blit( R.drawable.misc, image, 318, 38, 38, left+27, top+27);
	}

	private void connect(Teleporter other) {
		this.other = other;
		other.other = this;
	}

	public void affect_marble(Board board, Marble marble, int x, int y)
	{
		if( x == tile_size/2 && y == tile_size/2) {
			marble.left = other.left + (tile_size-Marble.marble_size)/2;
			marble.top = other.top + (tile_size-Marble.marble_size)/2;
			board.gr.play_sound( GameResources.teleport);
		}
	}
}

