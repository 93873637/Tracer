package com.liz.tracer.logic;

import android.location.Location;

import com.liz.androidutils.LocationUtils;
import com.liz.androidutils.LogUtils;
import com.liz.androidutils.MapPoint;
import com.liz.androidutils.NumUtils;
import com.liz.androidutils.TimeUtils;
import com.liz.androidutils.TranslationVector;
import com.liz.tracer.app.MyApp;

import java.util.ArrayList;
import java.util.List;

public class VirtualMap {

    public static final double MAP_MIN_DISTANCE = 1.0;
    public static final double SCALE_1_TO_1 = 1.0;
    public static int FIT_SCREEN_MARGIN_START = 80;
    public static int FIT_SCREEN_MARGIN_END = 80;
    public static int FIT_SCREEN_MARGIN_TOP = 80;
    public static int FIT_SCREEN_MARGIN_BOTTOM = 80;
    public static int FIT_SCREEN_MARGIN_HORZ = FIT_SCREEN_MARGIN_START + FIT_SCREEN_MARGIN_END;
    public static int FIT_SCREEN_MARGIN_VERT = FIT_SCREEN_MARGIN_TOP + FIT_SCREEN_MARGIN_BOTTOM;
    public static int AUTO_TRANS_MARGIN = 300;

    final private Object mDataLock = new Object();

    // real point list
    private ArrayList<MapPoint> mMapPointList = new ArrayList<>();

    // surface point list
    private ArrayList<MapPoint> mSurfacePointList = new ArrayList<>();

    // real map area, unit by meter
    private double Xmin = 0;
    private double Xmax = 0;
    private double Ymin = 0;
    private double Ymax = 0;

    private boolean mFitScreen = true;

    private boolean mUserTranslation = true;

    // start from 1, i.e. 1 meter/pixel
    private double mZoomScale = SCALE_1_TO_1;
    private double mZoomStart = mZoomScale;  // in case zooming recursively, recording zoom start point for every gesture zoom in/out
    private TranslationVector mTransStart = new TranslationVector();

    private double mDistanceTotal = 0;

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;

    private double mMapScale = SCALE_1_TO_1;
    private TranslationVector mMapTrans = new TranslationVector();

    private double mKMDistanceDiff = 0;  // unit by meter
    private double mKMDurationLast = 0;   // unit by ms
    private double mKMDurationMin = 0;  // unit by ms
    private double mKMDurationMax = 0;  // unit by ms
    private double mLastKMDistance = 0;  // unit by meter

    private ArrayList<Double> mKMDurationList = new ArrayList<>();
    private ArrayList<Double> mKMRecordList = new ArrayList<>();
    private long mLastKMTime = System.currentTimeMillis();  // unit by ms

    private ArrayList<Double> mMinuteMetersList = new ArrayList<>();
    private ArrayList<Double> mMinuteRecordList = new ArrayList<>();
    private long mLastMinuteTime = System.currentTimeMillis();  // unit by ms
    private double mLastMinuteDistance = 0;  // unit by meter

    public interface onNewRecordCallback {
        void onNewKMRecord(final int index);

        void onNewMinuteRecord(final int index);

        void onNewSpeedRecord();
    }

    onNewRecordCallback mOnNewRecordCallback = null;

    public void setOnNewRecordCallback(onNewRecordCallback callback) {
        mOnNewRecordCallback = callback;
    }

    public void onNewMaxSpeed() {
        if (mOnNewRecordCallback != null) {
            mOnNewRecordCallback.onNewSpeedRecord();
        }
    }

    public double getDistanceTotal() {
        return mDistanceTotal;
    }

    public String getDistanceTotalText(boolean withUnit) {
        return NumUtils.format(mDistanceTotal, 0) + (withUnit ? "m" : "");
    }

    public long getLastKMTime() {
        return mLastKMTime;
    }

    public double getLastKMDistance() {
        return mLastKMDistance;
    }

