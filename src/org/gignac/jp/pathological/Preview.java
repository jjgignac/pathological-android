package org.gignac.jp.pathological;
import android.graphics.*;

public class Preview
{
	public static Bitmap create( GameResources gr,
		int level, float scale)
	{
		Bitmap preview = Bitmap.createBitmap(
			Math.round(Board.screen_width * scale),
			Math.round(Board.screen_height * scale),
			Bitmap.Config.ARGB_8888);
		Blitter b = new BitmapBlitter(preview);
		new Board( gr, level).paint(b);
		return preview;
	}
}
