package com.liz.tracer.logic;

public class TranslationVector {
    public double x;
    public double y;

    public TranslationVector() {
        this.x = 0;
        this.y = 0;
    }

    public TranslationVector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void toZero() {
        this.x = 0;
        this.y = 0;
    }

    public String toString() {
        return String.format("%.1f", this.x) + "/" + String.format("%.1f", this.y);
    }
}
