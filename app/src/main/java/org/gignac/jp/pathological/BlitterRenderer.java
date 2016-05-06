package org.gignac.jp.pathological;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;

public class BlitterRenderer
    implements GLSurfaceView.Renderer, Blitter
{
    private Paintable painter;
    private final float[] vertices;
    private final float[] texture = new float[8];
    private final FloatBuffer textureBuffer;
    private final FloatBuffer vertexBuffer;
    private int width, height;
    private GL10 gl;
    private final SpriteCache sc;

    BlitterRenderer(SpriteCache sc)
    {
        vertices = new float[12];
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        vertexBuffer = byteBuffer.asFloatBuffer();

        byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        textureBuffer = byteBuffer.asFloatBuffer();

        this.sc = sc;
    }

    public void setPaintable( Paintable painter)
    {
        this.painter = painter;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//    gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, -10.0f, 10.0f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        sc.regenerateTextures();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
//        gl.glClearColor(0, 0, 0.5f, 1.0f);
//        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        gl.glTranslatef(-1f,1f,-5f);
        gl.glScalef(2f/width,-2f/height,1f);

        this.gl = gl;
        if(painter != null) painter.paint(this);
    }

    private static final float s = 1.0f / 255.0f;

    @Override
    public void transform(float scale, float dx, float dy)
    {
        gl.glScalef(scale,scale,1.0f);
        gl.glTranslatef(dx,dy,0.0f);
    }

    @Override
    public void fill(int color, int x, int y, int w, int h) {
        gl.glDisable(GL10.GL_TEXTURE_2D);
        gl.glColor4f(((color>>16)&0xff)*s,((color>>8)&0xff)*s,
            (color&0xff)*s,((color>>24)&0xff)*s);
        blit(0f,0f,1f,1f,x,y,w,h);
        gl.glColor4f(1f,1f,1f,1f);
    }

    @Override
    public void blit(int resid, int x, int y, int w, int h) {
        blit(resid&0xffffffffL, x, y, w, h);
    }

    @Override
    public void blit(int resid, int sx, int sy,
        int sw, int sh, int x, int y, int w, int h) {
        blit(resid&0xffffffffL, sx, sy, sw, sh, x, y, w, h);
    }

    private void blit(long uniq, int x, int y, int w, int h) {
        gl.glEnable(GL10.GL_TEXTURE_2D);
        sc.bind(gl, uniq);
        blit(0f,0f,1f,1f,x,y,w,h);
    }

    @Override
    public void blit( int resid, int sx, int sy,
        int sw, int sh, int x, int y) {
        blit(resid&0xffffffffL, sx, sy, sw, sh, x, y, sw, sh);
    }

    @Override
    public void blit( long uniq, int x, int y) {
        gl.glEnable(GL10.GL_TEXTURE_2D);
        sc.bind(gl, uniq);
        Bitmap b = sc.getBitmap(uniq);
        blit(0f, 0f, 1f, 1f,
            x, y, b.getWidth(), b.getHeight());
    }

    public void blit( long uniq, int sx, int sy,
        int sw, int sh, int x, int y, int w, int h) {
        gl.glEnable(GL10.GL_TEXTURE_2D);
        sc.bind(gl, uniq);
        Bitmap b = sc.getBitmap(uniq);
        int bw = b.getWidth();
        int bh = b.getHeight();
        blit((float)sx*(1f/bw), (float)sy*(1f/bh),
            (float)(sx+sw)*(1f/bw), (float)(sy+sh)*(1f/bh),
            x, y, w, h);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    private void blit( float x0, float y0, float x1, float y1,
            float x, float y, float w, float h)
    {
        texture[0] = texture[4] = x0;
        texture[1] = texture[3] = y0;
        texture[2] = texture[6] = x1;
        texture[5] = texture[7] = y1;

        textureBuffer.put(texture);
        textureBuffer.position(0);

        vertices[0] = vertices[6] = x;
        vertices[1] = vertices[4] = y;
        vertices[3] = vertices[9] = x + w;
        vertices[7] = vertices[10] = y + h;

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
}

