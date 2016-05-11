/*
 * Copyright (C) 2016  John-Paul Gignac
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
