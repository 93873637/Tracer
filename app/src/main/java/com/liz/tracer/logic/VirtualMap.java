package com.liz.tracer.logic;

import android.location.Location;

import com.liz.androidutils.LocationUtils;
import com.liz.androidutils.LogUtils;
import com.liz.androidutils.NumUtils;

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

    public double getDistanceTotal() {
        return mDistanceTotal;
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
     * NOTE:
     * we should calc distance total before add to list, or after to list
     * cur point will be last, distance will be zero
     * @param loc:
     */
    public void addMapPoint(Location loc) {
        synchronized (mDataLock) {
            MapPoint mp = new MapPoint(loc, getOriginPoint());
            if (mMapPointList.size() > 0) {
                mDistanceTotal += LocationUtils.getDistance(loc, getLastPoint().loc);
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
        MapPoint lsp = DataLogic.inst().getLastSurfacePoint();
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
        return NumUtils.format(getMapWidth(),0) + "m/" + NumUtils.format(getMapHeight(),0) + "m"
                + "\n" + NumUtils.format(mZoomScale) + "/" + NumUtils.format(mMapScale)
                + "\n" + getScreenSize()
                + "\n" + mMapTrans.toString()  /* map trans is first point, first point is unnecessary */
                + "\n" + lastPosInfo
                + "\n" + (mFitScreen ? "FitScreen" : mUserTranslation ? "UserTrans" : "AutoTrans")
                ;
    }

    public void switchZoomMode() {
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
        }
        else {
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
        }
        else {
            return null;
        }
    }

    public MapPoint getFirstSurfacePoint() {
        int num = mSurfacePointList.size();
        if (num > 0) {
            return mSurfacePointList.get(0);
        }
        else {
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

        if (lpd1.x < 0) {
            LogUtils.td("last point X out of left");
            mMapTrans.x = -lpd0.x + mScreenWidth - AUTO_TRANS_MARGIN;
        }
        if (lpd1.x > mScreenWidth) {
            LogUtils.td("last point X out of right");
            mMapTrans.x = -lpd0.x + AUTO_TRANS_MARGIN;
        }
        if (lpd1.y < 0) {
            LogUtils.td("last point Y out of top");
            mMapTrans.y = -lpd0.y + mScreenHeight - AUTO_TRANS_MARGIN;
        }
        if (lpd1.y > mScreenHeight) {
            LogUtils.td("last point Y out of bottom");
            mMapTrans.y = -lpd0.y + AUTO_TRANS_MARGIN;
        }

        MapPoint lpd2 = lpd0.duplicate();
        lpd2.translation(mMapTrans);
        LogUtils.td("lpd2=" + lpd2.toString());

        LogUtils.td("trans by last point=" + mMapTrans.toString());
    }
}
