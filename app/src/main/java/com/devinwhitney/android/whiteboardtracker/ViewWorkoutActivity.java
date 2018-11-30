package com.devinwhitney.android.whiteboardtracker;


import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.devinwhitney.android.whiteboardtracker.model.Workout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

/**
 * Created by devin on 10/1/2018.
 */

public class ViewWorkoutActivity extends AppCompatActivity {

    private WorkoutAdapter mWorkoutAdapter;
    private ArrayList<Workout> workouts = new ArrayList<>();

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mWorkoutDatabseReference;
    private ChildEventListener mChildEventListener;

    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_workout);

        RecyclerView recyclerView = findViewById(R.id.workouts_RV);
        mWorkoutAdapter = new WorkoutAdapter();
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mWorkoutAdapter);
        getWorkouts();
        fab = findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ViewWorkoutActivity.this, AddResults.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(ViewWorkoutActivity.this).toBundle());
                } else {
                    startActivity(intent);
                }
            }
        });


    }

    public void getWorkouts() {
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mWorkoutDatabseReference = mFirebaseDatabase.getReference().child("workouts");
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Workout workout = dataSnapshot.getValue(Workout.class);
                workouts.add(workout);
                mWorkoutAdapter.setWorkouts(workouts);

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
        mWorkoutDatabseReference.orderByChild("user").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid()).addChildEventListener(mChildEventListener);
    }

}
