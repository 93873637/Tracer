package com.liz.tracer.logic;

import android.location.Location;

import com.liz.androidutils.LocationUtils;
import com.liz.androidutils.LogUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class VirtualMap {

    public static final double MAP_MIN_DISTANCE = 1.0;
    public static final double SCALE_1_TO_1 = 1.0;

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

    // start from 1, i.e. 1 meter/pixel
    private double mZoomScale = SCALE_1_TO_1;
    private double mZoomStart = mZoomScale;  // in case zooming recursively, recording zoom start point for every gesture zoom in/out

    private double mDistanceTotal = 0;

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;

    private double mMapScale = SCALE_1_TO_1;
    private TranslationVector mMapTrans = new TranslationVector();

    public double getDistanceTotal() {
        return mDistanceTotal;
    }

    public String getDistanceTotalText() {
        return LocationUtils.formatDistance(mDistanceTotal) + "m";
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
                    + ": x/y/v=" + String.format("%.1f", mp.x) + "/" + String.format("%.1f", mp.y)
                    + ", Xmax/min=" + String.format("%.1f", Xmax) + "/" + String.format("%.1f", Xmin)
                    + ": Ymax/min=" + String.format("%.1f", Ymax) + "/" + String.format("%.1f", Ymin)
                    + ", mapW/H=" + String.format("%.1f", getMapWidth()) + "/" + String.format("%.1f", getMapHeight())
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
        mZoomStart = mZoomScale;
    }

    public void onUserZoom(double zoom) {
        mZoomScale = mZoomStart * zoom;
        LogUtils.td("mZoomScale=" + mZoomScale);
    }

    public void onUserTranslation(double dx, double dy) {
        LogUtils.trace();
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
        return LocationUtils.formatDistance(getMapWidth()) + "m/" + LocationUtils.formatDistance(getMapHeight()) + "m"
                + "\n" + new DecimalFormat("#0.00").format(mZoomScale) + "/" + new DecimalFormat("#0.00").format(mMapScale)
                + "\n" + getScreenSize()
                + "\n" + mMapTrans.toString()
                + "\n" + lastPosInfo
                ;
    }

    public void switchZoomMode() {
        mFitScreen = !mFitScreen;
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
            mMapScale = calcScaleToScreen();
            mMapTrans.x = - Xmin * mMapScale;
            mMapTrans.y = - Ymin * mMapScale;
        }
        else {
            mMapScale = mZoomScale;
            autoAdjustTranslation();
        }
        LogUtils.td(", mMapScale=" + String.format("%.2f", mMapScale) + "mMapTransX/Y=" + mMapTrans.toString());

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

    public double calcScaleToScreen() {
        double mapWidth = this.getMapWidth();
        double mapHeight = this.getMapHeight();
        if (mapWidth < MAP_MIN_DISTANCE) {
            mapWidth = MAP_MIN_DISTANCE;
        }
        if (mapHeight < MAP_MIN_DISTANCE) {
            mapHeight = MAP_MIN_DISTANCE;
        }
        double sx = mScreenWidth / mapWidth;
        double sy = mScreenHeight / mapHeight;
        double s = Math.min(sx, sy);
        LogUtils.td("screen W/H=" + mScreenWidth + "/" + mScreenHeight
                + ", map W/H=" + String.format("%.1f", getMapWidth()) + "/" + String.format("%.1f", getMapHeight())
                + ", sx/sy/s=" + String.format("%.1f", sx) + "/" + String.format("%.1f", sy) + "/" + String.format("%.1f", s)
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
            mMapTrans.x = -lpd0.x + mScreenWidth*0.5;
        }
        if (lpd1.x > mScreenWidth) {
            LogUtils.td("last point X out of right");
            mMapTrans.x = -lpd0.x + mScreenWidth*0.5;
        }
        if (lpd1.y < 0) {
            LogUtils.td("last point Y out of top");
            mMapTrans.y = -lpd0.y + mScreenHeight*0.5;
        }
        if (lpd1.y > mScreenHeight) {
            LogUtils.td("last point Y out of bottom");
            mMapTrans.y = -lpd0.y + mScreenHeight*0.5;
        }

        MapPoint lpd2 = lpd0.duplicate();
        lpd2.translation(mMapTrans);
        LogUtils.td("lpd2=" + lpd2.toString());

        LogUtils.td("trans by last point=" + mMapTrans.toString());
    }
}