    public String getCurrentKMDuration() {
        if (mKMDistanceDiff > 0) {
            return TimeUtils.formatDurationCompact((System.currentTimeMillis() - mLastKMTime) * 1000 / mKMDistanceDiff);
        } else {
            return "0";
        }
    }

    public String getCurrentKMDurationHtml() {
        double curDuration = 0;  // unit by ms
        if (mKMDistanceDiff > 0) {
            curDuration = (System.currentTimeMillis() - mLastKMTime) * 1000 / mKMDistanceDiff;
        }
        return getDurationTextHtml(curDuration, TimeUtils.formatDurationCompact(curDuration));
    }

    public String getCurrentMinuteMetersHtml() {
        // get current distance from last minute distance
        double curMinuteDist = mDistanceTotal - mLastMinuteDistance;  // unit by meter

        // To decide meter color within a minute,
        // we need to estimate total distance of this minute by current speed
        String color = "yellow";
        double minuteDuration = System.currentTimeMillis() - mLastMinuteTime;  // unit by ms
        if (minuteDuration > 1000) {
            double totalMinuteDist = curMinuteDist * ComDef.MINUTE_MS / minuteDuration;
            if (totalMinuteDist >= ComDef.DEFAULT_TARGET_MINUTE_METERS) {
                color = "green";
            } else {
                color = "red";
            }
        }

        return "<font color=" + color + ">"
                + ((int) curMinuteDist)
                + "</font>";
    }

    public String getMaxSpeedHtml() {
        double speed = LocationService.inst().getMaxSpeed();
        //String text = LocationUtils.getTriSpeedText2(speed);
        String text = LocationUtils.getDualSpeedText(speed);
        //String text = LocationUtils.getSpeedTextKMH(speed);
        return getSpeedTextHtml(speed, text);
    }

    public String getMaxSpeedKMTime() {
        double speed = LocationService.inst().getMaxSpeed();
        String text = LocationUtils.timePerKm(speed);
        return getSpeedTextHtml(speed, text);
    }

    public String timePerKmHtml(double speed) {
        return getSpeedTextHtml(speed, LocationUtils.timePerKm(speed));
    }

    public String getDualSpeedTextHtml(double speed) {
        return getSpeedTextHtml(speed, LocationUtils.getDualSpeedText(speed));
    }

    /**
     * @param duration: unit by ms
     * @param text:     raw text to decorate
     * @return html text to show
     */
    public String getDurationTextHtml(double duration, String text) {
        String color = "red";
        if (duration > ComDef.ZERO_DURATION && duration / 1000 <= ComDef.DEFAULT_TARGET_SPKM) {
            color = "green";
        }
        return "<font color=" + color + ">"
                + text
                + "</font>";
    }

    public String getSpeedTextHtml(double speed, String text) {
        String color = "red";
        if (speed >= ComDef.DEFAULT_TARGET_SPEED) {
            color = "green";
        }
        return "<font color=" + color + ">"
                + text
                + "</font>";
    }

    public String getDistanceTotalText() {
        return NumUtils.format(mDistanceTotal, 0) + "m";
    }

    public void clearMap() {
        mMapPointList.clear();
        mDistanceTotal = 0;
        mScreenWidth = 0;
        mScreenHeight = 0;
        mMapScale = SCALE_1_TO_1;
        mMapTrans.toZero();
        mKMDistanceDiff = 0;
        mKMDurationLast = 0;
        mKMDurationMin = 0;
        mKMDurationMax = 0;
        mLastKMDistance = 0;
        mLastKMTime = System.currentTimeMillis();
        mLastMinuteTime = System.currentTimeMillis();
    }

    public MapPoint getOriginPoint() {
        synchronized (mDataLock) {
            if (mMapPointList.isEmpty()) {
                return null;
            } else {
                return mMapPointList.get(0);
            }
        }
    }

    public MapPoint getLastPoint() {
        synchronized (mDataLock) {
            if (mMapPointList.isEmpty()) {
                return null;
            } else {
                return mMapPointList.get(mMapPointList.size() - 1);
            }
        }
    }

