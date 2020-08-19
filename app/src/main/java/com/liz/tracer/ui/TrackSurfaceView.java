package com.liz.tracer.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.liz.androidutils.LogUtils;
import com.liz.tracer.R;
import com.liz.tracer.logic.ComDef;
import com.liz.tracer.logic.DataLogic;
import com.liz.tracer.logic.MapPoint;

import java.util.List;

import static android.view.MotionEvent.ACTION_MOVE;


public class TrackSurfaceView extends SurfaceView implements SurfaceHolder.Callback
        , View.OnTouchListener {

    private static final float TRACK_LINE_WIDTH = 22f;

    public static final int SCREEN_MARGIN_TOP = 0;
    public static final int SCREEN_MARGIN_BOTTOM = 0;
    public static final int SCREEN_MARGIN_START = 0;
    public static final int SCREEN_MARGIN_END = 0;

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
        mDirectionBmp = ((BitmapDrawable)getResources().getDrawable(R.drawable.dir_plane)).getBitmap();
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
                }
                else if (checkMoving(event)) {
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
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN && 2 == nCnt) {
            double dx = Math.abs(event.getX(0) - event.getX(1));
            double dy = Math.abs(event.getY(0) - event.getY(1));
            mStartDistance = Math.sqrt(dx * dx + dy * dy);
            onZoomStart();
            return true;
        } else if ((event.getAction() & MotionEvent.ACTION_MASK) == ACTION_MOVE && 2 == nCnt) {
            double dx = Math.abs(event.getX(0) - event.getX(1));
            double dy = Math.abs(event.getY(0) - event.getY(1));
            double d = Math.sqrt(dx * dx + dy * dy);
            onZoom(d / mStartDistance);
            return true;
        }
        else {
            return false;
        }
    }

    public void onZoomStart() {
        DataLogic.inst().onZoomStart();
    }

    public void onZoom(double zoom) {
        DataLogic.inst().onUserZoom(zoom);
        updateTrackSurface();
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
        mClickCount ++;
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
        DataLogic.inst().switchZoomMode();
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
        mTrackPaint.setColor(DataLogic.getSpeedColor(v));
        return mTrackPaint;
    }

    public void updateTrackSurface() {
        drawSurface();
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
        MapPoint mp0 = dataList.get(0);
        int startX = mOriginX + (int) mp0.x;
        int startY = mOriginY + (int) mp0.y;
        int endX;
        int endY;

        // draw track lines by points
        MapPoint mp = null;
        for (int i = 1; i < dataList.size(); i++) {
            mp = dataList.get(i);
            setPaintColor(mp.loc.getSpeed());
            endX = mOriginX + (int) mp.x;
            endY = mOriginY + (int) mp.y;
            //LogUtils.tv("line (" + startX + ", " + startY + ") -> (" + endX + ", " + endY + ")");
            canvas.drawLine(startX, startY, endX, endY, mTrackPaint);
            startX = endX;
            startY = endY;
        }

        // draw direction at last point
        //canvas.drawBitmap(mDirectionBmp, startX, startY, mTrackPaint);
        if (mp != null) {
            canvas.drawBitmap(adjustPhotoRotation0(mDirectionBmp, mp.loc.getBearing()),
                    startX-mDirectionBmp.getWidth()/2-10, startY-mDirectionBmp.getHeight()/2-10, mTrackPaint);
        }
    }

    Bitmap adjustPhotoRotation0(Bitmap bm, final float orientationDegree) {
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        try {
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
            return bm1;
        } catch (OutOfMemoryError ex) {
        }
        return null;
    }

    Bitmap adjustPhotoRotation(Bitmap bm, final float orientationDegree) {
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        float targetX, targetY;
        if (orientationDegree == 90) {
            targetX = bm.getHeight();
            targetY = 0;
        } else {
            targetX = bm.getHeight();
            targetY = bm.getWidth();
        }
        final float[] values = new float[9];
        m.getValues(values);
        float x1 = values[Matrix.MTRANS_X];
        float y1 = values[Matrix.MTRANS_Y];
        m.postTranslate(targetX - x1, targetY - y1);
        Bitmap bm1 = Bitmap.createBitmap(bm.getHeight(), bm.getWidth(), Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bm1);
        canvas.drawBitmap(bm, m, paint);
        return bm1;
    }
}
