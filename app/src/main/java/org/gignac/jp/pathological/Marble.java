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

class Marble {
    public static final int marble_size = 28;
    public static final int marble_speed = 4;
    public static final int dx[] = {0,1,0,-1};
    public static final int dy[] = {-1,0,1,0};
    public int color;
    public int left, top;
    public int direction;

    public Marble(int color, int cx, int cy, int direction) {
        this.color = color;
        this.left = cx-marble_size/2;
        this.top = cy-marble_size/2;
        this.direction = direction;
    }

    public void update(Board board) {
        left += marble_speed * dx[direction];
        top += marble_speed * dy[direction];
        board.affect_marble(this);
    }

    public void draw(Blitter b) {
        b.blit( R.drawable.misc, 28*color, 357, 28, 28,
            left, top, marble_size, marble_size);
    }
}
