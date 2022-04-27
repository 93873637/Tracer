package com.liz.tracer.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.liz.tracer.logic.ComDef;
import com.liz.tracer.logic.DataLogic;
import com.liz.tracer.logic.SpeedColor;

import java.util.ArrayList;

public class SpeedRankBar {

    private ArrayList<TextView> mSpeedViewList = null;

    public void initSpeedRank(Context context, LinearLayout layout) {
        mSpeedViewList = new ArrayList<>();
        for (SpeedColor sc : ComDef.SPEED_BAR_COLORS) {
            TextView tv = new TextView(context);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, (float) 1.0));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tv.setTextColor(Color.GREEN);
            tv.setText(sc.getThresholdText());
            tv.setGravity(Gravity.END);
            tv.setBackgroundColor(sc.color);
            tv.setVisibility(View.INVISIBLE);
            tv.setIncludeFontPadding(false);
            mSpeedViewList.add(tv);
            layout.addView(tv);
        }
    }

    public void updateSpeedRank(double speed) {
        int rank = DataLogic.getSpeedBarRank(speed);
        for (int i = 0; i < mSpeedViewList.size(); i++) {
            mSpeedViewList.get(i).setVisibility(i < rank ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
