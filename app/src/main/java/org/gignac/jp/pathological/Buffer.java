package org.gignac.jp.pathological;

class Buffer extends TunnelTile
{
    private int marble;
    private Marble entering;

    public Buffer(Board board, int paths, int color) {
        super(board, paths);
        marble = color;
        entering = null;
        board.sc.cache(R.drawable.misc);
    }

    public void draw_back(Blitter surface) {
        super.draw_back(surface);
        int color = marble;
        if( color >= 0) {
            surface.blit( R.drawable.misc, color*28, 357, 28, 28,
                left + (tile_size-Marble.marble_size)/2,
                top + (tile_size-Marble.marble_size)/2);
        } else {
            surface.blit( R.drawable.misc, 418, 166, 38, 38, left+27, top+27);
        }
    }

    protected void draw_cap(Blitter surface) {
        surface.blit( R.drawable.misc, 456, 166, 38, 38, left+27, top+27);
    }

    public void affect_marble(Board board, Marble marble, int x, int y)
    {
        GameResources gr = board.gr;

        // Watch for marbles entering
        if((x+Marble.marble_size == tile_size/2 && marble.direction == 1) ||
           (x-Marble.marble_size == tile_size/2 && marble.direction == 3) ||
           (y+Marble.marble_size == tile_size/2 && marble.direction == 2) ||
           (y-Marble.marble_size == tile_size/2 && marble.direction == 0))
        {
            if( entering != null) {
                // Bump the marble that is currently entering
                Marble newmarble = entering;
                newmarble.left = left + (tile_size-Marble.marble_size)/2;
                newmarble.top = top + (tile_size-Marble.marble_size)/2;
                newmarble.direction = marble.direction;

                gr.play_sound( GameResources.ping);

                // Let the base class affect the marble
                super.affect_marble(board, newmarble,
                    tile_size/2, tile_size/2);
            } else if( this.marble >= 0) {
                // Bump the marble that is currently caught
                Marble newmarble = new Marble(this.marble,
                    left + tile_size/2, top + tile_size/2,
                    marble.direction);

                board.activateMarble( newmarble);

                gr.play_sound( GameResources.ping);

                // Let the base class affect the marble
                super.affect_marble(board, newmarble,
                    tile_size/2, tile_size/2);

                this.marble = -1;
            }

            // Remember which marble is on its way in
            entering = marble;
        } else if( x == tile_size/2 && y == tile_size/2) {
            // Catch this marble
            this.marble = marble.color;
            board.deactivateMarble( marble);
            entering = null;
        }
    }
}
