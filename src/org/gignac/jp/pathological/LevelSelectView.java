package org.gignac.jp.pathological;
import android.view.*;
import android.content.*;
import android.util.*;
import android.graphics.*;
import android.opengl.*;
import android.os.*;
import android.widget.*;

public class LevelSelectView extends GLSurfaceView
	implements Paintable
{
	private static final int rows = 2;
	private static final int cols = 3;
	private static final float previewScale = 0.28f;
	private static final int hmargin = Board.board_width/4;
	private static final int vmargin = Board.board_width/4;
	
	private GameResources gr;
	private BlitterRenderer renderer;
	private GestureDetector g;
	private float xOffset = 0.0f;
	private Bitmap[] preview;
	private float vel;
	private long prevTime;
	private int width, height;

	public LevelSelectView( Context context, AttributeSet a)
	{
		super(context,a);
		renderer = new BlitterRenderer();
		setRenderer(renderer);
		setRenderMode(RENDERMODE_CONTINUOUSLY);
	}

	public void setup(GameResources gr) {
		this.gr = gr;
		g = new GestureDetector(new LevelSelectGestureListener(this));
		renderer.setPaintable(this);
		preview = new Bitmap[gr.numlevels];
		for( int i=0; i < gr.numlevels; ++i)
			Sprite.cache(0x200000000l+i, Preview.create(gr,i,0.5f));
		Sprite.cache(R.drawable.intro);
	}

	private void update()
	{
		final long time = SystemClock.uptimeMillis();
		final long dt = time-prevTime;
		prevTime = time;

		int npages = (gr.numlevels + rows*cols - 1) / (rows*cols);
		float pos = xOffset / width;
		float prevPos = pos;
		int a = Math.round(pos);
		if( pos < 0) a = 0;
		else if( pos > npages-1) a = npages-1;
		pos += dt * vel / width;
		if( (prevPos < a) ^ (pos < a)) {
			vel = 0f;
			pos = a;
			setRenderMode(RENDERMODE_WHEN_DIRTY);
		}
		if( pos < -0.1f) pos = -0.1f;
		else if( pos > npages-1+0.1f) pos = npages-1+0.1f;
		xOffset = pos * width;

		if( pos < 0) vel = 0.005f * 300;
		else if( pos > npages-1) vel = -0.005f * 300;
		else vel += dt * (a-pos) * 0.005f;
	}

	@Override
	public synchronized void paint( Blitter b)
	{
		Rect v = b.getVisibleArea();
		width = v.right - v.left;
		height = v.bottom - v.top;
		update();

		int npages = (gr.numlevels + rows*cols - 1) / (rows*cols);
		b.blit(R.drawable.intro, 0, 0, width, height);
		b.transform( 1f, -xOffset, 0f);

		int previewWidth = Math.round(Board.screen_width * previewScale);
		int previewHeight = Math.round(Board.screen_height * previewScale);
		int hSpacing = (width - 2*hmargin - cols*previewWidth) / (cols-1) + previewWidth;
		int vSpacing = (height - 2*vmargin - rows*previewHeight) / (rows-1) + previewHeight;
		
		for( int page=0; page < npages; ++page) {
			for( int j=0; j < rows; ++j) {
				for( int i=0; i < cols; ++i) {
					int level = (page*rows+j)*cols+i;
					if(level >= gr.numlevels) continue;
					b.blit( 0x200000000l+level, page*width+hmargin+i*hSpacing, vmargin+j*vSpacing,
						previewWidth, previewHeight);
				}
			}
		}
		notify();
	}

	@Override
	public synchronized boolean onTouchEvent(MotionEvent e) {
		setRenderMode(RENDERMODE_WHEN_DIRTY);
		vel = 0f;
		switch(e.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_UP:
			prevTime = SystemClock.uptimeMillis();
			setRenderMode(RENDERMODE_CONTINUOUSLY);
			break;
		}
		return g.onTouchEvent(e);
	}

	private void scroll( float dx)
	{
		xOffset += dx;
		requestRender();
		try {
			wait();
		} catch( InterruptedException e) {}
	}

	private void tap( float x, float y)
	{
		int previewWidth = Math.round(Board.screen_width * previewScale);
		int previewHeight = Math.round(Board.screen_height * previewScale);
		int hSpacing = (width - 2*hmargin - cols*previewWidth) / (cols-1) + previewWidth;
		int vSpacing = (height - 2*vmargin - rows*previewHeight) / (rows-1) + previewHeight;
		x += xOffset;
		int page = (int)Math.floor(x / width);
		x -= page * width;
		y -= vmargin;
		int j = (int)Math.floor(y / vSpacing);
		if( j < 0 || j >= rows) return;
		if( y - j*vSpacing > previewHeight) return;
		x -= hmargin;
		int i = (int)Math.floor(x / hSpacing);
		if( i < 0 || i >= cols) return;
		if( x - i*hSpacing > previewWidth) return;
		int level = (page*rows+j)*cols+i;
		Toast.makeText(getContext(),"level="+level,Toast.LENGTH_SHORT).show();
	}

	private void fling( float velX)
	{
		vel = velX * -0.001f;
	}

	private class LevelSelectGestureListener
		extends GestureDetector.SimpleOnGestureListener
	{
		LevelSelectView view;

		LevelSelectGestureListener( LevelSelectView view)
		{
			this.view = view;
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
			final ViewConfiguration vc = ViewConfiguration.get(view.getContext());
			final int minFlingDistance = vc.getScaledTouchSlop();
			final int minFlingSpeed = vc.getScaledMinimumFlingVelocity();
			final int maxFlingSpeed = vc.getScaledMaximumFlingVelocity();
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
