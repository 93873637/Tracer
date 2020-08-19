package com.liz.tracer.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;

import com.liz.androidutils.LogEx;
import com.liz.androidutils.ui.AppCompatActivityEx;

public class MainActivity extends AppCompatActivityEx {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                new AppCompatActivityEx.PermissionCallback() {
                    @Override
                    public void onPermissionGranted() {
                        startActivity(new Intent(MainActivity.this, TracerMainActivity.class));
                        MainActivity.this.finish();
                    }

                    @Override
                    public void onPermissionDenied() {
                        LogEx.trace();
                    }
                }
        );
    }
}
