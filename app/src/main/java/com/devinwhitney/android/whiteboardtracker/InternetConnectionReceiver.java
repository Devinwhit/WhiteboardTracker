package com.devinwhitney.android.whiteboardtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by devin on 11/2/2018.
 */

public class InternetConnectionReceiver extends BroadcastReceiver {

    public static InternetConnectionReceiverListener internetConnectionReceiverListener;

    public InternetConnectionReceiver(InternetConnectionReceiverListener listener) {
        internetConnectionReceiverListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting();

        if (internetConnectionReceiverListener != null) {
            internetConnectionReceiverListener.onNetworkConnChange(isConnected);
        }

    }

    public interface InternetConnectionReceiverListener {
        void onNetworkConnChange(boolean isConnected);
    }
}
