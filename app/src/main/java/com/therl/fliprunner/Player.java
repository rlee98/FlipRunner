package com.therl.fliprunner;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.SurfaceHolder;

import static com.therl.fliprunner.GameSurface.GameThread.PHYS_DOWN_ACCEL_SEC;

/**
 * Created by Limmy on 1/3/2017.
 */

public class Player {

    /*
     * UI constants (i.e. the speed & fuel bars)
     */
    private static final String KEY_DX = "mDX";
    private static final String KEY_DY = "mDY";
    private static final String KEY_X = "mX";
    private static final String KEY_Y = "mY";

    /*
     * Jumping constants
     */
    public static final int JUMPING_DOWN = -1;
    public static final int JUMPING_NONE = 0;
    public static final int JUMPING_UP = 1;
    /*
     * Player Constants
     */
    public static final int JUMP_SPEED = 3000;

    /** Player Jumping Status */
    private int mJumping;

    /** Velocity dx. */
    private double mDX;
    /** Velocity dy. */
    private double mDY;

    /** X of player. */
    private double mX;
    /** Y of player. */
    private double mY;
    
    /** Width of player. */
    private int width;
    /** Height of player. */
    private int height;

    public Player(){
        mX = 0;
        mY = 0;
        mDX = 0;
        mDY = 0;

        // TODO: 1/3/2017 temp width/height
        width = 20;
        height = 20;
    }

    public void reset(){
        mX = 0;
        mY = 0;
        mDX = 0;
        mDY = 0;
    }
    /**
     * Dump game state to the provided Bundle. Typically called when the
     * Activity is being suspended.
     */
    public void saveState(Bundle map){
        map.putDouble(KEY_X, Double.valueOf(mX));
        map.putDouble(KEY_Y, Double.valueOf(mY));
        map.putDouble(KEY_DX, Double.valueOf(mDX));
        map.putDouble(KEY_DY, Double.valueOf(mDY));
    }
    /**
     * Restores game state from the indicated Bundle. Typically called when
     * the Activity is being restored after having been previously
     * destroyed.
     *
     * @param savedState Bundle containing the game state
     */
    public void restoreState(Bundle savedState){
        mX = savedState.getDouble(KEY_X);
        mY = savedState.getDouble(KEY_Y);
        mDX = savedState.getDouble(KEY_DX);
        mDY = savedState.getDouble(KEY_DY);
    }

    /**
     * Sets if the player is jumping. That is, whether upwards, downwards, or not.
     *
     * @param jumping one of the STATE_* constants
     * @return true
     */
    public boolean setJumping(int jumping) {
        if(mJumping != JUMPING_NONE && jumping != JUMPING_NONE)
            return false;
        mJumping = jumping;
        if(jumping == JUMPING_UP)
            mDY += JUMP_SPEED;
        else if(jumping == JUMPING_DOWN)
            mDY -= JUMP_SPEED;
        return true;
    }

    public void doDraw(Canvas canvas, int canvasWidth, int canvasHeight){

        int offset = canvasWidth/8;
        int yMid = canvasHeight/2 - ((int) mY + height/ 2);
        int xLeft = (int) mX + offset - width / 2;

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        int radius = 30;
        canvas.drawCircle(xLeft, yMid , radius, paint);
    }
    public void updatePhysics(double elapsed){
        // Base accelerations -- 0 for x, gravity for y
        double ddx = 0.0;
        double ddy = 0.0;
        if (mY > 0 && mJumping == JUMPING_UP)
            ddy = -PHYS_DOWN_ACCEL_SEC * elapsed;
        else if (mY < 0 && mJumping == JUMPING_DOWN)
            ddy = PHYS_DOWN_ACCEL_SEC * elapsed;

        double dxOld = mDX;
        double dyOld = mDY;
        // figure speeds for the end of the period
        mDX += ddx;
        mDY += ddy;
        // figure position based on average speed during the period
        mX += elapsed * (mDX + dxOld) / 2;
        mY += elapsed * (mDY + dyOld) / 2;

        if (mY <= 0 && mJumping == JUMPING_UP) {
            mY = 0;
            mDY = 0;
            setJumping(JUMPING_NONE);
        } else if (mY >= 0 && mJumping == JUMPING_DOWN) {
            mY = 0;
            mDY = 0;
            setJumping(JUMPING_NONE);
        }
    }
}
