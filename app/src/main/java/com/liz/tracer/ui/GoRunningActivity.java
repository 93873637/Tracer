package com.liz.tracer.ui;

import android.os.Bundle;
import android.text.Html;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.liz.androidutils.TimeUtils;
import com.liz.tracer.R;
import com.liz.tracer.logic.ComDef;
import com.liz.tracer.logic.DataLogic;
import com.liz.tracer.logic.LocationService;
import com.liz.tracer.logic.TestData;
import com.liz.tracer.logic.VirtualMap;

public class GoRunningActivity extends TracerBaseActivity {

    private TextView tvTimeCurrent;
    private TextView tvTimeStart;
    private TextView tvTimeElapsed;

    private TextView tvKMLastInfo;
    private TextView tvKMRecordInfo;
    private TextView tvSpeedRecordInfo;
    private TextView tvMinuteRecordInfo;
    private TextView tvTotalDistance;
    private TextView tvCurrentMinuteMeters;
    private TextView tvCurrentKMDuration;
    private TextView tvTotalPerform;
    private TextView tvCurrentPerform;

    private RelativeLayout mLayoutCurrentSpeedBar;
    private RelativeLayout mLayoutAverageSpeedBar;

    private TextView tvCurrentSpeedBarColor;
    private LinearLayout mLayoutCurrentSpeedRank;
    private TextView tvCurrentKMTime;
    private TextView tvCurrentSpeedInfo;
    private SpeedRankBar mCurrentSpeedRank = new SpeedRankBar();

    private TextView tvAverageSpeed;
    private LinearLayout mLayoutAverageSpeedRank;
    private TextView tvAverageKMTime;
    private TextView tvAverageSpeedInfo;
    private SpeedRankBar mAverageSpeedRank = new SpeedRankBar();

    private TextView tvMaxSpeed;
    private TextView tvLocationInfo;
    private TextView tvBearing;

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // for test mode

    private double testSpeed = 0;
    private static final double TEST_SPEED_INC = 1000.0 / 3600;  // unit by m/s

