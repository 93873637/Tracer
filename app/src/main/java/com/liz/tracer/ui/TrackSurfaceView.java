package com.liz.tracer.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.liz.androidutils.LogUtils;
import com.liz.tracer.logic.ComDef;
import com.liz.tracer.logic.DataLogic;
import com.liz.tracer.logic.MapPoint;

import java.util.List;


public class TrackSurfaceView extends SurfaceView implements SurfaceHolder.Callback
        , View.OnTouchListener {

    private static final float TRACK_LINE_WIDTH = 22f;

    public static final int SCREEN_MARGIN_TOP = 0;
    public static final int SCREEN_MARGIN_BOTTOM = 0;
    public static final int SCREEN_MARGIN_START = 0;
    public static final int SCREEN_MARGIN_END = 0;

    private static final int DEFAULT_ORIGIN_X = SCREEN_MARGIN_START;
    private static final int DEFAULT_ORIGIN_Y = SCREEN_MARGIN_TOP;

    private static final int CANVAS_BG_A = 255;
    private static final int CANVAS_BG_R = 0;
    private static final int CANVAS_BG_G = 0;
    private static final int CANVAS_BG_B = 0;

    private int mCanvasBgA = CANVAS_BG_A;
    private int mCanvasBgR = CANVAS_BG_R;
    private int mCanvasBgG = CANVAS_BG_G;
    private int mCanvasBgB = CANVAS_BG_B;

    private int mOriginX = DEFAULT_ORIGIN_X;
    private int mOriginY = DEFAULT_ORIGIN_Y;

    private Paint mTrackPaint = new Paint();
    //####@: private Bitmap mDirectionBmp = null;

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

        //####@:
        //load direction bitmap
//        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dir_plane);
//        Matrix matrix = new Matrix();
//        float scaleWidth = 1.0f;
//        float scaleHeight = 1.0f;
//        matrix.postScale(scaleWidth, scaleHeight);
//        mDirectionBmp = Bitmap.createBitmap(bmp, 0, 0, 256, 256, matrix, true);

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
    private long mDownTime = 0;
    private int mClickCount = 0;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                mDownTime = System.currentTimeMillis();
                LogUtils.td("mDownX=" + mDownX + ", mDownY=" + mDownY + ", mDownTime=" + mDownTime);
                break;
            case MotionEvent.ACTION_UP:
                mUpX = event.getX();
                mUpY = event.getY();
                LogUtils.td("mUpX=" + mUpX + ", mUpY=" + mUpY);
                onCheckClick();
                break;
            case MotionEvent.ACTION_MOVE:
                onCheckLongPress(event);
                break;
            default:
                break;
        }
        return true;
    }

    private void onCheckClick() {
        if (mDownTime > 0) {
            float offsetX = Math.abs(mUpX - mDownX);
            float offsetY = Math.abs(mUpY - mDownY);
            if (offsetX <= TOUCH_OFFSET_MIN && offsetY <= TOUCH_OFFSET_MIN) {
                long diff = System.currentTimeMillis() - mDownTime;
                onClickOnce(mUpX, mUpY, diff);
            }
        }
    }

    private void onCheckLongPress(MotionEvent event) {
        if (mDownTime > 0 && isLongPressed(mDownX, mDownY, event)) {
            onLongPressed(mDownX, mDownY);
            mDownTime = 0;
        }
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
//        getHandler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mClickCount == 1) {
//                    onSingleClick(x, y);
//                } else if (mClickCount == 2) {
//                    onDoubleClick(x, y);
//                }
//                mClickCount = 0;
//            }
//        }, FIRST_CLICK_TIME_OUT);
    }

    private void onSingleClick(final float x, final float y) {
        LogUtils.td(x + ", " + y);
    }

    private void onDoubleClick(final float x, final float y) {
        LogUtils.td(x + ", " + y);
        DataLogic.inst().switchZoomMode();
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
        MapPoint mp;
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
        ////####@:
        //canvas.drawBitmap(mDirectionBmp, startX, startY, mTrackPaint);
    }
}
