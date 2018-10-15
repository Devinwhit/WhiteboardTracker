package com.devinwhitney.android.whiteboardtracker.model;

import java.util.Date;

/**
 * Created by devin on 10/5/2018.
 */

public class Workout {

    private String user;
    private String date;
    private String wod;
    private String results;
    private String gym;
    private String title;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDate() {
        return date;
    }

    public String getGym() {
        return gym;
    }

    public void setGym(String gym) {
        this.gym = gym;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getWod() {
        return wod;
    }

    public void setWod(String wod) {
        this.wod = wod;
    }

    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
