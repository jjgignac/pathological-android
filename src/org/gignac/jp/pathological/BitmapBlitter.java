package org.gignac.jp.pathological;
import android.graphics.*;
import android.util.*;

public class BitmapBlitter
	implements Blitter
{
	private Canvas c;
	private Rect rect;
	private Paint paint;
	private Rect visibleArea;

	public BitmapBlitter( Bitmap dest) {
		this.c = new Canvas(dest);
		rect = new Rect();
		paint = new Paint();
		visibleArea = new Rect( 0, 0,
			dest.getWidth(), dest.getHeight());
	}

	public void transform(float scale, float dx, float dy)
	{
		c.scale(scale,scale);
		c.translate(dx,dy);		
	}

	public void blit(int resid, int x, int y)
	{
		Bitmap b = Sprite.getBitmap(resid);
		blit(b, x, y, b.getWidth(), b.getHeight());
	}

	public void blit(int resid, int x, int y, int w, int h)
	{
		Bitmap b = Sprite.getBitmap(resid);
		blit(b, x, y, w, h);
	}

	private void blit( Bitmap b, int x, int y, int w, int h)
	{
		rect.left = x;
		rect.top = y;
		rect.right = x + w;
		rect.bottom = y + h;
		c.drawBitmap( b, null, rect, null);
	}

	public void fill(int color, int x, int y, int w, int h)
	{
		paint.setColor( color);
		c.drawRect(x,y,x+w,y+h,paint);
	}

	public Rect getVisibleArea()
	{
		return visibleArea;
	}
}
