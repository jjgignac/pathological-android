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

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.widget.ImageView;

class MutableMusicPlayer {
    private final Context context;
    private final int resourceId;
    private final ImageView muteButton;
    static boolean isMuted;
    private boolean isPaused;
    private MediaPlayer mediaPlayer;

    public MutableMusicPlayer(Context context, int resourceId,
                              ImageView muteButton) {
        this.context = context;
        this.resourceId = resourceId;
        this.muteButton = muteButton;
        this.isPaused = false;
    }

    public void start() {
        updateMusicState(isMuted, isPaused);
    }

    public void stop() {
        if( mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void toggleMute() {
        updateMusicState(!isMuted, isPaused);
    }

    public void pause() {
        updateMusicState(isMuted, true);
    }

    public void resume() {
        updateMusicState(isMuted, false);
    }

    private void updateMusicState(boolean mute, boolean pause) {
        if( mute || pause) {
            if( mediaPlayer != null && !(isMuted || isPaused)) {
                mediaPlayer.pause();
            }
        } else {
            if( mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, resourceId);
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            } else if( isMuted || isPaused) {
                mediaPlayer.start();
            }
        }
        if( mute) {
            if( Build.VERSION.SDK_INT >= 16) {
                muteButton.setImageAlpha(128);
            } else {
                muteButton.setAlpha(0.5f);
            }
        } else {
            if( Build.VERSION.SDK_INT >= 16) {
                muteButton.setImageAlpha(255);
            } else {
                muteButton.setAlpha(1f);
            }
        }

        if( isMuted != mute) {
            // Save muted state
            SharedPreferences shp = context.getSharedPreferences(
                    "org.gignac.jp.pathological.Pathological", Context.MODE_PRIVATE);
            SharedPreferences.Editor e = shp.edit();
            e.putBoolean("mute", mute);
            e.apply();

            isMuted = mute;
        }

        isPaused = pause;
    }
}
