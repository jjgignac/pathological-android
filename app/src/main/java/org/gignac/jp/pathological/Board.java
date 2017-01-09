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
import java.io.*;
import java.util.*;
import android.graphics.*;
import android.os.*;

@SuppressWarnings("WeakerAccess")
class Board {
    public static final int INCOMPLETE = 0;
    public static final int COMPLETE = 1;
    public static final int LAUNCH_TIMEOUT = -1;
    public static final int BOARD_TIMEOUT = -2;
    private static final String default_colors = "23468";
    private static final String default_stoplight = "643";
    private static final int default_launch_timer = 6;
    private static final int default_board_timer = 30;
    public static final int vert_tiles = 8;
    public static final int horiz_tiles = 6;
    public static final int board_width = horiz_tiles * Tile.tile_size;
    public static final int board_height = vert_tiles * Tile.tile_size;
    private static final int screen_width = board_width + Marble.marble_size;
    public static final int screen_height = board_height + Marble.marble_size;
    public final GameResources gr;
    public Trigger trigger;
    public Stoplight stoplight;
    public final Vector<Marble> marbles;
    public final Tile[][] tiles;
    private final int[] launch_queue;
    private int board_state;
    private boolean paused;
    public String name;
    public int live_marbles_limit;
    private int launch_timeout;
    private int launch_timeout_start;
    public int board_timeout;
    public int board_timeout_start;
    public String colors;
    public String firstColors;
    private Marble[] marblesCopy = new Marble[20];
    private final HashMap<Integer,Point> down;
    private float launch_queue_offset;
    private float scale = 0f;
    private final Runnable onPainted;
    public final SpriteCache sc;
    private long pause_changed;
    public int delay = 50;
    private int score = 0;
    private Tutorial tutorial = null;

    public Board(GameResources gr, SpriteCache sc,
                 int level, Runnable onPainted)
    {
        this.gr = gr;
        this.marbles = new Vector<>();
        this.trigger = null;
        this.stoplight = null;
        this.launch_queue = new int[screen_width * 3 / Marble.marble_size];
        this.board_state = INCOMPLETE;
        this.paused = false;
        this.live_marbles_limit = 10;
        this.launch_timeout = -1;
        this.board_timeout = -1;
        this.colors = default_colors;
        this.firstColors = "";
        this.onPainted = onPainted;
        this.sc = sc;
        this.pause_changed = SystemClock.uptimeMillis()-10000;

        down = new HashMap<>();

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(0xfff0f0f0);
        paint.setTextSize(Marble.marble_size*4/5);
        paint.setTypeface(Typeface.DEFAULT_BOLD);

        // Seed the randomness based on the level number and
        // the current time.  Only use the time accurate to
        // the ten-minute interval.  This will discourage players
        // from reloading levels repeatedly in order to get
        // their choice of marbles/trigger/etc.
        gr.random.setSeed((System.currentTimeMillis()/600000)*1000+level);

        set_launch_timer( default_launch_timer);
        set_board_timer( default_board_timer);

        // Create the board array
        tiles = new Tile[vert_tiles][];
        for( int j=0; j < vert_tiles; ++j)
            tiles[j] = new Tile[horiz_tiles];

        sc.cache( R.drawable.backdrop);
        sc.cache( R.drawable.misc);

        // Load the level
        try {
            _load(gr,  level);
        } catch(IOException e) {
            //
        }

        // Fill up the launch queue
        for( int i=0; i < launch_queue.length; ++i) {
            if( i < firstColors.length()) {
                launch_queue[i] = firstColors.charAt(i)-'0';
                continue;
            }
            launch_queue[i] = colors.charAt(gr.random.nextInt(colors.length()))-'0';
        }

        // Set up the tutorial if required
        if( level == 0) {
            tutorial = new Tutorial(this, 0);
        } else if( level == 1) {
            tutorial = new Tutorial(this, 100);
        } else if( level == 2) {
            tutorial = new Tutorial(this, 200);
        } else if( level == 8) {
            tutorial = new Tutorial(this, 300);
        }
    }

