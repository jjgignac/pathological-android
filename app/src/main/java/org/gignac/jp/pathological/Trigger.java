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

class Trigger extends Tile
{
    private static final int trigger_time = 30; // 30 seconds
    public String marbles;
    private int countdown;

    public Trigger(Board board, String colors) {
        super(board,0); // Call base class intializer
        this.marbles = null;
        this.setup( colors);
        board.sc.cache(R.drawable.misc);
    }

    private void setup(String colors) {
        GameResources gr = board.gr;
        this.countdown = 0;
        this.marbles = ""+
            colors.charAt(gr.random.nextInt(colors.length())) +
            colors.charAt(gr.random.nextInt(colors.length())) +
            colors.charAt(gr.random.nextInt(colors.length())) +
            colors.charAt(gr.random.nextInt(colors.length()));
    }

    @Override
    public void update(Board board) {
        GameResources gr = board.gr;
        if( countdown > 0) {
            countdown -= 1;
            if( countdown == 0) {
                setup( board.colors);
                gr.play_sound( GameResources.trigger_setup);
            }
        }
    }

    @Override
    public void draw_back(Blitter b) {
        super.draw_back(b);
        GameResources gr = board.gr;
        b.blit( R.drawable.misc, 369, 0, 92, 92, left, top);
        if( marbles != null) {
            for(int i=0; i<4; ++i) {
                b.blit( R.drawable.misc,
                    28*(marbles.charAt(i)-'0'), 357, 28, 28,
                    gr.holecenters_x[0][i]+left-Marble.marble_size/2,
                    gr.holecenters_y[0][i]+top-Marble.marble_size/2);
            }
        }
    }

    public void complete(@SuppressWarnings("UnusedParameters") Board board) {
        marbles = null;
        countdown = trigger_time * GameActivity.frames_per_sec;
    }
}
