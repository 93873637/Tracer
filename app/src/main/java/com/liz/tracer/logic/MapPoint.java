package com.liz.tracer.logic;

import android.location.Location;

import com.liz.androidutils.LocationUtils;
import com.liz.androidutils.LogUtils;
import com.liz.androidutils.NumUtils;

public class MapPoint {

    public double x;   // point x, left -> right
    public double y;   // point y, up -> bottom
    public Location loc;   // real location got from gps

    public MapPoint(double x, double y, Location loc) {
        this.x = x;
        this.y = y;
        this.loc = loc;
    }

    public MapPoint(Location loc, MapPoint org) {
        this.loc = loc;
        if (org == null) {
            // no org, take this point as origin
            this.x = 0;
            this.y = 0;
        }
        else {
            // according to distance and angle to origin point, calc its coordinate
            double d = LocationUtils.getDistance(org.loc, loc);
            double a = LocationUtils.getAngle(org.loc, loc) * Math.PI / 180;
            this.x = d * Math.sin(a);
            this.y = -d * Math.cos(a);
            LogUtils.td("d=" + NumUtils.format(d, 1) + ", a=" + NumUtils.format(a, 1)
                    + ", x=" + NumUtils.format(x, 1) + ", y=" + NumUtils.format(y, 1));
        }
    }

    public MapPoint duplicate() {
        return new MapPoint(this.x, this.y, this.loc);
    }

    public MapPoint zoom(double scale) {
        this.x *= scale;
        this.y *= scale;
        return this;
    }

    public MapPoint flatY() {
        this.y *= -1;
        return this;
    }

    public MapPoint translation(int dx, int dy) {
        this.x += dx;
        this.y += dy;
        return this;
    }

    public MapPoint translation(TranslationVector t) {
        return translation(t.x, t.y);
    }

    public MapPoint translation(double dx, double dy) {
        this.x += dx;
        this.y += dy;
        return this;
    }

    public String toString() {
        return NumUtils.format(x, 1) + "/" + NumUtils.format(y, 1);
    }
}
