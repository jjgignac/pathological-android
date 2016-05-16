package org.gignac.jp.pathological;

import android.graphics.Paint;
import android.graphics.RectF;

class Tutorial {
    private static Paint paint = new Paint();
    private static RectF rect = new RectF();
    private static int[] marbles = { 0, 0, 0, 0 };

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
                drawSpinWheelTutorial(board.gr, b, Math.min(dt, 1.0f), 0.0f, false);
                if( dt >= 1.0f) {
                    stage = 2;
                    stageStartTime = time;
                }
                break;
            case 2:
                // Animate the spin tutorial
                drawSpinWheelTutorial( board.gr, b, 1.0f, dt, false);
                if( dt > 6.5f) {
                    stage = 3;
                    stageStartTime = time;
                    dtSnapshot = dt;
                }
                break;
            case 3:
                // Send the spin tutorial away
                if( dt < 1.0f) {
                    drawSpinWheelTutorial(board.gr, b, 1.0f - dt, dtSnapshot + dt, true);
                } else {
                    stage = -1; // done
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
                for( int i=0; i < 4; ++i) {
                    if( wheel2.marbles[i] < 0) continue;
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

    static void drawSpinWheelTutorial(GameResources gr, CanvasBlitter b,
                                      float visibility, float time, boolean done) {
        int x = Tile.tile_size / 2;
        int y = Tile.tile_size * 7 / 2;
        int w = Tile.tile_size * 5 / 2;
        int h = Tile.tile_size * 3;

        int offset = (int)Math.round(
                Math.pow(1f - visibility, 1.8) * Tile.tile_size * 7);

        if( done) y += offset;
        else x += offset;

        int wheelX = x + Tile.tile_size * 3 / 5;
        int wheelY = y + Tile.tile_size / 3;

        drawFrame(b, x, y, w, h);

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

    static void drawEjectTutorial(GameResources gr, CanvasBlitter b,
                                  float visibility, float time, boolean done) {
        int x = Tile.tile_size / 2;
        int y = Tile.tile_size * 7 / 2;
        int w = Tile.tile_size * 3;
        int h = Tile.tile_size * 3;

        int offset = (int)Math.round(
                Math.pow(1f - visibility, 1.8) * Tile.tile_size * 7);

        if( done) y += offset;
        else x += offset;

        int wheelX = x + Tile.tile_size * 3 / 5;
        int wheelY = y + Tile.tile_size / 3;

        drawFrame(b, x, y, w, h);

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
}
