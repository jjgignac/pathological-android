/*
 * Copyright (C) 2016  John-Paul Gignac
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gignac.jp.pathological;
import android.graphics.*;

public abstract class TunnelTile extends Tile
{
    private static final int tunnel_size = 58;
    private long uniq;
    private Bitmap fore;
    private BitmapBlitter b;

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
        b.pushTransform(1f,-left-offset,-top-offset);
    }

    @Override
    public final void draw_fore( Blitter b) {
        final int offset = (tile_size - tunnel_size)/2;
        draw_cap(this.b);
        board.sc.cache(uniq,fore);
        b.blit(uniq, left+offset, top+offset);
    }

    protected abstract void draw_cap(Blitter b);
}