    // for test mode
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_go_running);
        initCreate();
    }

    protected void initCreate() {
        addFlingView(findViewById(R.id.ll_track_main));

        tvTimeCurrent = findViewById(R.id.text_current_time);
        tvTimeStart = findViewById(R.id.text_start_time);
        tvTimeElapsed = findViewById(R.id.text_time_elapsed);

        tvKMLastInfo = findViewById(R.id.text_last_info);
        tvKMRecordInfo = findViewById(R.id.text_km_record);
        tvSpeedRecordInfo = findViewById(R.id.text_speed_record);

        tvMinuteRecordInfo = findViewById(R.id.text_minute_record);

        tvTotalDistance = findViewById(R.id.text_total_distance);
        tvCurrentMinuteMeters = findViewById(R.id.text_current_minute_meters);
        tvCurrentKMDuration = findViewById(R.id.text_current_km_duration);
        tvTotalPerform = findViewById(R.id.text_total_perform);
        tvCurrentPerform = findViewById(R.id.text_current_perform);

        // use current speed bar width
        mLayoutCurrentSpeedBar = findViewById(R.id.layout_current_speed_bar);
        mLayoutAverageSpeedBar = findViewById(R.id.layout_average_speed_bar);

        tvCurrentSpeedBarColor = findViewById(R.id.text_current_speed_bar_color);
        mLayoutCurrentSpeedRank = findViewById(R.id.layout_current_speed_rank);
        tvCurrentKMTime = findViewById(R.id.text_current_km_time);
        tvCurrentSpeedInfo = findViewById(R.id.text_current_speed_info);
        mCurrentSpeedRank.initSpeedRank(this, mLayoutCurrentSpeedRank);

        tvAverageSpeed = findViewById(R.id.text_average_speed);
        mLayoutAverageSpeedRank = findViewById(R.id.layout_average_speed_rank);
        tvAverageKMTime = findViewById(R.id.text_average_km_time);
        tvAverageSpeedInfo = findViewById(R.id.text_average_speed_info);
        mAverageSpeedRank.initSpeedRank(this, mLayoutAverageSpeedRank);

        tvMaxSpeed = findViewById(R.id.text_max_speed);
        tvLocationInfo = findViewById(R.id.text_location_info);
        tvBearing = findViewById(R.id.text_bearing);

        DataLogic.inst().setOnNewRecordCallback(new VirtualMap.onNewRecordCallback() {
            @Override
            public void onNewKMRecord(final int index) {
                new Thread(() -> {
                    // add color effect for new KM record
                    try {
                        for (int i = 0; i < 16; i++) {
                            checkUpdateNewKMRecord(i, index);
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            @Override
            public void onNewMinuteRecord(int index) {
                new Thread(() -> {
                    // add color effect for new KM record
                    try {
                        for (int i = 0; i < 16; i++) {
                            checkUpdateNewMinuteRecord(i, index);
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            @Override
            public void onNewSpeedRecord() {
                new Thread(() -> {
                    // add color effect for new speed record
                    try {
                        for (int i = 0; i < 16; i++) {
                            checkUpdateNewSpeedRecord(i);
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    private int getBackgroundColor(final int index) {
        if (index == 0) {
            return 0xffffef0e;  // gold
        } else if (index == 1) {
            return 0xffefefef;  // silver
        } else if (index == 2) {
            return 0xffdf6f01;  // bronze
        } else if (index == 4) {
            return 0xffcdcdcd;  // silver-2
        } else if (index == 5) {
            return 0xffababab;  // silver-3
        } else {
            return 0xff77ff77;
        }
    }

    private void checkUpdateNewKMRecord(final int s, final int index) {
        GoRunningActivity.this.runOnUiThread(() -> {
            if (s % 2 == 1) {
                tvKMRecordInfo.setBackgroundColor(0xff000919);
            } else {
                tvKMRecordInfo.setBackgroundColor(getBackgroundColor(index));
            }
        });
    }

    private void checkUpdateNewMinuteRecord(final int s, final int index) {
        GoRunningActivity.this.runOnUiThread(() -> {
            if (s % 2 == 1) {
                tvMinuteRecordInfo.setBackgroundColor(0xff000919);
            } else {
                tvMinuteRecordInfo.setBackgroundColor(getBackgroundColor(index));
            }
        });
    }

    private void checkUpdateNewSpeedRecord(final int s) {
        GoRunningActivity.this.runOnUiThread(() -> {
            if (s % 2 == 1) {
                tvSpeedRecordInfo.setBackgroundColor(0xff000919);
            } else {
                tvSpeedRecordInfo.setBackgroundColor(0xffffffff);
            }
        });
    }

    private int getSpeedWidth(double ratio) {
        int totalWidth = mLayoutCurrentSpeedBar.getWidth();
        int speedWidth = (int) (totalWidth * ratio);
        if (speedWidth < ComDef.SPEED_WIDTH_BASE) {
            speedWidth = ComDef.SPEED_WIDTH_BASE;
        }
        return speedWidth;
    }

    private void setCurrentSpeedView(double speed) {
        setSpeedBarColor(mLayoutCurrentSpeedBar, tvCurrentSpeedBarColor, speed);
        tvCurrentKMTime.setText(Html.fromHtml(DataLogic.inst().timePerKmHtml(speed)));
        tvCurrentSpeedInfo.setText(Html.fromHtml(DataLogic.inst().getDualSpeedTextHtml(speed)));
        mCurrentSpeedRank.updateSpeedRank(speed);
    }

    private void setAverageSpeedView(double speed) {
        setSpeedBarColor(mLayoutAverageSpeedBar, tvAverageSpeed, speed);
        tvAverageKMTime.setText(Html.fromHtml(DataLogic.inst().timePerKmHtml(speed)));
        tvAverageSpeedInfo.setText(Html.fromHtml(DataLogic.inst().getDualSpeedTextHtml(speed)));
        mAverageSpeedRank.updateSpeedRank(speed);
    }

    private void setSpeedBarColor(RelativeLayout layout, TextView tv, double speed) {
        layout.setBackgroundColor(DataLogic.getSpeedBaseColor(speed));
        tv.setWidth(1);  // must call this first to make width take effect?
        tv.getLayoutParams().width = getSpeedWidth(LocationService.inst().getSpeedRatio(speed));
        tv.setBackgroundColor(DataLogic.getSpeedBarColor(speed));
    }

    @Override
    protected void updateUI() {
        tvTimeCurrent.setText(TimeUtils.currentTime());
        tvTimeStart.setText(LocationService.inst().getStartTimeText());
        tvTimeElapsed.setText(LocationService.inst().getDurationTextFormat());

        tvTotalDistance.setText(DataLogic.inst().getDistanceTotalText(false));
        tvCurrentMinuteMeters.setText(Html.fromHtml(DataLogic.inst().getCurrentMinuteMetersHtml()));
        tvCurrentKMDuration.setText(Html.fromHtml(DataLogic.inst().getCurrentKMDurationHtml()));
        tvKMLastInfo.setText(Html.fromHtml(DataLogic.inst().getLastKMTimesInfo()));
        tvKMRecordInfo.setText(Html.fromHtml(DataLogic.inst().getKMRecordInfo()));
        tvMinuteRecordInfo.setText(Html.fromHtml(DataLogic.inst().getMinuteRecordInfo()));
        tvSpeedRecordInfo.setText(Html.fromHtml(DataLogic.inst().getMaxSpeedKMTime()));
        tvTotalPerform.setText(Html.fromHtml(DataLogic.inst().getTotalPerformance()));
        tvCurrentPerform.setText(Html.fromHtml(DataLogic.inst().getCurrentPerformance()));

        if (TestData.isTestMode()) {
            tvLocationInfo.setText(DataLogic.inst().getTestInfo());
            if (TestData.testSpeedBearing()) {
                testSpeed += TEST_SPEED_INC;
                setCurrentSpeedView(testSpeed);
                setAverageSpeedView(testSpeed);
            } else {
                setCurrentSpeedView(LocationService.inst().getCurrentSpeed());
                setAverageSpeedView(LocationService.inst().getAverageSpeed());
            }
        } else {
            tvLocationInfo.setText(LocationService.inst().getLocationInfo());
            setCurrentSpeedView(LocationService.inst().getCurrentSpeed());
            setAverageSpeedView(LocationService.inst().getAverageSpeed());
        }

        tvMaxSpeed.setText(Html.fromHtml(DataLogic.inst().getMaxSpeedHtml()));
        tvBearing.setText(LocationService.inst().getValidBearingText());
    }
}
