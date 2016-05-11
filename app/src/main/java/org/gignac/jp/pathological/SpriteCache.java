/*
 * Copyright (C) 2016  John-Paul Gignac
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        cache(uniq, BitmapFactory.decodeResource(res, resid, options));
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
}
