package org.gignac.jp.pathological;
import android.view.*;
import android.content.*;
import android.util.*;
import android.graphics.*;

public class LevelSelectView extends View
{
	private GameResources gr;
	private final Rect r = new Rect(0,0,Board.board_width/3,Board.board_height/3);
	private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
	private GestureDetector g;
	private float xOffset = 0.0f;

	public LevelSelectView( Context context, AttributeSet a)
	{
		super(context,a);
	}

	public void setup(GameResources gr) {
		this.gr = gr;
		g = new GestureDetector(new LevelSelectGestureListener(this));
	}

	@Override
	public void onDraw( Canvas c) {
		c.translate( -xOffset, 0.0f);
		c.drawBitmap( Preview.create(gr,7,0.5f), null, r, paint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		return g.onTouchEvent(e);
	}

	private void scroll( float dx)
	{
		xOffset += dx;
		invalidate();
	}

	private void tap( float x, float y)
	{
	}

	private void fling( float velX)
	{
	}

	private class LevelSelectGestureListener
		extends GestureDetector.SimpleOnGestureListener
	{
		LevelSelectView view;
		int minFlingDistance;
		int minFlingSpeed;
		int maxFlingSpeed;

		LevelSelectGestureListener( LevelSelectView view)
		{
			this.view = view;
			final ViewConfiguration vc = ViewConfiguration.get(view.getContext());
			minFlingDistance = vc.getScaledTouchSlop();
			minFlingSpeed = vc.getScaledMinimumFlingVelocity();
			maxFlingSpeed = vc.getScaledMaximumFlingVelocity();
		}

		@Override
		public boolean onDown( MotionEvent e) {
			return true;
		}

		@Override
		public boolean onScroll( MotionEvent e1,
			MotionEvent e2, float dx, float dy)
		{
			view.scroll(dx);
			return true;
		}
		
		@Override
		public boolean onFling( MotionEvent e1, MotionEvent e2,
			float velX, float velY)
		{
			if (Math.abs(e1.getY() - e2.getY()) >
				Math.abs(e1.getX() - e2.getX()) ||
				Math.abs(e1.getX() - e2.getX()) < minFlingDistance ||
				Math.abs(velX) < minFlingSpeed ||
				Math.abs(velX) > maxFlingSpeed) return false;
			view.fling(velX);
			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e)
		{
			view.tap(e.getX(), e.getY());
			return true;
		}
	}
}
