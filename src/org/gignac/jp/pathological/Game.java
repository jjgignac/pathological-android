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
import android.content.pm.*;

public class Game extends Activity
{
	public static final int frames_per_sec = 50;

	public int level;
	private Board board;
	private GameResources gr;
	private GameLoop gameLoop;
	private GameView gv;
	
	public Game()
	{
	}
	
	@Override
	public void onCreate(Bundle stat)
	{
		super.onCreate(stat);

		gr = GameResources.getInstance(this);
		gr.create( true);

		overridePendingTransition(R.anim.begin, R.anim.fadeout);
		setContentView( R.layout.in_game);
		setRequestedOrientation(
			ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

		if( stat != null) {
			// Restore the game state
			level = stat.getInt("level");
		} else {
			// Begin a new game
			Bundle extras = getIntent().getExtras();
			level = extras.getInt("level");
		}

		ButtonListener bl = new ButtonListener(this);
		((Button)findViewById(R.id.prevlevel)).setOnClickListener(bl);
		((Button)findViewById(R.id.nextlevel)).setOnClickListener(bl);
		((Button)findViewById(R.id.quit)).setOnClickListener(bl);
		((Button)findViewById(R.id.pause)).setOnClickListener(bl);
		((Button)findViewById(R.id.volume)).setOnClickListener(bl);
		((Button)findViewById(R.id.retry)).setOnClickListener(bl);

		gv = (GameView)findViewById(R.id.gameboard);

		playLevel(level);		
	}

	public void playLevel(int level) {
		// If applicable, synchronize on the previous board
		// to ensure that we don't load new sprites while
		// rendering is in progress.
		synchronized(board==null?this:board) {
			loadLevel(level);
		}
	}

	private void loadLevel(int level) {
		this.level = level;
		board = new Board(gr, gv.sc, level, new Runnable() {
			public void run() {
				gv.getHandler().post(gameLoop);
			}
		});
		((TextView)findViewById(R.id.board_name)).setText(board.name);
		board.launch_marble();
		gv.setBoard(board);
	}

	public void nextLevel() {
		if(level < gr.numlevels-1)
			playLevel( level+1);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		Runnable update = new Runnable() {
			public void run() {
				board.update();
			}
		};
		Runnable render = new Runnable() {
			public void run() {
				gv.requestRender();
			}
		};

		// Schedule the updates
		gameLoop = new GameLoop( update, render, 1000 / frames_per_sec);
	}

	@Override
	protected void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);
		out.putInt("level",level);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		gameLoop.stop();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		gr.destroy();
	}
	
	public void pause() {
		if(gameLoop.isStopped())
			gameLoop.go();
		else
			gameLoop.stop();
	}
}
