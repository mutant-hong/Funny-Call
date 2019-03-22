package com.myhexaville.androidwebrtc;

public class history {

    String friendId;
    String date;
    String time;
    String caller;
    String missied;

    public history(String friendId, String date, String time, String caller, String missied){
        this.friendId = friendId;
        this.date = date;
        this.time = time;
        this.caller = caller;
        this.missied = missied;
    }
}
