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

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.content.*;
import android.util.*;
import android.view.View;

@SuppressWarnings("unused")
public class GameView extends View
{
    private final CanvasBlitter b;
    private Board board;

    public GameView(Context c,AttributeSet a) {
        super(c,a);
        b = new CanvasBlitter(GameResources.getInstance(c).sc);
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    @Override
    protected void onDraw(Canvas c) {
        b.setCanvas(c, getWidth(), getHeight());
        if(board != null) board.paint(b, true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        final int action = e.getAction();
        final int index =
            (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
            MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int id = e.getPointerId(index);

        switch(action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN:
            board.downEvent(id,e.getX(index),e.getY(index));
            return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
            board.upEvent(id,e.getX(index),e.getY(index));
            return true;
        }

        return false;
    }
}
