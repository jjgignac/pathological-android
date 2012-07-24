package org.gignac.jp.pathological;
import android.graphics.*;

public interface Blitter
{
	public void blit( Bitmap b, int x, int y);
	public void blit( Bitmap b, int x, int y, int w, int h);
}
