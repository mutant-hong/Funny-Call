package com.myhexaville.androidwebrtc;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.myhexaville.androidwebrtc.app_rtc_sample.main.AppRTCMainActivity;

public class CallingActivity extends AppCompatActivity {

    TextView friendName;
    ImageView call, disconnect;

    String friendId, caller, roomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        friendName = (TextView) findViewById(R.id.friendId);
        call = (ImageView) findViewById(R.id.call);
        disconnect = (ImageView) findViewById(R.id.disconnect);

        Intent intent = getIntent();

        friendId = intent.getStringExtra("friendId");
        caller = intent.getStringExtra("caller");
        roomId = intent.getStringExtra("roomId");

        friendName.setText(friendId);

    }

    @Override
    protected void onStart() {
        super.onStart();

        //전화 받음
        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AppRTCMainActivity.class);
                intent.putExtra("friendId", friendId);
                intent.putExtra("caller", caller);
                intent.putExtra("roomId", roomId);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                startActivity(intent);

                finish();

            }
        });

        //전화 거절
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}