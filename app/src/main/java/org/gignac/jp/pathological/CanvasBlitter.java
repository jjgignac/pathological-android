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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

class CanvasBlitter
    implements Blitter
{
    Canvas c;
    private final Rect src = new Rect();
    private final Rect rect = new Rect();
    private final Paint paint = new Paint();
    private final SpriteCache sc;
    private int width;
    private int height;

    public CanvasBlitter(SpriteCache sc)
    {
        this.sc = sc;
    }

    public void setCanvas( Canvas c, int width, int height)
    {
        this.c = c;
        this.width = width;
        this.height = height;
    }

    @Override
    public void pushTransform(float scale, float dx, float dy)
    {
        c.save();
        c.scale(scale,scale);
        c.translate(dx,dy);
    }

    @Override
    public void popTransform()
    {
        c.restore();
    }

    @Override
    public void blit(int resid, int sx, int sy,
        int sw, int sh, int x, int y)
    {
        blit(resid&0xffffffffL, sx, sy, sw, sh, x, y, sw, sh);
    }

    @Override
    public void blit(long uniq, int x, int y)
    {
        Bitmap b = sc.getBitmap(uniq);
        blit(b, x, y, b.getWidth(), b.getHeight());
    }

    @Override
    public void blit(int resid, int x, int y, int w, int h)
    {
        blit(resid&0xffffffffL, x, y, w, h);
    }

    @Override
    public void blit(int resid, int sx, int sy,
        int sw, int sh, int x, int y, int w, int h)
    {
        blit(resid&0xffffffffL, sx, sy, sw, sh, x, y, w, h);
    }

    private void blit(long uniq, int x, int y, int w, int h)
    {
        Bitmap b = sc.getBitmap(uniq);
        blit(b, x, y, w, h);
    }

    @Override
    public void blit(long uniq, int sx, int sy,
        int sw, int sh, int x, int y, int w, int h)
    {
        Bitmap b = sc.getBitmap(uniq);
        blit(b, sx, sy, sw, sh, x, y, w, h);
    }

    private void blit( Bitmap b, int x, int y, int w, int h)
    {
        rect.left = x;
        rect.top = y;
        rect.right = x + w;
        rect.bottom = y + h;
        c.drawBitmap( b, null, rect, null);
    }

    private void blit( Bitmap b, int sx, int sy,
        int sw, int sh, int x, int y, int w, int h)
    {
        src.left = sx;
        src.top = sy;
        src.right = sx + sw;
        src.bottom = sy + sh;
        rect.left = x;
        rect.top = y;
        rect.right = x + w;
        rect.bottom = y + h;
        if(b != null) c.drawBitmap( b, src, rect, null);
    }

    @Override
    public void fill(int color, int x, int y, int w, int h)
    {
        paint.setColor( color);
        c.drawRect(x,y,x+w,y+h,paint);
    }

    @Override
    public int getWidth()
    {
        return width;
    }

    @Override
    public int getHeight()
    {
        return height;
    }
}
