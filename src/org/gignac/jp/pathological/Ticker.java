package org.gignac.jp.pathological;
import android.os.*;
import android.util.*;

public class Ticker
{
	final Handler handler = new Handler();
	Runnable command;
	long delayMillis;
	long targetTime;
	boolean stop;
	final Runnable runner = new Runnable() {
		public void run() {
			if(stop) return;
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
		stop = false;
		handler.postDelayed(runner, delayMillis);
	}

	public void stop() {
		stop = true;
	}
}
