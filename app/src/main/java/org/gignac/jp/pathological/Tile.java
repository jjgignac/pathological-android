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

class Tile
{
    public static final int tile_size = 92;
    public final int paths;
    public int left, top;
    int tile_x;
    int tile_y;
    final Board board;

    public Tile(Board board, int paths) {
        this.board = board;
        this.paths = paths;
        this.left = - tile_size/2;
        this.top = - tile_size/2;
        board.sc.cache(R.drawable.misc);
    }

    public void setxy(int x, int y) {
        left = Tile.tile_size * x;
        top = Tile.tile_size * y;
        tile_x = x;
        tile_y = y;
    }

    public void draw_back(Blitter b) {
        if(paths > 0) {
            if( (paths & 1) != 0)
                b.blit( R.drawable.misc,
                    192, 387, 30, 24, left+31, top);
            if( (paths & 2) != 0)
                b.blit( R.drawable.misc,
                    415, 394, 24, 30, left+68, top+31);
            if( (paths & 4) != 0)
                b.blit( R.drawable.misc,
                    192, 387, 30, 24, left+31, top+68);
            if( (paths & 8) != 0)
                b.blit( R.drawable.misc,
                    415, 394, 24, 30, left, top+31);
            b.blit( R.drawable.misc,
                46*((paths+10)%11), 386+46*(paths/11), 46, 46,
                left+23, top+23);
            if( (paths & 8) > 0 && tile_x == 0) {
                b.blit( R.drawable.misc, 299, 386, 28, 46,
                    left-14,top+23);
            }
        }
    }

    public void update( Board board) {}

    public void draw_fore( Blitter b) {}

    @SuppressWarnings("UnusedParameters")
    public void click(Board board, int posx, int posy) {}

    @SuppressWarnings("UnusedParameters")
    public void flick(Board board, int posx, int posy, int dir) {}

    public void affect_marble( Board board, Marble marble, int rposx, int rposy)
    {
        if(rposx == tile_size/2 && rposy == tile_size/2) {
            if((paths & (1 << marble.direction)) != 0) return;

            // Figure out the new direction
            int t = paths - (1 << (marble.direction^2));
            if(t == 1) marble.direction = 0;
            else if(t == 2) marble.direction = 1;
            else if(t == 4) marble.direction = 2;
            else if(t == 8) marble.direction = 3;
            else marble.direction = marble.direction ^ 2;
        }
    }
}
