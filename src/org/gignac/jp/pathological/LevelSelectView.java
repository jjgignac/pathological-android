package org.gignac.jp.pathological;
import android.view.*;
import android.content.*;
import android.util.*;
import android.opengl.*;
import android.os.*;

public class LevelSelectView extends GLSurfaceView
	implements Paintable
{
	private static final int rows = 2;
	private static final int cols = 3;
	private static final float previewScale = 0.28f;
	private static final int hmargin = Board.board_width/4;
	private static final int vmargin = Board.board_width/4;
	private static final int previewWidth = Preview.getWidth(previewScale);
	private static final int previewHeight = Preview.getHeight(previewScale);

	private GameResources gr;
	private BlitterRenderer renderer;
	private GestureDetector g;
	private float xOffset = 0.0f;
	private float vel;
	private long prevTime;
	private int width, height;
	private int nUnlocked;
	private int[] textWidth;
	private SpriteCache sc;
	private final Paint paint = new Paint();

	public LevelSelectView( Context context, AttributeSet a)
	{
		super(context,a);
		sc = new SpriteCache(getResources());
		renderer = new BlitterRenderer(sc);
		setRenderer(renderer);
		setRenderMode(RENDERMODE_CONTINUOUSLY);
		g = new GestureDetector(new LevelSelectGestureListener(this));
		renderer.setPaintable(this);
        gr = GameResources.getInstance(getContext());
        textWidth = new int[gr.numlevels];
	}

	public void onResume() {
		nUnlocked = GameResources.shp.getInt("nUnlocked",1);
		paint.setTextSize(previewWidth*0.1f);
		paint.setAntiAlias(true);
		paint.setMaskFilter(null);
		paint.setColor(0xff000000);
		Paint.FontMetrics fm = paint.getFontMetrics();
		int up = (int)Math.ceil(Math.max(-fm.ascent,-fm.top)+fm.leading);
		int down = (int)Math.ceil(Math.max(fm.descent,fm.bottom));
		Canvas c = new Canvas();
		for( int i=0; i < nUnlocked; ++i) {
			Preview.cache(getContext(),sc,gr,i,0.5f);
			String label = (i+1)+". "+gr.boardNames.elementAt(i);
			int txtWid = (int)Math.ceil(paint.measureText(label));
			int txtHei = up+down;
			Bitmap text = Bitmap.createBitmap(
				SpriteCache.powerOfTwo(txtWid),
                SpriteCache.powerOfTwo(txtHei),
                Bitmap.Config.ARGB_8888);
			c.setBitmap(text);
			c.drawText(label,0,up,paint);
			sc.cache(0x5700000000l+i,text);
			textWidth[i] = txtWid;
		}
		Bitmap shadow = Bitmap.createBitmap(
            SpriteCache.powerOfTwo(previewWidth+10),
			SpriteCache.powerOfTwo(previewHeight+10),
            Bitmap.Config.ARGB_8888);
		c.setBitmap(shadow);
		paint.setMaskFilter(new BlurMaskFilter(5,BlurMaskFilter.Blur.NORMAL));
		paint.setColor(0x30000000);
		c.drawRect(5,5,previewWidth+5,previewHeight+5,paint);
		sc.cache(0x5800000000l,shadow);
		IntroScreen.setup(sc);
		super.onResume();
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
		width = b.getWidth();
		height = b.getHeight();
		update();

		int npages = (gr.numlevels + rows*cols - 1) / (rows*cols);
		IntroScreen.draw_back(gr,b);
		IntroScreen.draw_fore(gr,b);
		b.transform( 1f, -xOffset, 0f);

		int hSpacing = (width - 2*hmargin - cols*previewWidth) / (cols-1) + previewWidth;
		int vSpacing = (height - 2*vmargin - rows*previewHeight) / (rows-1) + previewHeight;
		
		for( int page=0; page < npages; ++page) {
			for( int j=0; j < rows; ++j) {
				for( int i=0; i < cols; ++i) {
					int level = (page*rows+j)*cols+i;
					if(level >= gr.numlevels) continue;
					int x = page*width+hmargin+i*hSpacing;
					int y = vmargin+j*vSpacing;
					if(level < nUnlocked) {
						b.blit(0x5800000000l, x+5, y+5);
						b.fill(0xff000000, x-1,
							y-1, previewWidth+2,
							previewHeight+2);
						b.blit( 0x200000000l+level,
						    0, 0, Preview.getWidth(0.5f), Preview.getHeight(0.5f),
						    x, y, previewWidth, previewHeight);
						Bitmap text = sc.getBitmap(0x5700000000l+level);
						if(text!=null)
						b.blit( 0x5700000000l+level,
							x + (previewWidth-textWidth[level])/2,
							y + previewHeight + 1);
					} else
						b.fill(0xfff080a0, x, y,
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
		Intent intent = new Intent(getContext(),Game.class);
		intent.putExtra("level",level);
		getContext().startActivity(intent);
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
