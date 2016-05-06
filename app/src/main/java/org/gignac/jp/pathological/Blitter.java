package org.gignac.jp.pathological;

@SuppressWarnings("SameParameterValue")
interface Blitter
{
    void blit( int resid, int x, int y, int w, int h);
    void blit( int resid, int sx, int sy, int sw, int sh, int x, int y);
    void blit( int resid, int sx, int sy, int sw, int sh, int x, int y, int w, int h);
    void blit( long uniq, int x, int y);
    void blit( long uniq, int sx, int sy, int sw, int sh, int x, int y, int w, int h);
    void fill( int color, int x, int y, int w, int h);
    int getWidth();
    int getHeight();
    void pushTransform(float scale, float dx, float dy);
    void popTransform();
}
