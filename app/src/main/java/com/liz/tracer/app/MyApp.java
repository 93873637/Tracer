package com.liz.tracer.app;

import com.liz.androidutils.LogEx;
import com.liz.androidutils.SysUtils;
import com.liz.androidutils.app.AppEx;
import com.liz.tracer.R;
import com.liz.tracer.logic.ComDef;
import com.liz.tracer.logic.DataLogic;
import com.liz.tracer.logic.SoundPlayer;

/**
 * MyApp.java
 * Created by liz on 18-1-8.
 */

@SuppressWarnings("unused")
public class MyApp extends AppEx {

    private static SoundPlayer mSoundMaxSpeed;
    private static SoundPlayer mSoundKilometer;
    private static SoundPlayer mSoundKMChampion;
    private static SoundPlayer mSoundMinuteChampion;

    @Override
    public void onCreate() {
        super.onCreate();

        LogEx.setTag(ComDef.APP_NAME);
        LogEx.setLogDir(ComDef.TRACER_LOG_DIR);
        LogEx.setLogFilePrefix("tracer");
        LogEx.setMaxLogFileSize(20 * 1024 * 1024);
        LogEx.setSaveToFile(true);

        SysUtils.setAudioVolumeMax(this);
        DataLogic.inst().init();

        mSoundMaxSpeed = new SoundPlayer(R.raw.beep);
        mSoundKilometer = new SoundPlayer(R.raw.europa);
        mSoundKMChampion = new SoundPlayer(R.raw.victory_km_champion);
        mSoundMinuteChampion = new SoundPlayer(R.raw.victory_minute_champion);
    }

    public static void playSoundMaxSpeed() {
        mSoundMaxSpeed.playSound();
    }

    public static void playSoundKilometer() {
        mSoundKilometer.playSound();
    }

    public static void playSoundKMRecord(int index) {
        switch (index) {
            case ComDef.INDEX_CHAMPION:
                mSoundKMChampion.playSound();
                return;
            default:
                playSoundKilometer();
                break;
        }
    }

    public static void playSoundMinuteRecord(int index) {
        switch (index) {
            case ComDef.INDEX_CHAMPION:
                mSoundMinuteChampion.playSound();
                return;
            default:
                playSoundKilometer();
                break;
        }
    }
}
