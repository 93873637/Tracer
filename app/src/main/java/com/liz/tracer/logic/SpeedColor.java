package com.liz.tracer.logic;

public class SpeedColor {
    public double threshold;  // speed threshold of this range, unit by m/s
    public int color;  // color of this range, format as 0xAARRGGBB

    public SpeedColor(double th, int c) {
        threshold = th;
        color = c;
    }

    public String getThresholdText() {
        return "" + (int) (threshold * 3.6);
    }
}
