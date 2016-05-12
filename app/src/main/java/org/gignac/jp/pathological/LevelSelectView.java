/*
 * Copyright (C) 2016  John-Paul Gignac
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
    private float xOffset;
    private float vel;
    private long prevTime;
    private int nLoaded=0, nUnlocked=0;
    private int highlight;
    private Bitmap text;
    private final Canvas c = new Canvas();
    private final int[] textWidth;
    private int textHeight;
    private int maxTxtWid;
    private final SpriteCache sc;
    private final Paint paint = new Paint();
    private final Runnable updater;
    private boolean mNeedsPrep;
    private boolean mZooming;
    private long mZoomDelay;
    private static final Typeface normal = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
    private static final Typeface italic = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC);

    public LevelSelectView( Context context, AttributeSet a)
    {
        super(context,a);
        sc = GameResources.getInstance(context).sc;
        b = new CanvasBlitter(sc);
        g = new GestureDetector(context, new LevelSelectGestureListener(this));
        g.setIsLongpressEnabled(false);
        gr = GameResources.getInstance(getContext());
        textWidth = new int[gr.numlevels];
        xOffset = GameResources.shp.getInt("level", 0) / (rows * cols);

        updater = new Runnable() {
            public void run() { update(); }
        };
    }

    @Override
    protected void onDraw( Canvas c) {
        b.setCanvas(c, getWidth(), getHeight());
        paint(b);
    }

    public void onResume() {
        nUnlocked = GameResources.shp.getInt("nUnlocked",1);

        IntroScreen.setup(sc);
        mNeedsPrep = true;
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

        // Prepare the author attribution image
        paint.setTextSize(previewWidth*0.22f);
        paint.setTypeface(italic);
        paint.setAntiAlias(true);
        paint.setMaskFilter(null);
        paint.setColor(0xff000000);
        String text = getContext().getString(R.string.author_attrib);
        int width = Math.round(paint.measureText(text));
        int height = Math.round(-paint.ascent() + paint.descent());
        Bitmap authorAttrib = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        c.setBitmap(authorAttrib);
        c.drawText(text, 0, -paint.ascent(), paint);
        paint.setTypeface(normal);
        sc.cache(0x5800000002L,authorAttrib);
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
        textHeight = (up+down) * 2;
        maxTxtWid = getWidth()/cols;
        if(text == null) {
            text = Bitmap.createBitmap( maxTxtWid,
            gr.numlevels*textHeight, Bitmap.Config.ARGB_8888);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            setLayerType(LAYER_TYPE_SOFTWARE, paint);

        c.setBitmap(text);
        c.drawColor(0x00000000, PorterDuff.Mode.SRC);
        Preview.cache(getContext(),sc,gr,nUnlocked);
        for( int i=0; i < gr.numlevels; ++i) {
            String label = (i+1) + (i < nUnlocked ?
                ". "+gr.boardNames.elementAt(i) : "");
            int txtWid = (int)Math.ceil(paint.measureText(label));
            if(txtWid > maxTxtWid) txtWid = maxTxtWid;
            c.drawText(label,(maxTxtWid - txtWid)/2,up+textHeight*i,paint);

            int best = GameResources.shp.getInt("best_"+i, -1);
            int bestWid = 0;
            if( best >= 0) {
                String bestText = getContext().getString(R.string.best) + " " + best;
                bestWid = Math.min((int) Math.ceil(paint.measureText(bestText)), maxTxtWid);
                paint.setTypeface(italic);
                c.drawText(bestText, (maxTxtWid - bestWid) / 2, up + textHeight * i + (up + down), paint);
                paint.setTypeface(normal);
            }

            textWidth[i] = Math.max(txtWid, bestWid);
        }
        sc.cache(0x5700000000L,text);
        nLoaded = nUnlocked;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            setLayerType(LAYER_TYPE_HARDWARE, paint);
    }

    private void update()
    {
        final long time = SystemClock.uptimeMillis();
        final long dt = time-prevTime;
        prevTime = time;

        if( mZoomDelay > 0) {
            mZoomDelay -= dt;
            if (mZoomDelay <= 0) {
                mZoomDelay = 0;
                mZooming = true;
            }
        }

        int highlightPage = highlight / (rows * cols);
        int npages = (gr.numlevels + rows*cols - 1) / (rows*cols);
        float pos = xOffset;
        float prevPos = pos;
        int a = Math.round(pos);
        if( pos < 0) a = 0;
        else if( pos > npages-1) a = npages-1;
        if( a == highlightPage) {
            mZooming = false;
            mZoomDelay = 0;
        }
        pos += dt * vel / getWidth();
        if( !mZooming && ((prevPos < a) ^ (pos < a))) {
            vel = 0f;
            pos = a;
        }
        if( pos < -0.1f) pos = -0.1f;
        else if( pos > npages-1+0.1f) pos = npages-1+0.1f;
        xOffset = pos;

        if( mZooming && pos < highlightPage) vel = 0.003f * 300;
        else if( mZooming && pos > highlightPage) vel = -0.003f * 300;
        else if( pos < 0) vel = 0.005f * 300;
        else if( pos > npages-1) vel = -0.005f * 300;
        else vel += dt * (a-pos) * 0.005f;

        invalidate();
        removeCallbacks(updater);

        if( vel != 0) postDelayed(updater, 1000/60);
    }

    public void paint( Blitter b)
    {
        if( mNeedsPrep) {
            prepPaint();
            mNeedsPrep = false;
            highlight = GameResources.shp.getInt("level", 0);
            mZoomDelay = 1000;
            prevTime = SystemClock.uptimeMillis();
        }

        update();

        int npages = (gr.numlevels + rows*cols - 1) / (rows*cols);
        IntroScreen.draw_back(b);
        IntroScreen.draw_fore(gr,b);

        // Draw the author attribution
        b.blit(0x5800000002L, (getWidth() - sc.getBitmap(0x5800000002L).getWidth())/2, 90);

        b.pushTransform( 1f, -xOffset * getWidth(), 0f);

        int fromPage = Math.max(0, Math.round(xOffset * getWidth()) / getWidth());
        int toPage = Math.min(npages, fromPage+2);
        for( int page=fromPage; page < toPage; ++page) {
            for( int j=0; j < rows; ++j) {
                for( int i=0; i < cols; ++i) {
                    int level = (page*rows+j)*cols+i;
                    if(level >= gr.numlevels) continue;
                    int x = page*getWidth()+hmargin+i*hSpacing;
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
                            (maxTxtWid - textWidth[level])/2,
                            textHeight*level,textWidth[level],textHeight,
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
        mZoomDelay = 0;
        mZooming = false;
        switch(e.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_UP:
            prevTime = SystemClock.uptimeMillis();
            break;
        }
        return g.onTouchEvent(e);
    }

    private void scroll( float dx)
    {
        xOffset += dx / getWidth();
        invalidate();
    }

    private void tap( float x, float y)
    {
        int level = pickLevel(x,y);
        if(level == -1) return;
        if(BuildConfig.BUILD_TYPE.equals("release") && level >= nUnlocked) return;
        highlight = level;
        invalidate();
        Intent intent = new Intent(getContext(),GameActivity.class);
        intent.putExtra("level",level);
        getContext().startActivity(intent);
    }

    private int pickLevel( float x, float y)
    {
        x += xOffset * getWidth();
        int page = (int)Math.floor(x / getWidth());
        x -= page * getWidth();
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
    }
}
