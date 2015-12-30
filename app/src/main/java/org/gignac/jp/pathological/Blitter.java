package org.gignac.jp.pathological;

public interface Blitter
{
    public void blit( int resid, int x, int y);
    public void blit( int resid, int x, int y, int w, int h);
    public void blit( int resid, int sx, int sy, int sw, int sh, int x, int y);
    public void blit( int resid, int sx, int sy, int sw, int sh, int x, int y, int w, int h);
    public void blit( long uniq, int x, int y);
    public void blit( long uniq, int x, int y, int w, int h);
    public void blit( long uniq, int sx, int sy, int sw, int sh, int x, int y, int w, int h);
    public void fill( int color, int x, int y, int w, int h);
    public int getWidth();
    public int getHeight();
    public void transform(float scale, float dx, float dy);
}
