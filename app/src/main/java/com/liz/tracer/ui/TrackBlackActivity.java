package com.liz.tracer.ui;

import android.os.Bundle;

import com.liz.tracer.R;

public class TrackBlackActivity extends TrackActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_black);
        initCreate();
    }
}
