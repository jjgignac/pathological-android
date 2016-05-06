package org.gignac.jp.pathological;

class Painter extends TunnelTile
{
    private final int color;

    public Painter(Board board, int paths, int color)
    {
        super(board, paths);
        board.sc.cache(R.drawable.misc);
        this.color = color;
    }

    @Override
    protected void draw_cap(Blitter surface) {
        surface.blit( R.drawable.misc,
            38*color+(color<6?266:-228), color<6?242:280, 38, 38,
            left+27, top+27);
    }

    @Override
    public void affect_marble(Board board, Marble marble, int x, int y)
    {
        super.affect_marble( board, marble, x, y);
        if( x == tile_size/2 && y == tile_size/2) {
            if( marble.color != color) {
                // Change the color
                marble.color = color;
                board.gr.play_sound( GameResources.change_color);
            }
        }
    }
}
