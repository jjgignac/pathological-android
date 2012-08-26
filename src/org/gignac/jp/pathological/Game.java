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
import android.content.*;

public class Game extends Activity
{
	public static final int frames_per_sec = 50;

	public int level;
	private Board board;
	private GameResources gr;
	private GameLoop gameLoop;
	private GameView gv;
	private DialogInterface.OnClickListener clicker;
	
	public Game()
	{
		// Button listener for failure dialogs
		clicker = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface p1, int p2)
			{
				switch(p2) {
				case DialogInterface.BUTTON_POSITIVE:
					playLevel(level);
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					finish();
					break;
				}
			}
		};
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
		gv.onResume();

		Runnable update = new Runnable() {
			public void run() {
				switch(board.update()) {
				case Board.LAUNCH_TIMEOUT:
					onLaunchTimeout();
					break;
				case Board.BOARD_TIMEOUT:
					onBoardTimeout();
					break;
				case Board.COMPLETE:
					gr.play_sound(gr.levelfinish);
					gameLoop.stop();
					if(level == gr.shp.getInt("nUnlocked",1)-1) {
						SharedPreferences.Editor e = gr.shp.edit();
						e.putInt("nUnlocked",level+2);
						e.apply();
					}
					break;
				}
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
		gv.onPause();
		gameLoop.stop();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		gr.destroy();
	}

	private void onLaunchTimeout()
	{
		gr.play_sound(gr.die);
		AlertDialog.Builder b = new AlertDialog.Builder(Game.this);
		b.setTitle("Failed").
			setMessage("The launch timer has expired.").
			setPositiveButton("Retry", clicker).
			setNegativeButton("Quit", clicker).show();
	}

	private void onBoardTimeout()
	{
		gr.play_sound(gr.die);
		AlertDialog.Builder b = new AlertDialog.Builder(Game.this);
		b.setTitle("Failed").
			setMessage("The board timer has expired.").
			setPositiveButton("Retry", clicker).
			setNegativeButton("Quit", clicker).show();
	}

	public void pause() {
		board.setPaused(!board.isPaused());
	}
}
