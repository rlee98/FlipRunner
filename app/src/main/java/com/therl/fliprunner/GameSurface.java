package com.therl.fliprunner;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Limmy on 12/27/2016.
 */

public class GameSurface extends SurfaceView implements SurfaceHolder.Callback {
    class GameThread extends Thread {
        /*
         * Physics constants
         */
        public static final int PHYS_DOWN_ACCEL_SEC = 100;
        /*
         * State-tracking constants
         */
        public static final int STATE_LOSE = 1;
        public static final int STATE_PAUSE = 2;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;
        /*
         * UI constants (i.e. the speed & fuel bars)
         */
        private static final String KEY_DX = "mDX";
        private static final String KEY_DY = "mDY";
        private static final String KEY_X = "mX";
        private static final String KEY_Y = "mY";

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;
        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;
        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        private final Object mRunLock = new Object();

        /**
         * Current height of the surface/canvas.
         *
         * @see #setSurfaceSize
         */
        private int mCanvasHeight = 1;
        /**
         * Current width of the surface/canvas.
         *
         * @see #setSurfaceSize
         */
        private int mCanvasWidth = 1;

        /** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
        private int mMode;

        /** Velocity dx. */
        private double mDX;
        /** Velocity dy. */
        private double mDY;

        /** X of lander center. */
        private double mX;
        /** Y of lander center. */
        private double mY;

        /** Used to figure out elapsed time between frames */
        private long mLastTime;

        public GameThread(SurfaceHolder surfaceHolder, Context context,
                           Handler handler) {
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;

            Resources res = context.getResources();
            //mBackgroundImage = BitmapFactory.decodeResource(res,
            //        R.drawable.earthrise);
            // TODO: 12/27/2016
        }

        public void doStart() {
            synchronized (mSurfaceHolder) {
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                grid.drawCircle(w/2, h/2 , w/2, paint);

                // pick a convenient initial location for the lander sprite
                int offset = mCanvasWidth/8;
                mX = offset;
                mY = (mCanvasHeight - mPlayerHeight) / 2;

                mDY = 0;
                mDX = 0;

                mLastTime = System.currentTimeMillis() + 100;
                setState(STATE_RUNNING);
            }
        }

