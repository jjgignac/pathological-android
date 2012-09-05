package org.gignac.jp.pathological;
import android.graphics.*;
import android.content.*;
import java.io.*;

public class Preview
{
	private static Bitmap create( GameResources gr,
		SpriteCache s, int level, float scale)
	{
		BitmapBlitter b = new BitmapBlitter(s,
            getWidth(scale), getHeight(scale));
		new Board(gr,s,level,null,false).paint(b);
		return b.getDest();
	}

	public static void cache( Context c, SpriteCache s,
		GameResources gr, int level, float scale)
	{
		long uniq = 0x200000000l + level;
		InputStream in = null;
		OutputStream out = null;
		Bitmap preview = s.getBitmap(uniq);
		if( preview != null) return;
		try {
			try {
				String name = "preview-"+level+".png";

				// Is the preview already cached?
				try {
					in = c.openFileInput(name);
					preview = BitmapFactory.decodeStream(in);
				} catch(FileNotFoundException e) {
					preview = create(gr,s,level,scale);

					// Cache the image
					out = c.openFileOutput(name, Context.MODE_PRIVATE);
					preview.compress(Bitmap.CompressFormat.PNG, 90, out);
				}

				s.cache(uniq, preview);
			} finally {
				try {
					if( in != null) in.close();
				} finally {
					if( out != null) out.close();
				}
			}
		} catch( IOException e) {}
	}

	public static int getWidth( float scale) {
        return Math.round(Board.screen_width * scale);
	}

	public static int getHeight( float scale) {
        return Math.round((Board.screen_height+Marble.marble_size) * scale);
	}
}
