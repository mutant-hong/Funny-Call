package com.myhexaville.androidwebrtc;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DetailHistoryActivity extends AppCompatActivity {

    TextView friendId, date, time, call_time, call_kind;
    Button back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailhistory);

        friendId = (TextView)findViewById(R.id.friendId);
        date = (TextView)findViewById(R.id.date);
        time = (TextView)findViewById(R.id.time);
        call_time = (TextView)findViewById(R.id.call_time);
        call_kind = (TextView)findViewById(R.id.call_kind);
        back = (Button)findViewById(R.id.back);

        Intent intent = getIntent();
        String sfriendId = intent.getStringExtra("friendId");
        String sdate = intent.getStringExtra("date");
        String stime = intent.getStringExtra("time");
        String scaller = intent.getStringExtra("caller");
        String smissied = intent.getStringExtra("missied");

        String[] split = sdate.split("일");

        friendId.setText(intent.getStringExtra("friendId"));
        date.setText(split[0] + "일");
        time.setText(split[1]);

        //통화 시간 = 0 -> 취소된 통화, 부재중 전화
        if(stime.equals("0") && smissied.equals("false"))
            call_kind.setText("취소된 통화");
        else if(stime.equals("0") && smissied.equals("true"))
            call_kind.setText("부재중 전화");

        //발신, 수신 통화
        if(!stime.equals("0") && scaller.equals("true"))
            call_kind.setText("발신 통화");
        else if(!stime.equals("0") && scaller.equals("false"))
            call_kind.setText("수신 통화");

        if(!stime.equals("0")){
            int t = Integer.parseInt(stime);
            t = t / 1000;

            int h = t / 3600;
            int m = (t - (3600 * h)) / 60;
            int s = (t - (3600 * h)) % 60;

            if(h != 0 && m != 0 && s != 0)
                call_time.setText(h + "시간 " + m + "분 " + s + "초");
            else if(h != 0 && m == 0 && s != 0)
                call_time.setText(h + "시간 " + s + "초");
            else if(h != 0 && m != 0 && s == 0)
                call_time.setText(h + "시간 " + m + "분");
            else if(h == 0 && m != 0 && s != 0)
                call_time.setText(m + "분 " + s + "초");
            else if(h == 0 && m != 0 && s == 0)
                call_time.setText(m + "분");
            else if(h == 0 && m == 0 && s != 0)
                call_time.setText(s + "초");
        }


        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CallHistoryActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

}
