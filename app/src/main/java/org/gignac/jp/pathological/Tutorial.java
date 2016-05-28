package org.gignac.jp.pathological;

import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

class Tutorial {
    private static final Paint paint = new Paint();
    private static final RectF rect = new RectF();
    private static final int[] marbles = { 0, 0, 0, 0 };
    private static final TextPaint textPaint = new TextPaint();
    private StaticLayout staticLayout;

    private final Board board;
    private int stage;
    private float stageStartTime;
    private float dtSnapshot;
    private Wheel wheel1;
    private Wheel wheel2;

    public Tutorial(Board b, int initialStage) {
        board = b;
        stage = initialStage;

        // Get references to the two wheels on the board
        for( Tile[] tiles : b.tiles) {
            for( Tile tile : tiles) {
                if( !(tile instanceof Wheel)) continue;
                if( wheel1 == null) wheel1 = (Wheel)tile;
                else wheel2 = (Wheel)tile;
            }
        }
    }

    public void paint(CanvasBlitter b, float time) {
        float dt = time - stageStartTime;
        switch(stage) {
            case 0:
                // We are waiting for a marble to drop into the wheel
                if( wheel1.marbles[3] >= 0) {
                    stage = 1;
                    stageStartTime = time;
                }
                break;
            case 1:
                // Introduce the spin tutorial
                drawSpinWheelTutorial(board.gr, b, Math.min(dt, 1.0f), 0.0f);
                if( dt >= 1.0f) {
                    stage = 2;
                    stageStartTime = time;
                }
                break;
            case 2:
                // Animate the spin tutorial
                drawSpinWheelTutorial( board.gr, b, 1.0f, dt);
                if( dt >= 7f) {
                    stageStartTime = time;
                }
                break;
            case 4:
                // Wait for a marble in the right position
                if( wheel1.marbles[1] >= 0) {
                    stage = 5;
                    stageStartTime = time;
                }
                break;
            case 5:
                // Introduce the eject tutorial
                drawEjectTutorial( board.gr, b, Math.min(dt, 1.0f), 0.0f, false);
                if( dt >= 1.0f) {
                    stage = 6;
                    stageStartTime = time;
                }
                break;
            case 6:
                // Animate the eject tutorial
                drawEjectTutorial(board.gr, b, 1.0f, dt, false);
                int count = 0;
                for( int i=0; i < 4; ++i)
                    if( wheel2.marbles[i] >= 0) count ++;
                if( count > 1) {
                    stage = 7;
                    stageStartTime = time;
                    dtSnapshot = dt;
                    break;
                }
                break;
            case 7:
                // Send the eject tutorial away
                if( dt < 1.0f) {
                    drawEjectTutorial(board.gr, b, 1.0f - dt, dtSnapshot, true);
                } else {
                    staticLayout = null;
                    stage = 8;
                    stageStartTime = time;
                }
                break;
            case 8:
                // Draw the clear-wheels tutorial
                drawClearWheelsTutorial(board.gr, b);
                break;
            case 9:
                // Wait for the trigger to appear
                if( board.trigger.marbles != null) {
                    stage = 10;
                    stageStartTime = time;
                }
                break;
            case 10:
                // Introduce the trigger tutorial and wait for the trigger to be completed
                drawTriggerTutorial( board.gr, b, board, Math.min(dt, 1.0f));
                if( board.trigger.marbles == null) {
                    stage = 11;
                    stageStartTime = time;
                }
                break;
            case 11:
                // Send the trigger tutorial away
                if( dt < 1.0f) {
                    drawTriggerTutorial( board.gr, b, board, 1.0f - dt);
                } else {
                    stage = -1; // done
                }
                break;
        }
    }

    private static void drawFrame(CanvasBlitter b, int x, int y, int w, int h) {
        int cornerRadius = Tile.tile_size / 2;
        paint.setColor(0xffffffff);
        paint.setStyle(Paint.Style.FILL);
        rect.left = x;
        rect.top = y;
        rect.right = x + w;
        rect.bottom = y + h;
        b.c.drawRoundRect( rect, cornerRadius, cornerRadius, paint);
        paint.setColor(0xff000000);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        b.c.drawRoundRect( rect, cornerRadius, cornerRadius, paint);
    }

    private static void drawFingerTouchingXY(Blitter b, int x, int y) {
        b.blit(R.drawable.misc, 242, 434, 121, 275, x - 40, y - 10);
    }

