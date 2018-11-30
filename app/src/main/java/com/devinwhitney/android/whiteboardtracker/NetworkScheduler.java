package com.devinwhitney.android.whiteboardtracker;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.RequiresApi;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;

/**
 * Created by devin on 11/2/2018.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class NetworkScheduler extends JobService implements InternetConnectionReceiver.InternetConnectionReceiverListener {

    private InternetConnectionReceiver mInternetConnectionReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        mInternetConnectionReceiver = new InternetConnectionReceiver(this);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        registerReceiver(mInternetConnectionReceiver, new IntentFilter(CONNECTIVITY_ACTION));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        unregisterReceiver(mInternetConnectionReceiver);
        return true;
    }

    @Override
    public void onNetworkConnChange(boolean isConnected) {
        Intent intent = new Intent("android.intent.action.MAIN").putExtra("connection", isConnected);
        this.sendBroadcast(intent);

    }
}
