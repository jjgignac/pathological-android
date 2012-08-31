package org.gignac.jp.pathological;
import android.graphics.*;
import android.media.*;
import android.content.*;
import android.content.res.*;
import java.util.*;
import java.io.*;

public class GameResources
{
	private static GameResources instance;
	public static SharedPreferences shp;
	private Context context;
	public Random random;
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
	private int[] sound_id = new int[sound_resid.length];
	private SoundPool sp;

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
	public static final int extra_life = 15;
	public static final int menu_scroll = 16;
	public static final int menu_select = 17;
	
	public static final int wheel_margin = 4;
	public static final int wheel_steps = 9;
	public int holecenter_radius;
	public int[][] holecenters_x;
	public int[][] holecenters_y;

	public int numlevels;
	public Vector<String> boardNames;
	
	public static synchronized GameResources getInstance(Context context) {
		if(instance == null) instance = new GameResources(context);
		return instance;
	}

	private GameResources(Context context) {
		this.context = context;

		getBoardNames();
		numlevels = boardNames.size();

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

		shp = context.getSharedPreferences(
			"org.gignac.jp.pathological.Pathological", Context.MODE_PRIVATE);
		
	}

	public void create(boolean colorblind) {
		if(sp != null) return;

		// Load the sound effects
		sp = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
		for( int i=0; i < sound_resid.length; ++i)
			sound_id[i] = sp.load(context, sound_resid[i], 0);
	}
	
	public void destroy() {
		sp.release();
		sp = null;
	}

	public void play_sound( int id)
	{
		sp.play( sound_id[id], sound_volume[id],
			sound_volume[id], 0, 0, 1.0f);
	}

	public Bitmap loadBitmap( int resid) {
		return BitmapFactory.decodeResource(
			context.getResources(),resid);
	}

	public InputStream openRawResource( int resid) {
		return context.getResources().openRawResource(resid);
	}

	private void getBoardNames()
	{
		boardNames = new Vector<String>();
		BufferedReader f = null;

		try {
			f = new BufferedReader( new InputStreamReader(
				openRawResource( R.raw.all_boards)));
			while( true) {
				String line = f.readLine();
				if( line==null) break;
				if( line.startsWith("name="))
					boardNames.add(line.substring(5));
			}
		} catch(IOException e) {
		} finally {
			try {
				if(f != null) f.close();
			} catch(IOException e) {}
		}
	}
}
