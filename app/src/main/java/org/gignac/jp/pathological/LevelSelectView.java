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

@SuppressWarnings("PointlessArithmeticExpression")
public class LevelSelectView extends View
{
    private static final int rows = 3;
    private static final int cols = 2;
    private static final float hmargin_ratio = 1.1f;
    private static final float hspacing_ratio = 0.9f;
    private static final float vmargin_ratio = 1.1f;
    private static final float vspacing_ratio = 0.9f;
    private static final int highlightRadius = 50;
    private int hmargin;
    private int vmargin;
    private int previewWidth;
    private int previewHeight;
    private int lockSize;
    private int hSpacing;
    private int vSpacing;
    private final Point boardPos = new Point();
    private final Point parentPos = new Point();

    private final GameResources gr;
    private final CanvasBlitter b;
    private final GestureDetector g;
    private float xOffset;
    private float vel;
    private long prevTime;
    private int highlight;
    private final Canvas c = new Canvas();
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
        xOffset = GameResources.getCurrentLevel() / (rows * cols);

        updater = new Runnable() {
            public void run() { update(); }
        };
    }

    public void onResume() {
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
                previewWidth + 2 * highlightRadius,
                previewHeight + 2 * highlightRadius,
                Bitmap.Config.ARGB_8888);
        c.setBitmap(hilight);
        paint.setMaskFilter(new BlurMaskFilter(highlightRadius,BlurMaskFilter.Blur.NORMAL));
        paint.setColor(0xff40a0ff);
        c.drawRect(highlightRadius,highlightRadius,
                previewWidth+highlightRadius,previewHeight+highlightRadius,
                paint);
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

        int highlightPage = gr.boardInfo(highlight).position.x / cols;
        int npages = 1 + gr.maxXPos / cols;
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

    private void getBoardPosition(int level, Point center) {
        Point pos = gr.boardInfo(level).position;
        center.x = hmargin + pos.x / 2 * getWidth() +
                (pos.x % cols) * hSpacing + previewWidth / 2;
        center.y = vmargin + pos.y * vSpacing + previewHeight / 2;
    }

    @Override
    protected void onDraw( Canvas c) {
        b.setCanvas(c, getWidth(), getHeight());

        if( mNeedsPrep) {
            mNeedsPrep = false;
            highlight = Math.max(0, Math.min( gr.numlevels - 1,
                    GameResources.getCurrentLevel()));
            int highlightPage = gr.boardInfo(highlight).position.x / cols;
            if( xOffset > highlightPage + 1) {
                xOffset = highlightPage + 1;
            } else if( xOffset < highlightPage - 1) {
                xOffset = highlightPage - 1;
            }
            mZoomDelay = 1000;
            prevTime = SystemClock.uptimeMillis();

            int maxUnlocked = 0;
            for( int level = gr.numlevels-1; level > 0; level--) {
                if( gr.isUnlocked(level)) {
                    maxUnlocked = level;
                    break;
                }
            }

            Preview.cache(getContext(), sc, gr, maxUnlocked + 1);
        }

        update();

        paint.setTextSize(previewWidth*0.17f);
        paint.setAntiAlias(true);
        paint.setMaskFilter(null);
        paint.setColor(0xff000000);
        Paint.FontMetrics fm = paint.getFontMetrics();
        int up = (int)Math.ceil(Math.max(-fm.ascent,-fm.top)+fm.leading);
        int down = (int)Math.ceil(Math.max(fm.descent,fm.bottom));

        IntroScreen.draw_back(b);
        IntroScreen.draw_fore(gr,b);

        // Draw the author attribution
        b.blit(0x5800000002L, (getWidth() - sc.getBitmap(0x5800000002L).getWidth())/2, 90);

        b.pushTransform( 1f, -xOffset * getWidth(), 0f);

        for( int level = 0; level < gr.numlevels; level++) {
            getBoardPosition(level, boardPos);

            if (level > 0) {
                for( int from : gr.boardInfo(level).from) {
                    // Draw tracks from the parent board(s)
                    getBoardPosition(from, parentPos);

                    if (parentPos.x == boardPos.x) {
                        int minY = Math.min(parentPos.y, boardPos.y);
                        int maxY = Math.max(parentPos.y, boardPos.y);
                        b.blit(R.drawable.misc, 192, 400, 30, 10,
                                parentPos.x - previewWidth / 8, minY,
                                previewWidth / 4, maxY - minY);
                    } else {
                        int minX = Math.min(parentPos.x, boardPos.x);
                        int maxX = Math.max(parentPos.x, boardPos.x);
                        b.blit(R.drawable.misc, 420, 394, 10, 30,
                                minX, parentPos.y - previewWidth / 8,
                                maxX - minX, previewWidth / 4);
                    }
                }
            }
        }

        for( int level = 0; level < gr.numlevels; level++) {
            getBoardPosition(level, boardPos);

            if( gr.isUnlocked(level)) {
                // Draw the board preview

                int x = boardPos.x - previewWidth / 2;
                int y = boardPos.y - previewHeight / 2;

                if(highlight == level)
                    b.blit(0x5800000001L, 0, 0,
                            previewWidth + 2 * highlightRadius,
                            previewHeight + 2 * highlightRadius,
                            x - highlightRadius, y - highlightRadius,
                            previewWidth + 2 * highlightRadius,
                            previewHeight + 2 * highlightRadius);
                b.blit(0x5800000000L, x+5, y+5);
                b.fill(0xff000000, x-1,
                        y-1, previewWidth+2,
                        previewHeight+2);
                Preview.blit(b,level,x,y, previewWidth, previewHeight);

                // Draw the board title
                String label = gr.boardInfo(level).name;
                float txtX = boardPos.x - paint.measureText(label) / 2;
                float txtY = y + previewHeight + up;
                float txtShadowOffset = previewWidth * 0.02f;
                paint.setColor(0xff000000);
                c.drawText( label, txtX + txtShadowOffset, txtY + txtShadowOffset, paint);
                paint.setColor(0xffffffff);
                c.drawText( label, txtX, txtY, paint);

                // Draw the high score
                int best = GameResources.bestScore(level);
                if( best >= 0) {
                    String bestText = getContext().getString(R.string.best) + " " + best;
                    paint.setTypeface(italic);
                    txtX = boardPos.x - paint.measureText(bestText) / 2;
                    txtY = y + previewHeight + (up + down) + up;
                    paint.setColor(0xff000000);
                    c.drawText(bestText, txtX + txtShadowOffset, txtY + txtShadowOffset, paint);
                    paint.setColor(0xffffffff);
                    c.drawText(bestText, txtX, txtY, paint);
                    paint.setTypeface(normal);
                }
            } else {
                // Draw the lock icon

                int x = boardPos.x - lockSize * 96 / 256;
                int y = boardPos.y - lockSize * 91 / 128;

                b.blit(R.drawable.misc, 0, 479, 128, 128,
                        x - lockSize / 8 + 10, y + 10, lockSize, lockSize);
                b.blit(R.drawable.misc, 144, 479, 96, 128,
                        x, y, lockSize * 96 / 128, lockSize);

                if( !BuildConfig.BUILD_TYPE.equals("release")) {
                    // Draw the board number on the lock
                    String label = (level + 1) + "";
                    float txtX = boardPos.x - paint.measureText(label) / 2;
                    float txtY = boardPos.y + (up - down) / 2;
                    paint.setColor(0xff000000);
                    c.drawText(label, txtX, txtY, paint);
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
        if(BuildConfig.BUILD_TYPE.equals("release") && !gr.isUnlocked(level)) return;
        highlight = level;
        invalidate();
        Intent intent = new Intent(getContext(),GameActivity.class);
        intent.putExtra("level",level);
        getContext().startActivity(intent);
    }

    private int pickLevel( float x, float y)
    {
        x += xOffset * getWidth();

        for( int level = 0; level < gr.numlevels; ++level) {
            getBoardPosition(level, boardPos);
            if( Math.abs(x - boardPos.x) <= previewWidth / 2 &&
                    Math.abs(y - boardPos.y) <= previewHeight / 2)
                return level;
        }
        return -1;
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
