package org.gignac.jp.pathological;
import android.graphics.*;

public interface Blitter
{
	public void blit( int resid, int x, int y);
	public void blit( int resid, int x, int y, int w, int h);
	public void fill( int color, int x, int y, int w, int h);
	public Rect getVisibleArea();
	public void transform(float scale, float dx, float dy);
}
