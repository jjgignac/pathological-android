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

import android.app.*;
import android.net.Uri;
import android.os.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.content.pm.*;
import android.content.*;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

@SuppressWarnings("unused")
public class GameActivity extends Activity
{
    public static final int frames_per_sec = 50;
    private static final int firstAdLevel = 3;

    private final Handler h = new Handler();
    public int level;
    private Board board;
    private GameResources gr;
    private GameLoop gameLoop;
    private GameView gv;
    private View board_timer;
    private TextView score_view;
    public static BitmapBlitter bg;
    private MutableMusicPlayer music;
    private InterstitialAd mLevelFailedInterstitial;
    private AdView mBannerAd;
    private boolean mIsBannerAdLoaded;

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

        gv = (GameView)findViewById(R.id.game_board);
        board_timer = findViewById(R.id.board_timer);
        score_view = (TextView)findViewById(R.id.score);
        score_view.setText("0");

        mBannerAd = (AdView)findViewById(R.id.adView);

        mLevelFailedInterstitial = new InterstitialAd(this);
        mLevelFailedInterstitial.setAdUnitId("ca-app-pub-1344285941475721/4714906695");
        mLevelFailedInterstitial.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewLevelFailedInterstitial();
                playLevel(level);
            }
        });
        requestNewLevelFailedInterstitial();

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

    private void loadAd() {
        if(mIsBannerAdLoaded) return;
        AdRequest adRequest = Util.getAdMobRequest(this);
        if (adRequest != null) {
            // Set the banner to the correct size
            ViewGroup.LayoutParams params = mBannerAd.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            mBannerAd.setLayoutParams(params);

            // Start showing ads
            mBannerAd.loadAd(adRequest);
            mIsBannerAdLoaded = true;
        }
    }

    private void update_board_timer()
    {
        // Draw the board timer
        int timerColor = 0xff4040f0;
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
        GameResources.setCurrentLevel(level);
        loadLevel(level);
        music.resume();
        new ReportStatsTask(this, level).execute();
        if( level >= firstAdLevel) loadAd();
    }

    private void loadLevel(int level) {
        this.level = level;
        board = new Board(gr, gr.sc, level, new Runnable() {
            public void run() {
                h.post(gameLoop);
            }
        });
        final String title = (BuildConfig.BUILD_TYPE.equals("release") ?
                "" : (level+1) + ". ") + board.name;

        h.post( new Runnable() {
            public void run() {
                ((TextView)findViewById(R.id.board_name)).setText(title);
                board.launch_marble();
                gv.setBoard(board);
                gameLoop.start();
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Schedule the updates
        gameLoop.start();

        if(mIsBannerAdLoaded) mBannerAd.resume();
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
        if(mIsBannerAdLoaded) mBannerAd.pause();
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
        mBannerAd.destroy();
    }

    private void requestNewLevelFailedInterstitial() {
        AdRequest adRequest = Util.getAdMobRequest(this);
        if( adRequest != null) {
            mLevelFailedInterstitial.loadAd(adRequest);
        }
    }

    private void onLaunchTimeout()
    {
        gr.play_sound(GameResources.die);

        new ReportStatsTask(this, level, board.score(),
                board.emptyHolePercentage(), board.timeRemainingPercentage(),
                ReportStatsTask.REASON_LAUNCH_TIMEOUT).execute();

        AlertDialog dialog = new AlertDialog.Builder(GameActivity.this)
                .setTitle("Failed")
                .setMessage("The launch timer has expired.")
                .setCancelable(false)
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        retry(null);
                    }
                })
                .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create();
        dialog.getWindow().setWindowAnimations(R.style.dialog_animation);
        dialog.show();

        temporarilyDisableButtons(dialog);
    }

    private void onBoardTimeout()
    {
        gr.play_sound(GameResources.die);

        new ReportStatsTask(this, level, board.score(),
                board.emptyHolePercentage(), board.timeRemainingPercentage(),
                ReportStatsTask.REASON_BOARD_TIMEOUT).execute();

        AlertDialog dialog = new AlertDialog.Builder(GameActivity.this)
                .setTitle("Failed")
                .setMessage("The board timer has expired.")
                .setCancelable(false)
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        retry(null);
                    }
                })
                .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create();
        dialog.getWindow().setWindowAnimations(R.style.dialog_animation);
        dialog.show();

        temporarilyDisableButtons(dialog);
    }

    private void onBoardComplete()
    {
        gr.play_sound(GameResources.levelfinish);
        gameLoop.stop();

        // Calculate the final score
        int score = board.score();
        int emptyHolePercentage = board.emptyHolePercentage();
        int emptyHoleBonus = emptyHolePercentage * 2;
        int timeRemainingPercentage = board.timeRemainingPercentage();
        int timeRemainingBonus = timeRemainingPercentage * 5;
        int total = score + emptyHoleBonus + timeRemainingBonus;

        new ReportStatsTask(this, level, score,
                emptyHolePercentage, timeRemainingPercentage,
                ReportStatsTask.REASON_COMPLETED).execute();

        int prevBest = GameResources.bestScore(level);
        if( total > prevBest) gr.setBestScore(level, total);

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

        if( prevBest >= 0) {
            view.findViewById(R.id.prev_best_row).setVisibility(View.VISIBLE);
            ((TextView)view.findViewById(R.id.prev_best)).setText(String.valueOf(prevBest));
        }

        if( total > prevBest) {
            view.findViewById(R.id.new_best).setVisibility(View.VISIBLE);
        }

        if( total >= board.twoStarThreshold) {
            ((ImageView)view.findViewById(R.id.star2)).setImageResource(R.drawable.star);
        }
        if( total >= board.threeStarThreshold) {
            ((ImageView)view.findViewById(R.id.star3)).setImageResource(R.drawable.star);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.level_cleared)
                .setView(view)
                .setCancelable(false)
                .setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GameResources.setCurrentLevel(Math.max(level, gr.nextLevel(level)));
                        if(gr.boardInfo(level).name.equals("The Auto-Solver") &&
                            GameResources.ratePromptCount() < 2) {
                            showRateDialog(false);
                        } else {
                            finish();
                        }
                    }
                })
                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        playLevel(level);
                    }
                });
        if(gr.nextLevel(level) != -1) {
            builder.setNeutralButton(R.string.next_level, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(gr.boardInfo(level).name.equals("It's All in the Wrist") &&
                            GameResources.ratePromptCount() < 1) {
                        GameResources.setCurrentLevel(gr.nextLevel(level));
                        showRateDialog(true);
                    } else {
                        playLevel(gr.nextLevel(level));
                    }
                }
            });
        }
        AlertDialog dialog = builder.create();
        dialog.getWindow().setWindowAnimations(R.style.dialog_animation);
        dialog.show();

        temporarilyDisableButtons(dialog);
    }

    private void showRateDialog(final boolean play_next) {
        final int ratePromptCount = GameResources.ratePromptCount();
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.rate_this_app)
                .setMessage(R.string.rate_this_app_message)
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        GameResources.setRatePromptCount(ratePromptCount+1);
                        if(play_next) playLevel(gr.nextLevel(level));
                        else finish();
                    }
                })
                .setNegativeButton(R.string.no_thanks, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GameResources.setRatePromptCount(100);
                        if(play_next) playLevel(gr.nextLevel(level));
                        else finish();
                    }
                })
                .setPositiveButton(R.string.rate_it_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GameResources.setRatePromptCount(100);
                        finish();
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + getPackageName())));
                    }
                });
        if(ratePromptCount == 0) {
            builder.setNeutralButton(R.string.remind_me_later, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    GameResources.setRatePromptCount(ratePromptCount+1);
                    if(play_next) playLevel(gr.nextLevel(level));
                    else finish();
                }
            });
        }
        AlertDialog dialog = builder.create();
        dialog.getWindow().setWindowAnimations(R.style.dialog_animation);
        dialog.show();
    }

    private void temporarilyDisableButtons(final AlertDialog dialog) {
        final int[] buttons = {
                AlertDialog.BUTTON_POSITIVE,
                AlertDialog.BUTTON_NEGATIVE,
                AlertDialog.BUTTON_NEUTRAL};
        for( int id : buttons) {
            Button b = dialog.getButton(id);
            if( b != null) b.setEnabled(false);
        }

        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if( !dialog.isShowing()) return;
                for( int id : buttons) {
                    Button b = dialog.getButton(id);
                    if( b != null) b.setEnabled(true);
                }
            }
        }, 1500);
    }

    public void pause() {
        music.pause();
        board.setPaused(true);
    }

    public void resume() {
        music.resume();
        board.setPaused(false);
    }

    public void togglePause(View v) {
        if( board.isPaused()) resume();
        else pause();
    }

    public void retry(View v) {
        if( level > 5 && mLevelFailedInterstitial.isLoaded() &&
                MainActivity.lastInterstitialTime <
                        System.currentTimeMillis() - MainActivity.minInterstitialDelay) {
            pause();
            mLevelFailedInterstitial.show();
            MainActivity.lastInterstitialTime = System.currentTimeMillis();
        } else {
            playLevel(level);
        }
    }

    public void toggleMusic(View v) {
        music.toggleMute();
    }
}
