package com.liz.tracer.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import com.liz.androidutils.LogUtils;
import com.liz.androidutils.TimeUtils;
import com.liz.tracer.R;
import com.liz.tracer.app.MyApp;
import com.liz.tracer.logic.ComDef;
import com.liz.tracer.logic.LocationService;
import com.liz.tracer.logic.TestData;

import java.util.Timer;
import java.util.TimerTask;

public class TracerMainActivity extends TracerBaseActivity {

    private TextView tvTimeCurrent;
    private TextView tvTimeStart;
    private TextView tvTimeElapsed;

    private ImageView ivOrientation;
    private TextView tvStatisInfo;

    private TextView tvCurrentSpeed;
    private TextView tvCurrentSpeedInfo;
    private TextView tvAverageSpeed;
    private TextView tvAverageSpeedInfo;

    private Button btnSwitch;

    private ScrollView scrollInfo;
    private TextView tvLogInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracer_main);

        addFlingView(findViewById(R.id.ll_main));
        addFlingView(findViewById(R.id.scroll_info));

        tvTimeCurrent = findViewById(R.id.text_current_time);
        tvTimeStart = findViewById(R.id.text_start_time);
        tvTimeElapsed = findViewById(R.id.text_time_elapsed);
        tvStatisInfo = findViewById(R.id.text_statis_info);

        tvCurrentSpeed = findViewById(R.id.text_current_speed_bar_color);
        tvCurrentSpeedInfo = findViewById(R.id.text_current_speed_info);
        tvAverageSpeed = findViewById(R.id.text_average_speed);
        tvAverageSpeedInfo = findViewById(R.id.text_average_speed_info);

        ivOrientation = findViewById(R.id.iv_bearing_orientation);
        ivOrientation.setRotation(0);

        btnSwitch = findViewById(R.id.btn_switch_tracing);
        btnSwitch.setOnClickListener(view -> {
            LocationService.inst().switchTracing();
            updateUI();
        });

        findViewById(R.id.btn_reset_running).setOnClickListener(view -> LocationService.inst().onReset());
        findViewById(R.id.btn_config_menu).setOnClickListener(view -> showPopupMenu(view));
        findViewById(R.id.btn_exit_app).setOnClickListener(view -> new AlertDialog
                .Builder(TracerMainActivity.this)
                .setTitle("Confirm Exit?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", (dialogInterface, i) -> MyApp.exitApp()).setNegativeButton("Cancel", null).show());

        scrollInfo = findViewById(R.id.scroll_info);
        tvLogInfo = findViewById(R.id.text_log_info);
        tvLogInfo.setMovementMethod(ScrollingMovementMethod.getInstance());

        LocationService.inst().init(TracerMainActivity.this);

        mCurrentBearing = LocationService.inst().getBearing();
        ivOrientation.setRotation(mCurrentBearing);
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
     *
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
                TracerMainActivity.this.runOnUiThread(new Runnable() {
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
                        } else {
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
        int speedWidth = (int) (totalWidth * ratio);
        if (speedWidth < ComDef.SPEED_WIDTH_BASE) {
            speedWidth = ComDef.SPEED_WIDTH_BASE;
        }
        return speedWidth;
    }

    @Override
    protected void updateUI() {
        tvTimeCurrent.setText(TimeUtils.currentTime());
        tvTimeStart.setText(LocationService.inst().getStartTimeText());
        tvTimeElapsed.setText(LocationService.inst().getDurationTextFormat());
        tvCurrentSpeedInfo.setText(LocationService.inst().getCurrentSpeedText());
        tvAverageSpeedInfo.setText(LocationService.inst().getAverageSpeedText());
        tvStatisInfo.setText(Html.fromHtml(LocationService.inst().getStatisInfo()));

        Location loc = LocationService.inst().getValidBearingLocation();
        if (loc == null) {
            ivOrientation.setRotation(0);
            ivOrientation.setBackgroundResource(R.drawable.compass_earth);
        } else {
            ivOrientation.setBackgroundResource(R.drawable.orientation);
            if (ComDef.BEARING_ANIMATION) {
                setBearingAnimation(LocationService.inst().getValidBearing());
            } else {
                ivOrientation.setRotation(LocationService.inst().getValidBearing());
            }
        }

        // set threshold bar showing
        tvCurrentSpeed.setWidth(1);  // must call this first to make width take effect?
        tvCurrentSpeed.getLayoutParams().width = getSpeedWidth(LocationService.inst().getCurrentSpeedRatio());
        tvAverageSpeed.setWidth(1);
        tvAverageSpeed.getLayoutParams().width = getSpeedWidth(LocationService.inst().getAverageSpeedRatio());

        if (LocationService.inst().isRunning()) {
            btnSwitch.setText("STOP");
            btnSwitch.setBackgroundColor(Color.RED);
            btnSwitch.setTextColor(Color.rgb(0x00, 0xff, 0xff));
        } else {
            btnSwitch.setText("START");
            btnSwitch.setBackgroundColor(Color.GREEN);
            btnSwitch.setTextColor(Color.rgb(0xff, 0x00, 0xff));
        }
    }

    private void updateLogInfo() {
        tvLogInfo.append(TimeUtils.getLogTime() + " - " + LocationService.inst().getLastLocationInfo() + "\n");
        scrollInfo.post(new Runnable() {
            @Override
            public void run() {
                scrollInfo.smoothScrollTo(0, tvLogInfo.getBottom());
            }
        });
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.main_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
                switch (item.getItemId()) {
//                    case R.id.action_play_trace_file:
//                        onPlayTraceFile();
//                        return true;
//                    case R.id.action_load_trace_file:
//                        onLoadTraceFile();
//                        return true;
                    case R.id.action_play_test_data:
                        TestData.setTestMode(ComDef.TEST_MODE_TRACKING);
                        TestData.loadTestData();
                        TestData.setReverseLoad(false);
                        return true;
                    case R.id.action_play_test_data_reverse:
                        TestData.setTestMode(ComDef.TEST_MODE_TRACKING);
                        TestData.loadTestData();
                        TestData.setReverseLoad(true);
                        return true;
                    case R.id.action_load_test_data:
                        TestData.setTestMode(ComDef.TEST_MODE_LOAD);
                        TestData.loadTestData();
                        TestData.setReverseLoad(false);
                        return true;
                    case R.id.action_set_test_play_speed:
                        onSetTestPlaySpeed();
                        return true;
                    default:
                        break;
                }
                return false;
            }
        });
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                //Toast.makeText(getApplicationContext(), "关闭PopupMenu", Toast.LENGTH_SHORT).show();
            }
        });
        popupMenu.show();
    }

    protected void onPlayTraceFile() {
        //##@:todo:
    }

    protected void onLoadTraceFile() {
        //##@:todo:
    }

    protected void onSetTestPlaySpeed() {
        EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setText(TestData.getTestPlaySpeedStr());
        new AlertDialog.Builder(this)
                .setTitle("请输入播放速率(%[" + TestData.TEST_PLAY_SPEED_MIN + " ~ " + TestData.TEST_PLAY_SPEED_MAX + "])")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(editText)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        TestData.setTestPlaySpeed(editText.getText().toString());
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
