package com.liz.tracer.logic;

import android.content.Context;
import android.media.SoundPool;

import com.liz.androidutils.LogUtils;
import com.liz.tracer.app.MyApp;

public class SoundPlayer {

    private SoundPool mSoundPool;
    private int mSoundID = 0;
    private boolean mLoaded = false;

    public SoundPlayer(int resId) {
        loadSound(MyApp.getAppContext(), resId);
    }

    public void loadSound(Context context, int resId) {
        mSoundPool = new SoundPool(10, 3, 5);
        mSoundID = mSoundPool.load(context, resId, 1);
        mSoundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            //mSoundPool.play(mSoundID, 1.0F, 1.0F, 1, 0, 1.0F);
            mLoaded = true;
        });
    }

    public void playSound() {
        if (!mLoaded) {
            LogUtils.te("sound file load failed.");
        }
        else {
            new Thread() {
                public void run() {
                    mSoundPool.play(mSoundID, 1.0F, 1.0F, 1, 0, 1.0F);
                }
            }.start();
        }
    }
}
