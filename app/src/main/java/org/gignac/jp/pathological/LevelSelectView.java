package org.gignac.jp.pathological;
import android.view.*;
import android.content.*;
import android.util.*;
import android.os.*;
import android.graphics.*;

@SuppressWarnings("unused,PointlessArithmeticExpression")
public class LevelSelectView extends View
{
    private static final int rows = 3;
    private static final int cols = 2;
    private static final float hmargin_ratio = 1.1f;
    private static final float hspacing_ratio = 0.9f;
    private static final float vmargin_ratio = 1.1f;
    private static final float vspacing_ratio = 0.9f;
    private int hmargin;
    private int vmargin;
    private int previewWidth;
    private int previewHeight;
    private int lockSize;
    private int hSpacing;
    private int vSpacing;

    private final GameResources gr;
    private final CanvasBlitter b;
    private final GestureDetector g;
    private float xOffset = 0.0f;
    private float vel;
    private long prevTime;
    private int nLoaded=0, nUnlocked=0;
    private int highlight = -1;
    private Bitmap text;
    private final Canvas c = new Canvas();
    private final int[] textWidth;
    private int textHeight;
    private final SpriteCache sc;
    private final Paint paint = new Paint();
    private final Runnable updater;

    public LevelSelectView( Context context, AttributeSet a)
    {
        super(context,a);
        sc = GameResources.getInstance(context).sc;
        b = new CanvasBlitter(sc);
        g = new GestureDetector(context, new LevelSelectGestureListener(this));
        g.setIsLongpressEnabled(false);
        gr = GameResources.getInstance(getContext());
        textWidth = new int[gr.numlevels];

        updater = new Runnable() {
            public void run() { update(); };
        };
    }

    @Override
    protected void onDraw( Canvas c) {
        b.setCanvas(c, getWidth(), getHeight());
        paint(b);
    }

    public void onResume() {
        nUnlocked = GameResources.shp.getInt("nUnlocked",1);
        highlight = -1;

        IntroScreen.setup(sc);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        float boxWidth = w / (cols + 2*hmargin_ratio + (cols-1)*hspacing_ratio);
        float boxHeight = h / (rows + 2*vmargin_ratio + (rows-1)*vspacing_ratio);

        hmargin = Math.round(boxWidth * hmargin_ratio);
        vmargin = Math.round(boxHeight * vmargin_ratio);

        if( boxWidth * Preview.height > boxHeight * Preview.width) {
            previewWidth = Math.round(boxHeight * Preview.width / Preview.height);
            previewHeight = Math.round(boxHeight);
            hmargin += Math.round((boxWidth - previewWidth) * 0.5);
        } else {
            previewWidth = Math.round(boxWidth);
            previewHeight = Math.round(boxWidth * Preview.height / Preview.width);
            vmargin += Math.round((boxHeight - previewHeight) * 0.5);
        }

        hSpacing = (w - 2*hmargin - cols*previewWidth) / (cols-1) + previewWidth;
        vSpacing = (h - 2*vmargin - rows*previewHeight) / (rows-1) + previewHeight;

        lockSize = previewHeight * 3 / 4;

        Bitmap shadow = Bitmap.createBitmap(
            previewWidth+10, previewHeight+10,
            Bitmap.Config.ARGB_8888);
        c.setBitmap(shadow);
        paint.setMaskFilter(new BlurMaskFilter(5,BlurMaskFilter.Blur.NORMAL));
        paint.setColor(0x30000000);
        c.drawRect(5,5,previewWidth+5,previewHeight+5,paint);
        sc.cache(0x5800000000L,shadow);

        Bitmap hilight = Bitmap.createBitmap(
            previewWidth+20, previewHeight+20,
            Bitmap.Config.ARGB_8888);
        c.setBitmap(hilight);
        paint.setMaskFilter(new BlurMaskFilter(10,BlurMaskFilter.Blur.NORMAL));
        paint.setColor(0xff40a0ff);
        c.drawRect(10,10,previewWidth+10,previewHeight+10,paint);
        sc.cache(0x5800000001L,hilight);
    }

    private void prepPaint()
    {
        paint.setTextSize(previewWidth*0.17f);
        paint.setAntiAlias(true);
        paint.setMaskFilter(null);
        paint.setColor(0xff000000);
        Paint.FontMetrics fm = paint.getFontMetrics();
        int up = (int)Math.ceil(Math.max(-fm.ascent,-fm.top)+fm.leading);
        int down = (int)Math.ceil(Math.max(fm.descent,fm.bottom));
        textHeight = up+down;
        int maxTxtWid = getWidth()/cols;
        if(text == null) {
            text = Bitmap.createBitmap( maxTxtWid,
            gr.numlevels*textHeight, Bitmap.Config.ARGB_8888);
        }
        c.setBitmap(text);
        c.drawColor(0x00000000, PorterDuff.Mode.SRC);
        Preview.cache(getContext(),sc,gr,nUnlocked);
        for( int i=0; i < gr.numlevels; ++i) {
            String label = (i+1) + (i < nUnlocked ?
                ". "+gr.boardNames.elementAt(i) : "");
            int txtWid = (int)Math.ceil(paint.measureText(label));
            if(txtWid > maxTxtWid) txtWid = maxTxtWid;
            c.drawText(label,0,up+textHeight*i,paint);
            textWidth[i] = txtWid;
        }
        sc.cache(0x5700000000L,text);
        nLoaded = nUnlocked;
    }