    private void drawSpinWheelTutorial(GameResources gr, CanvasBlitter b,
                                       float visibility, float time) {
        int x = Tile.tile_size / 2;
        int y = Tile.tile_size * 3;
        int w = Tile.tile_size * 9 / 2;
        int h = Tile.tile_size * 3;
        int textWidth = Tile.tile_size * 9 / 4;
        int textMargin = 14;

        int offset = (int)Math.round(
                Math.pow(1f - visibility, 1.8) * Tile.tile_size * 7);

        x += offset;

        int wheelX = x + Tile.tile_size * 3 / 5;
        int wheelY = y + Tile.tile_size / 3;

        drawFrame(b, x, y, w, h);

        if( staticLayout == null) {
            textPaint.setTextSize(24);
            staticLayout = new StaticLayout(gr.context.getString(R.string.turn_wheel_instructions),
                    textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        }

        b.c.save();
        b.c.translate(x + w - textWidth - textMargin, y + textMargin);
        staticLayout.draw(b.c);
        b.c.restore();

        // Adjust time a bit to control the animation speed and phase
        time = time * 0.5f + 0.2f;

        float phase = time - (float)Math.floor(time);

        int spinPos = 0;
        if( phase < 0.2f) {
            spinPos = GameResources.wheel_steps - 1 -
                    (int)Math.floor(phase * GameResources.wheel_steps / 0.2f);
        }

        // Draw a path
        b.blit(R.drawable.misc, 415, 394, 24, 30,
                x, wheelY + Tile.tile_size / 2 - 15,
                wheelX + Tile.tile_size / 2 - x, 30);

        // Draw the wheel
        for( int i=0; i < 4; ++i) {
            marbles[(2-i)&3] = (time > i+1 ? 6 : -2);
        }
        Wheel.draw(gr, b, marbles, wheelX, wheelY, spinPos, false);

        // Draw the marble
        int marbleX = wheelX + gr.holecenters_x[0][3] - Marble.marble_size / 2;
        int marbleY = wheelY + gr.holecenters_y[0][3] - Marble.marble_size / 2;
        if( phase > 0.1f && phase < 0.5f) {
            marbleX -= Math.round((0.5f - phase) * 400);
        }
        int wid = Math.min(marbleX + Marble.marble_size - x, Marble.marble_size);
        if( wid > 0 && phase > 0.1f) {
            b.blit(R.drawable.misc,
                    28 * 6 + Marble.marble_size - wid, 357, wid, Marble.marble_size,
                    marbleX + Marble.marble_size - wid, marbleY, wid, Marble.marble_size);
        }

        float fingerPos = (float)Math.sin(phase * 2 * Math.PI) + 1f;

        if( fingerPos < 0.3f) {
            // Show the press
            int opacity = Math.round((0.3f - fingerPos) * 400);
            paint.setColor((opacity << 24) | 0x3080ff);
            paint.setStyle(Paint.Style.FILL);
            b.c.drawCircle( wheelX + Tile.tile_size / 2,
                    wheelY + Tile.tile_size / 2, 20, paint);
        }

        // Show the finger
        drawFingerTouchingXY(b,
                wheelX + Tile.tile_size / 2 + Math.round(fingerPos * 2),
                wheelY + Tile.tile_size / 2 + Math.round(fingerPos * 10));
    }

    private void drawEjectTutorial(GameResources gr, CanvasBlitter b,
                                   float visibility, float time, boolean done) {
        int x = Tile.tile_size / 2;
        int y = Tile.tile_size * 7 / 2;
        int w = Tile.tile_size * 5;
        int h = Tile.tile_size * 3;
        int textWidth = Tile.tile_size * 9 / 4;
        int textMargin = 14;

        int offset = (int)Math.round(
                Math.pow(1f - visibility, 1.8) * Tile.tile_size * 7);

        if( done) y += offset;
        else x += offset;

        int wheelX = x + Tile.tile_size / 4;
        int wheelY = y + Tile.tile_size / 4;

        drawFrame(b, x, y, w, h);

        if( staticLayout == null) {
            textPaint.setTextSize(24);
            staticLayout = new StaticLayout(gr.context.getString(R.string.eject_instructions),
                    textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        }

        b.c.save();
        b.c.translate(x + w - textWidth - textMargin,
                wheelY + Tile.tile_size / 2 + Marble.marble_size / 2 + textMargin);
        staticLayout.draw(b.c);
        b.c.restore();

        // Adjust time a bit to control the animation speed and phase
        time = (time + 1.0f) * 0.5f;

        float phase = time - (float)Math.floor(time);

        for( int i=0; i < 4; ++i) {
            marbles[i] = (i < 2 ? -2 : 3);
        }

        // Draw a path
        b.blit(R.drawable.misc, 415, 394, 24, 30,
                wheelX + Tile.tile_size / 2,
                wheelY + Tile.tile_size / 2 - 15,
                x + w - wheelX - Tile.tile_size / 2, 30);

        // Draw the wheel
        Wheel.draw(gr, b, marbles, wheelX, wheelY, 0, false);

        // Draw the marble
        int marbleX = wheelX + gr.holecenters_x[0][1] - Marble.marble_size / 2;
        int marbleY = wheelY + gr.holecenters_y[0][1] - Marble.marble_size / 2;
        if( phase < 0.5f) {
            marbleX += Math.round(phase * 400);
        }

        int wid = Math.min(x + w - marbleX, 28);
        b.blit( R.drawable.misc, 28*3, 357, wid, 28,
                marbleX, marbleY, wid, Marble.marble_size);

        float fingerPos = (float)Math.cos(phase * 2 * Math.PI) + 1f;

        float fingerX = wheelX + Tile.tile_size / 2 + fingerPos * 30;
        float fingerY = wheelY + Tile.tile_size / 2 + 5;

        if( phase < 0.5f) {
            // Swing the finger downwards slightly on its way back
            fingerY += 10 * Math.sin(phase * 2.0 * Math.PI);
        }

        if( phase > 0.5f) {
            // Show the press as an oval
            int opacity = Math.round(Math.min(phase - 0.5f, 0.1f) * 1200);
            paint.setColor((opacity << 24) | 0x3080ff);
            paint.setStyle(Paint.Style.FILL);
            rect.left = wheelX + Tile.tile_size / 2 - 20;
            rect.top = fingerY - 20;
            rect.right = fingerX + 20;
            rect.bottom = fingerY + 20;
            b.c.drawRoundRect( rect, 20, 20, paint);
        }

        // Show the finger
        drawFingerTouchingXY(b, Math.round(fingerX), Math.round(fingerY));
    }

    private void drawClearWheelsTutorial(GameResources gr, CanvasBlitter b) {
        int x = Tile.tile_size / 4;
        int y = 16;
        int w = Tile.tile_size * 23 / 4;
        int h = Tile.tile_size + 40;
        int textWidth = Tile.tile_size * 5 / 2;
        int textMargin = 17;

        int wheel1X = x + 20;
        int wheel2X = wheel1X + Tile.tile_size + 46;
        int wheelY = y + 20;

        drawFrame(b, x, y, w, h);

        if( staticLayout == null) {
            textPaint.setTextSize(24);
            staticLayout = new StaticLayout(gr.context.getString(R.string.completion_instructions),
                    textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        }

        b.c.save();
        b.c.translate(x + w - textWidth - textMargin, y + textMargin);
        staticLayout.draw(b.c);
        b.c.restore();

        // Draw the incomplete wheel
        for( int i=0; i < 4; ++i) marbles[i] = 3;
        Wheel.draw(gr, b, marbles, wheel1X, wheelY, 0, false);

        // Draw the arrow
        b.blit( R.drawable.misc, 393, 732, 29, 30,
                wheel1X + Tile.tile_size + 11,
                wheelY + 30);

        // Draw the completed wheel
        for( int i=0; i < 4; ++i) marbles[i] = -2;
        Wheel.draw(gr, b, marbles, wheel2X, wheelY, 0, true);
    }

    private static void drawTriggerTutorial(GameResources gr, CanvasBlitter b,
                                            Board board, float visibility) {
        int x = Tile.tile_size * 3 + 20;
        int y = Tile.tile_size * 3 - 20;
        int w = Tile.tile_size * 3 / 2;
        int h = Tile.tile_size * 2 - 20;

        int offset = (int)Math.round(
                Math.pow(1f - visibility, 1.8) * Tile.tile_size * 4);
        x += offset;

        int wheelX = x + (w - Tile.tile_size) / 2;
        int wheelY = y + 20;

        drawFrame(b, x, y, w, h);

        // Draw the wheel
        if( board.trigger.marbles != null) {
            for (int i = 0; i < 4; ++i)
                marbles[i] = board.trigger.marbles.charAt(i) - '0';
        }
        Wheel.draw(gr, b, marbles, wheelX, wheelY, 0, false);

        // Draw the check
        b.blit( R.drawable.misc, 392, 677, 29, 29,
                wheelX + (Tile.tile_size - 29) / 2, wheelY + Tile.tile_size + 8);
    }
}
