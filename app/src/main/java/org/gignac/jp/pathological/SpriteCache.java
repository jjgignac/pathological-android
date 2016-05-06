package org.gignac.jp.pathological;
import android.graphics.*;
import java.util.*;
import android.util.Log;
import android.content.res.*;

public class SpriteCache
{
    private final HashMap<Long,Sprite> sprites = new HashMap<>();
    private final Resources res;

    public SpriteCache( Resources res) {
        this.res = res;
    }

    private class Sprite
    {
        public Bitmap bitmap;

        public Sprite(Bitmap b) {
            this.bitmap = b;
        }
    }

    public void cache(int resid) {
        long uniq = resid&0xffffffffL;
        if( sprites.containsKey(uniq)) return;
        cache(uniq, BitmapFactory.decodeResource(res, resid));
    }

    // This function catches incorrect API uses
    @SuppressWarnings("unused")
    public void cache(int resid, Bitmap b) {
        throw new IllegalArgumentException();
    }

    public void cache(long uniq, Bitmap b) {
        Sprite s = sprites.get(uniq);
        if( s == null) {
            sprites.put(uniq, new Sprite(b));
        } else {
            if( s.bitmap != b) {
                Log.e("SpriteCache","Recycling: 0x"+Long.toHexString(uniq));
                s.bitmap.recycle();
                s.bitmap = b;
            }
        }
    }

    public Bitmap getBitmap(long uniq) {
        Sprite s = sprites.get(uniq);
        return s == null ? null : s.bitmap;
    }

    public static int powerOfTwo( int x)
    {
        --x;
        x |= (x>>1);
        x |= (x>>2);
        x |= (x>>4);
        x |= (x>>8);
        x |= (x>>16);
        return x+1;
    }
}
