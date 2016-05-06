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
        invalidate();
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
        countdown = trigger_time * Game.frames_per_sec;
        invalidate();
    }
}
