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
    private int w,h;

	public BitmapBlitter( SpriteCache sc, int w, int h)
    {
        dest = Bitmap.createBitmap( SpriteCache.powerOfTwo(w),
            SpriteCache.powerOfTwo(h), Bitmap.Config.ARGB_8888);
        this.w = w;
        this.h = h;
		c = new Canvas(dest);
        c.clipRect(0,0,w,h);
		src = new Rect();
		rect = new Rect();
		paint = new Paint();
		this.sc = sc;
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
        c.drawBitmap( b, src, rect, null);
    }

	public void fill(int color, int x, int y, int w, int h)
	{
		paint.setColor( color);
		c.drawRect(x,y,x+w,y+h,paint);
	}

	public int getWidth()
	{
		return w;
	}

	public int getHeight()
	{
		return h;
	}

    public Bitmap getDest()
    {
        return dest;
    }
}
