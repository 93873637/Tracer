package com.liz.tracer.logic;

import android.annotation.SuppressLint;
import android.graphics.Color;

public class ComDef {

    public static final String APP_NAME = "Tracer";

    @SuppressLint("SdCardPath")
    public static final String TRACER_LOG_DIR = "/sdcard/0.sd/tracer/log";

    public static final int LOCATION_UPDATE_MIN_TIME = 1000;  // unit by mill-second
    public static final float LOCATION_UPDATE_MIN_DISTANCE = 0.5f;  // unit by meter

    public static final int LOCATION_CHECK_TIMER_DELAY = 100;
    public static final int LOCATION_CHECK_TIMER_PERIOD = 1000;

    public static final String TIME_RESET_STRING = "00:00:00";

    public static final double DEFAULT_TARGET_SPKM = 4.0 * 60;  // target seconds per km
    public static final double DEFAULT_TARGET_SPEED = 1000.0 / DEFAULT_TARGET_SPKM;  // 4.167 m/s, i.e. 1 km / 4 minutes, 15km / 1 hour
    public static final double DEFAULT_TARGET_MINUTE_METERS = 1000.0 / (DEFAULT_TARGET_SPKM / 60);  // 250 meters
    
    public static final SpeedColor[] SPEED_BAR_COLORS = {
            new SpeedColor(5.0 / 3.6, 0xff7788ff),
            new SpeedColor(10.0 / 3.6, 0xffaa66ff),
            new SpeedColor(20.0 / 3.6, 0xffff00ff),
            new SpeedColor(50.0 / 3.6, 0xffff0000),
            new SpeedColor(100.0 / 3.6, 0xffff7700),
            new SpeedColor(200.0 / 3.6, 0xffff77ff),
            new SpeedColor(500.0 / 3.6, 0xff99ffff),
            new SpeedColor(1000.0 / 3.6, 0xffffffff),
    };

    public static final int FINAL_SPEED_COLOR = 0xffffff00;

    public static final int SPEED_WIDTH_BASE = 8;    // base threshold with for color show

    public static final double SPEED_ERROR = 1e-3;   // unit by m/s

    public static final double ZERO_SPEED = 1e-3;   // unit by m/s

    public static final double ZERO_DURATION = 1e-1;   // unit by ms

    public static final float VALID_ACCURACY_MAX = 10f;

    public static final int KM_DURATION_RECORD_NUM = 3;

    public static final int MINUTE_METERS_RECORD_NUM = 5;

    //##@: set true to using animation effect
    public static final boolean BEARING_ANIMATION = true;

    public static final int TEST_MODE_NONE = 0;
    public static final int TEST_MODE_TRACKING = 1;
    public static final int TEST_MODE_LOAD = 2;  // load all test data file without playing
    public static final int TEST_MODE_SPEED_BEARING = 3;

    public static final int TRACK_LINE_COLOR = Color.rgb(255, 255, 255);

    public static double TRACK_SPEED_MAX = 50 / 3.6;  // unit by m/s

    public static final int UI_TIMER_DELAY = 200;
    public static final int UI_TIMER_PERIOD = 1000;

    public static final int INDEX_CHAMPION = 0;
    public static final int INDEX_RUNNER_UP = 1;
    public static final int INDEX_THIRD_PLACE = 2;

    public static final long MINUTE_MS = 60 * 1000;
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // List Menu Definitions

    private static int TracerListMenuEnumID = 0;

    public enum TracerListMenu {
        TEST_MODE_TRACK("Test Mode Track"),
        TEST_MODE_LOAD("Test Mode Load");

        public int id;
        public String name;

        TracerListMenu(String name) {
            this.name = name;
            this.id = (TracerListMenuEnumID++);
        }
    }

    // List Menu Definitions
    ///////////////////////////////////////////////////////////////////////////////////////////////
}
