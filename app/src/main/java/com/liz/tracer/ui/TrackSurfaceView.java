package com.liz.tracer.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

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

    float xDown, yDown;
    float xUp, yUp;
    boolean isLongClickModule = false;
    boolean isLongClicking = false;

    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            xDown = motionEvent.getX();
            yDown = motionEvent.getY();
            LogUtils.td("xDown=" + xDown + ", yDown=" + yDown);
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            xUp = motionEvent.getX();
            yUp = motionEvent.getY();
            LogUtils.td("xUp=" + xUp + ", yUp=" + yUp);

            if (isLongClickModule) {
                isLongClickModule = false;
                isLongClicking = false;
            }
            xUp = motionEvent.getX();

            Log.v("OnTouchListener", "Up");
            //按下和松开绝对值差当大于20时滑动，否则不显示
            if ((xUp - xDown) > 20) {
                //添加要处理的内容
                Toast.makeText(this.getContext(), "scroll right", Toast.LENGTH_SHORT).show();
            } else if ((xUp - xDown) < -20) {
                Toast.makeText(this.getContext(), "scroll left", Toast.LENGTH_SHORT).show();
                //添加要处理的内容
            } else if (0 == (xDown - xUp)) {
                int viewWidth = this.getWidth();
                if (xDown < viewWidth / 3) {
                    //靠左点击
                } else if (xDown > viewWidth / 3 && xDown < viewWidth * 2 / 3) {
                    //中间点击

                } else {
                    //靠右点击
                }
                /**
                 * not scroll
                 */
                //showNavigation();
                Toast.makeText(this.getContext(), "not scroll", Toast.LENGTH_SHORT).show();
            }
        } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            //当滑动时背景为选中状态 //检测是否长按,在非长按时检测
            if (!isLongClickModule) {
                isLongClickModule = isLongPressed(xDown, yDown, motionEvent.getX(),
                        motionEvent.getY(), motionEvent.getDownTime(), motionEvent.getEventTime(), 300);
            }
            if (isLongClickModule && !isLongClicking) {
                //处理长按事件
                isLongClicking = true;
            }
        } else {
            //其他模式
        }
        return true;
    }

    /* 判断是否有长按动作发生
     * @param lastX 按下时X坐标
     * @param lastY 按下时Y坐标
     * @param thisX 移动时X坐标
     * @param thisY 移动时Y坐标
     * @param lastDownTime 按下时间
     * @param thisEventTime 移动时间
     * @param longPressTime 判断长按时间的阀值
     */
    private boolean isLongPressed(float lastX, float lastY,
                                  float thisX, float thisY,
                                  long lastDownTime, long thisEventTime,
                                  long longPressTime) {
        float offsetX = Math.abs(thisX - lastX);
        float offsetY = Math.abs(thisY - lastY);
        long intervalTime = thisEventTime - lastDownTime;
        if (offsetX <= 10 && offsetY <= 10 && intervalTime >= longPressTime) {
            return true;
        }
        return false;
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