    private void draw_mid( Blitter b)
    {
        // Draw the launch timer
        int timerColor = 0x40404040;
        float timeLeft = (float)launch_timeout / GameActivity.frames_per_sec;
        if( timeLeft < 3.5f) {
            // Make the timer flash to indicate that time
            // is running out.
            float s = (float)Math.sin(timeLeft*5);
            int phase = Math.round(s*s*191);
            timerColor = 0x40404040 + (phase<<16) + ((phase*2/3)<<24);
        }
        int y = (launch_timeout*board_height+launch_timeout_start/2) /
            launch_timeout_start;
        b.fill(timerColor, 0, Marble.marble_size,
            Marble.marble_size, board_height - y);

        // Indicate how many live balls remain
        int ybase = board_height-2;
        for( int i=0; i < live_marbles_limit - marbles.size(); ++i) {
            b.blit(R.drawable.misc, 0, 611, 16, 16,
                    Marble.marble_size/2 - 8, ybase - Marble.marble_size * i,
                    16, 16);
        }

        draw_marble_queue(b);
    }

    private void draw_marble_queue(Blitter b)
    {
        // Draw the marble queue
        int iOffset = Math.round(launch_queue_offset);
        for(int i=0; i < launch_queue.length; ++i)
            b.blit(R.drawable.misc, 28*launch_queue[i], 357, 28, 28,
                iOffset + i * Marble.marble_size, 0);
    }

    private void draw_fore( Blitter b) {
        for(Tile[] row : tiles)
            for(Tile tile : row)
                tile.draw_fore(b);
    }

    private int makeRGBA(int rgb, int alpha) {
        return (alpha<<24)|rgb;
    }

    private void drawPauseButton(Blitter b)
    {
        if(board_state != INCOMPLETE) return;
        int intensity = (int)((SystemClock.uptimeMillis() - pause_changed) / 2);
        if( intensity > 255) intensity = 255;
        if(!paused) intensity ^= 0xff;
        if(intensity == 0) return;

        int borderColor = makeRGBA(0x000000, intensity/2);
        int color = makeRGBA(0xd0d0d0, intensity);

        int thickness = b.getWidth()/30;
        int spacing = thickness * 4/5;
        int height = thickness * 4;
        int x = (b.getWidth() - 2*thickness - spacing) / 2;
        int y = (b.getHeight() - height) / 2;
        b.fill(borderColor, 0, 0,
            b.getWidth(), b.getHeight());
        b.fill(color, x, y, thickness, height);
        b.fill(color, x+thickness+spacing, y,
               thickness, height);
    }

    public int update()
    {
        // Return INCOMPLETE even if the board is complete.
        // This ensures that the end of level signal is only
        // sent once.
        if(paused || board_state != INCOMPLETE) return INCOMPLETE;

        // Animate the marbles
        marblesCopy = marbles.toArray(marblesCopy);
        for(Marble marble : marblesCopy) {
            if( marble == null) break;
            marble.update(this);
        }

        // Animate the tiles
        for(Tile[] row : tiles)
            for(Tile tile : row)
                tile.update(this);

        // Complete any wheels, if appropriate
        boolean try_again = true;
        while(try_again) {
            try_again = false;
            for(Tile[] row : tiles)
                for(Tile tile : row)
                    if( tile instanceof Wheel) {
                        int deltaScore = ((Wheel)tile).maybe_complete(this);
                        if( deltaScore > 0) {
                            score += deltaScore;
                            try_again = true;
                        }
                    }
        }

        // Check if the board is complete
        board_state = COMPLETE;
        for(Tile[] row : tiles)
            for(Tile tile : row)
                if(tile instanceof Wheel)
                    if(!((Wheel)tile).completed)
                        board_state = INCOMPLETE;

        // Decrement the launch timer
        if(board_state == INCOMPLETE && launch_timeout > 0) {
            launch_timeout -= 1;
            if(launch_timeout == 0) board_state = LAUNCH_TIMEOUT;
        }

        // Decrement the board timer
        if( board_state == INCOMPLETE && board_timeout > 0) {
            board_timeout -= 1;
            if(board_timeout == 0) board_state = BOARD_TIMEOUT;
        }

        // Animate the launch queue
        if( launch_queue_offset > 0) {
            float speed = Marble.marble_speed*0.2f;  // Nice and slow
            // If we expect the marble to drop into the
            // top-right tile, accelerate the animation so
            // the launch queue doesn't have to jump.  But
            // wait for 10% of the marble height before we
            // accelerate, so the marbles don't appear to
            // overlap.
            Tile topLeft = tiles[0][0];
            if( launch_queue_offset < Marble.marble_size * 0.9f &&
                (topLeft.paths & 8) == 8 &&
               (!(topLeft instanceof Wheel) ||
                ((Wheel)topLeft).marbles[3] < 0))
                speed = Marble.marble_speed*0.7f;
            launch_queue_offset -= speed;
            if(launch_queue_offset < 0) launch_queue_offset = 0;
        }

        return board_state;
    }

