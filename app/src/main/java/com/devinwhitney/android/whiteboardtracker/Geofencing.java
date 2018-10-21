package com.devinwhitney.android.whiteboardtracker;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;

import java.util.Calendar;

/**
 * Created by devin on 10/19/2018.
 */

public class Geofencing implements ResultCallback<Status> {

    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private PendingIntent mGeofencePendingIntent;
    private Geofence mGeofence;

    public Geofencing(GoogleApiClient googleApiClient, Context context) {
        mGoogleApiClient = googleApiClient;
        mContext = context;
        mGeofencePendingIntent = null;
    }


    @SuppressLint("MissingPermission")
    public void registerGymGeofence() {
/*
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected() ||
                mGeofence == null) {
            return;
        }
*/
        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(),
                getGeofencePendingIntent())
                .setResultCallback(this);
    }

    public void setGeofence(Place place) {
        String placeUID = place.getId();
        double placeLatitude = place.getLatLng().latitude;
        double placeLongitude = place.getLatLng().longitude;

        Geofence geofence = new Geofence.Builder()
                .setRequestId(placeUID)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(placeLatitude, placeLongitude, 500)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        mGeofence = geofence;
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT);
        builder.addGeofence(mGeofence);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(mContext, GeofenceBroadcastReceiver.class);
        mGeofencePendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    @Override
    public void onResult(@NonNull Status status) {
        System.out.println(status.getStatus().toString());
    }
}
