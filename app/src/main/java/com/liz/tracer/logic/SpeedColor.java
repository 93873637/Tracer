package com.liz.tracer.logic;

public class SpeedColor {
    public SpeedColor(double s, int c) {
        speed = s;
        color = c;
    }
    public double speed;  // max speed, unit by m/s
    public int color;  // color of speed less than max
}
