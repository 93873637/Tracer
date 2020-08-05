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

    public static final int ZOOM_MODE_1_TO_1 = 1;  // 1 meter : 1 pixel
    public static final int ZOOM_MODE_FIT_SCREEN = 2;  // fit to surface screen size
    public static final int ZOOM_MODE_DEFAULT = ZOOM_MODE_FIT_SCREEN;//###@:ZOOM_MODE_1_TO_1;

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

    private int mZoomMode = ZOOM_MODE_DEFAULT;
    private boolean mUserZoom = false;

    // start from 1, i.e. 1 meter/pixel
    private double mUserZoomValue = SCALE_1_TO_1;
    private double mZoomStart = mUserZoomValue;  // in case zooming recursively, recording zoom start point for every gesture zoom in/out

    private double mTranslationX = 0;
    private double mTranslationY = 0;
    private double mDistanceTotal = 0;

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;

    private double mMapScale = SCALE_1_TO_1;
    private double mMapTransX = 0;
    private double mMapTransY = 0;

    public double getDistanceTotal() {
        return mDistanceTotal;
    }

    public String getDistanceTotalText() {
        return LocationUtils.formatDistance(mDistanceTotal) + "m";
    }

    public void clearMap() {
        mMapPointList.clear();
        mDistanceTotal = 0;
        mTranslationX = 0;
        mTranslationY = 0;
        mScreenWidth = 0;
        mScreenHeight = 0;
        mMapScale = SCALE_1_TO_1;
        mMapTransX = 0;
        mMapTransY = 0;
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
        mZoomStart = mUserZoomValue;
    }

    public void onUserZoom(double zoom) {
        mUserZoomValue = mZoomStart * zoom;
        mUserZoom = true;
        LogUtils.td("mUserZoomValue=" + mUserZoomValue);
    }

    public void onUserTranslation(double dx, double dy) {
        mTranslationX += dx;
        mTranslationY -= dy;
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
                + "\n" + new DecimalFormat("#0.00").format(mUserZoomValue) + "/" + new DecimalFormat("#0.00").format(mMapScale)
                + "\n" + (int)mTranslationX + "/" + (int)mTranslationY
                + "\n" + (int)mMapTransX + "/" + (int)mMapTransY
                + "\n" + getScreenSize()
                + "\n" + lastPosInfo
                ;
    }

    public void switchZoomMode() {
        if (mUserZoom) {
            // close user zoom, make zoom mode take effect
            mUserZoom = false;
        } else {
            // switch between 1to1 and fit screen
            if (mZoomMode == ZOOM_MODE_1_TO_1) {
                mZoomMode = ZOOM_MODE_FIT_SCREEN;
            } else {
                mZoomMode = ZOOM_MODE_1_TO_1;
            }
        }
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

        mMapScale = mUserZoomValue;
        mMapTransX = mTranslationX;
        mMapTransY = mTranslationY;
        switch(mZoomMode) {
            case ZOOM_MODE_1_TO_1:
                mMapScale = SCALE_1_TO_1;
                autoTranslation();
                break;
            case ZOOM_MODE_FIT_SCREEN:
                scaleToScreen();
                mMapTransX -= (Xmin * mMapScale);
                mMapTransY -= (Ymin * mMapScale);
                break;
            default:
                break;
        }
        LogUtils.td("translation X/Y=" + String.format("%.2f", mTranslationX) + "/" + String.format("%.2f", mTranslationY)
                + ", mMapScale=" + String.format("%.2f", mMapScale));

        synchronized (mDataLock) {
            mSurfacePointList.clear();
            for (MapPoint mp : mMapPointList) {
                MapPoint mpSurface = mp.duplicate();
                mpSurface.zoom(mMapScale);
                mpSurface.translation(mMapTransX, mMapTransY);
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

    public void scaleToScreen() {
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
        mMapScale = Math.min(sx, sy);
        LogUtils.td("screen W/H=" + mScreenWidth + "/" + mScreenHeight
                + ", map W/H=" + String.format("%.1f", getMapWidth()) + "/" + String.format("%.1f", getMapHeight())
                + ", sx/sy/s=" + String.format("%.1f", sx) + "/" + String.format("%.1f", sy) + "/" + String.format("%.1f", mMapScale)
        );
    }

    /**
     * update translation X, Y automatically
     * if last point move out of screen, translation last point to center
     */
    private void autoTranslation() {
        MapPoint lp = getLastPoint();
        if (lp != null) {
            MapPoint lpd = lp.duplicate();
            lpd.zoom(mMapScale);
            lpd.translation(mTranslationX, mTranslationY);
            LogUtils.td("lpd.translation=" + String.format("%.1f", lpd.x) + "/" + String.format("%.1f", lpd.y));

            if (lpd.x <= 0) {
                LogUtils.td("last point X out of left");
                mTranslationX += mScreenWidth;
                mTranslationX -= lpd.x;
            }
            if (lpd.x > mScreenWidth) {
                LogUtils.td("last point X out of right");
                //mTranslationX -= mScreenWidth;
                mTranslationX -= lpd.x;
            }
            if (lpd.y <= 0) {
                LogUtils.td("last point Y out of top");
                mTranslationY += mScreenHeight;
                mTranslationY -= lpd.y;
            }
            if (lpd.y > mScreenHeight) {
                LogUtils.td("last point Y out of bottom");
                //mTranslationY -= mScreenHeight;
                mTranslationY -= lpd.y;
            }
        }
        LogUtils.td("trans by last point=" + String.format("%.1f", mTranslationX) + ", mTranslationY=" + String.format("%.1f", mTranslationY));
    }
}
