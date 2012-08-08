package org.gignac.jp.pathological;
import android.os.*;
import android.util.*;

public class GameLoop
	implements Runnable
{
	final Handler handler = new Handler();
	private Runnable update;
	private Runnable render;
	private long delayMillis;
	private long targetTime;
	private boolean stop;
	private boolean stopped = true;
	private boolean rendering = false;

	public GameLoop(Runnable update, Runnable render, int delayMillis) {
		this.update = update;
		this.render = render;
		this.delayMillis = delayMillis;
		go();
	}

	public void stop() {
		stop = true;
	}

	public void go() {
		targetTime = SystemClock.uptimeMillis()+delayMillis;
		stop = false;
		if(stopped) {
			stopped = false;
			handler.postAtTime(this, targetTime);
		}
	}

	public boolean isStopped() {
		return stop;
	}

	public void run() {
		if(stop) {
			stopped = true;
			return;
		}
		long curTime = SystemClock.uptimeMillis();
		if(rendering) {
			if( curTime < targetTime) {
				handler.postAtTime(this,targetTime);
				rendering = false;
				return;
			}
			
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
