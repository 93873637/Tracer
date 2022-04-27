package com.liz.tracer.logic;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;

import com.liz.androidutils.AssertUtils;
import com.liz.androidutils.LogUtils;
import com.liz.androidutils.NumUtils;
import com.liz.androidutils.TimeUtils;

public class DataLogic extends VirtualMap {

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

    /**
     * @param speed: unit by m/s
     * @return color by speed
     */
    public static int getSpeedBarColor(double speed) {
        for (SpeedColor sc : ComDef.SPEED_BAR_COLORS) {
            if (speed < sc.threshold) {
                return sc.color;
            }
        }
        return ComDef.FINAL_SPEED_COLOR;
    }

    public static int getSpeedBaseColor(double speed) {
        for (int i = ComDef.SPEED_BAR_COLORS.length - 1; i >= 0; i--) {
            if (speed >= ComDef.SPEED_BAR_COLORS[i].threshold) {
                return ComDef.SPEED_BAR_COLORS[i].color;
            }
        }
        return 0;
    }

    /**
     * @param speed: unit by m/s
     * @return color rank by speed
     */
    public static int getSpeedBarRank(double speed) {
        if (speed > 0) {
            for (int i = 0; i < ComDef.SPEED_BAR_COLORS.length; i++) {
                if (speed < ComDef.SPEED_BAR_COLORS[i].threshold) {
                    return i + 1;
                }
            }
            return ComDef.SPEED_BAR_COLORS.length + 1;
        } else {
            return 0;
        }
    }

    /**
     * get track color by threshold
     *
     * @param speed: unit by m/s
     * @return
     */
    public static int getTrackColor(double speed) {
        AssertUtils.assertTrue(speed >= 0);
        double ratio = speed / ComDef.TRACK_SPEED_MAX;
        if (ratio > 1) {
            ratio = 1;
        }
        return getColorByRatio(ratio);
    }

    public static int getColorByRatio(double ratio) {
        int r = (int) (255 * ratio);
        int g = (int) (255 * (1 - ratio));
        int b = 0;
        return Color.rgb(r, g, b);
    }

    // static interface
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private double mTargetSpeed = ComDef.DEFAULT_TARGET_SPEED;

    public void resetRunningParameters() {
        this.clearMap();
    }

    public void init() {
        mTargetSpeed = ComDef.DEFAULT_TARGET_SPEED;
    }

    /**
     * loadTestLocation: Create map point by new location
     *
     * @param newLoc: new location detected by gps
     *                <p>
     *                NOTE:
     *                Virtual map using left-hand coordinate(clockwise, same as screen coordinate)
     *                Origin: first location point
     *                X: Horizontal from left to right
     *                Y: Vertical from up to bottom
     *                Here x, y unit by meter
     *                While location's angle(a) is clockwise with north zero degree, So map a' = a - 90
     *                i.e.
     *                location bearing = 0, map angle = -90(north)
     *                location bearing = 90, map angle = 0(east)
     *                location bearing = 180, map angle = 90(south)
     */
    @SuppressLint("DefaultLocale")
    public void onNewLocation(Location newLoc) {
        addMapPoint(newLoc);
    }

    public String getTestInfo() {
        return TestData.getTestInfo();
    }

    public String getTotalPerformance() {
        long duration = LocationService.inst().getDuration();
        double distance = getDistanceTotal();
        double targetDistance = mTargetSpeed * duration / 1000;  // unit by meter
        double diffDistance = distance - targetDistance;
        return getPerformanceText(diffDistance);
    }

    public String getCurrentPerformance() {
        double curDiffDistance = 0;
        if (LocationService.inst().isRunning()) {
            double distance = getDistanceTotal();
            double lastKMDistance = getLastKMDistance();
            long lastKMTime = getLastKMTime();
            double curTargetDistance = lastKMDistance + (System.currentTimeMillis() - lastKMTime) * mTargetSpeed / 1000;
            curDiffDistance = distance - curTargetDistance;
        }
        return getPerformanceText(curDiffDistance);
    }

    public String getPerformanceText(double diffDistance) {
        long diffDistanceLong = (long) Math.abs(diffDistance);
        long diffTime = (long) Math.abs(diffDistance / mTargetSpeed * 1000);
        LogUtils.tv("mTargetSpeed = " + mTargetSpeed
                + ", diffDistanceLong = " + diffDistanceLong
                + ", diffTimeLong = " + diffTime
        );

        String color = "red";
        if (diffDistance > 0) {
            color = "green";
        }

        return "<font color=" + color + " size=3>"
                + NumUtils.format(diffDistanceLong) + "/"
                + TimeUtils.formatDurationCompact(diffTime)
                + "</font>";
    }
}
