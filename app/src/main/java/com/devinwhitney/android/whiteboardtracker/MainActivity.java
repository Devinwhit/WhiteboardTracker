package com.devinwhitney.android.whiteboardtracker;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.devinwhitney.android.whiteboardtracker.model.Workout;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadata;
import com.google.android.gms.location.places.PlacePhotoMetadataBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadataResponse;
import com.google.android.gms.location.places.PlacePhotoResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    public static final int RC_SIGN_IN = 1;
    private static final int CAMERA = 100;

    private static int PLACE_PICKER_REQUEST = 2;

    private Button mRecordWorkout;
    private ImageView mMainPhoto;

    private TextView mGymLocation;
    private TextView mTodaysWod;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mWorkoutDatabseReference;
    private ChildEventListener mChildEventListener;

    private GoogleApiClient mClient;
    private Geofencing mGeofencing;

    protected GeoDataClient mGeoDataClient;

    private boolean mIsConnected;
    private BroadcastReceiver mBroadcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //Toast.makeText(MainActivity.this, "Signed in!", Toast.LENGTH_SHORT).show();
                } else {
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        mFirebaseAuth = FirebaseAuth.getInstance();

        mRecordWorkout = findViewById(R.id.record_workout_button);
        mTodaysWod = findViewById(R.id.main_screen_wod);
        mRecordWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getmIsConnected()){
                    Intent intent = new Intent(MainActivity.this, ViewWorkoutActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.record_workout_no_connection, Toast.LENGTH_SHORT).show();
                }

            }

        });

        mMainPhoto = findViewById(R.id.mainPhoto);

        mGymLocation = findViewById(R.id.gymName);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mGymLocation.setText(sharedPreferences.getString("gym", getString(R.string.No_location_found)));
        setupSharedPreferences();
        mGeoDataClient = Places.getGeoDataClient(this, null);
        getMainPhoto();
        if (mClient == null) {
            mClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .enableAutoManage(this, this)
                    .build();
            mGeofencing = new Geofencing(mClient, this);
        }


        checkTodaysWod();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            scheduleConnectionCheck();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void scheduleConnectionCheck() {
        JobInfo jobInfo = new JobInfo.Builder(5000, new ComponentName(this, NetworkScheduler.class))
                .setRequiresCharging(false)
                .setMinimumLatency(1000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setPersisted(true)
                .build();

        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(jobInfo);

    }

    private void checkTodaysWod() {
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mWorkoutDatabseReference = mFirebaseDatabase.getReference().child("workouts");
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Workout workout = dataSnapshot.getValue(Workout.class);
                if (workout != null) {
                    mTodaysWod.setText(workout.getWod());
                }
                Intent updateWidget = new Intent(getApplicationContext(), WorkoutWidget.class);
                updateWidget.putExtra(WorkoutWidget.UPDATE_WIDGET, workout.getWod());
                updateWidget.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = AppWidgetManager.getInstance(getApplication())
                        .getAppWidgetIds(new ComponentName(getApplication(), WorkoutWidget.class));
                updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                sendBroadcast(updateWidget);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mWorkoutDatabseReference.orderByChild("date").equalTo(getCurrentDate()).addChildEventListener(mChildEventListener);
    }


    private String getCurrentDate() {
        String format = "MM/dd/yyyy";
        SimpleDateFormat df = new SimpleDateFormat(format);
        Date date = new Date();
        String currentDate = df.format(date);
        return currentDate;
    }

    private void getMainPhoto() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        final String placeId = pref.getString("placeID", getString(R.string.Set_home_gym));
        final Task<PlacePhotoMetadataResponse> photoMetadataResponse = mGeoDataClient.getPlacePhotos(placeId);
        photoMetadataResponse.addOnCompleteListener(new OnCompleteListener<PlacePhotoMetadataResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlacePhotoMetadataResponse> task) {
                try {
                    // Get the list of photos.
                    PlacePhotoMetadataResponse photos = task.getResult();
                    // Get the PlacePhotoMetadataBuffer (metadata for all of the photos).
                    PlacePhotoMetadataBuffer photoMetadataBuffer = photos.getPhotoMetadata();
                    int numPhotos = photoMetadataBuffer.getCount();
                    PlacePhotoMetadata photoMetadata = photoMetadataBuffer.get(new Random().nextInt(numPhotos));

                    Task<PlacePhotoResponse> photoResponse = mGeoDataClient.getPhoto(photoMetadata);
                    photoResponse.addOnCompleteListener(new OnCompleteListener<PlacePhotoResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<PlacePhotoResponse> task) {
                            PlacePhotoResponse photo = task.getResult();
                            Bitmap bitmap = photo.getBitmap();
                            mMainPhoto.setImageBitmap(bitmap);
                        }
                    });
                } catch (Exception e) {
                    System.out.println("No id set");
                    Picasso.get().load("https://images.unsplash.com/photo-1534258936925-c58bed479fcb?ixlib=rb-0.3.5&ixid=eyJhcHBfaWQiOjEyMDd9&s=de05b46a8ac91fcff2b134811e62d79f&auto=format&fit=crop&w=1489&q=80").into(mMainPhoto);
                }
            }
        });

    }

    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("gym", place.getName().toString());
                editor.putString("placeID", place.getId());
                refreshPlacesData();
                editor.apply();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, NetworkScheduler.class);
        startService(intent);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.MAIN");
        mBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean connection = intent.getBooleanExtra("connection", false);
                setmIsConnected(connection);
            }
        };
        this.registerReceiver(mBroadcast, intentFilter);
    }


    public boolean getmIsConnected() {
        return mIsConnected;
    }

    public void setmIsConnected(boolean mIsConnected) {
        this.mIsConnected = mIsConnected;
    }


    @Override
    protected void onStop() {
        stopService(new Intent(this, NetworkScheduler.class));
        super.onStop();

    }
    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);

        mClient.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        this.unregisterReceiver(mBroadcast);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.shared_preferences, menu);
        /* Return true so that the menu is displayed in the Toolbar */
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case R.id.log_out:
                if (getmIsConnected()){
                    AuthUI.getInstance().signOut(this);
                    return true;
                } else {
                    Toast.makeText(this, R.string.need_network_connection, Toast.LENGTH_SHORT).show();
                    return false;
                }
            case R.id.set_gym:
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                return true;

            default:
                return super.onContextItemSelected(item);

        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals("gym")) {
            mGymLocation.setText(sharedPreferences.getString(s, getString(R.string.gym_not_found)));
        } else if (s.equals("placeID")) {
            getMainPhoto();
        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //check for access to fine location
        if (checkLocationPermissions()) {

            refreshPlacesData();
            Log.i(MainActivity.class.toString(), "API Client Connection Successful!");
        } else {
            //don't register broadcast receiver
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    public void refreshPlacesData() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String placeID = sharedPreferences.getString("placeID", getString(R.string.Not_set));
        if (placeID.equals("Not Set")) {
            return;
        }
        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mClient, placeID);
        placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                mGeofencing.setGeofence(places.get(0));
                mGeofencing.registerGymGeofence();
            }
        });
    }

    public boolean checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 105);
        } else {
            return true;
        }
        return true;
    }

}