    public void paint(Blitter b)
    {
        int width = b.getWidth();
        int height = b.getHeight();
        scale = height * screen_width < width * screen_height ?
            (float)height / screen_height : (float)width / screen_width;

        // Draw the background
        b.blit( R.drawable.backdrop, 0, 0, width, b.getHeight());

        // Black-out the top edge of the backdrop
        b.fill( 0xff000000, 0, 0,
                width, Math.round(Marble.marble_size/2 * scale));

        b.pushTransform( scale, 1, 0);

        // Draw the launcher
        b.blit( R.drawable.misc, 192, 387, 30, 1,
                -1, Marble.marble_size, 30, board_height - Marble.marble_size);
        b.blit( R.drawable.misc, 415, 394, 1, 30,
                30, -1, (int)Math.ceil(width / scale), 30);
        b.blit( R.drawable.misc, 8, 386, 30, 38,
                -1, board_height + Marble.marble_size - 38);
        b.blit( R.drawable.misc, 238, 394, 38, 38, -1, -1);

        b.pushTransform( 1f, Marble.marble_size, Marble.marble_size);

        for( Tile[] row : tiles)
            for( Tile tile : row)
                tile.draw_back(b);

        b.popTransform();

        // Draw the middle
        draw_mid(b);

        b.pushTransform( 1f, Marble.marble_size, Marble.marble_size);
        // Draw all of the marbles
        for(Marble marble : marbles)
            marble.draw(b);

        // Draw the foreground
        draw_fore(b);

        if( tutorial != null && b instanceof CanvasBlitter) {
            tutorial.paint((CanvasBlitter)b,
                    - (float)board_timeout / GameActivity.frames_per_sec);
        }

        b.popTransform();
        b.popTransform();

        drawPauseButton(b);

        // Trigger the update step
        if(onPainted != null) onPainted.run();
    }

    public void set_tile( int x, int y, Tile tile) {
        tiles[y][x] = tile;
        tile.setxy(x,y);

        // If it's a trigger, keep track of it
        if( tile instanceof Trigger)
            trigger = (Trigger)tile;

        // If it's a stoplight, keep track of it
        if( tile instanceof Stoplight)
            stoplight = (Stoplight)tile;
    }

    public void set_launch_timer( int passes) {
        launch_timeout_start = (Marble.marble_size +
            (vert_tiles * Tile.tile_size - Marble.marble_size)
                * passes) / Marble.marble_speed;
    }

    public void set_board_timer(int seconds) {
        board_timeout_start = seconds * GameActivity.frames_per_sec;
        board_timeout = board_timeout_start;
    }

    public void activateMarble( Marble m) {
        marbles.add(m);
    }

    public void deactivateMarble( Marble m) {
        marbles.remove(m);
    }

