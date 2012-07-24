package org.gignac.jp.pathological;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.content.*;
import javax.microedition.khronos.opengles.*;
import javax.microedition.khronos.egl.*;
import android.util.*;

public class GameView extends GLSurfaceView
	implements Blitter
{
	private BoardRenderer renderer;
	private Game game;
	public GL10 blitter_gl;

	public GameView(Context c,AttributeSet a) {
		super(c,a);
	}

	public void setup(Game game) {
		this.game = game;
		renderer = new BoardRenderer(game);
		setRenderer(renderer);
		setRenderMode(RENDERMODE_WHEN_DIRTY);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		switch(e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			game.downEvent(
				(int)Math.round(e.getX()),
				(int)Math.round(e.getY()));
			return true;
		case MotionEvent.ACTION_UP:
			game.upEvent(
				(int)Math.round(e.getX()),
				(int)Math.round(e.getY()));
			return true;
		}

		return false;
	}

	public void blit(Bitmap b, int x, int y)
	{
		renderer.blit(blitter_gl,b,x,y,b.getWidth(),b.getHeight());
	}

	public void blit(Bitmap b, int x, int y, int w, int h)
	{
		renderer.blit(blitter_gl,b,x,y,w,h);
	}
}
