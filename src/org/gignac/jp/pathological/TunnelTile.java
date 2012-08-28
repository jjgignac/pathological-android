package org.gignac.jp.pathological;
import android.graphics.*;

public abstract class TunnelTile extends Tile
{
	private static final int[] tunnel_images = {
		R.drawable.tunnel_0, R.drawable.tunnel_1, R.drawable.tunnel_2,
		R.drawable.tunnel_3, R.drawable.tunnel_4, R.drawable.tunnel_5,
		R.drawable.tunnel_6, R.drawable.tunnel_7, R.drawable.tunnel_8,
		R.drawable.tunnel_9, R.drawable.tunnel_10, R.drawable.tunnel_11,
		R.drawable.tunnel_12, R.drawable.tunnel_13, R.drawable.tunnel_14,
		R.drawable.tunnel_15
	};
	private static final int tunnel_size = 58;
	private long uniq;
	private Bitmap fore;
	private BitmapBlitter b;
	private boolean fore_dirty;

	public TunnelTile( Board board, int paths) {
		super(board,paths);
	}

	@Override
	public void setxy(int x, int y) {
		super.setxy(x,y);
		board.sc.cache(tunnel_images[paths]);
		uniq = 0x700000000l+(left<<16)+top;
		fore = Bitmap.createBitmap(
			tunnel_size,tunnel_size,Bitmap.Config.ARGB_8888);
		b = new BitmapBlitter(board.sc,fore);
		final int offset = (tile_size - tunnel_size)/2;
		b.blit(tunnel_images[paths],-offset,-offset);
		b.transform(1f,-left-offset,-top-offset);
		fore_dirty = true;
	}

	@Override
	public final void draw_fore( Blitter b) {
		final int offset = (tile_size - tunnel_size)/2;
		if( fore_dirty) {
			draw_cap(this.b);
			board.sc.cache(uniq,fore);
			fore_dirty = false;
		}
		b.blit(uniq, left+offset, top+offset);
	}

	protected abstract void draw_cap(Blitter b);

	public void invalidate_fore() {
		fore_dirty = true;
	}
}
