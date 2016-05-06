package org.gignac.jp.pathological;
import android.graphics.*;
import android.content.*;
import java.io.*;

class Preview
{
    public static final int height = 170;
    public static final int width = 224;
    private static final int rows = 3;
    private static final int cols = 2;
    private static final int supersample = 2;
    private static final Rect dest = new Rect();
    private static BitmapBlitter b = null;
    private static final Paint paint = new Paint();

    public static void blit( Blitter d, int level, int x, int y)
    {
        int segment = level / (rows*cols);
        int relLevel = level % (rows*cols);
        d.blit( 0x200000000L+segment,
            (width+1)*(relLevel/rows), (height+1)*(relLevel%rows),
            width, height, x, y, width, height);
    }

    private static void render( Canvas c,
        GameResources gr, SpriteCache s, int level)
    {
        int relLevel = level % (rows*cols);

        if(b == null) b = new BitmapBlitter(s,
            width*supersample, height*supersample);
        else b.reset();
        new Board(gr,s,level,null,false).paint(b);

        dest.left = (width+1)*(relLevel/rows);
        dest.top = (height+1)*(relLevel%rows);
        dest.right = dest.left + width;
        dest.bottom = dest.top + height;

        paint.setFilterBitmap(true);
        c.drawBitmap( b.getDest(), null, dest, paint);
    }

    public static void cache( Context c, SpriteCache s,
        GameResources gr, int nUnlocked)
    {
        for( int i=(nUnlocked-1)/(rows*cols); i >= 0; --i)
            cacheSegment(c,s,gr,i);
    }

    private static void cacheSegment(
        Context c, SpriteCache s, GameResources gr, int segment)
    {
        long uniq = 0x200000000L+segment;
        InputStream in = null;
        OutputStream out = null;
        Bitmap preview = s.getBitmap(uniq);
        if( preview != null) return;
        try {
            try {
                String name = "preview-s"+rows+"x"+cols+"-"+segment+".png";

                // Is the preview already cached?
                try {
                    in = c.openFileInput(name);
                    preview = BitmapFactory.decodeStream(in);
                } catch(FileNotFoundException e) {
                    int rw = (width+1) * cols - 1;
                    int rh = (height+1) * rows - 1;
                    preview = Bitmap.createBitmap(
                        rw, rh, Bitmap.Config.ARGB_8888);
                    Canvas cv = new Canvas(preview);
                    for( int j=0; j < cols; ++j) {
                        for( int i=0; i < rows; ++i) {
                            int level = (segment*cols+j)*rows+i;
                            if(level < gr.numlevels) render( cv, gr, s, level);
                        }
                    }

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
        } catch( IOException e) {
            // Ignore
        }
    }
}
