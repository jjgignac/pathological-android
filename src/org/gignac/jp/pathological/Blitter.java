package org.gignac.jp.pathological;
import android.graphics.*;

public interface Blitter
{
	public void blit( Sprite b, int x, int y);
	public void blit( Sprite b, int x, int y, int w, int h);
}
