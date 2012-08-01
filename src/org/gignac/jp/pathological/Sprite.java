package org.gignac.jp.pathological;
import android.graphics.*;
import java.util.*;
import javax.microedition.khronos.opengles.*;
import android.opengl.*;
import android.content.res.*;

public class Sprite
{
    private static int[] textures;
	private static final HashMap<Integer,Sprite> sprites =
		new HashMap<Integer,Sprite>();
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

	private int resid;
	private int glName;
	private Bitmap bitmap;
	private boolean needsPrep;

	private Sprite(int resid, Bitmap b) {
		this.bitmap = b;
		this.resid = resid;
	}

	public static void cache(int resid) {
		if( sprites.containsKey(Integer.valueOf(resid))) return;
		cache(resid, BitmapFactory.decodeResource(res, resid));
	}

	public static void cache(int[] resids) {
		for( int resid : resids) cache(resid);
	}

	public static void cache(int resid, Bitmap b) {
		Sprite s = sprites.get(resid);
		if( s == null) {
			sprites.put(Integer.valueOf(resid), new Sprite(resid,b));
			dirty = true;
		} else {
			s.bitmap = b;
			s.needsPrep = true;
		}
	}

	public static void bind(GL10 gl, int resid) {
		if(dirty) genTextures(gl);
		Sprite s = sprites.get(resid);
		if(s.needsPrep) s.prep(gl);
		else gl.glBindTexture(GL10.GL_TEXTURE_2D,s.glName);
	}

	public static Bitmap getBitmap(int resid) {
		return sprites.get(Integer.valueOf(resid)).bitmap;
	}
}
