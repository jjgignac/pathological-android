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

