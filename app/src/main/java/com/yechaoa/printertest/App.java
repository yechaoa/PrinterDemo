package com.yechaoa.printertest;

import android.app.Application;
import android.content.Context;

/**
 * Created by yechao on 2020/3/26/026.
 * Describe :
 */
public class App extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }
}
