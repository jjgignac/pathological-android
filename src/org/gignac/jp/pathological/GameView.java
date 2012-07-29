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
	public float scale = 1.3f;
	public float offsetx, offsety;

	public GameView(Context c,AttributeSet a) {
		super(c,a);
		Sprite.setResources(getResources());
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
				(int)Math.round((e.getX()-offsetx)/scale),
				(int)Math.round((e.getY()-offsety)/scale));
			return true;
		case MotionEvent.ACTION_UP:
			game.upEvent(
				(int)Math.round((e.getX()-offsetx)/scale),
				(int)Math.round((e.getY()-offsety)/scale));
			return true;
		}

		return false;
	}

	@Override
	protected void onSizeChanged(int w,int h,int oldw,int oldh)
	{
		scale = w * Board.screen_height < h * Board.screen_width ?
			(float)w / Board.screen_width : (float)h / Board.screen_height;
		offsetx = (w-Board.screen_width*scale)*0.5f;
		offsety = (h-Board.screen_height*scale)*0.5f;
	}

	public void blit(int b, int x, int y)
	{
		renderer.blit(blitter_gl,b,
			x*scale+offsetx,y*scale+offsety,scale);
	}

	public void blit(int b, int x, int y, int w, int h)
	{
		renderer.blit(blitter_gl,b,
			x*scale+offsetx,y*scale+offsety,w*scale,h*scale);
	}
}
