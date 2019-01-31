package com.myhexaville.androidwebrtc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoinActivity extends AppCompatActivity {

    private static String IP_ADDRESS = "115.68.216.237";
    private static String TAG = "phptest";

    private EditText userId;
    private EditText userPw, userPwConfirm;
    private Button joinBtn, userIdCheck;
    private CheckBox checkBox1, checkBox2;

    boolean idCheck = false, pwCheck = false, check = false;

    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        userId = (EditText) findViewById(R.id.userId);
        userPw = (EditText)findViewById(R.id.userPw);
        userPwConfirm = (EditText)findViewById(R.id.userPwConfirm);
        userIdCheck = (Button)findViewById(R.id.userIdCheck);
        joinBtn = (Button)findViewById(R.id.joinBtn);
        checkBox1 = (CheckBox)findViewById(R.id.checkbox1);
        checkBox2 = (CheckBox)findViewById(R.id.checkbox2);

        pref = getSharedPreferences("token", 0);
        //String token = pref.getString("token","");
        //Toast.makeText(this, token, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart(){
        super.onStart();

        userIdCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = userId.getText().toString();

                if(id == "" || id == null){
                    Toast.makeText(JoinActivity.this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
                else{
                    UserIdCheck task = new UserIdCheck();
                    task.execute("http://" + IP_ADDRESS + "/faceToface/IdCheck.php", id);
                }
            }
        });

        joinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String password = userPw.getText().toString();
                 //pwPattern = "^(?=.*\\d)(?=.*[~`!@#$%\\^&*()-])(?=.*[a-z])(?=.*[A-Z]).{8,16}$";
                String pwPattern = "^(?=.*\\d)(?=.*[~`!@#$%\\^&*()-])(?=.*[a-zA-Z]).{8,16}$";
                Matcher matcher = Pattern.compile(pwPattern).matcher(password);

                //비밀번호 확인
                if(matcher.matches()){
                    Log.d("비밀번호 유효성", "ok");
                    if(password.equals(userPwConfirm.getText().toString())){
                        pwCheck = true;
                    }
                }

                //약관 동의
                if(checkBox1.isChecked() && checkBox2.isChecked()){
                    check = true;
                }

                if(idCheck && pwCheck && check){
                    String id = userId.getText().toString();
                    String pw = userPw.getText().toString();

                    //SharedPreferences pref = getSharedPreferences("token", 0);
                    String token = pref.getString("token","");

                    JoinUser task = new JoinUser();
                    task.execute("http://" + IP_ADDRESS + "/faceToface/join.php", id,pw,token);
                }

                else if(!idCheck){
                    Toast.makeText(JoinActivity.this, "아이디 중복확인을 해주세요.", Toast.LENGTH_SHORT).show();
                }

                else if(!pwCheck){
                    Toast.makeText(JoinActivity.this, "비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
                }

                else if(!check){
                    Toast.makeText(JoinActivity.this, "약관 동의를 해주세요.", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    class UserIdCheck extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d(TAG, "POST response  - " + result);

            //아이디 중복확인

            if(result.equals("true")){
                Toast.makeText(JoinActivity.this, "사용 가능한 아이디 입니다.", Toast.LENGTH_SHORT).show();

                idCheck = true;
            }

            else if(result.equals("false")){
                Toast.makeText(JoinActivity.this, "이미 사용 중인 아이디 입니다.", Toast.LENGTH_SHORT).show();

                idCheck = false;
            }
        }

        @Override
        protected String doInBackground(String... params) {

            String id = (String)params[1];

            String serverURL = (String)params[0];
            String postParameters = "id=" + id;


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

    class JoinUser extends AsyncTask<String, Void, String> {
        //ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //progressDialog = ProgressDialog.show(JoinActivity.this,"Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            //progressDialog.dismiss();
            Log.d(TAG, "POST response  - " + result);

            if(result.equals("true")){
                Toast.makeText(JoinActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
            else if(result.equals("false")){
                Toast.makeText(JoinActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(String... params) {

            String id = (String)params[1];
            String pw = (String)params[2];
            String token = (String)params[3];

            String serverURL = (String)params[0];
            String postParameters = "id=" + id + "&pw=" + pw + "&token=" + token;


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