    private void update()
    {
        final long time = SystemClock.uptimeMillis();
        final long dt = time-prevTime;
        prevTime = time;

        int npages = (gr.numlevels + rows*cols - 1) / (rows*cols);
        float pos = xOffset / getWidth();
        float prevPos = pos;
        int a = Math.round(pos);
        if( pos < 0) a = 0;
        else if( pos > npages-1) a = npages-1;
        pos += dt * vel / getWidth();
        if( (prevPos < a) ^ (pos < a)) {
            vel = 0f;
            pos = a;
        }
        if( pos < -0.1f) pos = -0.1f;
        else if( pos > npages-1+0.1f) pos = npages-1+0.1f;
        xOffset = pos * getWidth();

        if( pos < 0) vel = 0.005f * 300;
        else if( pos > npages-1) vel = -0.005f * 300;
        else vel += dt * (a-pos) * 0.005f;

        invalidate();
        removeCallbacks(updater);

        if( vel != 0) postDelayed(updater, 1000/60);
    }

    public void paint( Blitter b)
    {
        int width = b.getWidth();
        int height = b.getHeight();

        if(nUnlocked > nLoaded) prepPaint();

        update();

        int npages = (gr.numlevels + rows*cols - 1) / (rows*cols);
        IntroScreen.draw_back(b);
        IntroScreen.draw_fore(gr,b);
        b.pushTransform( 1f, -xOffset, 0f);

        int fromPage = Math.max(0, Math.round(xOffset) / width);
        int toPage = Math.min(npages, fromPage+2);
        for( int page=fromPage; page < toPage; ++page) {
            for( int j=0; j < rows; ++j) {
                for( int i=0; i < cols; ++i) {
                    int level = (page*rows+j)*cols+i;
                    if(level >= gr.numlevels) continue;
                    int x = page*width+hmargin+i*hSpacing;
                    int y = vmargin+j*vSpacing;
                    if(level < nUnlocked) {
                        if(highlight == level)
                            b.blit(0x5800000001L, x-10, y-10);
                        b.blit(0x5800000000L, x+5, y+5);
                        b.fill(0xff000000, x-1,
                            y-1, previewWidth+2,
                            previewHeight+2);
                        Preview.blit(b,level,x,y, previewWidth, previewHeight);
                    } else {
                        b.blit(R.drawable.misc, 0, 479, 128, 128,
                            x+(previewWidth-lockSize)/2+10,
                            y+(previewHeight-lockSize)/2+10,
                            lockSize, lockSize);
                        b.blit(R.drawable.misc, 128, 479, 128, 128,
                            x+(previewWidth-lockSize)/2,
                            y+(previewHeight-lockSize)/2,
                            lockSize, lockSize);
                        y -= previewHeight*4/9;
                    }

                    Bitmap text = sc.getBitmap(0x5700000000L);
                    if(text!=null)
                        b.blit( 0x5700000000L,
                            0,textHeight*level,textWidth[level],textHeight,
                            x + (previewWidth-textWidth[level])/2,
                            y + previewHeight + 1,textWidth[level],textHeight);
                }
            }
        }
        b.popTransform();
    }

    @Override
    public synchronized boolean onTouchEvent(MotionEvent e) {
        vel = 0f;
        switch(e.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_UP:
            prevTime = SystemClock.uptimeMillis();
            break;
        }
        return g.onTouchEvent(e);
    }

    private void scroll( float dx)
    {
        xOffset += dx;
        highlight = -1;
        invalidate();
    }

    private void tap( float x, float y)
    {
        int level = pickLevel(x,y);
        if(level == -1) return;
        highlight = level;
        invalidate();
        Intent intent = new Intent(getContext(),GameActivity.class);
        intent.putExtra("level",level);
        getContext().startActivity(intent);
    }

    private void showPress( float x, float y)
    {
        highlight = pickLevel(x,y);
        invalidate();
    }

    private int pickLevel( float x, float y)
    {
        int width = getWidth();
        int height = getHeight();
        x += xOffset;
        int page = (int)Math.floor(x / width);
        x -= page * width;
        y -= vmargin;
        int j = (int)Math.floor(y / vSpacing);
        if( j < 0 || j >= rows) return -1;
        if( y - j*vSpacing > previewHeight) return -1;
        x -= hmargin;
        int i = (int)Math.floor(x / hSpacing);
        if( i < 0 || i >= cols) return -1;
        if( x - i*hSpacing > previewWidth) return -1;
        int level = (page*rows+j)*cols+i;
        if(level < 0 || level >= gr.numlevels) return -1;
        return level;
    }

    private void fling( float velX)
    {
        vel = velX * -0.001f;
    }

    private class LevelSelectGestureListener
        extends GestureDetector.SimpleOnGestureListener
    {
        final LevelSelectView view;

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

        @Override
        public void onShowPress(MotionEvent e)
        {
            view.showPress(e.getX(), e.getY());
        }
    }
}
