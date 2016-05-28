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
import android.graphics.Point;
import android.media.*;
import android.content.*;
import android.os.Build;

import java.util.*;
import java.io.*;

@SuppressWarnings("WeakerAccess")
public class GameResources
{
    private static GameResources instance;
    private static SharedPreferences shp;
    public Context context;
    public final Random random;
    private static final int[] sound_resid = {
        R.raw.filter_admit, R.raw.wheel_turn, R.raw.wheel_completed,
        R.raw.change_color, R.raw.direct_marble, R.raw.ping,
        R.raw.trigger_setup, R.raw.teleport, R.raw.menu_scroll,
        R.raw.levelfinish, R.raw.die, R.raw.incorrect,
        R.raw.switched, R.raw.shredder, R.raw.replicator,
        R.raw.extra_life, R.raw.menu_scroll, R.raw.switched };
    private static final float[] sound_volume = {
        0.8f, 0.8f, 0.7f, 0.8f, 0.6f, 0.8f, 1.0f, 0.6f, 0.5f,
        0.6f, 1.0f, 0.15f, 1.0f, 1.0f, 1.0f, 1.0f, 0.8f, 1.0f};
    private final int[] sound_id = new int[sound_resid.length];
    private SoundPool sp;
    public final SpriteCache sc;

    // Sounds
    public static final int filter_admit = 0;
    public static final int wheel_turn = 1;
    public static final int wheel_completed = 2;
    public static final int change_color = 3;
    public static final int direct_marble = 4;
    public static final int ping = 5;
    public static final int trigger_setup = 6;
    public static final int teleport = 7;
    public static final int marble_release = 8;
    public static final int levelfinish = 9;
    public static final int die = 10;
    public static final int incorrect = 11;
    public static final int switched = 12;
    public static final int shredder = 13;
    public static final int replicator = 14;

    public static final int wheel_margin = 4;
    public static final int wheel_steps = 9;
    public final int holecenter_radius;
    public final int[][] holecenters_x;
    public final int[][] holecenters_y;

    public final int numlevels;
    public Vector<String> boardNames;
    public Vector<Point> boardPositions;
    public Vector<int[]> fromBoards;
    public int maxXPos;
    private final boolean[] mIsUnlocked;

    public static synchronized GameResources getInstance(Context context) {
        if(instance == null) instance = new GameResources(context);
        else instance.context = context;
        return instance;
    }

    private GameResources(Context context) {
        this.context = context;

        sc = new SpriteCache( context.getResources());

        getBoardInfo();
        numlevels = boardNames.size();

        mIsUnlocked = new boolean[numlevels];

        // Determine which levels are unlocked
        for( int level=0; level < numlevels; ++level) {
            if( level == 0 || GameResources.bestScore(level) != -1) {
                mIsUnlocked[level] = true;
                continue;
            }
            for( int from : fromBoards.elementAt(level)) {
                if( GameResources.bestScore(from) != -1) {
                    mIsUnlocked[level] = true;
                    break;
                }
            }
        }

        random = new Random();

        // The positions of the holes in the wheels in
        // each rotational position
        holecenter_radius = (Tile.tile_size - Marble.marble_size) / 2 - wheel_margin;
        holecenters_x = new int[wheel_steps][];
        holecenters_y = new int[wheel_steps][];
        for( int i=0; i<wheel_steps; ++i) {
            double theta = Math.PI * i / (2 * wheel_steps);
            double c = Math.floor( 0.5 + Math.cos(theta)*holecenter_radius);
            double s = Math.floor( 0.5 + Math.sin(theta)*holecenter_radius);
            holecenters_x[i] = new int[4];
            holecenters_y[i] = new int[4];
            holecenters_x[i][0] = (int)Math.round(Tile.tile_size/2 + s);
            holecenters_y[i][0] = (int)Math.round(Tile.tile_size/2 - c);
            holecenters_x[i][1] = (int)Math.round(Tile.tile_size/2 + c);
            holecenters_y[i][1] = (int)Math.round(Tile.tile_size/2 + s);
            holecenters_x[i][2] = (int)Math.round(Tile.tile_size/2 - s);
            holecenters_y[i][2] = (int)Math.round(Tile.tile_size/2 + c);
            holecenters_x[i][3] = (int)Math.round(Tile.tile_size/2 - c);
            holecenters_y[i][3] = (int)Math.round(Tile.tile_size/2 - s);
        }
    }

