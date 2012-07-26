package org.gignac.jp.pathological;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;

public class BoardRenderer implements GLSurfaceView.Renderer
{
	private Game game;
    private int[] textures = new int[1];
	private float[] vertices;
	private static final float[] texture = {
		0.0f, 1.0f, 0.0f, 0.0f,
		1.0f, 1.0f, 1.0f, 0.0f
	};
	private FloatBuffer textureBuffer;
    private FloatBuffer vertexBuffer;
	private int width;
	private int height;

	BoardRenderer( Game game)
	{
		this.game = game;

		vertices = new float[12];
   		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
   		byteBuffer.order(ByteOrder.nativeOrder());
   		vertexBuffer = byteBuffer.asFloatBuffer();

   		byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
   		byteBuffer.order(ByteOrder.nativeOrder());
   		textureBuffer = byteBuffer.asFloatBuffer();

		textureBuffer.put(texture);
		textureBuffer.position(0);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
	    gl.glEnable(GL10.GL_TEXTURE_2D);
	    gl.glShadeModel(GL10.GL_SMOOTH);
	    gl.glDisable(GL10.GL_DEPTH_TEST);
	    gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	    gl.glEnable(GL10.GL_CULL_FACE);
	}
	
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		this.width = width;
		this.height = height;
		if(height == 0) height = 1;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
		gl.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, -10.0f, 10.0f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
    	gl.glGenTextures(1, textures, 0);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(0, 0, 0.5f, 1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();
		gl.glTranslatef(0.0f, 0.0f, -5.0f);

		game.paint(gl);
	}

	public void blit( GL10 gl, Bitmap bitmap,
		float x, float y, float w, float h)
	{
		float left = x * 2.0f / width - 1.0f;
		float top = y * -2.0f / height + 1.0f;
		float wid = w * 2.0f / width;
		float hei = h * -2.0f / height;
		
		vertices[0] = vertices[3] = left;
		vertices[4] = vertices[10] = top;
		vertices[6] = vertices[9] = left + wid;
		vertices[1] = vertices[7] = top + hei;
		
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);

	    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
    	gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
    	gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
    	gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
	    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
	    gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	    gl.glFrontFace(GL10.GL_CW);
	    synchronized(vertexBuffer) {
	    	gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
	    	gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
	    	gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertexBuffer.capacity() / 3);
	    }
	    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	    gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	}
}

