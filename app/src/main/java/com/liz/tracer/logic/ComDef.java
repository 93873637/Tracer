package com.liz.tracer.logic;

import android.graphics.Color;

public class ComDef {

    public static final String APP_NAME = "Tracer";

    public static final String TIME_RESET_STRING = "00:00:00";

    public static final SpeedColor[] SPEED_BAR_COLORS = {
            new SpeedColor(   5.0/3.6, 0xff7788ff),
            new SpeedColor(  10.0/3.6, 0xffaa66ff),
            new SpeedColor(  20.0/3.6, 0xffff00ff),
            new SpeedColor(  30.0/3.6, 0xffff22aa),
            new SpeedColor(  40.0/3.6, 0xffff7700),
            new SpeedColor(  50.0/3.6, 0xffff0000),
            new SpeedColor(  70.0/3.6, 0xff00aaff),
            new SpeedColor( 100.0/3.6, 0xffff7700),
            new SpeedColor( 150.0/3.6, 0xff0000ff),
            new SpeedColor( 200.0/3.6, 0xffff77ff),
            new SpeedColor( 300.0/3.6, 0xffffff77),
            new SpeedColor( 500.0/3.6, 0xff88ffff),
            new SpeedColor(1000.0/3.6, 0xffffffff),
    };

    public static final int SPEED_WIDTH_BASE = 8;    // base speed with for color show

    public static final double SPEED_ERROR = 1e-3;   // unit by m/s

    public static final double ZERO_SPEED = 1e-3;   // unit by m/s

    public static final float VALID_ACCURACY_MAX = 10f;

    //##@: set true to using animation effect
    public static final boolean BEARING_ANIMATION = true;

    public static final int TEST_MODE_NONE = 0;
    public static final int TEST_MODE_TRACK = 1;
    public static final int TEST_MODE_LOAD = 2;
    public static final int TEST_MODE_SPEED_BEARING = 3;

    public static final int TRACK_LINE_COLOR = Color.rgb(255, 255, 255);

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
