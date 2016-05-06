package org.gignac.jp.pathological;

class Wheel extends Tile
{
    public final int[] marbles;
    private final Marble[] entering = new Marble[4];
    public int spinpos;
    public boolean completed;

    public Wheel( Board board, int paths) {
        super(board, paths); // Call base class intializer
        spinpos = 0;
        completed = false;
        marbles = new int[4];
        marbles[0] = -3;
        marbles[1] = -3;
        marbles[2] = -3;
        marbles[3] = -3;

        board.sc.cache(R.drawable.misc);
    }

    @Override
    public void draw_back(Blitter b)
    {
        super.draw_back(b);
        GameResources gr = board.gr;

        if( spinpos != 0) {
            b.blit(R.drawable.misc, completed?0:92, 0, 92, 92, left, top);
            for(int i=0; i<4; ++i) {
                int holecenter_x = gr.holecenters_x[spinpos][i];
                int holecenter_y = gr.holecenters_y[spinpos][i];
                b.blit( R.drawable.misc, completed?362:334, 662, 28, 28,
                    holecenter_x-Marble.marble_size/2+left,
                    holecenter_y-Marble.marble_size/2+top);
            }
        } else {
            b.blit(R.drawable.misc, completed?184:276, 0, 92, 92, left, top);
        }

        for( int i=0; i < 4; ++i) {
            int color = marbles[i];
            if( color >= 0) {
                int holecenter_x = gr.holecenters_x[spinpos][i];
                int holecenter_y = gr.holecenters_y[spinpos][i];
                b.blit( R.drawable.misc, 28*color, 357, 28, 28,
                    holecenter_x-Marble.marble_size/2+left,
                    holecenter_y-Marble.marble_size/2+top);
            }
        }
    }

    @Override
    public void update( Board board) {
        if( spinpos > 0) {
            spinpos -= 1;
        }
    }

    @Override
    public void click( Board board, int posx, int posy)
    {
        // Ignore all clicks while rotating
        if( spinpos != 0) return;

        // First, make sure that no marbles are currently entering
        for( int i=0; i < 4; ++i)
            if( marbles[i] == -2) return;

        // Suck in any marbles that are entering
        for( int i=0; i < 4; ++i) {
            if( marbles[i] == -1) {
                marbles[i] = entering[i].color;
                board.deactivateMarble(entering[i]);
                entering[i] = null;
            }
        }

        // Start the wheel spinning
        spinpos = GameResources.wheel_steps - 1;
        board.gr.play_sound( GameResources.wheel_turn);

        // Reposition the marbles
        int t = marbles[0];
        marbles[0] = marbles[1];
        marbles[1] = marbles[2];
        marbles[2] = marbles[3];
        marbles[3] = t;
    }

    @Override
    public void flick( Board board, int posx, int posy, int dir)
    {
        // Ignore all flicks while rotating
        if( spinpos != 0) return;
        eject( dir, board, tile_x, tile_y);
    }

    private void eject(int i, Board board, int tile_x, int tile_y)
    {
        GameResources gr = board.gr;

        // Determine the neighboring tile
        Tile neighbor = board.tiles
            [(tile_y+Marble.dy[i]+Board.vert_tiles) % Board.vert_tiles]
            [(tile_x+Marble.dx[i]+Board.horiz_tiles) % Board.horiz_tiles];

        if ( marbles[i] < 0 ||
            // Disallow marbles to go off the top of the board
            (tile_y == 0 && i==0) ||

            // If there is no way out here, skip it
            ((paths & (1 << i)) == 0) ||

            // If the neighbor is a wheel that is either turning
            // or has a marble already in the hole, disallow
            // the ejection
            (neighbor instanceof Wheel &&
            (((Wheel)neighbor).spinpos != 0 ||
             ((Wheel)neighbor).marbles[i^2] != -3))
            )
            gr.play_sound( GameResources.incorrect);
        else {
            // If the neighbor is a wheel, apply a special lock
            if( neighbor instanceof Wheel)
                ((Wheel)neighbor).marbles[i^2] = -2;
            else if( board.marbles.size() >= board.live_marbles_limit) {
                // Impose the live marbles limit
                gr.play_sound( GameResources.incorrect);
                return;
            }

            // Eject the marble
            board.activateMarble(
                new Marble(marbles[i],
                    gr.holecenters_x[0][i]+left,
                    gr.holecenters_y[0][i]+top,
                    i));
            marbles[i] = -3;
            gr.play_sound( GameResources.marble_release);
        }
    }

    @Override
    public void affect_marble(Board board, Marble marble, int rposx, int rposy)
    {
        GameResources gr = board.gr;

        // Watch for marbles entering
        if( rposx+Marble.marble_size/2 == GameResources.wheel_margin ||
            rposx-Marble.marble_size/2 == tile_size - GameResources.wheel_margin ||
            rposy+Marble.marble_size/2 == GameResources.wheel_margin ||
            rposy-Marble.marble_size/2 == tile_size - GameResources.wheel_margin) {
            if( spinpos != 0 || marbles[marble.direction^2] >= -1) {
                // Reject the marble
                marble.direction = marble.direction ^ 2;
                gr.play_sound( GameResources.ping);
            } else {
                marbles[marble.direction^2] = -1;
                entering[marble.direction^2] = marble;
            }
        }

        for( int i=0; i<4; ++i) {
            if( rposx == gr.holecenters_x[0][i] &&
                rposy == gr.holecenters_y[0][i]) {
                // Accept the marble
                board.deactivateMarble( marble);
                marbles[marble.direction^2] = marble.color;
                entering[marble.direction^2] = null;
                break;
            }
        }
    }

    private void complete(Board board) {
        // Complete the wheel
        for( int i=0; i<4; ++i) marbles[i] = -3;
        completed = true;
        board.gr.play_sound( GameResources.wheel_completed);
    }

    public boolean maybe_complete(Board board) {
        if( spinpos > 0) return false;

        // Is there a trigger?
        if(board.trigger != null &&
           board.trigger.marbles != null) {
            // Compare against the trigger
            for( int i=0; i<4; ++i) {
                if( marbles[i] != board.trigger.marbles.charAt(i)-'0' &&
                    marbles[i] != 8) return false;
            }
            complete( board);
            board.trigger.complete( board);
            return true;
        }

        // Do we have four the same color?
        int color = 8;
        for( int i=0; i<4; ++i) {
            int c = marbles[i];
            if( c < 0) return false;
            if( color==8) color=c;
            else if( c != 8 && c != color) return false;
        }

        // Is there a stoplight?
        if((board.stoplight != null) &&
           (board.stoplight.current < 3)) {
            // Compare against the stoplight
            if( color != 8 &&
                color != board.stoplight.marbles[board.stoplight.current])
                return false;
            else
                board.stoplight.complete( board);
        }

        complete( board);
        return true;
    }
}

