/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.myhexaville.androidwebrtc.app_rtc_sample.main;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.myhexaville.androidwebrtc.LauncherActivity;
import com.myhexaville.androidwebrtc.R;
import com.myhexaville.androidwebrtc.app_rtc_sample.call.CallActivity;
import com.myhexaville.androidwebrtc.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.EXTRA_ROOMID;

/**
 * Handles the initial setup where the user selects which room to join.
 */
public class AppRTCMainActivity extends AppCompatActivity {

    private static String IP_ADDRESS = "115.68.216.237";

    private static final int CONNECTION_REQUEST = 1;
    private static final int RC_CALL = 111;
    private ActivityMainBinding binding;

    ImageView xBtn;

    SharedPreferences pref;
    String myId;

    String caller;

    String roomID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        //binding.connectButton.setOnClickListener(v -> connect());


        binding.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });



        //binding.roomEdittext.requestFocus();
        Log.d("apprtc", "oncreate");

        //Toast.makeText(this, binding.roomEdittext.getText().toString(), Toast.LENGTH_SHORT).show();
        xBtn = (ImageView)findViewById(R.id.xBtn);

        Intent intent = getIntent();
        String friendId = intent.getStringExtra("friendId");
        caller = intent.getStringExtra("caller");

        //상대방에게 왔을때
        if(caller.equals("false"))
            roomID = intent.getStringExtra("roomId");

        binding.roomEdittext.setText(friendId);

        pref = getSharedPreferences("login", 0);
        myId = pref.getString("id","");

        //내가 걸었을 때
        if(!caller.equals("true"))
            binding.connectButton.performClick();

    }

    @Override
    protected void onStart(){
        super.onStart();
        Log.d("apprtc", "onStart");

        xBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LauncherActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d("apprtc", "onRequestPermissionsResult");
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_CALL)
    public void connect() {

        Log.d("apprtc", "connect");

        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

        Log.d("apprtc", "1");


        if (EasyPermissions.hasPermissions(this, perms)) {
            Log.d("apprtc", "2");
            //내가 걸었을때
            if(caller.equals("true")){
                //방 번호 생성
                Random random = new Random();
                String roomId = Integer.toString(random.nextInt(900000000) + 100000000);
                connectToRoom(roomId);
                //connectToRoom("11" + myId+""+binding.roomEdittext.getText().toString());
            }

            //상대방에게 왔을때
            else{
                connectToRoom(roomID);
                //connectToRoom("11" + binding.roomEdittext.getText().toString()+""+myId);
            }

        } else {
            EasyPermissions.requestPermissions(this, "Need some permissions", RC_CALL, perms);
        }
    }

    private void connectToRoom(String roomId) {
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra(EXTRA_ROOMID, roomId);
        startActivityForResult(intent, CONNECTION_REQUEST);

        if(caller.equals("true")){
            //상대방에게 알림
            callNoti task = new callNoti();
            task.execute("http://" + IP_ADDRESS + "/faceToface/callNoti.php", myId, binding.roomEdittext.getText().toString(), roomId);

            //결과 받고 activity finish();
        }

    }

    //친구 추가
    class callNoti extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d("AppRTCMainActivity", "POST response  - " + result);

            //Toast.makeText(AppRTCMainActivity.this, result, Toast.LENGTH_SHORT).show();

            /*
            if(result.equals("true")){
                Toast.makeText(AppRTCMainActivity.this, "친구 추가 성공", Toast.LENGTH_SHORT).show();


            }
            else if(result.equals("false")){
                Toast.makeText(AppRTCMainActivity.this, "친구 추가 실패", Toast.LENGTH_SHORT).show();
            }
            */
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
