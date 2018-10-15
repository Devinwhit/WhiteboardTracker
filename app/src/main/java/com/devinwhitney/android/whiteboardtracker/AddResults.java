package com.devinwhitney.android.whiteboardtracker;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import com.devinwhitney.android.whiteboardtracker.model.Workout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by devin on 10/5/2018.
 */

public class AddResults extends AppCompatActivity {

    private EditText calendarText;
    private Calendar calendar;
    private EditText resultsText;
    private EditText wodText;
    private Button submit;
    private EditText wodTitle;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mWorkoutDatabseReference;
    private ChildEventListener mChildEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_results);
        calendar = Calendar.getInstance();
        calendarText = findViewById(R.id.wodDatePicker);
        resultsText = findViewById(R.id.results);
        wodText = findViewById(R.id.wod);
        submit = findViewById(R.id.submitResultsButton);
        wodTitle = findViewById(R.id.wodTitle);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Workout workout = new Workout();
                workout.setDate(calendarText.getText().toString());
                workout.setResults(resultsText.getText().toString());
                workout.setUser(FirebaseAuth.getInstance().getCurrentUser().getUid());
                workout.setWod(wodText.getText().toString());
                workout.setTitle(wodTitle.getText().toString());
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(AddResults.this);
                workout.setGym(sharedPreferences.getString("gym", "Set home gym"));

                mWorkoutDatabseReference.push().setValue(workout);
                finish();

            }
        });


        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                calendar.set(Calendar.YEAR, i);
                calendar.set(Calendar.MONTH, i1);
                calendar.set(Calendar.DAY_OF_MONTH, i2);
                updateSelection();
            }
        };
        calendarText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(AddResults.this, date, calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mWorkoutDatabseReference = mFirebaseDatabase.getReference().child("workouts");
    }

    private String getCurrentDate() {
        String format = "MM/dd/yyyy";
        SimpleDateFormat df = new SimpleDateFormat(format);
        Date date = new Date();
        String currentDate = df.format(date);
        return currentDate;
    }

    private void updateSelection() {
        String format = "MM/dd/yyyy";
        SimpleDateFormat df = new SimpleDateFormat(format);
        calendarText.setText(df.format(calendar.getTime()));
        checkDatabase();
    }

    private void checkDatabase() {
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Workout workout = dataSnapshot.getValue(Workout.class);
                if (workout != null) {
                    wodText.setText(workout.getWod());
                } else {
                    wodText.setText(R.string.wodPlaceholder);
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
        String format = "MM/dd/yyyy";
        SimpleDateFormat df = new SimpleDateFormat(format);
        mWorkoutDatabseReference.orderByChild("date").equalTo(df.format(calendar.getTime())).addChildEventListener(mChildEventListener);
    }


}
