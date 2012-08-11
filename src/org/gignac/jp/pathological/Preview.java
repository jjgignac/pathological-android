package org.gignac.jp.pathological;
import android.graphics.*;

public class Preview
{
	public static Bitmap create( GameResources gr,
		SpriteCache sc, int level, float scale)
	{
		Bitmap preview = Bitmap.createBitmap(
			Math.round(Board.screen_width * scale),
			Math.round(Board.screen_height * scale),
			Bitmap.Config.ARGB_8888);
		Blitter b = new BitmapBlitter(sc, preview);
		new Board(gr,sc,level,null).paint(b);
		return preview;
	}
}
