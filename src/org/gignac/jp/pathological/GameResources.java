package org.gignac.jp.pathological;
import android.graphics.*;
import android.media.*;
import android.content.*;
import android.content.res.*;

public class GameResources
{
	private Context context;
	private static final int[] sound_resid = {
		R.raw.filter_admit, R.raw.wheel_turn, R.raw.wheel_completed,
		R.raw.change_color, R.raw.direct_marble, R.raw.ping,
		R.raw.trigger_setup, R.raw.teleport, R.raw.marble_release,
		R.raw.levelfinish, R.raw.die, R.raw.incorrect,
		R.raw.switched, R.raw.shredder, R.raw.replicator,
		R.raw.extra_life, R.raw.menu_scroll, R.raw.switched };
	private static final float[] sound_volume = {
		0.8f, 0.8f, 0.7f, 0.8f, 0.6f, 0.8f, 1.0f, 0.6f, 0.5f,
		0.6f, 1.0f, 0.15f, 1.0f, 1.0f, 1.0f, 1.0f, 0.8f, 1.0f};
	private int[] sound_id;
	private static final int[] marble_resid = {
		R.drawable.marble_0, R.drawable.marble_1,
		R.drawable.marble_2, R.drawable.marble_3,
		R.drawable.marble_4, R.drawable.marble_5,
		R.drawable.marble_6, R.drawable.marble_7,
		R.drawable.marble_8
	};
	private static final int[] marble_resid_cb = {
		R.drawable.marble_0_cb, R.drawable.marble_1_cb,
		R.drawable.marble_2_cb, R.drawable.marble_3_cb,
		R.drawable.marble_4_cb, R.drawable.marble_5_cb,
		R.drawable.marble_6_cb, R.drawable.marble_7_cb,
		R.drawable.marble_8_cb
	};
	public Bitmap[] marble_images;
	private static final int[] tileimg_resid = {
		R.drawable.path_0, R.drawable.path_1, R.drawable.path_2,
		R.drawable.path_3, R.drawable.path_4, R.drawable.path_5,
		R.drawable.path_6, R.drawable.path_7, R.drawable.path_8,
		R.drawable.path_9, R.drawable.path_10, R.drawable.path_11,
		R.drawable.path_12, R.drawable.path_13, R.drawable.path_14,
		R.drawable.path_15
	};
	public Bitmap[] plain_tiles;
	public Bitmap launcher_background;
	public Bitmap launcher_v;
	public Bitmap launcher_corner;
	public Bitmap launcher_entrance;
	private SoundPool sp;

	// Sounds
	public static final int filter_admit = 0;
	public static final int wheel_turn = 1;
	public static final int wheel_completed = 2;
	public static final int change_color = 3;
	public static final int direct_marble = 3;
	public static final int ping = 4;
	public static final int trigger_setup = 5;
	public static final int teleport = 6;
	public static final int marble_release = 7;
	public static final int levelfinish = 8;
	public static final int die = 9;
	public static final int incorrect = 10;
	public static final int switched = 11;
	public static final int shredder = 12;
	public static final int replicator = 13;
	public static final int extra_life = 14;
	public static final int menu_scroll = 15;
	public static final int menu_select = 16;
	
	public GameResources(Context context) {
		this.context = context;
	}
	
	public void create(boolean colorblind) {
		Resources res = context.getResources();

		// Load the marble images
		marble_images = new Bitmap[marble_resid.length];
		for( int i=0; i < marble_resid.length; ++i) {
			marble_images[i] = BitmapFactory.decodeResource( res,
				colorblind ? marble_resid_cb[i] : marble_resid[i]);
		}

		// Load the plain tile images
		plain_tiles = new Bitmap[tileimg_resid.length];
		for( int i=0; i < tileimg_resid.length; ++i) {
			plain_tiles[i] = BitmapFactory.decodeResource(
				res, tileimg_resid[i]);
		}

		launcher_background = BitmapFactory.decodeResource(
			res, R.drawable.launcher);
		launcher_v = BitmapFactory.decodeResource(
			res, R.drawable.launcher_v);
		launcher_corner = BitmapFactory.decodeResource(
			res, R.drawable.launcher_corner);
		launcher_entrance = BitmapFactory.decodeResource(
			res, R.drawable.entrance);
		
		// Load the sound effects
		sp = new SoundPool(6, AudioManager.STREAM_MUSIC, 0);
		sound_id = new int[sound_resid.length];
		for( int i=0; i < sound_resid.length; ++i)
			sound_id[i] = sp.load(context, sound_resid[i], 0);
	}

	public void destroy() {
		sp.release();
		launcher_entrance = null;
		launcher_corner = null;
		launcher_v = null;
		launcher_background = null;
		plain_tiles = null;
		marble_images = null;
	}

	public void play_sound( int id)
	{
		sp.play( sound_id[id], sound_volume[id],
			sound_volume[id], 0, 0, 1.0f);
	}
}
