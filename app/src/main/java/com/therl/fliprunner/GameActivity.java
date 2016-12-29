package com.therl.fliprunner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class GameActivity extends AppCompatActivity {

    /** A handle to the thread that's actually running the animation. */
    private GameSurface.GameThread mGameThread;
    /** A handle to the View in which the game is running. */
    private GameSurface mGameSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        // get handles to the LunarView from XML, and its LunarThread
        mGameSurface = (GameSurface) findViewById(R.id.game);
        mGameThread = mGameSurface.getThread();
        if (savedInstanceState == null) {
            // we were just launched: set up a new game
            mGameThread.setState(mGameThread.STATE_READY);
            Log.w(this.getClass().getName(), "SIS is null");
        } else {
            // we are being restored: resume a previous game
            mGameThread.restoreState(savedInstanceState);
            Log.w(this.getClass().getName(), "SIS is nonnull");
        }
    }

    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        mGameSurface.getThread().pause(); // pause game when Activity pauses
        super.onPause();
    }

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     *
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mGameThread.saveState(outState);
        Log.w(this.getClass().getName(), "SIS called");
    }
}
