package org.gignac.jp.pathological;
import android.graphics.*;
import java.util.*;
import javax.microedition.khronos.opengles.*;
import android.opengl.*;
import android.content.res.*;

public class Sprite
{
    private static int[] textures;
	private static final HashMap<Long,Sprite> sprites =
		new HashMap<Long,Sprite>();
	private static boolean dirty = true;
	private static Resources res;

	public static void setResources( Resources res) {
		Sprite.res = res;
	}

	private static void genTextures(GL10 gl) {
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

	private void prep(GL10 gl) {
		gl.glBindTexture(GL10.GL_TEXTURE_2D, glName);
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		needsPrep = false;
	}

	public static void regenerateTextures() {
		dirty = true;
	}

	private long uniq;
	private int glName;
	private Bitmap bitmap;
	private boolean needsPrep;

	private Sprite(long uniq, Bitmap b) {
		this.bitmap = b;
		this.uniq = uniq;
	}

	public static void cache(int resid) {
		long uniq = resid&0xffffffffl;
		if( sprites.containsKey(uniq)) return;
		cache(uniq, BitmapFactory.decodeResource(res, resid));
	}

	public static void cache(int[] resids) {
		for( int resid : resids) cache(resid);
	}

	public static void cache(int resid, Bitmap b) {
		throw new IllegalArgumentException();
	}

	public static void cache(long uniq, Bitmap b) {
		Sprite s = sprites.get(uniq);
		if( s == null) {
			sprites.put(uniq, new Sprite(uniq,b));
			dirty = true;
		} else {
			s.bitmap = b;
			s.needsPrep = true;
		}
	}

	public static void bind(GL10 gl, int resid) {
		bind(gl, resid&0xffffffffl);
	}

	public static void bind(GL10 gl, long uniq) {
		if(dirty) genTextures(gl);
		Sprite s = sprites.get(uniq);
		if(s.needsPrep) s.prep(gl);
		else gl.glBindTexture(GL10.GL_TEXTURE_2D,s.glName);
	}

	public static Bitmap getBitmap(long uniq) {
		return sprites.get(uniq).bitmap;
	}
}
