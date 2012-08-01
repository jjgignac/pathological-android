package org.gignac.jp.pathological;
import android.os.*;
import android.util.*;

public class Ticker
{
	final Handler handler = new Handler();
	private Runnable command;
	private long delayMillis;
	private long targetTime;
	private boolean stop;
	private boolean stopped = true;
	final Runnable runner = new Runnable() {
		public void run() {
			if(stop) {
				stopped = true;
				return;
			}
			command.run();
			long curTime = SystemClock.uptimeMillis();
			if(curTime < targetTime) targetTime = curTime;
			targetTime += delayMillis;
			if(curTime > targetTime) targetTime = curTime;
			handler.postAtTime(this,targetTime);
		}
	};

	public Ticker(Runnable command, long delayMillis) {
		this.command = command;
		this.delayMillis = delayMillis;
		this.targetTime = SystemClock.uptimeMillis();
		go();
	}

	public void stop() {
		stop = true;
	}

	public void go() {
		stop = false;
		if(stopped) {
			stopped = false;
			handler.postDelayed(runner, delayMillis);
		}
	}

	public boolean isStopped() {
		return stop;
	}
}
