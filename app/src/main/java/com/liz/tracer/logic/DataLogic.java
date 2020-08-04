package com.liz.tracer.logic;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;

import com.liz.androidutils.AssertUtils;

public class DataLogic extends VirtualMap {

    public static int mTestMode = ComDef.TEST_MODE_NONE;

    public static boolean testTrack() {
        return mTestMode == ComDef.TEST_MODE_TRACK;
    }

    public static boolean testLoad() {
        return mTestMode == ComDef.TEST_MODE_LOAD;
    }

    public static boolean testSpeedBearing() {
        return mTestMode == ComDef.TEST_MODE_SPEED_BEARING;
    }

    public static boolean isTestMode() {
        return mTestMode != ComDef.TEST_MODE_NONE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // singleton

    public static DataLogic inst() {
        return inst_;
    }

    private static DataLogic inst_ = new DataLogic();
    private DataLogic() {
    }

    // singleton
    ///////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // static interface

    public static void startTestModeTrack() {
        mTestMode = ComDef.TEST_MODE_TRACK;
        TestData.loadTestData();
    }

    public static void startTestModeLoad() {
        mTestMode = ComDef.TEST_MODE_LOAD;
        TestData.loadTestData();
    }

    public static int getSpeedBarColor(double speed) {
        for (SpeedColor sc : ComDef.SPEED_BAR_COLORS) {
            if (speed < sc.speed) {
                return sc.color;
            }
        }
        return Color.WHITE;
    }

    private static double SPEED_MAX_FOR_COLOR = 50/3.6;  // unit by m/s

    public static int getSpeedColor(double speed) {
        AssertUtils.assertTrue(speed >= 0);
        double ratio = speed / SPEED_MAX_FOR_COLOR;
        if (ratio > 1) {
            ratio = 1;
        }
        return getColorByRatio(ratio);
    }

    public static int getColorByRatio(double ratio) {
        int r = (int)(255 * ratio);
        int g = (int)(255 * (1 - ratio));
        int b = (int)(255 * (1 - ratio));
        return Color.rgb(r, g, b);
    }

    // static interface
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public void resetRunningParameters() {
        this.clearMap();
    }

    public void init() {
    }

    /**
     * loadTestLocation: Create map point by new location
     *
     * @param newLoc: new location detected by gps
     *
     * NOTE:
     * Virtual map using left-hand coordinate(clockwise, same as screen coordinate)
     * Origin: first location point
     * X: Horizontal from left to right
     * Y: Vertical from up to bottom
     * Here x, y unit by meter
     * While location's angle(a) is clockwise with north zero degree, So map a' = a - 90
     * i.e.
     * location bearing = 0, map angle = -90(north)
     * location bearing = 90, map angle = 0(east)
     * location bearing = 180, map angle = 90(south)
     *
     */
    @SuppressLint("DefaultLocale")
    public void onNewLocation(Location newLoc) {
        addMapPoint(newLoc);
    }

    public String getTestInfo() {
        String lastPosInfo = "NA/NA";
        MapPoint lsp = DataLogic.inst().getLastSurfacePoint();
        if (lsp != null) {
            lastPosInfo = (int) lsp.x + "/" + (int) lsp.y;
        }
        return getScreenSize() + "\n"
                + lastPosInfo + "\n"
                + TestData.getTestInfo();
    }
}
