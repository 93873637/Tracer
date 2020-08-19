package com.liz.tracer.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.liz.androidutils.LocationUtils;
import com.liz.androidutils.TimeUtils;
import com.liz.tracer.R;
import com.liz.tracer.logic.ComDef;
import com.liz.tracer.logic.DataLogic;
import com.liz.tracer.logic.LocationService;
import com.liz.tracer.logic.TestData;

abstract class TrackActivity extends TracerBaseActivity {

    private TextView tvTimeCurrent;
    private TextView tvTimeStart;
    private TextView tvTimeElapsed;

    private TrackSurfaceView mTrackSurface;
    private TextView tvTestInfo;
    private TextView tvBearing;
    private TextView tvMapInfo;

    private TextView tvCurrentSpeed;
    private TextView tvCurrentSpeedInfo;
    private TextView tvAverageSpeed;
    private TextView tvAverageSpeedInfo;

    private TextView tvTotalDistance;
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
    }

    protected void initCreate() {
        addFlingView(findViewById(R.id.ll_track_main));

        tvTimeCurrent = findViewById(R.id.text_current_time);
        tvTimeStart = findViewById(R.id.text_start_time);
        tvTimeElapsed = findViewById(R.id.text_time_elapsed);

        mTrackSurface = findViewById(R.id.track_surface_view);
        tvTestInfo = findViewById(R.id.text_test_info);
        tvBearing = findViewById(R.id.text_bearing);
        tvMapInfo = findViewById(R.id.text_map_info);

        tvCurrentSpeed = findViewById(R.id.text_current_speed);
        tvCurrentSpeedInfo = findViewById(R.id.text_current_speed_info);
        tvAverageSpeed = findViewById(R.id.text_average_speed);
        tvAverageSpeedInfo = findViewById(R.id.text_average_speed_info);

        tvTotalDistance = findViewById(R.id.text_total_distance);
        tvMaxSpeed = findViewById(R.id.text_max_speed);

        LocationService.inst().addLocationCallback(new LocationService.LocationCallback() {
            @Override
            public void onLocationUpdate() {
                TrackActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        updateUI();
                    }
                });
            }
        });
    }

    protected void setBackground(int r, int g, int b) {
        mTrackSurface.setBackground(255, r, g, b);
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
    protected void updateUI() {
        tvTimeCurrent.setText(TimeUtils.currentTime());
        tvTimeStart.setText(LocationService.inst().getStartTimeText());
        tvTimeElapsed.setText(LocationService.inst().getDurationTextFormat());

        mTrackSurface.updateTrackSurface();
        tvBearing.setText(LocationService.inst().getValidBearingText());
        tvMapInfo.setText(DataLogic.inst().getMapInfo());

        if (TestData.isTestMode()) {
            tvTestInfo.setVisibility(View.VISIBLE);
            tvTestInfo.setText(DataLogic.inst().getTestInfo());
            if (TestData.testSpeedBearing()) {
                testSpeed += TEST_SPEED_INC;
                setSpeedView(tvCurrentSpeed, testSpeed);
                setSpeedView(tvAverageSpeed, testSpeed);
                tvCurrentSpeedInfo.setText(LocationUtils.getDualSpeedText(testSpeed));
                tvAverageSpeedInfo.setText(LocationUtils.getDualSpeedText(testSpeed));
            }
            else {
                setSpeedView(tvCurrentSpeed, LocationService.inst().getCurrentSpeed());
                setSpeedView(tvAverageSpeed, LocationService.inst().getAverageSpeed());
                tvCurrentSpeedInfo.setText(LocationService.inst().getCurrentSpeedText());
                tvAverageSpeedInfo.setText(LocationService.inst().getAverageSpeedText());
            }
        } else {
            tvTestInfo.setVisibility(View.INVISIBLE);
            setSpeedView(tvCurrentSpeed, LocationService.inst().getCurrentSpeed());
            setSpeedView(tvAverageSpeed, LocationService.inst().getAverageSpeed());
            tvCurrentSpeedInfo.setText(LocationService.inst().getCurrentSpeedText());
            tvAverageSpeedInfo.setText(LocationService.inst().getAverageSpeedText());
        }

        tvTotalDistance.setText(DataLogic.inst().getDistanceTotalText());
        tvMaxSpeed.setText(LocationService.inst().getMaxSpeedText());
    }
}
