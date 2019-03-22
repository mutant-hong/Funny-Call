package com.myhexaville.androidwebrtc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.myhexaville.androidwebrtc.app_rtc_sample.main.AppRTCMainActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;

public class CallingActivity extends AppCompatActivity {

    private static String IP_ADDRESS = "115.68.216.237";

    TextView friendName;
    ImageView call, disconnect;

    String friendId, caller, roomId;

    public static Activity Calling;

    //5초안에 안받을시 부재중
    int timer=0;
    boolean missied = false;

    SharedPreferences pref;
    String myId;

    long callTime;
    CallDataDB callDataDB;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        callTime = System.currentTimeMillis();

        //mHandler.sendEmptyMessage(0);

        Calling = CallingActivity.this;

        friendName = (TextView) findViewById(R.id.friendId);
        call = (ImageView) findViewById(R.id.call);
        disconnect = (ImageView) findViewById(R.id.disconnect);

        Intent intent = getIntent();

        friendId = intent.getStringExtra("friendId");
        caller = intent.getStringExtra("caller");
        roomId = intent.getStringExtra("roomId");

        friendName.setText(friendId);

        pref = getSharedPreferences("login", 0);
        myId = pref.getString("id","");

    }

//    Handler mHandler = new Handler() {
//        public void handleMessage(Message msg) {
//            timer++;
//            //Toast.makeText(CallingActivity.this, "" + timer, Toast.LENGTH_SHORT).show();
//            // 메세지를 처리하고 또다시 핸들러에 메세지 전달 (1000ms 지연)
//            if(timer < 10)
//                mHandler.sendEmptyMessageDelayed(0,1000);
//
//            //부재중 처리
//            else {
//                disconnect.performClick();
//                missied = true;
//            }
//        }
//    };

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

                refuseNoti task = new refuseNoti();
                task.execute("http://" + IP_ADDRESS + "/faceToface/callNoti.php", myId, friendId, "수신거부");


                SimpleDateFormat dataformat = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분");
                String date = dataformat.format(callTime);

                callDataDB = new CallDataDB(getApplicationContext(), CallDataDB.tableName, null, 1);
                database = callDataDB.getWritableDatabase();

                callDataDB.insertData(database, friendId, date, 0, "false", Boolean.toString(missied));

                finish();
            }
        });

    }

    //수신 거부 알림
    class refuseNoti extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d("AppRTCMainActivity", "POST response  - " + result);
        }

        @Override
        protected String doInBackground(String... params) {

            String myId = (String)params[1];
            String friendId = (String)params[2];
            String roomId = (String)params[3];

            String serverURL = (String)params[0];
            String postParameters = "myId=" + myId + "&friendId=" + friendId + "&roomId=" + roomId;


            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d("AppRTCMainActivity", "POST response code - " + responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString();

            } catch (Exception e) {

                Log.d("AppRTCMainActivity", "InsertData: Error ", e);

                return new String("Error: " + e.getMessage());
            }

        }
    }
}