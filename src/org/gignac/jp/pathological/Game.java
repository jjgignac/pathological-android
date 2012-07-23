package org.gignac.jp.pathological;
import java.io.*;
import android.graphics.*;
import android.app.*;
import android.os.*;
import android.media.*;
import android.content.res.*;
import java.util.*;
import android.view.*;
import java.util.concurrent.*;
import android.widget.*;

public class Game extends Activity
{
	public static final int initial_lives = 3;
	public static final int max_spare_lives = 10;
	public static final int extra_life_frequency = 5000;
	public static final int timer_width = 36;
	public static final int screen_width = 800;
	public static final int screen_height = 600;
	
	public int numlevels;
	public int level;
	public int lives;
	public long score;
	private Board board;
	private GameResources gr;
	private Ticker ticker;
	
	public Game()
	{
	}
	
	@Override
	public void onCreate(Bundle stat)
	{
		super.onCreate(stat);
		Resources res = getResources();
		try {
			// Count the number of levels
			BufferedReader f = new BufferedReader( new InputStreamReader(
				res.openRawResource(R.raw.all_boards)));
			int j = 0;
			while(true) {
				String line = f.readLine();
				if( line == null) break;
				if( line.isEmpty()) continue;
				if( line.charAt(0) == '|') j += 1;
			}
			f.close();
			numlevels = j / Board.vert_tiles;
		} catch(IOException e) {}

		gr = new GameResources(this);
		gr.create( true);
		
		setContentView( R.layout.in_game);

		if( stat != null) {
			// Restore the game state
			level = stat.getInt("level");
			score = stat.getLong("score");
			lives = stat.getInt("lives");
		} else {
			// Begin a new game
			level = 0;
			score = 0;
			lives = initial_lives;
		}

		board = new Board( this, gr, timer_width,
			Board.info_height + Marble.marble_size);

		board.launch_marble();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		final SurfaceView g = (SurfaceView)findViewById(R.id.gameboard);
		g.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent e)
			{
				if( e.getAction() == e.ACTION_DOWN) {
					board.click(
						(int)Math.round(e.getX()),
						(int)Math.round(e.getY()));
				}
				return false;
			}
		});

		// Schedule the updates
		ticker = new Ticker( new Runnable() {
			public void run() {
				board.update();
				SurfaceHolder h = g.getHolder();
				Canvas c = h.lockCanvas();
				board.paint(c);
				h.unlockCanvasAndPost(c);
			}
		}, 1000 / Board.frames_per_sec);		
	}

	@Override
	protected void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);
/*		out.putInt("level",level);
		out.putLong("score",score);
		out.putInt("lives",lives);*/
	}

	@Override
	protected void onPause()
	{
		super.onPause();
//		ticker.stop();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
//		gr.destroy();
	}

	public void increase_score(int amount) {
		// Add the amount to the score
		score += amount;

		// Award any extra lives that are due
		int extra_lives = amount / extra_life_frequency +
			((score % extra_life_frequency < amount % extra_life_frequency)
			? 1 : 0);
		extra_lives = Math.min( extra_lives, max_spare_lives+1 - lives);
		if( extra_lives > 0) {
			lives += extra_lives;
			gr.play_sound( gr.extra_life);
		}
	}
}
