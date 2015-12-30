package org.gignac.jp.pathological;

class Tile
{
    public static final int tile_size = 92;
    public int paths;
    public int left, top;
    public int tile_x, tile_y;
    protected Board board;
    public boolean dirty;

    public Tile( Board board, int paths, int cx, int cy, int x, int y) {
        this.board = board;
        this.paths = paths;
        this.left = cx - tile_size/2;
        this.top = cy - tile_size/2;
        this.tile_x = x;
        this.tile_y = y;
        this.dirty = true;
        board.sc.cache(R.drawable.misc);
    }

    public Tile( Board board, int paths) {
        this(board,paths,0,0, 0, 0);
    }

    public void setxy(int x, int y) {
        left = Tile.tile_size * x;
        top = Marble.marble_size + Tile.tile_size * y;
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
            if( (paths & 1) > 0 && tile_y == 0) {
                b.blit( R.drawable.misc, 92, 455, 46, 23,
                    left+23,top-14);
            }
        }
    }

    public void update( Board board) {}

    public void draw_fore( Blitter b) {}

    public void click( Board board, int posx, int posy) {}

    public void flick( Board board, int posx, int posy, int dir) {}

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

    public void invalidate() {
        dirty = true;
    }
}
