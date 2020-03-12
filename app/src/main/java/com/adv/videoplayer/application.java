package com.adv.videoplayer;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

public class application extends Application {
    /*@Override
    public void onCreate() {
        super.onCreate();
        //泄漏分析自身也会有个进程，这里判断如果是那个泄漏分析进程的话则不需要进行分析
        if(!LeakCanary.isInAnalyzerProcess(this)){
            LeakCanary.install(this);
        }
    }*/
}
