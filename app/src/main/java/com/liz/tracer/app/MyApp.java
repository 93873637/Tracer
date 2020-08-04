package com.liz.tracer.app;

import com.liz.androidutils.LogEx;
import com.liz.androidutils.app.AppEx;
import com.liz.tracer.logic.ComDef;
import com.liz.tracer.logic.DataLogic;

/**
 * MyApp.java
 * Created by liz on 18-1-8.
 */

@SuppressWarnings("unused")
public class MyApp extends AppEx {

    @Override
    public void onCreate() {
        super.onCreate();

        LogEx.setTag(ComDef.APP_NAME);
        LogEx.setLogDir("/sdcard/0.log");
        LogEx.setSaveToFile(true);
        LogEx.setMaxLogFileSize(20 * 1024 * 1024);

        DataLogic.inst().init();
    }
}
