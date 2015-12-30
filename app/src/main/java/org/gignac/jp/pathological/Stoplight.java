package org.gignac.jp.pathological;

class Stoplight extends Tile {
    public int current;
    public int[] marbles;

    public Stoplight(Board board, String colors) {
        super(board, 0); // Call base class intializer
        marbles = new int[3];
        for(int i=0; i<3; ++i)
            marbles[i] = colors.charAt(i)-'0';
        current = 0;
        board.sc.cache(R.drawable.misc);
    }

    @Override
    public void draw_back( Blitter b) {
        super.draw_back(b);
        b.blit( R.drawable.misc, 462, 0, 36, 92, left+28, top);
        for(int i=current; i < 3; ++i) {
            b.blit( R.drawable.misc, marbles[i]*28, 357, 28, 28,
                left + Tile.tile_size/2 - 14, top + 3 + (29*i));
        }
    }

    public void complete( Board board) {
        for( int i=0; i<3; ++i) {
            if( marbles[i] >= 0) {
                marbles[i] = -1;
                break;
            }
        }
        current += 1;
        invalidate();
    }
}
