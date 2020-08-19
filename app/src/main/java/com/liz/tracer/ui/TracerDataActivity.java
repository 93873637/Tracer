package com.liz.tracer.ui;

import android.location.Location;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.liz.androidutils.LocationUtils;
import com.liz.androidutils.LogUtils;
import com.liz.androidutils.TimeUtils;
import com.liz.tracer.R;
import com.liz.tracer.logic.ComDef;
import com.liz.tracer.logic.DataLogic;
import com.liz.tracer.logic.LocationService;
import com.liz.tracer.logic.TestData;

import java.util.Timer;
import java.util.TimerTask;

public class TracerDataActivity extends TracerBaseActivity {

    private static final int UI_TIMER_DELAY = 200;
    private static final int UI_TIMER_PERIOD = 1000;

    private TextView tvTimeCurrent;
    private TextView tvTimeStart;
    private TextView tvTimeElapsed;

    private ImageView ivOrientation;
    private TextView tvOrientation;

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
    private static final double TEST_SPEED_INC = 1000.0/3600;  // unit by m/s

    private float testBearing = 0;
    private static final float TEST_BEARING_INC = 1;  // unit by degree

    // for test mode
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracer_data);

        addFlingView(findViewById(R.id.ll_tracer_main));

        tvTimeCurrent = findViewById(R.id.text_current_time);
        tvTimeStart = findViewById(R.id.text_start_time);
        tvTimeElapsed = findViewById(R.id.text_time_elapsed);
        tvCurrentSpeed = findViewById(R.id.text_current_speed);
        tvCurrentSpeedInfo = findViewById(R.id.text_current_speed_info);
        tvAverageSpeed = findViewById(R.id.text_average_speed);
        tvAverageSpeedInfo = findViewById(R.id.text_average_speed_info);

        ivOrientation = findViewById(R.id.iv_bearing_orientation);
        ivOrientation.setRotation(0);

        tvOrientation = findViewById(R.id.text_orientation);

        tvTotalDistance = findViewById(R.id.text_total_distance);
        tvDuration = findViewById(R.id.text_duration);
        tvMaxSpeed = findViewById(R.id.text_max_speed);

        mCurrentBearing = LocationService.inst().getBearing();
        ivOrientation.setRotation(mCurrentBearing);

        LocationService.inst().addLocationCallback(new LocationService.LocationCallback() {
            @Override
            public void onLocationUpdate() {
                TracerDataActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        updateUI();
                    }
                });
            }
        });

        //##@:
        //mCurrentBearing = LocationService.inst().getBearing();
        //ivOrientation.setRotation(mCurrentBearing);
        //setBearingAnimation(170);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Bearing Animation

    private static final int BEARING_MIN = 0;
    private static final int BEARING_MAX = 360;
    private static final int BEARING_ROTATION_MAX = BEARING_MAX / 2;
    private static final int BEARING_ANIMATION_TIMER_DELAY = 0;    // unit by milli-seconds
    private static final int BEARING_ANIMATION_TIMER_PERIOD = 10;  // unit by milli-seconds
    private static final int BEARING_ANIMATION_NUM = 40;
    private static final float BEARING_INC_MIN = 0.5f;

    private float mCurrentBearing = 0;
    private Timer mBearingTimer = null;

    private void stopBearingTimer() {
        if (mBearingTimer != null) {
            mBearingTimer.cancel();
            mBearingTimer = null;
        }
    }

    /**
     * setBearingAnimation: moving to target bearing with animation effect
     * NOTE: all bearing should be 0~359
     * @param targetBearing: number of 0~359, clockwise
     */
    private void setBearingAnimation(final float targetBearing) {
        LogUtils.td("current=" + mCurrentBearing + ", target=" + targetBearing);
        // calc bearing diff, negative means anti-clockwise
        float bearingDiff = targetBearing - mCurrentBearing;
        if (Math.abs(bearingDiff) < BEARING_INC_MIN) {
            LogUtils.td("diff too small, no animation, set directly");
            ivOrientation.setRotation(targetBearing);
            return;
        }

        if (bearingDiff > BEARING_ROTATION_MAX) {
            // anti-clockwise rotation
            bearingDiff = bearingDiff - BEARING_MAX;
        }
        if (bearingDiff < 0) {
            if (bearingDiff <= -BEARING_ROTATION_MAX) {
                bearingDiff += BEARING_MAX;
            }
        }

        float bearingInc = 1.0f * bearingDiff / BEARING_ANIMATION_NUM;
        if (bearingInc > 0 && bearingInc < BEARING_INC_MIN) {
            bearingInc = BEARING_INC_MIN;
        }
        if (bearingInc < 0 && bearingInc > -BEARING_INC_MIN) {
            bearingInc = -BEARING_INC_MIN;
        }
        final float timerInc = bearingInc;
        LogUtils.td("diff=" + bearingDiff + ", inc=" + bearingInc + ", timerInc=" + timerInc);

        stopBearingTimer();

        mBearingTimer = new Timer();
        mBearingTimer.schedule(new TimerTask() {
            public void run() {
                TracerDataActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        mCurrentBearing += timerInc;
                        if (mCurrentBearing < BEARING_MIN) {
                            mCurrentBearing += BEARING_MAX;
                        }
                        if (mCurrentBearing >= BEARING_MAX) {
                            mCurrentBearing -= BEARING_MAX;
                        }
                        float diff = mCurrentBearing - targetBearing;
                        LogUtils.td("target=" + targetBearing + ", current=" + mCurrentBearing + ", inc=" + timerInc + ", diff=" + diff);
                        if (Math.abs(diff) < Math.abs(timerInc)) {
                            stopBearingTimer();
                            ivOrientation.setRotation(targetBearing);
                            LogUtils.td("rotation in place!");
                        }
                        else {
                            ivOrientation.setRotation(mCurrentBearing);
                        }
                    }
                });
            }
        }, BEARING_ANIMATION_TIMER_DELAY, BEARING_ANIMATION_TIMER_PERIOD);
    }

    // Bearing Animation
    //////////////////////////////////////////////////////////////////////////////////////////////

    private int getSpeedWidth(double ratio) {
        int totalWidth = tvCurrentSpeedInfo.getWidth();
        int speedWidth = (int)(totalWidth * ratio);
        if (speedWidth < ComDef.SPEED_WIDTH_BASE) {
            speedWidth = ComDef.SPEED_WIDTH_BASE;
        }
        return speedWidth;
    }

    private void updateBearing() {
        Location loc = LocationService.inst().getValidBearingLocation();
        if (loc == null) {
            ivOrientation.setRotation(0);
            ivOrientation.setBackgroundResource(R.drawable.compass_earth);
            tvOrientation.setText("NA");
        } else {
            ivOrientation.setBackgroundResource(R.drawable.orientation);
            if (ComDef.BEARING_ANIMATION) {
                setBearingAnimation(LocationService.inst().getValidBearing());
            } else {
                ivOrientation.setRotation(LocationService.inst().getValidBearing());
            }
            tvOrientation.setText(LocationService.inst().getValidBearingText());
        }
    }

    private void testUpdateBearing() {
        testBearing += TEST_BEARING_INC;
        testBearing %= 360;
        ivOrientation.setBackgroundResource(R.drawable.orientation);
        if (ComDef.BEARING_ANIMATION) {
            setBearingAnimation(testBearing);
        } else {
            ivOrientation.setRotation(testBearing);
        }
        String bearingText = "" + testBearing;
        bearingText += " " + LocationUtils.getBearingName(testBearing);
        tvOrientation.setText(bearingText);
    }

    private void setSpeedView(TextView tv, double speed) {
        tv.setWidth(1);  // must call this first to make width take effect?
        tv.getLayoutParams().width = getSpeedWidth(LocationService.inst().getSpeedRatio(speed));
        tv.setBackgroundColor(DataLogic.getSpeedBarColor(speed));
    }

    @Override
    protected void updateUI() {
        LogUtils.trace();
        tvTimeCurrent.setText(TimeUtils.currentTime());
        tvTimeStart.setText(LocationService.inst().getStartTimeText());
        tvTimeElapsed.setText(LocationService.inst().getDurationTextFormat());

        if (TestData.testSpeedBearing()) {
            testUpdateBearing();
            testSpeed += TEST_SPEED_INC;
            setSpeedView(tvCurrentSpeed, testSpeed);
            setSpeedView(tvAverageSpeed, testSpeed);
            tvCurrentSpeedInfo.setText(LocationUtils.getDualSpeedText(testSpeed));
            tvAverageSpeedInfo.setText(LocationUtils.getDualSpeedText(testSpeed));
        }
        else {
            updateBearing();
            setSpeedView(tvCurrentSpeed, LocationService.inst().getCurrentSpeed());
            setSpeedView(tvAverageSpeed, LocationService.inst().getAverageSpeed());
            tvCurrentSpeedInfo.setText(LocationService.inst().getCurrentSpeedText());
            tvAverageSpeedInfo.setText(LocationService.inst().getAverageSpeedText());
        }

        tvMaxSpeed.setText(LocationService.inst().getMaxSpeedText());

        tvTotalDistance.setText(DataLogic.inst().getDistanceTotalText());
        tvDuration.setText(LocationService.inst().getDurationText());
    }
}
