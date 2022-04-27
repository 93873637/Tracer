package com.liz.tracer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.liz.androidutils.LogUtils;
import com.liz.androidutils.ui.AppCompatActivityEx;
import com.liz.androidutils.ui.FlingAction;
import com.liz.androidutils.ui.FlingActionLeft;
import com.liz.androidutils.ui.FlingActionRight;
import com.liz.tracer.app.MyApp;
import com.liz.tracer.logic.ComDef;
import com.liz.tracer.logic.LocationService;

public class TracerBaseActivity extends AppCompatActivityEx {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setProp(PROP_NO_TITLE);
        setProp(PROP_KEEP_SCREEN_ON);
        MyApp.registerActivity(this);
        // SysUtils.changeAppBrightness(this, MAX_APP_BRIGHTNESS);
        setFlingAction();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUITimer(ComDef.UI_TIMER_DELAY, ComDef.UI_TIMER_PERIOD);
        LocationService.inst().setLocationCallback(() -> TracerBaseActivity.this.runOnUiThread(this::updateUI));
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeUITimer();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
        }
        return super.onKeyDown(keyCode, event);
    }

    private final String[] activityClassNames = {
            "ui.TracerMainActivity", "ui.GoRunningActivity", "ui.TracerCompassActivity", "ui.TrackBlackActivity", "ui.TrackWhiteActivity"
    };

    private int getActivityIndex(String clsName) {
        for (int i = 0; i < activityClassNames.length; i++) {
            if (activityClassNames[i].equals(clsName)) {
                return i;
            }
        }
        return -1;
    }

    protected void setFlingAction() {
        LogUtils.trace(this.getLocalClassName());

        int index = getActivityIndex(this.getLocalClassName());
        if (index < 0) {
            LogUtils.e("unsupported class name " + getLocalClassName());
            return;
        }
        int leftIndex = index - 1;
        if (leftIndex < 0) {
            leftIndex = activityClassNames.length - 1;
        }
        int rightIndex = index + 1;
        if (rightIndex >= activityClassNames.length) {
            rightIndex = 0;
        }

        try {
            final Class<?> clsLeft = Class.forName("com.liz.tracer." + activityClassNames[leftIndex]);
            final Class<?> clsRight = Class.forName("com.liz.tracer." + activityClassNames[rightIndex]);
            addFlingAction(new FlingActionLeft(new FlingAction.FlingCallback() {
                @Override
                public void onFlingAction(FlingAction flingAction) {
                    Intent intent = new Intent(TracerBaseActivity.this, clsLeft);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                }
            }));
            addFlingAction(new FlingActionRight(new FlingAction.FlingCallback() {
                @Override
                public void onFlingAction(FlingAction flingAction) {
                    Intent intent = new Intent(TracerBaseActivity.this, clsRight);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                }
            }));
        } catch (Exception e) {
            LogUtils.e("set fling action failed, e = " + e.toString());
            e.printStackTrace();
        }
    }
}
