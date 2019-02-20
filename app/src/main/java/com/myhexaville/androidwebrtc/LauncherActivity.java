package com.myhexaville.androidwebrtc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.myhexaville.androidwebrtc.tutorial.MediaStreamActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class LauncherActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static String IP_ADDRESS = "115.68.216.237";
    private static String TAG = "LauncherActivity";

    ImageView list, history, friends;
    TextView userId;

    SharedPreferences pref;

    //리사이클러뷰
    RecyclerView recycler;
    RecyclerView.LayoutManager layoutManager;

    ArrayList<String> friendsList;

    String myId;

    //새로고침
    SwipeRefreshLayout swipeRefreshLayout;

    //opencv 테스트 버튼
    Button opencv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        list = (ImageView)findViewById(R.id.list);
        history = (ImageView)findViewById(R.id.history);
        friends = (ImageView)findViewById(R.id.friends);

        userId = (TextView)findViewById(R.id.userId);

        //opencv 테스트 버튼
        opencv = (Button)findViewById(R.id.opencv);

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);

        pref = getSharedPreferences("login", 0);
        myId = pref.getString("id","");

        userId.setText(myId);
        //친구목록 갱신

        friendsList = new ArrayList<>();
        friendsList task = new friendsList();
        task.execute("http://" + IP_ADDRESS + "/faceToface/friendsList.php", myId);

        recycler = (RecyclerView)findViewById(R.id.recycler);
        recycler.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(layoutManager);


    }

    @Override
    protected void onStart(){
        super.onStart();

        //원래는 최근 기록 -> 일단 테스트 버튼으로 사용

        history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MediaStreamActivity.class);
                startActivity(intent);
                //overridePendingTransition(0,0);
                //finish();
            }
        });

        friends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FriendsActivity.class);
                startActivity(intent);
                overridePendingTransition(0,0);
                finish();
            }
        });

        //opencv 테스트 버튼
        opencv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), OpencvActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);

        //3초후에 해당 adapoter를 갱신하고 동글뱅이를 닫아준다.setRefreshing(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                friendsList task = new friendsList();
                task.execute("http://" + IP_ADDRESS + "/faceToface/friendsList.php", myId);

                swipeRefreshLayout.setRefreshing(false);
            }
        },2000);
    }

    class friendsList extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d(TAG, "POST response  - " + result);

            friendsList.clear();

            String[] split = result.split("/");

            for(int i=0; i<split.length; i++){
                if(split[i]!="")
                    friendsList.add(split[i]);
            }

            mainAdapter mainAdapter = new mainAdapter(friendsList);
            recycler.setAdapter(mainAdapter);
        }

        @Override
        protected String doInBackground(String... params) {

            String myId = (String)params[1];

            String serverURL = (String)params[0];
            String postParameters = "myId=" + myId;


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
                Log.d(TAG, "POST response code - " + responseStatusCode);

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

                Log.d(TAG, "InsertData: Error ", e);

                return new String("Error: " + e.getMessage());
            }

        }
    }

/*
    public void openAppRTCActivity(View view) {
        startActivity(new Intent(this, AppRTCMainActivity.class));
    }

    public void openSampleActivity(View view) {
        startActivity(new Intent(this, CameraRenderActivity.class));
    }

    public void openSamplePeerConnectionActivity(View view) {
        startActivity(new Intent(this, MediaStreamActivity.class));
    }

    public void openSampleDataChannelActivity(View view) {
        startActivity(new Intent(this, DataChannelActivity.class));
    }

    public void openSampleSocketActivity(View view) {
        startActivity(new Intent(this, CompleteActivity.class));

    }
    */
}
