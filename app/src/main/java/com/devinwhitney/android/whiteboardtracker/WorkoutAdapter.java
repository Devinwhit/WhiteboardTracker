package com.devinwhitney.android.whiteboardtracker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.devinwhitney.android.whiteboardtracker.model.Workout;

import java.util.ArrayList;

/**
 * Created by devin on 10/5/2018.
 */

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutAdapterViewHolder> {

    private ArrayList<Workout> mWorkouts = new ArrayList<>();

    public WorkoutAdapter() {
    }

    @NonNull
    @Override
    public WorkoutAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layout = R.layout.single_workout;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(layout, parent, false);
        return new WorkoutAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WorkoutAdapterViewHolder holder, int position) {
        System.out.println(String.valueOf(position));
        holder.mWorkoutText.setText(mWorkouts.get(position).getResults());
        holder.mWorkoutTitle.setText(mWorkouts.get(position).getTitle());
    }

    @Override
    public int getItemCount() {
        if (null == mWorkouts) return 0;
        return mWorkouts.size();
    }

    public class WorkoutAdapterViewHolder extends RecyclerView.ViewHolder {
        public TextView mWorkoutText;
        public TextView mWorkoutTitle;
        public Workout singleWorkout;
        public WorkoutAdapterViewHolder(View view) {
            super(view);
            mWorkoutText = view.findViewById(R.id.workout_cardview_text);
            mWorkoutTitle = view.findViewById(R.id.workout_cardview_title);
            //mWorkoutText.setText(""));
        }
    }

    public void setWorkouts(ArrayList<Workout> workouts) {
        if (workouts != null) {
            mWorkouts.clear();
            mWorkouts.addAll(workouts);
            notifyDataSetChanged();
        } else {
            mWorkouts.clear();
        }
    }
}
