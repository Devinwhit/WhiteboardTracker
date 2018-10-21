package com.devinwhitney.android.whiteboardtracker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
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
import com.googlecode.tesseract.android.TessBaseAPI;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseAuth = FirebaseAuth.getInstance();

        mRecordWorkout = findViewById(R.id.record_workout_button);
        mTodaysWod = findViewById(R.id.main_screen_wod);
        mRecordWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //startActivityForResult(intent, CAMERA);
                Intent intent = new Intent(MainActivity.this, ViewWorkoutActivity.class);
                startActivity(intent);
            }

        });

        mMainPhoto = findViewById(R.id.mainPhoto);
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
        mGymLocation = findViewById(R.id.gymName);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mGymLocation.setText(sharedPreferences.getString("gym", "No Location Found"));
        setupSharedPreferences();
        mGeoDataClient = Places.getGeoDataClient(this, null);
        getMainPhoto();
        mClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, this)
                .build();
        mGeofencing = new Geofencing(mClient, this);

        checkTodaysWod();

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

            final String placeId = pref.getString("placeID", "Set home gym");
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
        if (requestCode == CAMERA && resultCode == RESULT_OK) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");

            String dataPath = this.getFilesDir().toString();
            // or any other dir where you app has file write permissions
            File dir = new File(dataPath + "tessdata");
            dir.mkdirs();

            if (!(new File(dataPath + "tessdata/eng.traineddata")).exists()) {
                try {

                    AssetManager assetManager = getAssets();
                    InputStream in = assetManager.open("eng.traineddata");
                    OutputStream out = new FileOutputStream(dataPath
                            + "tessdata/eng.traineddata");

                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                }
            }

            imageBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
            TessBaseAPI baseAPI = new TessBaseAPI();
            baseAPI.init(dataPath, "eng");
            baseAPI.setImage(imageBitmap);

            String text = baseAPI.getUTF8Text();
            baseAPI.end();
        }

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("gym", place.getName().toString());
                editor.putString("placeID", place.getId());
                mGeofencing.setGeofence(place);
                editor.apply();
            }
        }
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
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
                AuthUI.getInstance().signOut(this);
                return true;
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
            mGymLocation.setText(sharedPreferences.getString(s, "Gym not found"));
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
        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mClient,
                sharedPreferences.getString("placeID", "f44868d96a3c47f112e9ca74c26c58e57f00e956"));
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
