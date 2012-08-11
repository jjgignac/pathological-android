package org.gignac.jp.pathological;

class Teleporter extends TunnelTile
{
	private int image;
	private Teleporter other;

	public Teleporter(Board board, int paths, Teleporter other)
	{
		super(board, paths);
		image = ((paths & 5) == 0) ?
			R.drawable.teleporter_h :
			R.drawable.teleporter_v;
		if( other != null) connect( other);
		Sprite.cache(image);
	}

	public void draw_fore(Blitter surface) {
		surface.blit( image, pos.left, pos.top);
	}

	private void connect(Teleporter other) {
		this.other = other;
		other.other = this;
	}

	public void affect_marble(Board board, Marble marble, int x, int y)
	{
		if( x == tile_size/2 && y == tile_size/2) {
			marble.pos.left = other.pos.left + (tile_size-Marble.marble_size)/2;
			marble.pos.top = other.pos.top + (tile_size-Marble.marble_size)/2;
			board.gr.play_sound( board.gr.teleport);
		}
	}
}

