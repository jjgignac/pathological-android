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
import android.view.*;
import android.graphics.*;

public class BoardRenderer implements GLSurfaceView.Renderer
{
	private Game game;
	private float[] vertices;
	private static final float[] texture = {
		0.0f, 1.0f, 0.0f, 0.0f,
		1.0f, 1.0f, 1.0f, 0.0f
	};
	private FloatBuffer textureBuffer;
    private FloatBuffer vertexBuffer;
	private int width;
	private int height;
	private float scale;
	private float offsetx;
	private float offsety;
	private Rect rect;

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

		rect = new Rect();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//	    gl.glEnable(GL10.GL_TEXTURE_2D);
	    gl.glShadeModel(GL10.GL_SMOOTH);
	    gl.glDisable(GL10.GL_DEPTH_TEST);
	    gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	    gl.glEnable(GL10.GL_CULL_FACE);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
	}
	
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		int px = Board.screen_width + Board.timer_width;
		this.width = width;
		this.height = height;
		if(height == 0) height = 1;

		scale = width * Board.screen_height < height * px ?
			(float)width / px : (float)height / Board.screen_height;
		offsetx = width - px*scale;
		offsety = 0.0f;

		gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
		gl.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, -10.0f, 10.0f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
		Sprite.regenerateTextures();
	}

	@Override
	public void onDrawFrame(GL10 gl) {
//		gl.glClearColor(0, 0, 0.5f, 1.0f);
//		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();
		gl.glTranslatef(0.0f, 0.0f, -5.0f);

		game.paint(gl);
	}

	static final float s = 1.0f / 255.0f;

	public void fill( GL10 gl, int color,
		int x, int y, int w, int h) {
	    gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(((color>>16)&0xff)*s,((color>>8)&0xff)*s,
			(color&0xff)*s,((color>>24)&0xff)*s);
		blit(gl,x*scale+offsetx,y*scale+offsety,w*scale,h*scale);		
		gl.glColor4f(1f,1f,1f,1f);
	}
	
	public void blit( GL10 gl, int resid,
		int x, int y, int w, int h) {
	    gl.glEnable(GL10.GL_TEXTURE_2D);
		Sprite.bind(gl, resid);
		blit(gl,x*scale+offsetx,y*scale+offsety,w*scale,h*scale);		
	}

	public void blit( GL10 gl, int resid, int x, int y) {
	    gl.glEnable(GL10.GL_TEXTURE_2D);
		Sprite.bind(gl,resid);
		Bitmap b = Sprite.getBitmap(resid);
		blit(gl, x*scale+offsetx, y*scale+offsety,
			b.getWidth()*scale, b.getHeight()*scale);
	}

	public Rect getVisibleArea() {
		rect.left = (int)Math.floor(-offsetx/scale);
		rect.top = (int)Math.floor(-offsety/scale);
		rect.right = (int)Math.ceil((width-offsetx)/scale);
		rect.bottom = (int)Math.ceil((height-offsety)/scale);
		return rect;
	}

	private void blit( GL10 gl, float x, float y, float w, float h)
	{
		float left = x * 2.0f / width - 1.0f;
		float top = y * -2.0f / height + 1.0f;
		float wid = w * 2.0f / width;
		float hei = h * -2.0f / height;

		// Small optimization
		if(x > width || y > height) return;
		
		vertices[0] = vertices[3] = left;
		vertices[4] = vertices[10] = top;
		vertices[6] = vertices[9] = left + wid;
		vertices[1] = vertices[7] = top + hei;
		
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);

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

	public int evX( MotionEvent e, int index) {		
		return (int)Math.round((e.getX(index)-offsetx)/scale);
	}

	public int evY( MotionEvent e, int index) {		
		return (int)Math.round((e.getY(index)-offsety)/scale);
	}
}

