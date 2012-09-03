package org.gignac.jp.pathological;

import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.content.*;
import android.util.*;

public class GameView extends GLSurfaceView
{
	private BlitterRenderer renderer;
	private Board board;
	public SpriteCache sc;

	public GameView(Context c,AttributeSet a) {
		super(c,a);
		sc = new SpriteCache(getResources());
		renderer = new BlitterRenderer(sc);
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
