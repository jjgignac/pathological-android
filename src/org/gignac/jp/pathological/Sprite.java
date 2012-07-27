package org.gignac.jp.pathological;
import android.graphics.*;
import java.util.*;
import javax.microedition.khronos.opengles.*;
import android.opengl.*;

public class Sprite
{
    private static int[] textures;
	private static final Vector<Sprite> sprites = new Vector<Sprite>();
	private static boolean dirty = true;
	
	public static void genTextures(GL10 gl) {
		if(textures == null || textures.length != sprites.size()) {
			if( textures != null)
				gl.glDeleteTextures(textures.length, textures, 0);
			textures = new int[sprites.size()];
			gl.glGenTextures(textures.length, textures, 0);
		}
		
		for( int i=0; i < sprites.size(); ++i) {
			Sprite s = sprites.elementAt(i);
			s.index = i;
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[i]);
		    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, s.bitmap, 0);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		}

		dirty = false;
	}
	
	public static void release() {
		sprites.clear();
		dirty = true;
	}

	public int index;
	public Bitmap bitmap;

	public Sprite(Bitmap b) {
		this.bitmap = b;
		sprites.addElement(this);
		dirty = true;
	}

	public void bind(GL10 gl) {
		if(dirty) genTextures(gl);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[index]);
	}
}
