package org.gignac.jp.pathological;
import android.graphics.*;
import java.util.*;
import javax.microedition.khronos.opengles.*;
import android.opengl.*;
import android.content.res.*;

public class SpriteCache
{
    private int[] textures;
	private final HashMap<Long,Sprite> sprites =
		new HashMap<Long,Sprite>();
	private boolean dirty = true;
	private Resources res;

	public SpriteCache( Resources res) {
		this.res = res;
	}

	private void genTextures(GL10 gl) {
		if(textures == null || textures.length != sprites.size()) {
			if( textures != null)
				gl.glDeleteTextures(textures.length, textures, 0);
			textures = new int[sprites.size()];
			gl.glGenTextures(textures.length, textures, 0);
		}

		int i=0;
		for( Sprite s : sprites.values()) {
			s.glName = textures[i];
			s.prep(gl);
			++i;
		}

		dirty = false;
	}

	private class Sprite
	{
		public int glName;
		public Bitmap bitmap;
		public boolean needsPrep;

		public Sprite(long uniq, Bitmap b) {
			this.bitmap = b;
		}

		public void prep(GL10 gl) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, glName);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			needsPrep = false;
		}
	}

	public void regenerateTextures() {
		dirty = true;
	}

	public void cache(int resid) {
		long uniq = resid&0xffffffffl;
		if( sprites.containsKey(uniq)) return;
		cache(uniq, BitmapFactory.decodeResource(res, resid));
	}

	public void cache(int[] resids) {
		for( int resid : resids) cache(resid);
	}

	public void cache(int resid, Bitmap b) {
		throw new IllegalArgumentException();
	}

	public void cache(long uniq, Bitmap b) {
		Sprite s = sprites.get(uniq);
		if( s == null) {
			sprites.put(uniq, new Sprite(uniq,b));
			dirty = true;
		} else {
			s.bitmap = b;
			s.needsPrep = true;
		}
	}

	public void bind(GL10 gl, int resid) {
		bind(gl, resid&0xffffffffl);
	}

	public void bind(GL10 gl, long uniq) {
		if(dirty) genTextures(gl);
		Sprite s = sprites.get(uniq);
		if(s.needsPrep) s.prep(gl);
		else gl.glBindTexture(GL10.GL_TEXTURE_2D,s.glName);
	}

	public Bitmap getBitmap(long uniq) {
		Sprite s = sprites.get(uniq);
		return s == null ? null : s.bitmap;
	}
}
