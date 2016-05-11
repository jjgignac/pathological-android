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
import android.os.*;

class GameLoop
    implements Runnable
{
    private final Handler handler = new Handler();
    private final Runnable update;
    private final Runnable render;
    private final long delayMillis;
    private long targetTime;
    private boolean rendering = false;

    public GameLoop(Runnable update, Runnable render, int delayMillis) {
        this.update = update;
        this.render = render;
        this.delayMillis = delayMillis;
    }

    public void start() {
        targetTime = SystemClock.uptimeMillis()+delayMillis;
        handler.removeCallbacks(this);
        handler.postAtTime(this, targetTime);
    }

    public void stop() {
        handler.removeCallbacks(this);
    }

    public void run() {
        long curTime = SystemClock.uptimeMillis();
        if(rendering) {
            if( curTime < targetTime) {
                handler.postAtTime(this,targetTime);
                rendering = false;
                return;
            }
            while( SystemClock.uptimeMillis() >= targetTime + 4*delayMillis)
                targetTime += delayMillis;
            while( SystemClock.uptimeMillis() >= targetTime) {
                update.run();
                targetTime += delayMillis;
            }
        }
        update.run();
        targetTime += delayMillis;
        render.run();
        rendering = true;
    }
}
