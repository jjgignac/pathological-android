package org.gignac.jp.pathological;

class Replicator extends TunnelTile
{
	private int count;
	private int[] pending_col;
	private int[] pending_dir;
	private int[] pending_count;
	private int[] pending_delay;
	private int npending;
	private static final int replicator_delay = 35;
	
	public Replicator(GameResources gr, int paths, int count)
	{
		super(gr, paths);
		this.count = count;
		Sprite.cache(R.drawable.replicator);
		pending_col = new int[10];
		pending_dir = new int[10];
		pending_count = new int[10];
		pending_delay = new int[10];
		npending = 0;
	}

	@Override
	public void draw_fore(Blitter surface) {
		surface.blit( R.drawable.replicator, pos.left, pos.top);
	}

	@Override
	public void update(Board board)
	{
		int i=0;
		while( i < npending) {
			pending_delay[i] -= 1;
			if( pending_delay[i] == 0) {
				pending_delay[i] = replicator_delay;

				// Make sure that the active marble limit isn't exceeded
				if( board.marbles.size() >= board.live_marbles_limit) {
					// Clear the pending list
					npending = 0;
					return;
				}

				// Add the new marble
				board.marbles.addElement( new Marble( gr,
					pending_col[i], pos.left + tile_size/2,
					pos.top + tile_size/2, pending_dir[i]));
				gr.play_sound( gr.replicator);

				pending_count[i] -= 1;
				if( pending_count[i] <= 0) {
					--npending;
					pending_col[i] = pending_col[npending];
					pending_dir[i] = pending_dir[npending];
					pending_count[i] = pending_count[npending];
					pending_delay[i] = pending_delay[npending];
					--i;
				}
			}
			++i;
		}
	}

	@Override
	public void affect_marble(Board board, Marble marble, int x, int y)
	{
		super.affect_marble( board, marble, x, y);
		if( x == tile_size/2 && y == tile_size/2) {
			// Make sure there's enough room in the arrays
			if(pending_col.length == npending) {
				int[] new_col = new int[npending * 2];
				int[] new_dir = new int[npending * 2];
				int[] new_count = new int[npending * 2];
				int[] new_delay = new int[npending * 2];
				System.arraycopy(pending_col,0,new_col,0,npending);
				System.arraycopy(pending_dir,0,new_dir,0,npending);
				System.arraycopy(pending_count,0,new_count,0,npending);
				System.arraycopy(pending_delay,0,new_delay,0,npending);
			}
			// Add the marble to the pending list
			pending_col[npending] = marble.color;
			pending_dir[npending] = marble.direction;
			pending_count[npending] = count - 1;
			pending_delay[npending] = replicator_delay;
			++npending;
			gr.play_sound( gr.replicator);
		}
	}
}
