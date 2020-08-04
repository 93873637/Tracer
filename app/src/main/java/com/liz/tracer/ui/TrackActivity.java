package com.liz.tracer.ui;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.liz.androidutils.LocationUtils;
import com.liz.androidutils.LogUtils;
import com.liz.androidutils.TimeUtils;
import com.liz.androidutils.ui.AppCompatActivityEx;
import com.liz.androidutils.ui.FlingAction;
import com.liz.androidutils.ui.FlingActionLeft;
import com.liz.androidutils.ui.FlingActionRight;
import com.liz.tracer.R;
import com.liz.tracer.app.MyApp;
import com.liz.tracer.logic.ComDef;
import com.liz.tracer.logic.DataLogic;
import com.liz.tracer.logic.LocationService;
import com.liz.tracer.logic.MapPoint;

public class TrackActivity extends AppCompatActivityEx {

    private static final int UI_TIMER_DELAY = 200;
    private static final int UI_TIMER_PERIOD = 1000;

    private TextView tvTimeCurrent;
    private TextView tvTimeStart;
    private TextView tvTimeElapsed;

    private TrackSurfaceView mTrackSurface;
    private TextView tvTestInfo;
    private TextView tvBearing;
    private TextView tvMapInfo;
    private ImageView ivDirection;

    private TextView tvCurrentSpeed;
    private TextView tvCurrentSpeedInfo;
    private TextView tvAverageSpeed;
    private TextView tvAverageSpeedInfo;

    private TextView tvTotalDistance;
    private TextView tvDuration;
    private TextView tvMaxSpeed;

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // for test mode

    private double testSpeed = 0;
    private static final double TEST_SPEED_INC = 1000.0 / 3600;  // unit by m/s

    private float testBearing = 0;
    private static final float TEST_BEARING_INC = 1;  // unit by degree

    // for test mode
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setProp(PROP_NO_TITLE);
        setProp(PROP_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_track);

        LogUtils.trace();
        MyApp.registerActivity(this);

        addFlingView(findViewById(R.id.ll_track_main));
        addFlingAction(new FlingActionRight(new FlingAction.FlingCallback() {
            @Override
            public void onFlingAction(FlingAction flingAction) {
                Intent intent = new Intent(TrackActivity.this, TracerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        }));
        addFlingAction(new FlingActionLeft(new FlingAction.FlingCallback() {
            @Override
            public void onFlingAction(FlingAction flingAction) {
                Intent intent = new Intent(TrackActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        }));

        tvTimeCurrent = findViewById(R.id.text_current_time);
        tvTimeStart = findViewById(R.id.text_start_time);
        tvTimeElapsed = findViewById(R.id.text_time_elapsed);

        mTrackSurface = findViewById(R.id.track_surface_view);
        tvTestInfo = findViewById(R.id.text_test_info);
        tvBearing = findViewById(R.id.text_bearing);
        tvMapInfo = findViewById(R.id.text_map_info);
        ivDirection = findViewById(R.id.image_direction);

        tvCurrentSpeed = findViewById(R.id.text_current_speed);
        tvCurrentSpeedInfo = findViewById(R.id.text_current_speed_info);
        tvAverageSpeed = findViewById(R.id.text_average_speed);
        tvAverageSpeedInfo = findViewById(R.id.text_average_speed_info);

        tvTotalDistance = findViewById(R.id.text_total_distance);
        tvDuration = findViewById(R.id.text_duration);
        tvMaxSpeed = findViewById(R.id.text_max_speed);

        setUITimer(UI_TIMER_DELAY, UI_TIMER_PERIOD);
    }

    private int getSpeedWidth(double ratio) {
        int totalWidth = tvCurrentSpeedInfo.getWidth();
        int speedWidth = (int) (totalWidth * ratio);
        if (speedWidth < ComDef.SPEED_WIDTH_BASE) {
            speedWidth = ComDef.SPEED_WIDTH_BASE;
        }
        return speedWidth;
    }

    private void setSpeedView(TextView tv, double speed) {
        tv.setWidth(1);  // must call this first to make width take effect?
        tv.getLayoutParams().width = getSpeedWidth(LocationService.inst().getSpeedRatio(speed));
        tv.setBackgroundColor(DataLogic.getSpeedBarColor(speed));
    }

    @Override
    public void onZoomStart() {
        DataLogic.inst().onZoomStart();
    }

    @Override
    public void onZoom(double zoom) {
        LogUtils.td("zoom=" + zoom);
        DataLogic.inst().onUserZoom(zoom);
        mTrackSurface.updateTrackSurface();
    }

    @Override
    public void onMove(Point p1, Point p2, float dx, float dy, float d, float b) {
        LogUtils.td("dx=" + dx + ", dy=" + dy + ", d=" + d + ", b=" + b);
        DataLogic.inst().onUserTranslation(dx, dy);
        mTrackSurface.updateTrackSurface();
    }

    @Override
    protected void onDoubleClick() {
        LogUtils.trace();
        DataLogic.inst().switchZoomMode();
        mTrackSurface.updateTrackSurface();
    }

    protected void updateDirectionIcon() {
        if (!LocationService.inst().isRunning()) {
            ivDirection.setVisibility(View.INVISIBLE);
        } else {
            ivDirection.setVisibility(View.VISIBLE);
            MapPoint lsp = DataLogic.inst().getLastSurfacePoint();
            if (lsp == null) {
                ivDirection.setTranslationX(0);
                ivDirection.setTranslationY(0);
            }
            else {
                ivDirection.setTranslationX((int)lsp.x);
                ivDirection.setTranslationY((int)lsp.y);
            }
            ivDirection.setRotation(LocationService.inst().getBearing());
        }
    }

    @Override
    protected void updateUI() {
        tvTimeCurrent.setText(TimeUtils.currentTime());
        tvTimeStart.setText(LocationService.inst().getStartTimeText());
        tvTimeElapsed.setText(LocationService.inst().getDurationTextFormat());

        mTrackSurface.updateTrackSurface();
        tvBearing.setText(LocationService.inst().getBearingText());
        tvMapInfo.setText(DataLogic.inst().getMapInfo());
        updateDirectionIcon();

        if (DataLogic.isTestMode()) {
            tvTestInfo.setVisibility(View.VISIBLE);
            tvTestInfo.setText(DataLogic.inst().getTestInfo());
            if (DataLogic.testSpeedBearing()) {
                testSpeed += TEST_SPEED_INC;
                tvTestInfo.setVisibility(View.INVISIBLE);
                setSpeedView(tvCurrentSpeed, testSpeed);
                setSpeedView(tvAverageSpeed, testSpeed);
                tvCurrentSpeedInfo.setText(LocationUtils.getDualSpeedText(testSpeed));
                tvAverageSpeedInfo.setText(LocationUtils.getDualSpeedText(testSpeed));
            }
        } else {
            tvTestInfo.setVisibility(View.INVISIBLE);
            setSpeedView(tvCurrentSpeed, LocationService.inst().getCurrentSpeed());
            setSpeedView(tvAverageSpeed, LocationService.inst().getAverageSpeed());
            tvCurrentSpeedInfo.setText(LocationService.inst().getCurrentSpeedText());
            tvAverageSpeedInfo.setText(LocationService.inst().getAverageSpeedText());
        }

        tvTotalDistance.setText(DataLogic.inst().getDistanceTotalText());
        tvDuration.setText(LocationService.inst().getDurationText());
        tvMaxSpeed.setText(LocationService.inst().getMaxDualSpeedText());
    }
}
