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
import android.graphics.*;

public class GameView extends GLSurfaceView
{
	private BlitterRenderer renderer;
	private Board board;
	public GL10 blitter_gl;

	public GameView(Context c,AttributeSet a) {
		super(c,a);
		Sprite.setResources(getResources());
		renderer = new BlitterRenderer();
		setRenderer(renderer);
		setRenderMode(RENDERMODE_WHEN_DIRTY);
	}

	public void setBoard(Board board) {
		this.board = board;
		renderer.setPaintable(board);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		final int action = e.getAction();
		final int index =
			(action & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
			MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		final int id = e.getPointerId(index);

		switch(action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			board.downEvent(id,e.getX(index),e.getY(index));
			return true;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			board.upEvent(id,e.getX(index),e.getY(index));
			return true;
		}

		return false;
	}
}
