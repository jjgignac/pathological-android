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

class Teleporter extends TunnelTile
{
    private final int image;
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