    public void launch_marble() {
        activateMarble( new Marble(
                launch_queue[0],
            -Marble.marble_size/2,
            -Marble.marble_size/2, 2));
        System.arraycopy(launch_queue, 1, launch_queue, 0, launch_queue.length-1);
        launch_queue[launch_queue.length-1] =
            colors.charAt(gr.random.nextInt(colors.length()))-'0';
        launch_timeout = launch_timeout_start;
        launch_queue_offset = Marble.marble_size;
    }

    public void affect_marble( Marble marble)
    {
        int cx = marble.left + Marble.marble_size/2;
        int cy = marble.top + Marble.marble_size/2;

        // Bounce marbles off of the left
        if( cx == Marble.marble_size/2) {
            marble.direction = 1;
            return;
        }

        int effective_cx = cx + Marble.marble_size/2 * Marble.dx[marble.direction];
        int effective_cy = cy + Marble.marble_size/2 * Marble.dy[marble.direction];

        if( cx < 0) {
            if(cy == board_height - Marble.marble_size/2) {
                marble.direction = 0;
                return;
            }
            if( cy == Marble.marble_size/2) {
                marble.direction = 2;
                return;
            }

            // The special case of new marbles at the top
            effective_cx = cx + 1 + Marble.marble_size;
            effective_cy = cy;
        }

        int tile_x = effective_cx / Tile.tile_size;
        int tile_y = effective_cy / Tile.tile_size;
        int tile_xr = cx - tile_x * Tile.tile_size;
        int tile_yr = cy - tile_y * Tile.tile_size;

        if( tile_y < 0) return;

        Tile tile = tiles[tile_y][tile_x];

        if( cx < 0 && marble.direction != 1) {
            // The special case of new marbles on the left
            if( tile_yr == Tile.tile_size / 2 && ((tile.paths & 8) == 8)) {
                if( tile instanceof Wheel) {
                    Wheel w = (Wheel)tile;
                    if( w.spinpos > 0 || w.marbles[3] != -3) return;
                    w.marbles[3] = -2;
                    marble.direction = 1;
                    this.launch_marble();
                } else if( this.marbles.size() < live_marbles_limit) {
                    marble.direction = 1;
                    this.launch_marble();
                }
            }
        } else
            tile.affect_marble( this, marble, tile_xr, tile_yr);
    }

    private Tile whichTile(int posx, int posy) {
        // Determine which tile the pointer is in
        int tile_x = (posx - Marble.marble_size) / Tile.tile_size;
        int tile_y = (posy - Marble.marble_size) / Tile.tile_size;
        if( tile_x >= 0 && tile_x < horiz_tiles &&
            tile_y >= 0 && tile_y < vert_tiles) {
            return tiles[tile_y][tile_x];
        }
        return null;
    }

    public void downEvent(int pointerId, float x, float y)
    {
        if(board_state != INCOMPLETE) return;
        if(scale == 0f) return;
        int posx = Math.round(x / scale);
        int posy = Math.round(y / scale);
        Point pos = down.get(pointerId);
        if( pos == null) {
            down.put(pointerId,new Point(posx,posy));
        } else {
            pos.x = posx;
            pos.y = posy;
        }
    }

