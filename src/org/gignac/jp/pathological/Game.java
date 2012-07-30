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
import javax.microedition.khronos.opengles.*;

public class Game extends Activity
{
	private static final int frameskip = 2;
	public static final int frames_per_sec = 100;

	public int numlevels;
	public int level;
	public long score;
	private Board board;
	private GameResources gr;
	private Ticker ticker;
	private GameView view;
	private int framenum;
	
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
		} else {
			// Begin a new game
			level = 0;
			score = 0;
		}

		ButtonListener bl = new ButtonListener(this);
		((Button)findViewById(R.id.prevlevel)).setOnClickListener(bl);
		((Button)findViewById(R.id.nextlevel)).setOnClickListener(bl);
		((Button)findViewById(R.id.quit)).setOnClickListener(bl);
		((Button)findViewById(R.id.pause)).setOnClickListener(bl);
		((Button)findViewById(R.id.volume)).setOnClickListener(bl);
		((Button)findViewById(R.id.retry)).setOnClickListener(bl);

		playLevel(level);		
	}

	public void playLevel(int level) {
		this.level = level;
		board = new Board( this, gr);
		((TextView)findViewById(R.id.board_name)).setText(board.name);
		board.launch_marble();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		view = (GameView)findViewById(R.id.gameboard);
		view.setup(this);
		
		framenum = frameskip;

		// Schedule the updates
		ticker = new Ticker( new Runnable() {
			public void run() {
				if(--framenum == 0) {
					framenum = frameskip;

					// Render a frame
					synchronized(board) {
						view.requestRender();
						try { board.wait(); }
						catch(InterruptedException e) {}
					}
				}				
				board.update();
			}
		}, 1000 / frames_per_sec);		
	}

	@Override
	protected void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);
		out.putInt("level",level);
		out.putLong("score",score);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		ticker.stop();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		gr.destroy();
	}
	
	public void paint(GL10 gl) {
		view.blitter_gl = gl;
		board.paint(view);
	}

	public void downEvent( int x, int y) {
		board.downEvent(x,y);
	}

	public void upEvent( int x, int y) {
		board.upEvent(x,y);
	}

	public void increase_score(int amount) {
		// Add the amount to the score
		score += amount;
	}
}
