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

class IntroScreen
{
    private static final int[] pyramid = {
        2, 6, 3, 4, 4, 3, 2, 6, 4, 6
    };

    public static void setup(SpriteCache sc) {
        sc.cache(R.drawable.misc);
    }

    public static void draw_back(Blitter b)
    {
        b.blit(R.drawable.misc,0,768,512,256,0,0,b.getWidth(),b.getHeight()+1);
    }

    public static void draw_fore(GameResources gr, Blitter b)
    {
        // Draw the logo
        b.blit(R.drawable.misc, 0, 93, 342, 55,
            (b.getWidth()-342)/2, Marble.marble_size);

        // Draw the pipe on the right
        b.blit(R.drawable.misc, 92, 612, 96, 80, b.getWidth() - 96, b.getHeight() - 80);

        // Top-left wheel
        b.blit(R.drawable.misc,277,1,90,90,0,b.getHeight()-180);

        // Marble in top-left wheel
        b.blit(R.drawable.misc,56,357,28,28,
            gr.holecenters_x[0][0]-Marble.marble_size/2,
            b.getHeight()-181 +
                gr.holecenters_y[0][0]-Marble.marble_size/2);

        // Bottom-left wheel
        b.blit(R.drawable.misc,185,1,90,90,0,b.getHeight()-90);

        // Other bottom wheels
        b.blit(R.drawable.misc,277,1,90,90,200,b.getHeight()-90);
        b.blit(R.drawable.misc,422,662,90,90,290,b.getHeight()-90);

        // Pyramid
        int p = 0;
        int x = b.getWidth()-Tile.tile_size*4;
        int y = b.getHeight()-Marble.marble_size;
        int dy = (int)Math.round(Marble.marble_size*Math.sqrt(3.0)/2.0);
        for(int j=0; j<4; ++j)
            for(int i=0; i<4-j; ++i,++p)
                b.blit(R.drawable.misc,
                    28*pyramid[p], 357, 28, 28,
                    x+(i+i+j)*Marble.marble_size/2,
                    y-dy*j);

        // Stationary green marble
        b.blit(R.drawable.misc,84,357,28,28,x+Marble.marble_size*5,y);

        // Moving blue marble
        b.blit(R.drawable.misc,93,693,49,28,x+Marble.marble_size*8,y);
    }
}
