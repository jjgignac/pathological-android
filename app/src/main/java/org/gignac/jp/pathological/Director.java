package org.gignac.jp.pathological;

class Director extends TunnelTile
{
    private int direction;

    public Director(Board board, int paths, int direction)
    {
        super(board, paths);
        this.direction = direction;
        board.sc.cache(R.drawable.misc);
    }

    @Override
    protected void draw_cap(Blitter surface) {
        surface.blit( R.drawable.misc,
            38*direction, 204, 38, 38, left+27, top+27);
    }

    @Override
    public void affect_marble(Board board, Marble marble, int x, int y) {
        if(x == tile_size/2 && y == tile_size/2) {
            marble.direction = direction;
            board.gr.play_sound( GameResources.direct_marble);
        }
    }
}

