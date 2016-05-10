package org.gignac.jp.pathological;

import android.app.*;
import android.os.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.content.pm.*;
import android.content.*;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

@SuppressWarnings("unused")
public class GameActivity extends Activity
{
    public static final int frames_per_sec = 50;

    private final Handler h = new Handler();
    public int level;
    private Board board;
    private GameResources gr;
    private GameLoop gameLoop;
    private GameView gv;
    private View board_timer;
    private TextView score_view;
    private ActionListener bl;
    public static BitmapBlitter bg;
    private MutableMusicPlayer music;

    public GameActivity()
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
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

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
        board_timer = findViewById(R.id.board_timer);
        score_view = (TextView)findViewById(R.id.score);
        score_view.setText("0");

        AdView mAdView = (AdView)findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mAdView.loadAd(adRequest);

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
                int status = board.update();
                update_board_timer();
                score_view.setText(String.valueOf(board.score()));
                switch(status) {
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

        music = new MutableMusicPlayer(this, R.raw.background,
                (ImageView)findViewById(R.id.mute_music));
        music.start();

        playLevel(level);
    }

    private void update_board_timer()
    {
        // Draw the board timer
        int timerColor = 0xff000080;
        float timeLeft = (float)board.board_timeout / frames_per_sec;
        if( timeLeft < 60f && board.board_timeout*2 < board.board_timeout_start) {
            // Make the timer flash to indicate that time
            // is running out.
            float s = (float)Math.sin(timeLeft*3);
            int phase = Math.round(s*s*255);
            timerColor = 0xff000000 | phase | (255-phase)<<16;
        }

        int x = Math.round((float)board.board_timeout * gv.getWidth() /
                board.board_timeout_start);
        ViewGroup.LayoutParams params = board_timer.getLayoutParams();
        params.width = x;
        board_timer.setLayoutParams(params);
        board_timer.setBackgroundColor(timerColor);
    }

    public void playLevel(final int level) {
        loadLevel(level);
        music.resume();
    }

    private void loadLevel(int level) {
        this.level = level;
        board = new Board(gr, gr.sc, level, new Runnable() {
            public void run() {
                h.post(gameLoop);
            }
        });

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
    protected void onStop() {
        super.onStop();
        pause();
    }

    @Override
    public void finish() {
        music.stop();
        super.finish();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        gr.destroy();
        music.stop();
    }

    private void onLaunchTimeout()
    {
        gr.play_sound(GameResources.die);
        AlertDialog.Builder b = new AlertDialog.Builder(GameActivity.this);
        b.setTitle("Failed").
            setMessage("The launch timer has expired.").
            setCancelable(false).
            setPositiveButton("Retry", bl).
            setNegativeButton("Quit", bl).show();
    }

    private void onBoardTimeout()
    {
        gr.play_sound(GameResources.die);
        AlertDialog.Builder b = new AlertDialog.Builder(GameActivity.this);
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

        // Calculate the final score
        int score = board.score();
        int emptyHolePercentage = board.emptyHolePercentage();
        int emptyHoleBonus = emptyHolePercentage * 2;
        int timeRemainingPercentage = board.timeRemainingPercentage();
        int timeRemainingBonus = timeRemainingPercentage * 5;
        int total = score + emptyHoleBonus + timeRemainingBonus;

        View view = getLayoutInflater().inflate( R.layout.level_cleared,
                (ViewGroup)gv.getRootView(), false);
        ((TextView)view.findViewById(R.id.score))
                .setText(String.valueOf(score));
        ((TextView)view.findViewById(R.id.empty_hole_bonus_text))
                .setText(getString(R.string.empty_hole_bonus, emptyHolePercentage));
        ((TextView)view.findViewById(R.id.empty_hole_bonus))
                .setText(String.valueOf(emptyHoleBonus));
        ((TextView)view.findViewById(R.id.time_remaining_bonus_text))
                .setText(getString(R.string.time_remaining_bonus, timeRemainingPercentage));
        ((TextView)view.findViewById(R.id.time_remaining_bonus))
                .setText(String.valueOf(timeRemainingBonus));
        ((TextView)view.findViewById(R.id.total))
                .setText(String.valueOf(total));

        new AlertDialog.Builder(this)
                .setTitle(R.string.level_cleared)
                .setView(view)
                .setNegativeButton(R.string.quit, bl)
                .setPositiveButton(R.string.retry, bl)
                .setNeutralButton(R.string.next_level, bl).show();
    }

    public void pause() {
        music.pause();
        board.setPaused(true);
    }

    public void resume() {
        music.resume();
        board.setPaused(false);
    }

    public void togglePause() {
        if( board.isPaused()) resume();
        else pause();
    }

    public void toggleMusic(View v) {
        music.toggleMute();
    }
}