    public void create() {
        if(sp != null) return;

        // Load the sound effects
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            sp = new SoundPool.Builder()
                    .setAudioAttributes(aa)
                    .setMaxStreams(2).build();
        } else {
            @SuppressWarnings("deprecation")
            SoundPool sp = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
            this.sp = sp;
        }

        for( int i=0; i < sound_resid.length; ++i)
            sound_id[i] = sp.load(context, sound_resid[i], 0);
    }

    public void destroy() {
        if(sp != null) sp.release();
        sp = null;
    }

    public void play_sound( int id)
    {
        if(sp == null) return;
        sp.play( sound_id[id], sound_volume[id],
            sound_volume[id], 0, 0, 1.0f);
    }

    @SuppressWarnings("SameParameterValue")
    public InputStream openRawResource(int resid) {
        return context.getResources().openRawResource(resid);
    }

    public int nextLevel(int level) {
        return nextLevel(level, level+1);
    }

    private int nextLevel(int level, int minResult) {
        if( level == numlevels - 1) return -1;
        for( int i = minResult; i < numlevels; i++) {
            for( int from : fromBoards.elementAt(i)) {
                if( from == level) return i;
            }
        }
        int result = minResult;
        for( int from : fromBoards.elementAt(level)) {
            result = Math.min(result, nextLevel(from, minResult));
        }
        return result;
    }

    public boolean isUnlocked(int level) {
        return mIsUnlocked[level];
    }

    private void getBoardInfo()
    {
        boardNames = new Vector<>();
        boardPositions = new Vector<>();
        fromBoards = new Vector<>();
        BufferedReader f = null;

        try {
            f = new BufferedReader( new InputStreamReader(
                openRawResource( R.raw.all_boards)));
            while( true) {
                String line = f.readLine();
                if( line==null) break;
                if( line.startsWith("name=")) {
                    boardNames.add(line.substring(5));
                    if( fromBoards.size() < boardNames.size())
                        fromBoards.add(new int[] {fromBoards.size()-1});
                } else if( line.startsWith("pos=")) {
                    String[] pos = line.substring(4).split(",");
                    int x = Integer.parseInt(pos[0]);
                    int y = Integer.parseInt(pos[1]);
                    boardPositions.add(new Point(x,y));
                    maxXPos = Math.max(maxXPos, x);
                } else if( line.startsWith("from=")) {
                    String[] fromStr = line.substring(5).split(",");
                    int[] from = new int[fromStr.length];
                    for (int i=0; i < fromStr.length; i++) {
                        from[i] = Integer.parseInt(fromStr[i])-1;
                    }
                    if( fromBoards.size() < boardNames.size()) fromBoards.add(from);
                    else fromBoards.set(fromBoards.size()-1, from);
                }
            }
        } catch(IOException e) {
            //
        } finally {
            try {
                if(f != null) f.close();
            } catch(IOException e) {
                //
            }
        }
    }

    public static void setup(Context context) {
        shp = context.getSharedPreferences(
                "org.gignac.jp.pathological.Pathological", Context.MODE_PRIVATE);

        // If we just upgraded, delete the cached preview images
        if( BuildConfig.VERSION_CODE != GameResources.shp.getInt("version", 1)) {
            SharedPreferences.Editor e = GameResources.shp.edit();
            e.putInt("version", BuildConfig.VERSION_CODE);
            e.apply();
            Preview.clearCache(context);
        }
    }

    public static int getCurrentLevel() {
        return shp.getInt("level", 0);
    }

    public static void setCurrentLevel(int level) {
        if( level == getCurrentLevel()) return;
        SharedPreferences.Editor e = GameResources.shp.edit();
        e.putInt("level", level);
        e.apply();
    }

    public static int bestScore(int level) {
        return GameResources.shp.getInt("best_"+level, -1);
    }

    public void setBestScore(int level, int score) {
        SharedPreferences.Editor e = GameResources.shp.edit();
        e.putInt("best_" + level, score);
        e.apply();

        for( int i = 0; i < numlevels; ++i) {
            for( int from : fromBoards.elementAt(i)) {
                if( from == level) mIsUnlocked[i] = true;
            }
        }
    }
}