        /**
         * Pauses the physics update & animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING) setState(STATE_PAUSE);
            }
        }

        /**
         * Restores game state from the indicated Bundle. Typically called when
         * the Activity is being restored after having been previously
         * destroyed.
         *
         * @param savedState Bundle containing the game state
         */
        public synchronized void restoreState(Bundle savedState) {
            synchronized (mSurfaceHolder) {
                setState(STATE_PAUSE);

                mX = savedState.getDouble(KEY_X);
                mY = savedState.getDouble(KEY_Y);
                mDX = savedState.getDouble(KEY_DX);
                mDY = savedState.getDouble(KEY_DY);
            }
        }
        
        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mMode == STATE_RUNNING) updatePhysics();
                        // Critical section. Do not allow mRun to be set false until
                        // we are sure all canvas draw operations are complete.
                        //
                        // If mRun has been toggled false, inhibit canvas operations.
                        synchronized (mRunLock) {
                            if (mRun) doDraw(c);
                        }
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }
        /**
         * Dump game state to the provided Bundle. Typically called when the
         * Activity is being suspended.
         *
         * @return Bundle with this view's state
         */
        public Bundle saveState(Bundle map) {
            synchronized (mSurfaceHolder) {
                if (map != null) {
                    map.putDouble(KEY_X, Double.valueOf(mX));
                    map.putDouble(KEY_Y, Double.valueOf(mY));
                    map.putDouble(KEY_DX, Double.valueOf(mDX));
                    map.putDouble(KEY_DY, Double.valueOf(mDY));
                }
            }
            return map;
        }
        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         *
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            // Do not allow mRun to be modified while any canvas operations
            // are potentially in-flight. See doDraw().
            synchronized (mRunLock) {
                mRun = b;
            }
        }
        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         *
         * @see #setState(int, CharSequence)
         * @param mode one of the STATE_* constants
         */
        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
                setState(mode, null);
            }
        }
        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         *
         * @param mode one of the STATE_* constants
         * @param message string to add to screen or null
         */
        public void setState(int mode, CharSequence message) {
            /*
             * This method optionally can cause a text message to be displayed
             * to the user when the mode changes. Since the View that actually
             * renders that text is part of the main View hierarchy and not
             * owned by this thread, we can't touch the state of that View.
             * Instead we use a Message + Handler to relay commands to the main
             * thread, which updates the user-text View.
             */
            synchronized (mSurfaceHolder) {
                mMode = mode;
                if (mMode == STATE_RUNNING) {
                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", "");
                    b.putInt("viz", View.INVISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                } else {
                    Resources res = mContext.getResources();
                    CharSequence str = "";
                    if (mMode == STATE_READY)
                        str = res.getText(R.string.mode_ready);
                    else if (mMode == STATE_PAUSE)
                        str = res.getText(R.string.mode_pause);
                    else if (mMode == STATE_LOSE)
                        str = res.getText(R.string.mode_lose);
                    if (message != null) {
                        str = message + "\n" + str;
                    }
                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", str.toString());
                    b.putInt("viz", View.VISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
            }
        }
        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;
                // don't forget to resize the background image
                mBackgroundImage = Bitmap.createScaledBitmap(
                        mBackgroundImage, width, height, true);
            }
        }
        /**
         * Resumes from a pause.
         */
        public void unpause() {
            // Move the real time clock up to now
            synchronized (mSurfaceHolder) {
                mLastTime = System.currentTimeMillis() + 100;
            }
            setState(STATE_RUNNING);
        }
//        /**
//         * Handles a key-down event.
//         *
//         * @param keyCode the key that was pressed
//         * @param msg the original event object
//         * @return true
//         */
//        boolean doKeyDown(int keyCode, KeyEvent msg) {
//            synchronized (mSurfaceHolder) {
//                boolean okStart = false;
//                // TODO: 12/27/2016 replace keyevents
//                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) okStart = true;
//                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) okStart = true;
//                if (keyCode == KeyEvent.KEYCODE_S) okStart = true;
//                if (okStart
//                        && (mMode == STATE_READY || mMode == STATE_LOSE )) {
//                    // ready-to-start -> start
//                    doStart();
//                    return true;
//                } else if (mMode == STATE_PAUSE && okStart) {
//                    // paused -> running
//                    unpause();
//                    return true;
//                } else if (mMode == STATE_RUNNING) {
//                    // center/space -> fire
//                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
//                            || keyCode == KeyEvent.KEYCODE_SPACE) {
//                        setFiring(true);
//                        return true;
//                        // left/q -> left
//                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
//                            || keyCode == KeyEvent.KEYCODE_Q) {
//                        mRotating = -1;
//                        return true;
//                        // right/w -> right
//                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
//                            || keyCode == KeyEvent.KEYCODE_W) {
//                        mRotating = 1;
//                        return true;
//                        // up -> pause
//                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
//                        pause();
//                        return true;
//                    }
//                }
//                return false;
//            }
//        }
//        /**
//         * Handles a key-up event.
//         *
//         * @param keyCode the key that was pressed
//         * @param msg the original event object
//         * @return true if the key was handled and consumed, or else false
//         */
//        boolean doKeyUp(int keyCode, KeyEvent msg) {
//            boolean handled = false;
//            synchronized (mSurfaceHolder) {
//                if (mMode == STATE_RUNNING) {
//                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
//                            || keyCode == KeyEvent.KEYCODE_SPACE) {
//                        setFiring(false);
//                        handled = true;
//                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
//                            || keyCode == KeyEvent.KEYCODE_Q
//                            || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
//                            || keyCode == KeyEvent.KEYCODE_W) {
//                        mRotating = 0;
//                        handled = true;
//                    }
//                }
//            }
//            return handled;
//        }
        /**
         * Draws the ship, fuel/speed bars, and background to the provided
         * Canvas.
         */
        private void doDraw(Canvas canvas) {
            // Draw the background image. Operations on the Canvas accumulate
            // so this is like clearing the screen.
            canvas.drawBitmap(mBackgroundImage, 0, 0, null);
            int yTop = mCanvasHeight - ((int) mY + mLanderHeight / 2);
            int xLeft = (int) mX - mLanderWidth / 2;

            mScratchRect.set(4, 4, 4 + fuelWidth, 4 + UI_BAR_HEIGHT);
            canvas.drawRect(mScratchRect, mLinePaint);
            // Draw the speed gauge, with a two-tone effect
        }
        /**
         * Figures the lander state (x, y, fuel, ...) based on the passage of
         * realtime. Does not invalidate(). Called at the start of draw().
         * Detects the end-of-game and sets the UI to the next state.
         */
        private void updatePhysics() {
            long now = System.currentTimeMillis();
            // Do nothing if mLastTime is in the future.
            // This allows the game-start to delay the start of the physics
            // by 100ms or whatever.
            if (mLastTime > now) return;
            double elapsed = (now - mLastTime) / 1000.0;

            // Base accelerations -- 0 for x, gravity for y
            double ddx = 0.0;
            double ddy = 0.0;
            if (mY > 0)
                ddy = -PHYS_DOWN_ACCEL_SEC * elapsed;
            else if (mY < 0)
                ddy = PHYS_DOWN_ACCEL_SEC * elapsed;
//            if (mEngineFiring) {
//                // taking 0 as up, 90 as to the right
//                // cos(deg) is ddy component, sin(deg) is ddx component
//                double elapsedFiring = elapsed;
//                double fuelUsed = elapsedFiring * PHYS_FUEL_SEC;
//                // tricky case where we run out of fuel partway through the
//                // elapsed
//                if (fuelUsed > mFuel) {
//                    elapsedFiring = mFuel / fuelUsed * elapsed;
//                    fuelUsed = mFuel;
//                    // Oddball case where we adjust the "control" from here
//                    mEngineFiring = false;
//                }
//                mFuel -= fuelUsed;
//                // have this much acceleration from the engine
//                double accel = PHYS_FIRE_ACCEL_SEC * elapsedFiring;
//                double radians = 2 * Math.PI * mHeading / 360;
//                ddx = Math.sin(radians) * accel;
//                ddy += Math.cos(radians) * accel;
//            }
            double dxOld = mDX;
            double dyOld = mDY;
            // figure speeds for the end of the period
            mDX += ddx;
            mDY += ddy;
            // figure position based on average speed during the period
            mX += elapsed * (mDX + dxOld) / 2;
            mY += elapsed * (mDY + dyOld) / 2;
            mLastTime = now;
//            if (mY <= yLowerBound) {
//                mY = yLowerBound;
//                int result = STATE_LOSE;
//                CharSequence message = "";
//                Resources res = mContext.getResources();
//                double speed = Math.hypot(mDX, mDY);
//                boolean onGoal = (mGoalX <= mX - mLanderWidth / 2 && mX
//                        + mLanderWidth / 2 <= mGoalX + mGoalWidth);
//                // "Hyperspace" win -- upside down, going fast,
//                // puts you back at the top.
//                if (onGoal && Math.abs(mHeading - 180) < mGoalAngle
//                        && speed > PHYS_SPEED_HYPERSPACE) {
//                    result = STATE_WIN;
//                    mWinsInARow++;
//                    doStart();
//                    return;
//                    // Oddball case: this case does a return, all other cases
//                    // fall through to setMode() below.
//                } else if (!onGoal) {
//                    message = res.getText(R.string.message_off_pad);
//                } else if (!(mHeading <= mGoalAngle || mHeading >= 360 - mGoalAngle)) {
//                    message = res.getText(R.string.message_bad_angle);
//                } else if (speed > mGoalSpeed) {
//                    message = res.getText(R.string.message_too_fast);
//                } else {
//                    result = STATE_WIN;
//                    mWinsInARow++;
//                }
//                setState(result, message);
//            }
        }
    }

    /** Handle to the application context, used to e.g. fetch Drawables. */
    private Context mContext;
    /** The thread that actually draws the animation */
    private GameThread thread;
    /** Pointer to the text view to display "Paused.." etc. */
    private TextView mStatusText;


    public GameSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        // create thread only; it's started in surfaceCreated()
        thread = new GameThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
                mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(m.getData().getString("text"));
            }
        });
        setFocusable(true); // make sure we get key events
    }
    /**
     * Fetches the animation thread corresponding to this LunarView.
     *
     * @return the animation thread
     */
    public GameThread getThread() {
        return thread;
    }
//    /**
//     * Standard override to get key-press events.
//     */
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent msg) {
//        return thread.doKeyDown(keyCode, msg);
//    }
//    /**
//     * Standard override for key-up. We actually care about these, so we can
//     * turn off the engine or stop rotating.
//     */
//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent msg) {
//        return thread.doKeyUp(keyCode, msg);
//    }
    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) thread.pause();
    }
    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        thread.setSurfaceSize(width, height);
    }
    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread.setRunning(true);
        thread.start();
    }
    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }
}
