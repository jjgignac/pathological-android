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

class Switch extends TunnelTile
{
    private int curdir;
    private int otherdir;

    public Switch(Board board, int paths, int dir1, int dir2)
    {
        super(board, paths);
        curdir = dir1;
        otherdir = dir2;
    }

    private void switch_()
    {
        int t = curdir;
        curdir = otherdir;
        otherdir = t;
        board.gr.play_sound( GameResources.switched);
    }

    protected void draw_cap(Blitter surface)
    {
        final int i = (curdir*4+otherdir)*4/5;
        surface.blit( R.drawable.misc,
            38*i + (i<1?456:-38), i<1?280:318, 38, 38,
            left+27, top+27);
    }

    public void affect_marble(Board board, Marble marble, int x, int y)
    {
        if( x == tile_size/2 && y == tile_size/2) {
            marble.direction = curdir;
            switch_();
        }
    }
}