    public void upEvent(int pointerId, float x, float y)
    {
        if(board_state != INCOMPLETE) return;
        if(scale == 0f) return;
        int posx = Math.round(x / scale);
        int posy = Math.round(y / scale);
        final Point dpos = down.get(pointerId);
        if(dpos == null) return;
        final int downx = dpos.x, downy = dpos.y;
        int dx = posx - downx;
        int dy = posy - downy;
        int dx2 = dx*dx;
        int dy2 = dy*dy;
        if(paused) {
            if(dx2+dy2 <= Marble.marble_size*Marble.marble_size) {
                if( gr.context instanceof GameActivity) {
                    ((GameActivity)gr.context).resume();
                }
            }
            return;
        }
        Tile downtile = whichTile(downx,downy);
        if(downtile == null) return;
        int downtile_x = downx / Tile.tile_size;
        int downtile_y = (downy - Marble.marble_size) / Tile.tile_size;
        int tile_xr = downx-(downtile_x*Tile.tile_size);
        int tile_yr = downx-Marble.marble_size-(downtile_y*Tile.tile_size);

        // Use a distance threshold to decide whether the gesture is a tap
        // or a flick.  But use a smaller threshold if the flick starts near
        // a hole position and heads in the outward direction.
        int flickThreshold = Marble.marble_size;
        int dir = (dx2>dy2)?(dx>0?1:3):(dy>0?2:0);
        int xmo = tile_xr-gr.holecenters_x[0][dir];
        int ymo = tile_yr-gr.holecenters_y[0][dir];
        int nearThreshold = Marble.marble_size * 5/3;
        boolean startedNearMarble =
            (xmo*xmo+ymo*ymo) <= nearThreshold * nearThreshold;
        if(startedNearMarble) flickThreshold /= 2;
        if(dx2+dy2 <= flickThreshold*flickThreshold) {
            downtile.click(this, tile_xr, tile_yr);
        } else {
            downtile.flick(this, tile_xr, tile_yr, dir);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void _load(GameResources gr, int level)
        throws IOException
    {
        BufferedReader f = new BufferedReader( new InputStreamReader(
            gr.openRawResource( R.raw.all_boards)));

        // Skip the previous levels
        int j = 0;
        while( j < horiz_tiles * level) {
            String line = f.readLine();
            if( line==null) {
                f.close();
                return;
            }
            if( line.isEmpty()) continue;
            if( line.charAt(0) == '|') j += 1;
        }

        Vector<Tile> teleporters = new Vector<>();
        String teleporter_names = "";
        String stoplight = default_stoplight;

        int numwheels = 0;
        int boardtimer = -1;

        j = 0;
        while( j < horiz_tiles) {
            String line = f.readLine();
            if(line.isEmpty()) continue;

            if( line.charAt(0) != '|') {
                if( line.startsWith("name="))
                    name = line.substring(5);
                else if( line.startsWith("maxmarbles="))
                    live_marbles_limit = Integer.parseInt(line.substring(11));
                else if( line.startsWith("launchtimer="))
                    set_launch_timer( Integer.parseInt(line.substring(12)));
                else if( line.startsWith("boardtimer="))
                    boardtimer = Integer.parseInt(line.substring(11));
                else if( line.startsWith("colors=")) {
                    colors = "";
                    for( char c : line.substring(7).toCharArray()) {
                        if (c >= '0' && c <= '8') colors = colors + c;
                    }
                } else if( line.startsWith("firstcolors=")) {
                        firstColors = "";
                        for( char c : line.substring(12).toCharArray()) {
                            if( c >= '0' && c <= '8') firstColors = firstColors + c;
                        }
                } else if( line.startsWith("stoplight=")) {
                    stoplight = "";
                    for(char c : line.substring(10).toCharArray())
                        if( c >= '0' && c <= '7')
                            stoplight = stoplight + c;
                }

                continue;
            }

            for( int i=0; i < vert_tiles; ++i) {
                char type = line.charAt(i*4+1);
                char paths = line.charAt(i*4+2);
                int pathsint = paths-'0';
                if( paths == ' ') pathsint = 0;
                else if( paths >= 'a') pathsint = paths-'a'+10;
                char color = line.charAt(i*4+3);
                int colorint;
                if( color == ' ') colorint = 0;
                else if( color >= 'a') colorint = color-'a'+10;
                else if( color >= '0' && color <= '9') colorint = color-'0';
                else colorint = 0;

                // Rotate the paths to account for portrait mode
                pathsint = (pathsint >> 1) | ((pathsint & 1)<<3);

                Tile tile = null;
                if( type == 'O') {
                    tile = new Wheel(this, pathsint);
                    numwheels += 1;
                } else if( type == '%') tile = new Trigger(this, colors);
                else if( type == '!') tile = new Stoplight(this, stoplight);
                else if( type == '&') tile = new Painter(this, pathsint, colorint);
                else if( type == '#') tile = new Filter(this, pathsint, colorint);
                else if( type == '@') {
                    if( color == ' ') tile = new Buffer(this, pathsint, -1);
                    else tile = new Buffer(this, pathsint, colorint);
                }
                else if( type == ' ' ||
                    (type >= '0' && type <= '8')) tile = new Tile(this, pathsint);
                else if( type == 'X') tile = new Shredder(this, pathsint);
                else if( type == '*') tile = new Replicator(this, pathsint, colorint);
                else if( type == '^') {
                    if( color == ' ') tile = new Director(this, pathsint, 3);
                    else if( color == '>') tile = new Switch(this, pathsint, 3, 0);
                    else if( color == 'v') tile = new Switch(this, pathsint, 3, 1);
                    else if( color == '<') tile = new Switch(this, pathsint, 3, 2);
                } else if( type == '>') {
                    if( color == ' ') tile = new Director(this, pathsint, 0);
                    else if( color == '^') tile = new Switch(this, pathsint, 0, 3);
                    else if( color == 'v') tile = new Switch(this, pathsint, 0, 1);
                    else if( color == '<') tile = new Switch(this, pathsint, 0, 2);
                } else if( type == 'v') {
                    if( color == ' ') tile = new Director(this, pathsint, 1);
                    else if( color == '^') tile = new Switch(this, pathsint, 1, 3);
                    else if( color == '>') tile = new Switch(this, pathsint, 1, 0);
                    else if( color == '<') tile = new Switch(this, pathsint, 1, 2);
                } else if( type == '<') {
                    if( color == ' ') tile = new Director(this,pathsint, 2);
                    else if( color == '^') tile = new Switch(this, pathsint, 2, 3);
                    else if( color == '>') tile = new Switch(this, pathsint, 2, 0);
                    else if( color == 'v') tile = new Switch(this, pathsint, 2, 1);
                }
                else if( type == '=') {
                    if( teleporter_names.indexOf(color) >= 0) {
                        Tile other = teleporters.get( teleporter_names.indexOf(color));
                        tile = new Teleporter( this, pathsint, (Teleporter)other);
                    } else {
                        tile = new Teleporter( this, pathsint, null);
                        teleporters.addElement( tile);
                        teleporter_names = teleporter_names + color;
                    }
                }

                this.set_tile( j, vert_tiles-i-1, tile);

                if( type >= '0' && type <= '8') {
                    int direction;
                    if( color == '^') direction = 3;
                    else if( color == '>') direction = 0;
                    else if( color == 'v') direction = 1;
                    else direction = 2;
                    activateMarble( new Marble(type-'0',
                        tile.left + Tile.tile_size/2,
                        tile.top + Tile.tile_size/2,
                        direction));
                }
            }

            j += 1;
        }
        if( boardtimer < 0) boardtimer = default_board_timer * numwheels;
        this.set_board_timer( boardtimer);

        // Make the crazy marbles less frequent than the other colors
        String adjColors = "";
        for( char c : colors.toCharArray()) {
            if( c >= '0' && c <= '7') {
                for(int i=0; i < 3; ++i) adjColors = adjColors + c;
            } else if( c == '8') {
                adjColors = adjColors + c;
            }
        }
        colors = adjColors;

        f.close();
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        if(paused == this.paused) return;
        pause_changed = SystemClock.uptimeMillis();
        this.paused = paused;
    }

    public int score() {
        return score;
    }

    public int emptyHolePercentage() {
        int nHoles = 0;
        int nEmpty = 0;
        for(Tile[] row : tiles) {
            for (Tile tile : row) {
                if (!(tile instanceof Wheel)) continue;
                nHoles += 4;
                for( int marble : ((Wheel)tile).marbles) {
                    if( marble < 0) nEmpty += 1;
                }
            }
        }
        return (nEmpty * 100 + nHoles/2) / nHoles;
    }

    public int timeRemainingPercentage() {
        return (board_timeout * 100 + board_timeout_start/2)
                / board_timeout_start;
    }
}
