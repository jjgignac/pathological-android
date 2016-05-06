package org.gignac.jp.pathological;
import android.graphics.*;

public abstract class TunnelTile extends Tile
{
    private static final int tunnel_size = 58;
    private long uniq;
    private Bitmap fore;
    private BitmapBlitter b;
    private boolean fore_dirty;

    TunnelTile(Board board, int paths) {
        super(board,paths);
    }

    @Override
    public void setxy(int x, int y) {
        super.setxy(x,y);
        board.sc.cache(R.drawable.misc);
        uniq = 0x700000000L+(left<<16)+top;
        b = new BitmapBlitter(board.sc,tunnel_size,tunnel_size);
        fore = b.getDest();
        final int offset = (tile_size - tunnel_size)/2;
        b.blit( R.drawable.misc, 446, (paths&1)==0?93:113,
            56, 9, 18-offset, 18-offset);
        b.blit( R.drawable.misc, 503, (paths&2)==0?57:171,
            9, 56, 65-offset, 18-offset);
        b.blit( R.drawable.misc, 446, (paths&4)==0?103:123,
            56, 9, 18-offset, 65-offset);
        b.blit( R.drawable.misc, 503, (paths&8)==0?0:114,
            9, 56, 18-offset, 18-offset);
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

    void invalidate_fore() {
        fore_dirty = true;
    }
}
