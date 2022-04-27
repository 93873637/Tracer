package com.liz.tracer.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.liz.androidutils.BitmapUtils;
import com.liz.androidutils.LogUtils;
import com.liz.androidutils.MapPoint;
import com.liz.androidutils.Point2D;
import com.liz.tracer.R;
import com.liz.tracer.logic.ComDef;
import com.liz.tracer.logic.DataLogic;

import java.util.List;

import static android.view.MotionEvent.ACTION_MOVE;


public class TrackSurfaceView extends SurfaceView implements SurfaceHolder.Callback
        , View.OnTouchListener {

    private static final float TRACK_LINE_WIDTH = 22f;

    // margin for showing full plane on edge
    public static final int SCREEN_MARGIN_TOP = 120;
    public static final int SCREEN_MARGIN_BOTTOM = 120;
    public static final int SCREEN_MARGIN_START = 120;
    public static final int SCREEN_MARGIN_END = 120;

    private static final int DEFAULT_ORIGIN_X = SCREEN_MARGIN_START;
    private static final int DEFAULT_ORIGIN_Y = SCREEN_MARGIN_TOP;

    private int mCanvasBgA = 255;
    private int mCanvasBgR = 0;
    private int mCanvasBgG = 0;
    private int mCanvasBgB = 0;

    private int mOriginX = DEFAULT_ORIGIN_X;
    private int mOriginY = DEFAULT_ORIGIN_Y;

    private Paint mTrackPaint = new Paint();
    private Bitmap mDirectionBmp = null;

    /**
     * the point coordinates x, y which measured from direction bitmap
     * origin: center of direction bmp
     * x: left->right
     * y: up->bottom
     */
    private Point2D mAnchorPoint = null;

    public interface SurfaceCallback {
        void onSurfaceUpdated();
    }

    private SurfaceCallback mCallback = null;

    public void setSurfaceCallback(SurfaceCallback callback) {
        mCallback = callback;
    }

    public TrackSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LogUtils.trace();
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LogUtils.trace();
        initSurfaceView();
    }

    @SuppressLint("ClickableViewAccessibility")
    protected void initSurfaceView() {
        LogUtils.trace();
        mTrackPaint.setColor(ComDef.TRACK_LINE_COLOR);
        mTrackPaint.setStrokeWidth(TRACK_LINE_WIDTH);
        setOnTouchListener(this);
        mDirectionBmp = ((BitmapDrawable) getResources().getDrawable(R.drawable.dir_plane)).getBitmap();
        LogUtils.d("get direction bmp: W/H=" + mDirectionBmp.getWidth() + "/" + mDirectionBmp.getHeight());
        mAnchorPoint = new Point2D(2, 91);
        new Thread() {
            public void run() {
                drawSurface();
            }
        }.start();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // onTouch Handler

    private static final int TOUCH_OFFSET_MIN = 5;  // unit by pixel
    private static final int FIRST_CLICK_TIME_OUT = 200;  // unit by ms
    private static final int LONG_PRESS_INTERVAL = 500;  // unit by ms

    private float mDownX, mDownY;
    private float mUpX, mUpY;
    private float mMoveStartX, mMoveStartY;
    private long mDownTime = 0;
    private int mClickCount = 0;
    private double mStartDistance = 0;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (checkZoom(event)) {
            LogUtils.td("action zoom");
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                mMoveStartX = event.getX();
                mMoveStartY = event.getY();
                mDownTime = System.currentTimeMillis();
                LogUtils.td("mDownX=" + mDownX + ", mDownY=" + mDownY + ", mDownTime=" + mDownTime);
                break;
            case MotionEvent.ACTION_UP:
                mUpX = event.getX();
                mUpY = event.getY();
                LogUtils.td("mUpX=" + mUpX + ", mUpY=" + mUpY);
                if (checkClick()) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (checkLongPress(event)) {
                    return true;
                } else if (checkMoving(event)) {
                    return true;
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 通过两个手指开始距离和结束距离，来判断放大缩小
     *
     * @param event:
     * @return true if zoom action
     */
    public boolean checkZoom(MotionEvent event) {
        LogUtils.td("event=" + event.toString());
        int nCnt = event.getPointerCount();
        if (nCnt != 2) {
            return false;
        }
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
            double dx = Math.abs(event.getX(0) - event.getX(1));
            double dy = Math.abs(event.getY(0) - event.getY(1));
            mStartDistance = Math.sqrt(dx * dx + dy * dy);
            onZoomStart();
            return true;
        } else if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP) {
            double dx = Math.abs(event.getX(0) - event.getX(1));
            double dy = Math.abs(event.getY(0) - event.getY(1));
            double d = Math.sqrt(dx * dx + dy * dy);
            onZoomAction(d / mStartDistance);
            return true;
        } else if ((event.getAction() & MotionEvent.ACTION_MASK) == ACTION_MOVE) {
            double dx = Math.abs(event.getX(0) - event.getX(1));
            double dy = Math.abs(event.getY(0) - event.getY(1));
            double d = Math.sqrt(dx * dx + dy * dy);
            onInstantZoom(d / mStartDistance);
            return true;
        } else {
            return false;
        }
    }

    public void onZoomStart() {
        DataLogic.inst().onZoomStart();
    }

    public void onZoomAction(double zoom) {
        DataLogic.inst().onUserZoom(zoom);
        updateTrackSurface();
        if (mCallback != null) {
            mCallback.onSurfaceUpdated();
        }
    }

    public void onInstantZoom(double zoom) {
        //DataLogic.inst().onUserZoom(zoom);
        //updateTrackSurface();
    }

    private boolean checkLongPress(MotionEvent event) {
        if (mDownTime > 0 && isLongPressed(mDownX, mDownY, event)) {
            onLongPressed(mDownX, mDownY);
            mDownTime = 0;
            return true;
        }
        return false;
    }

    /**
     * check if long pressed according to last down point
     *
     * @param xDown 按下时X坐标
     * @param yDown 按下时Y坐标
     * @param event 移动事件
     */
    private boolean isLongPressed(float xDown, float yDown, MotionEvent event) {
        float offsetX = Math.abs(event.getX() - xDown);
        float offsetY = Math.abs(event.getY() - yDown);
        long interval = event.getEventTime() - event.getDownTime();  // unit by ms
        return (offsetX <= TOUCH_OFFSET_MIN && offsetY <= TOUCH_OFFSET_MIN && interval >= LONG_PRESS_INTERVAL);
    }

    private void onLongPressed(final float x, final float y) {
        LogUtils.td(x + ", " + y);
    }

    private boolean checkClick() {
        if (mDownTime > 0) {
            float offsetX = Math.abs(mUpX - mDownX);
            float offsetY = Math.abs(mUpY - mDownY);
            if (offsetX <= TOUCH_OFFSET_MIN && offsetY <= TOUCH_OFFSET_MIN) {
                long diff = System.currentTimeMillis() - mDownTime;
                onClickOnce(mUpX, mUpY, diff);
                return true;
            }
        }
        return false;
    }

    private void onClickOnce(final float x, final float y, final long timeDiff) {
        LogUtils.td(x + ", " + y + ", t=" + timeDiff + ", count=" + mClickCount);
        mClickCount++;
        if (mClickCount > 1) {
            onDoubleClick(x, y);
            mClickCount = 0;
        } else {
            // set timer to wait for next click, if time out, perform single click
            getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mClickCount == 1) {
                        onSingleClick(x, y);
                        mClickCount = 0;
                    }
                }
            }, FIRST_CLICK_TIME_OUT);
        }
    }

    private void onSingleClick(final float x, final float y) {
        LogUtils.td(x + ", " + y);
    }

    private void onDoubleClick(final float x, final float y) {
        LogUtils.td(x + ", " + y);
        DataLogic.inst().switchSurfaceMode();
        updateTrackSurface();
        if (mCallback != null) {
            mCallback.onSurfaceUpdated();
        }
    }

    public boolean checkMoving(MotionEvent event) {
        float moveEndX = event.getX();
        float moveEndY = event.getY();
        float dx = moveEndX - mMoveStartX;
        float dy = moveEndY - mMoveStartY;
        onMoving(dx, dy);
        mMoveStartX = moveEndX;
        mMoveStartY = moveEndY;
        return true;
    }

    private void onMoving(double dx, double dy) {
        DataLogic.inst().onUserTranslation(dx, dy);
        updateTrackSurface();
    }

    // onTouch Handler
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        LogUtils.trace();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        LogUtils.trace();
    }

    public void setBackground(int alpha, int red, int green, int blue) {
        mCanvasBgA = alpha;
        mCanvasBgR = red;
        mCanvasBgG = green;
        mCanvasBgB = blue;
    }

    private void drawSurface() {
        LogUtils.trace();
        Canvas canvas = getHolder().lockCanvas(
                new Rect(0, 0, this.getWidth(), this.getHeight()));
        if (canvas == null) {
            LogUtils.te("ERROR: initSurfaceView: canvas null");
        } else {
            prepareCanvas(canvas);
            drawCanvas(canvas, getSurfaceData());
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    private void prepareCanvas(Canvas canvas) {
        LogUtils.trace();

        // draw background
        canvas.drawARGB(mCanvasBgA, mCanvasBgR, mCanvasBgG, mCanvasBgB);
    }

    private List<MapPoint> getSurfaceData() {
        /*
        //##@: test
        ArrayList<MapPoint> dataList = new ArrayList<>();
        dataList.add(new MapPoint(123, 234, 45));
        dataList.add(new MapPoint(1000, 349, 45));
        dataList.add(new MapPoint(400, 600, 45));
        dataList.add(new MapPoint(340, 900, 45));
        dataList.add(new MapPoint(700, 1800, 45));
        return dataList;
        //*/

        int screenWidthForMap = this.getWidth() - SCREEN_MARGIN_START - SCREEN_MARGIN_END;
        int screenHeightForMap = this.getHeight() - SCREEN_MARGIN_TOP - SCREEN_MARGIN_BOTTOM;
        return DataLogic.inst().generateSurfaceData(screenWidthForMap, screenHeightForMap);
    }

    private Paint setPaintColor(double v) {
        mTrackPaint.setColor(DataLogic.getTrackColor(v));
        return mTrackPaint;
    }

    public void updateTrackSurface() {
        drawSurface();
    }

    public static float getValidBearing(List<MapPoint> dataList) {
        MapPoint mp = getValidBearingLocation(dataList);
        if (mp == null) {
            return 0;
        }
        return mp.loc.getBearing();
    }

    public static MapPoint getValidBearingLocation(List<MapPoint> dataList) {
        int index = dataList.size() - 1;
        while (index >= 0) {
            float bearing = dataList.get(index).loc.getBearing();
            if (Math.abs(bearing) > 1e-3f) {
                return dataList.get(index);
            }
            index--;
        }
        return null;
    }

    /**
     * draw each item on canvas based on middle line
     *
     * @param canvas:
     * @param dataList:
     */
    private void drawCanvas(Canvas canvas, List<MapPoint> dataList) {
        LogUtils.trace();
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        //LogUtils.tv("canvasW/H=" + canvasWidth + "/" + canvasHeight);

        // move to first point
        Point2D p0 = dataList.get(0).createPoint2D();
        Point2D pStart = p0.generateTranslationPoint(mOriginX, mOriginY);
        Point2D pEnd;

        // draw track lines by points
        MapPoint mp = null;
        for (int i = 1; i < dataList.size(); i++) {
            mp = dataList.get(i);
            setPaintColor(mp.loc.getSpeed());
            pEnd = mp.generateTranslationPoint(mOriginX, mOriginY);
            canvas.drawLine((float) pStart.x, (float) pStart.y, (float) pEnd.x, (float) pEnd.y, mTrackPaint);
            pStart = pEnd;
        }

        // draw direction image at last point
        Point2D pTopLeft;
        Point2D pAnchor;
        Bitmap bmpRotation;
        if (mp != null) {
            float bearing = getValidBearing(dataList);
            bmpRotation = BitmapUtils.adjustPhotoRotation(mDirectionBmp, bearing);
            pTopLeft = new Point2D(pStart.x - bmpRotation.getWidth() / 2.0, pStart.y - bmpRotation.getHeight() / 2.0);

            pAnchor = mAnchorPoint.generateRotationPoint(bearing);
            pTopLeft.translation(-pAnchor.x, -pAnchor.y);

            canvas.drawBitmap(bmpRotation, (float) pTopLeft.x, (float) pTopLeft.y, mTrackPaint);
        }
    }
}
