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
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[i]);
		    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, s.bitmap, 0);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			++i;
		}

		dirty = false;
	}
	
	public static void regenerateTextures() {
		dirty = true;
	}

	private int resid;
	private int glName;
	private Bitmap bitmap;

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
		if( sprites.containsKey(Integer.valueOf(resid))) return;
		sprites.put(Integer.valueOf(resid), new Sprite(resid,b));
		dirty = true;
	}

	public static void bind(GL10 gl, int resid) {
		if(dirty) genTextures(gl);
		gl.glBindTexture(GL10.GL_TEXTURE_2D,
			sprites.get(Integer.valueOf(resid)).glName);
	}

	public static Bitmap getBitmap(int resid) {
		return sprites.get(Integer.valueOf(resid)).bitmap;
	}
}