    /**
     * @param kmDuration: duration for this kilometer, unit by milli-second
     * @return String with color info
     */
    public String getColorDuration(double kmDuration) {
        return getDurationTextHtml(kmDuration, TimeUtils.formatDurationCompact(kmDuration));
    }

    /**
     * @param minuteMeter: meters of this minute(total 60s)
     * @return String with color info
     */
    public String getColorMinuteDistance(double minuteMeter) {
        // transform meters/minute to milli-seconds/kilo-meter
        // double kmDuration = 60.0 * 1000 * 1000 / minuteMeter;
        // return getColorDuration(kmDuration);
        String color = "red";
        if (minuteMeter >= ComDef.DEFAULT_TARGET_MINUTE_METERS) {
            color = "green";
        }
        return "<font color=" + color + ">"
                + ((int) minuteMeter)
                + "</font>";
    }

    public String getBestDurationHtmlText(double kmDuration) {
        return "<span style=\"background-color: #ff7711;\">" + getColorDuration(kmDuration) + "</span>";
    }

    public double getLastAverageKMDuration(int n) {
        int count = n;
        int size = mKMDurationList.size();
        if (size < n) {
            count = size;
        }
        if (count == 0) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += mKMDurationList.get(size - 1 - i);
        }
        return sum / count;
    }

    public double getLastKMDuration(int i) {
        int size = mKMDurationList.size();
        if (i >= size) {
            return 0;
        } else {
            return mKMDurationList.get(size - 1 - i);
        }
    }

    public String getLastKMTimesInfo() {
        int num = mKMDurationList.size(); // Math.min(ComDef.MAX_KM_DURATION_NUM, mKMDurationList.size());
        if (num == 0) {
            return "";
        }
        StringBuilder text = new StringBuilder(getColorDuration(getLastKMDuration(0)));
        for (int i = 1; i < num; i++) {
            text.append("/").append(getColorDuration(getLastKMDuration(i)));
        }
        return text.toString();
    }

    public String getKMRecordInfo() {
        int num = Math.min(ComDef.KM_DURATION_RECORD_NUM, mKMRecordList.size());
        if (num == 0) {
            return "";
        }
        StringBuilder text = new StringBuilder(getColorDuration(mKMRecordList.get(0)));
        for (int i = 1; i < num; i++) {
            text.append("/").append(getColorDuration(mKMRecordList.get(i)));
        }
        return text.toString();
    }

    public String getMinuteRecordInfo() {
        int num = Math.min(ComDef.MINUTE_METERS_RECORD_NUM, mMinuteRecordList.size());
        if (num == 0) {
            return "";
        }
        StringBuilder text = new StringBuilder(getColorMinuteDistance(mMinuteRecordList.get(0)));
        for (int i = 1; i < num; i++) {
            text.append("/").append(getColorMinuteDistance(mMinuteRecordList.get(i)));
        }
        return text.toString();
    }

    private void insertKMRecordList(double duration) {
        // first add to km duration list
        mKMDurationList.add(mKMDurationLast);

        // find position and insert into current list
        int index = mKMRecordList.size();
        for (int i = 0; i < mKMRecordList.size(); i++) {
            if (duration < mKMRecordList.get(i)) {
                index = i;
                break;
            }
        }
        mKMRecordList.add(index, duration);

        // update ui on new records
        if (index < ComDef.KM_DURATION_RECORD_NUM && mOnNewRecordCallback != null) {
            mOnNewRecordCallback.onNewKMRecord(index);
        }

        // update min/max
        if (mKMDurationMin == 0 || mKMDurationLast < mKMDurationMin) {
            mKMDurationMin = mKMDurationLast;
        }
        if (mKMDurationMax == 0 || mKMDurationLast > mKMDurationMax) {
            mKMDurationMax = mKMDurationLast;
        }

        // play sound for new records
        MyApp.playSoundKMRecord(index);
    }

    private void insertMinuteRecordList(double minuteDistance) {
        // first put minute meters list in queue
        mMinuteMetersList.add(minuteDistance);

        // find position and insert into record list
        int index = mMinuteRecordList.size();
        for (int i = 0; i < mMinuteRecordList.size(); i++) {
            if (minuteDistance > mMinuteRecordList.get(i)) {
                index = i;
                break;
            }
        }
        mMinuteRecordList.add(index, minuteDistance);

        // update ui on new records
        if (index < ComDef.MINUTE_METERS_RECORD_NUM && mOnNewRecordCallback != null) {
            mOnNewRecordCallback.onNewMinuteRecord(index);
        }

        // play sound for new records
        MyApp.playSoundMinuteRecord(index);
    }

    public String getKMTimesInfo() {
        return "<font color=yellow>" + TimeUtils.formatDurationCompact(mKMDurationLast) + "</font>"
                + "/"
                + "<font color=red>" + TimeUtils.formatDurationCompact(mKMDurationMin) + "</font>"
                + "/"
                + "<font color=blue>" + TimeUtils.formatDurationCompact(mKMDurationMax) + "</font>"
                ;
    }

    public void updateDistance(double deltaDist) {
        mDistanceTotal += deltaDist;
        long current = System.currentTimeMillis();

        // calculate speed info for one kilometer
        mKMDistanceDiff = mDistanceTotal - mLastKMDistance;
        if (mKMDistanceDiff >= 1000) {
            long timeDiff = current - mLastKMTime;
            mKMDurationLast = timeDiff * 1000 / mKMDistanceDiff;
            insertKMRecordList(mKMDurationLast);
            mLastKMDistance = ((int) (mDistanceTotal / 1000)) * 1000;
            mLastKMTime = current;
            mKMDistanceDiff = 0;
            //LogUtils.d("updateDistance: dist=" + mDistanceTotal + ", last dist=" + mLastKMDistance
            //         + ", dur=" + mKMDurationLast + ", min=" + mKMDurationMin + ", max=" + mKMDurationMax
            //        + ", time=" + mLastKMTime);
        }

        // normalize to one minute distance and add to record
        long minuteDiff = current - mLastMinuteTime;
        if (minuteDiff > ComDef.MINUTE_MS) {
            double distanceDiff = mDistanceTotal - mLastMinuteDistance;
            double minuteDistance = distanceDiff * ComDef.MINUTE_MS / minuteDiff;
            insertMinuteRecordList(minuteDistance);
            mLastMinuteDistance = mDistanceTotal;
            mLastMinuteTime = current;
        }
    }

    /**
     * NOTE:
     * we should calc distance total before add to list, or after to list
     * cur point will be last, distance will be zero
     *
     * @param loc:
     */
    public void addMapPoint(Location loc) {
        synchronized (mDataLock) {
            MapPoint mp = new MapPoint(loc, getOriginPoint());
            if (mMapPointList.size() > 0) {
                updateDistance(LocationUtils.getDistance(loc, getLastPoint().loc));
            }
            mMapPointList.add(mp);
            if (mp.x > Xmax) Xmax = mp.x;
            if (mp.x < Xmin) Xmin = mp.x;
            if (mp.y > Ymax) Ymax = mp.y;
            if (mp.y < Ymin) Ymin = mp.y;
            LogUtils.td("ADD MapPoint #" + (mMapPointList.size() + 1)
                    + ": x/y/v=" + NumUtils.format(mp.x) + "/" + NumUtils.format(mp.y)
                    + ", Xmax/min=" + NumUtils.format(Xmax) + "/" + NumUtils.format(Xmin)
                    + ": Ymax/min=" + NumUtils.format(Ymax) + "/" + NumUtils.format(Ymin)
                    + ", mapW/H=" + NumUtils.format(getMapWidth()) + "/" + NumUtils.format(getMapHeight())
            );
        }
    }

    public double getMapWidth() {
        return Xmax - Xmin;
    }

    public double getMapHeight() {
        return Ymax - Ymin;
    }

    /**
     * in case zooming recursively, we need record start point for every gesture zoom in/out
     */
    public void onZoomStart() {
        LogUtils.trace("mFitScreen=" + mFitScreen);
        if (!mFitScreen) {
            mZoomStart = mZoomScale;
            mTransStart.x = mMapTrans.x;
            mTransStart.y = mMapTrans.y;
        }
    }

    public void onUserZoom(double zoom) {
        LogUtils.trace("zoom=" + zoom + ", mFitScreen=" + mFitScreen);
        if (!mFitScreen) {
            mZoomScale = mZoomStart * zoom;
            mMapTrans.x = mTransStart.x * zoom;
            mMapTrans.y = mTransStart.y * zoom;
            LogUtils.td("mZoomScale=" + mZoomScale + ", mMapTrans=" + mMapTrans.toString());
        }
    }

    public void onUserTranslation(double dx, double dy) {
        LogUtils.trace("dx=" + dx + ", dy=" + dy + ", mFitScreen=" + mFitScreen);
        if (!mFitScreen) {
            mMapTrans.x += dx;
            mMapTrans.y += dy;
            mUserTranslation = true;
        }
    }

    public String getScreenSize() {
        return mScreenWidth + "/" + mScreenHeight;
    }

    public String getMapInfo() {
        String lastPosInfo = "NA/NA";
        MapPoint lsp = getLastSurfacePoint();
        if (lsp != null) {
            lastPosInfo = (int) lsp.x + "/" + (int) lsp.y;
        }
        /*
        // NOTE: since map coordinate origin on first point, so first point is (0, 0)
        // No matter what zoom, its surface point just equal to MapTrans.x/y
        String firstPosInfo = "NA/NA";
        MapPoint fsp = DataLogic.inst().getFirstSurfacePoint();
        if (fsp != null) {
            firstPosInfo = (int) fsp.x + "/" + (int) fsp.y;
        }
        //*/
        return NumUtils.format(getMapWidth(), 0) + "m/" + NumUtils.format(getMapHeight(), 0) + "m"
                + "\n" + NumUtils.format(mZoomScale) + "/" + NumUtils.format(mMapScale)
                + "\n" + getScreenSize()
                + "\n" + mMapTrans.toString()  /* map trans is first point, first point is unnecessary */
                + "\n" + lastPosInfo
                + "\n" + (mFitScreen ? "FitScreen" : mUserTranslation ? "UserTrans" : "AutoTrans")
                ;
    }

    public void switchSurfaceMode() {
        mFitScreen = !mFitScreen;
        mUserTranslation = false;
    }

    /**
     * generateSurfaceData: since map coordinate same as screen coordinate, we only need zoom and translation
     *
     * @param screenWidth:
     * @param screenHeight:
     * @return surface points to draw lines
     */
    public List<MapPoint> generateSurfaceData(int screenWidth, int screenHeight) {
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        LogUtils.td("screen W/H=" + screenWidth + "/" + screenHeight);

        if (mFitScreen) {
            mMapScale = autoFitScreen();
            mMapTrans.x = -Xmin * mMapScale + FIT_SCREEN_MARGIN_START;
            mMapTrans.y = -Ymin * mMapScale + FIT_SCREEN_MARGIN_TOP;
        } else {
            mMapScale = mZoomScale;
            if (!mUserTranslation) {
                autoAdjustTranslation();
            }
        }
        LogUtils.td("mMapScale=" + NumUtils.format(mMapScale) + "mMapTransX/Y=" + mMapTrans.toString());

        synchronized (mDataLock) {
            mSurfacePointList.clear();
            for (MapPoint mp : mMapPointList) {
                MapPoint mpSurface = mp.duplicate();
                mpSurface.zoom(mMapScale);
                mpSurface.translation(mMapTrans);
                mSurfacePointList.add(mpSurface);
            }
        }
        return mSurfacePointList;
    }

    public MapPoint getLastSurfacePoint() {
        int num = mSurfacePointList.size();
        if (num > 0) {
            return mSurfacePointList.get(num - 1);
        } else {
            return null;
        }
    }

    public MapPoint getFirstSurfacePoint() {
        int num = mSurfacePointList.size();
        if (num > 0) {
            return mSurfacePointList.get(0);
        } else {
            return null;
        }
    }

    public double autoFitScreen() {
        double mapWidth = this.getMapWidth();
        double mapHeight = this.getMapHeight();
        if (mapWidth < MAP_MIN_DISTANCE) {
            mapWidth = MAP_MIN_DISTANCE;
        }
        if (mapHeight < MAP_MIN_DISTANCE) {
            mapHeight = MAP_MIN_DISTANCE;
        }
        double sx = (mScreenWidth - FIT_SCREEN_MARGIN_HORZ) / mapWidth;
        double sy = (mScreenHeight - FIT_SCREEN_MARGIN_VERT) / mapHeight;
        double s = Math.min(sx, sy);
        LogUtils.td("screen W/H=" + mScreenWidth + "/" + mScreenHeight
                + ", map W/H=" + NumUtils.format(getMapWidth()) + "/" + NumUtils.format(getMapHeight())
                + ", sx/sy/s=" + NumUtils.format(sx) + "/" + NumUtils.format(sy) + "/" + NumUtils.format(s)
        );
        return s;
    }

    /**
     * update translation X, Y automatically
     * first simulate last point's transform, if last point move out of screen, adjust translation
     */
    private void autoAdjustTranslation() {
        MapPoint lp = getLastPoint();
        if (lp == null) {
            return;
        }

        // simulate last point's transform
        MapPoint lpd0 = lp.duplicate();
        lpd0.zoom(mMapScale);
        LogUtils.td("lpd0=" + lpd0.toString());

        MapPoint lpd1 = lpd0.duplicate();
        lpd1.translation(mMapTrans);
        LogUtils.td("lpd1=" + lpd1.toString());

        ///*
        boolean xAdjust = false;
        boolean yAdjust = false;
        if (lpd1.x < 0) {
            LogUtils.td("last point X out of left");
            mMapTrans.x = -lpd0.x + mScreenWidth - AUTO_TRANS_MARGIN;
            xAdjust = true;
        }
        if (lpd1.x > mScreenWidth) {
            LogUtils.td("last point X out of right");
            mMapTrans.x = -lpd0.x + AUTO_TRANS_MARGIN;
            xAdjust = true;
        }
        if (lpd1.y < 0) {
            LogUtils.td("last point Y out of top");
            mMapTrans.y = -lpd0.y + mScreenHeight - AUTO_TRANS_MARGIN;
            yAdjust = true;
        }
        if (lpd1.y > mScreenHeight) {
            LogUtils.td("last point Y out of bottom");
            mMapTrans.y = -lpd0.y + AUTO_TRANS_MARGIN;
            yAdjust = true;
        }
        if (xAdjust && !yAdjust) {
            mMapTrans.y = -lpd0.y + mScreenHeight / 2.0;
        }
        if (!xAdjust && yAdjust) {
            mMapTrans.x = -lpd0.x + mScreenWidth / 2.0;
        }
        //*/

        /*
        // both x, y to center if adjust
        if (lpd1.x < 0 || lpd1.x > mScreenWidth || lpd1.y < 0 || lpd1.y > mScreenHeight) {
            mMapTrans.x = -lpd0.x + mScreenWidth / 2.0;
            mMapTrans.y = -lpd0.y + mScreenHeight / 2.0;
        }
        //*/

        MapPoint lpd2 = lpd0.duplicate();
        lpd2.translation(mMapTrans);
        LogUtils.td("lpd2=" + lpd2.toString());

        LogUtils.td("trans by last point=" + mMapTrans.toString());
    }
}
