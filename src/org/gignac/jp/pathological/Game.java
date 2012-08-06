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
	private static final int frameskip = 2;
	public static final int frames_per_sec = 100;

	public int level;
	private Board board;
	private GameResources gr;
	private Ticker ticker;
	private int framenum;
	
	public Game()
	{
	}
	
	@Override
	public void onCreate(Bundle stat)
	{
		super.onCreate(stat);

		gr = new GameResources(this);
		gr.create( true);
		
		setContentView( R.layout.in_game);

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

		playLevel(level);		
	}

	public void playLevel(int level) {
		this.level = level;
		board = new Board(gr, level);
		((TextView)findViewById(R.id.board_name)).setText(board.name);
		board.launch_marble();
		((GameView)findViewById(R.id.gameboard)).setBoard(board);
	}

	public void nextLevel() {
		if(level < gr.numlevels-1)
			playLevel( level+1);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		framenum = frameskip;

		// Schedule the updates
		ticker = new Ticker( new Runnable() {
			public void run() {
				if(--framenum == 0) {
					framenum = frameskip;

					// Render a frame
					synchronized(board) {
						((GameView)findViewById(R.id.gameboard)).requestRender();
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
	
	public void pause() {
		if(ticker.isStopped())
			ticker.go();
		else
			ticker.stop();
	}
}
