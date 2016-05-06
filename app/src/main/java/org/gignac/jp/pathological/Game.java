package org.gignac.jp.pathological;

import android.app.*;
import android.os.*;
import android.widget.*;
import android.content.pm.*;
import android.content.*;

@SuppressWarnings("unused")
public class Game extends Activity
{
    public static final int frames_per_sec = 50;

    private final Handler h = new Handler();
    public int level;
    private Board board;
    private GameResources gr;
    private GameLoop gameLoop;
    private GameView gv;
    private ActionListener bl;
    public static BitmapBlitter bg;

    public Game()
    {
    }

    @Override
    public void onCreate(Bundle stat)
    {
        super.onCreate(stat);

        gr = GameResources.getInstance(this);
        gr.create();

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

        bl = new ActionListener(this);
        findViewById(R.id.pause).setOnClickListener(bl);
        findViewById(R.id.retry).setOnClickListener(bl);

        gv = (GameView)findViewById(R.id.game_board);

        Runnable update = new Runnable() {
            public void run() {
                if(board == null) return;
                if(board.delay>20) {
                    --board.delay;
                    return;
                }
                if(board.delay>0) {
                    --board.delay;
                    if((board.delay&1) != 0) return;
                }
                switch(board.update()) {
                case Board.LAUNCH_TIMEOUT:
                    onLaunchTimeout();
                    break;
                case Board.BOARD_TIMEOUT:
                    onBoardTimeout();
                    break;
                case Board.COMPLETE:
                    onBoardComplete();
                    break;
                }
            }
        };
        Runnable render = new Runnable() {
            public void run() {
                gv.invalidate();
            }
        };
        gameLoop = new GameLoop( update, render, 1000 / frames_per_sec);

        playLevel(level);
    }

    public void playLevel(final int level) {
        loadLevel(level);
    }

    private void loadLevel(int level) {
        this.level = level;
        board = new Board(gr, gr.sc, level, new Runnable() {
            public void run() {
                h.post(gameLoop);
            }
        }, true);

        h.post( new Runnable() {
            public void run() {
                ((TextView)findViewById(R.id.board_name)).setText(board.name);
                board.launch_marble();
                gv.setBoard(board);
                gameLoop.start();
            }
        });
    }

    public void nextLevel() {
        if(level < gr.numlevels-1)
            playLevel( level+1);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Schedule the updates
        gameLoop.start();
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
    public void onWindowFocusChanged(boolean hasFocus)
    {
        if(!hasFocus) board.setPaused(true);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        gr.destroy();
    }

    private void onLaunchTimeout()
    {
        gr.play_sound(GameResources.die);
        AlertDialog.Builder b = new AlertDialog.Builder(Game.this);
        b.setTitle("Failed").
            setMessage("The launch timer has expired.").
            setCancelable(false).
            setPositiveButton("Retry", bl).
            setNegativeButton("Quit", bl).show();
    }

    private void onBoardTimeout()
    {
        gr.play_sound(GameResources.die);
        AlertDialog.Builder b = new AlertDialog.Builder(Game.this);
        b.setTitle("Failed").
            setMessage("The board timer has expired.").
            setCancelable(false).
            setPositiveButton("Retry", bl).
            setNegativeButton("Quit", bl).show();
    }

    private void onBoardComplete()
    {
        gr.play_sound(GameResources.levelfinish);
        gameLoop.stop();
        if(level >= GameResources.shp.getInt("nUnlocked",1)-1
            && level < gr.numlevels - 1) {
            SharedPreferences.Editor e = GameResources.shp.edit();
            e.putInt("nUnlocked",level+2);
            e.apply();
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Level Complete!").
            setMessage("Bonus for 50% time remaining: 100\n"+
                "Bonus for 25% empty holes: 50\n").
            setCancelable(false).
            setPositiveButton("Retry", bl).
            setNeutralButton("Continue", bl).show();
    }

    public void pause() {
        board.setPaused(!board.isPaused());
    }
}
