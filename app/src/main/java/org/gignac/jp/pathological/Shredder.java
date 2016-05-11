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

class Shredder extends TunnelTile
{
    public Shredder(Board board, int paths) {
        super(board, paths);
        board.sc.cache(R.drawable.misc);
    }

    protected void draw_cap(Blitter surface) {
        surface.blit( R.drawable.misc, 418, 280, 38, 38, left+27, top+27);
    }

    public void affect_marble(Board board, Marble marble, int x, int y)
    {
        if( x == tile_size/2 && y == tile_size/2) {
            board.deactivateMarble( marble);
            board.gr.play_sound( GameResources.shredder);
        }
    }
}

