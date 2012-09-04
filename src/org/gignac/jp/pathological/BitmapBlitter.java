package org.gignac.jp.pathological;
import android.graphics.*;

public class BitmapBlitter
	implements Blitter
{
	private Canvas c;
	private Rect src,rect;
	private Paint paint;
	private SpriteCache sc;
	private Bitmap dest;

	public BitmapBlitter( SpriteCache sc, Bitmap dest) {
		this.c = new Canvas(dest);
		src = new Rect();
		rect = new Rect();
		paint = new Paint();
		this.sc = sc;
		this.dest = dest;
	}

	public void transform(float scale, float dx, float dy)
	{
		c.scale(scale,scale);
		c.translate(dx,dy);		
	}

    public void blit(int resid, int sx, int sy,
        int sw, int sh, int x, int y)
	{
		blit(resid&0xffffffffl, sx, sy, sw, sh, x, y, sw, sh);
	}

	public void blit(int resid, int x, int y)
	{
		blit(resid&0xffffffffl, x, y);
	}

	public void blit(long uniq, int x, int y)
	{
		Bitmap b = sc.getBitmap(uniq);
		blit(b, x, y, b.getWidth(), b.getHeight());
	}

	public void blit(int resid, int x, int y, int w, int h)
	{
		blit(resid&0xffffffffl, x, y, w, h);
	}

	public void blit(int resid, int sx, int sy,
	    int sw, int sh, int x, int y, int w, int h)
	{
		blit(resid&0xffffffffl, sx, sy, sw, sh, x, y, w, h);
	}

	public void blit(long uniq, int x, int y, int w, int h)
	{
		Bitmap b = sc.getBitmap(uniq);
		blit(b, x, y, w, h);
	}

    private void blit(long uniq, int sx, int sy,
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
        c.drawBitmap( b, src, rect, null);
    }

	public void fill(int color, int x, int y, int w, int h)
	{
		paint.setColor( color);
		c.drawRect(x,y,x+w,y+h,paint);
	}

	public int getWidth()
	{
		return dest.getWidth();
	}

	public int getHeight()
	{
		return dest.getHeight();
	}
}
