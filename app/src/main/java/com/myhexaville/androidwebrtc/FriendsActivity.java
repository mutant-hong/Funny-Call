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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class FriendsActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static String IP_ADDRESS = "115.68.216.237";
    private static String TAG = "FriendsActivity";

    //탭
    ImageView list, history, friends;

    EditText friendId;
    Button searchBtn;
    LinearLayout friendSearchLayout;
    TextView searchRst;

    SharedPreferences pref;
    //임시 저장
    public static String myId;

    //리사이클러뷰
    RecyclerView recycler_apply, recycler_req;
    RecyclerView.LayoutManager layoutManager_apply, layoutManager_req;

    ArrayList<String> friendsList_apply, friendsList_req;

    //새로고침
    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        friendId = (EditText) findViewById(R.id.friendId);
        searchBtn = (Button)findViewById(R.id.searchBtn);
        friendSearchLayout = (LinearLayout) findViewById(R.id.friendSearchLayout);
        searchRst = (TextView)findViewById(R.id.searchRst);
        //addfriend = (TextView)findViewById(R.id.addfriend);

        list = (ImageView)findViewById(R.id.list);
        history = (ImageView)findViewById(R.id.history);
        friends = (ImageView)findViewById(R.id.friends);

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);

        friendSearchLayout.setVisibility(View.GONE);

        pref = getSharedPreferences("login", 0);

        myId = pref.getString("id", "");

        recycler_req = (RecyclerView)findViewById(R.id.recycler_req);
        recycler_req.setHasFixedSize(true);

        friendsList_req = new ArrayList<>();

        recycler_apply = (RecyclerView)findViewById(R.id.recycler_apply);
        recycler_apply.setHasFixedSize(true);

        friendsList_apply = new ArrayList<>();

        //친구 요청 리스트 가져오기
        reqFriends task = new reqFriends();
        task.execute("http://" + IP_ADDRESS + "/faceToface/reqFriends.php", myId);

        //친구 수락 리스트
        applyFriends task_apply = new applyFriends();
        task_apply.execute("http://" + IP_ADDRESS + "/faceToface/applyFriends.php", myId);

        layoutManager_req = new LinearLayoutManager(this);
        recycler_req.setLayoutManager(layoutManager_req);

        layoutManager_apply = new LinearLayoutManager(this);
        recycler_apply.setLayoutManager(layoutManager_apply);

        /*
        try {
            String addFriendID = getIntent().getStringExtra("친구요청");

            Log.d("frID", addFriendID);
            if(addFriendID != "" || addFriendID != null)
                friendsList.add(addFriendID);
        }catch (Exception e){

        }
        */

    }

    @Override
    protected void onStart(){
        super.onStart();

        //친구 검색
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String search = friendId.getText().toString();

                searchFriends task = new searchFriends();
                task.execute("http://" + IP_ADDRESS + "/faceToface/searchFriends.php", search);
            }
        });

        //리사이클러뷰로 바꿔야함... 요청 수락 -> DB에 친구 등록
        /*
        addfriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!addfriend.getText().toString().isEmpty()){
                    String friendId = addfriend.getText().toString();

                    //acceptFriends task = new acceptFriends();
                    //task.execute("http://" + IP_ADDRESS + "/faceToface/acceptFriends.php", myId,friendId);
                }
            }
        });
        */

        //탭
        history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CallHistoryActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }
        });

        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LauncherActivity.class);
                startActivity(intent);
                overridePendingTransition(0,0);
                finish();
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

                reqFriends task = new reqFriends();
                task.execute("http://" + IP_ADDRESS + "/faceToface/reqFriends.php", myId);

                applyFriends task_apply = new applyFriends();
                task_apply.execute("http://" + IP_ADDRESS + "/faceToface/applyFriends.php", myId);

                swipeRefreshLayout.setRefreshing(false);
            }
        },2000);
    }

    //친구 검색
    class searchFriends extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d(TAG, "POST response  - " + result);

            if(result.equals("true")){
                Toast.makeText(FriendsActivity.this, "친구 검색 성공", Toast.LENGTH_SHORT).show();

                friendSearchLayout.setVisibility(View.VISIBLE);
                searchRst.setText(friendId.getText().toString());

                friendId.setText("");

                //친구 추가
                searchRst.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String friendId = searchRst.getText().toString();

                        addFriends task = new addFriends();
                        task.execute("http://" + IP_ADDRESS + "/faceToface/addFriends.php", myId,friendId);


                        //친구 신청목록에 추가
                        friendsList_apply.add(friendId);

                        FriendsApplyAdapter friendsApplyAdapter = new FriendsApplyAdapter(friendsList_apply);
                        recycler_apply.setAdapter(friendsApplyAdapter);

                    }
                });

            }
            else if(result.equals("false")){
                Toast.makeText(FriendsActivity.this, "친구 검색 실패", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(String... params) {

            String search = (String)params[1];

            String serverURL = (String)params[0];
            String postParameters = "search=" + search;


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

    //친구 추가
    class addFriends extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d(TAG, "POST response  - " + result);

            if(result.equals("true")){
                Toast.makeText(FriendsActivity.this, "친구 추가 성공", Toast.LENGTH_SHORT).show();

                //friend = searchRst.getText().toString();
                friendSearchLayout.setVisibility(View.GONE);


            }
            else if(result.equals("false")){
                Toast.makeText(FriendsActivity.this, "친구 추가 실패", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(String... params) {

            String myId = (String)params[1];
            String friendId = (String)params[2];

            String serverURL = (String)params[0];
            String postParameters = "myId=" + myId + "&friendId=" + friendId;


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

    //친구 요청 리스트 가져오기
    class reqFriends extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d(TAG, "reqFriends response  - " + result);

            friendsList_req.clear();
            String[] split = result.split("/");

            for(int i=0; i<split.length; i++){
                if(split[i]!="")
                    friendsList_req.add(split[i]);
            }

            friendsAdapter friendsAdapter = new friendsAdapter(friendsList_req);
            recycler_req.setAdapter(friendsAdapter);

        }

        @Override
        protected String doInBackground(String... params) {

            String myId = (String)params[1];
            //String friendId = (String)params[2];

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

    //친구 요청 리스트 가져오기
    class applyFriends extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d(TAG, "applyFriends response  - " + result);

            friendsList_apply.clear();
            String[] split = result.split("/");

            for(int i=0; i<split.length; i++){
                if(split[i]!="")
                    friendsList_apply.add(split[i]);
            }

            FriendsApplyAdapter friendsApplyAdapter = new FriendsApplyAdapter(friendsList_apply);
            recycler_apply.setAdapter(friendsApplyAdapter);
            /*
            if(result.equals("true")){
                Toast.makeText(FriendsActivity.this, "친구 추가 성공", Toast.LENGTH_SHORT).show();

                //friend = searchRst.getText().toString();
                friendSearchLayout.setVisibility(View.GONE);

            }
            else if(result.equals("false")){
                Toast.makeText(FriendsActivity.this, "친구 추가 실패", Toast.LENGTH_SHORT).show();
            }
            */
        }

        @Override
        protected String doInBackground(String... params) {

            String myId = (String)params[1];
            //String friendId = (String)params[2];

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


}
