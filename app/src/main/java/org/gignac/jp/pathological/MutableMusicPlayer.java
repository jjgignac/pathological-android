package org.gignac.jp.pathological;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.widget.ImageView;

class MutableMusicPlayer {
    private final Context context;
    private final int resourceId;
    private final ImageView muteButton;
    private static boolean isMuted;
    private MediaPlayer mediaPlayer;

    public MutableMusicPlayer(Context context, int resourceId,
                              ImageView muteButton) {
        this.context = context;
        this.resourceId = resourceId;
        this.muteButton = muteButton;
    }

    public void start() {
        updateMusicState(isMuted);
    }

    public void stop() {
        if( mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void toggleMute() {
        updateMusicState(!isMuted);
    }

    private void updateMusicState(boolean mute) {
        if( mute) {
            if( mediaPlayer != null && !isMuted) {
                mediaPlayer.pause();
            }
            if( Build.VERSION.SDK_INT >= 16) {
                muteButton.setImageAlpha(128);
            } else {
                muteButton.setAlpha(0.5f);
            }
        } else {
            if( mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, resourceId);
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            } else if( isMuted) {
                mediaPlayer.start();
            }
            if( Build.VERSION.SDK_INT >= 16) {
                muteButton.setImageAlpha(255);
            } else {
                muteButton.setAlpha(1f);
            }
        }
        isMuted = mute;
    }
}
