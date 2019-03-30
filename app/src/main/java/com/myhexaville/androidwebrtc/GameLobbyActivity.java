package com.myhexaville.androidwebrtc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class GameLobbyActivity extends AppCompatActivity {

    Socket member_socket;
    SharedPreferences pref;
    String myId;
    String friendId;
    boolean isConnect = false;
    boolean isRunning=false;

    LinearLayout connectList, inviteList;
    TextView connectId, inviteId;
    Button inviteBtn, acceptBtn;

    boolean master = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_lobby);
        Log.d("GameLobbyActivity", "onCreate");

        pref = getSharedPreferences("login", 0);
        myId = pref.getString("id","");

        connectList = (LinearLayout)findViewById(R.id.connectList);
        inviteList = (LinearLayout)findViewById(R.id.inviteList);

        connectId = (TextView)findViewById(R.id.connectId);
        inviteId = (TextView)findViewById(R.id.inviteId);

        inviteBtn = (Button)findViewById(R.id.inviteBtn);
        acceptBtn = (Button)findViewById(R.id.acceptBtn);

        ConnectionThread thread = new ConnectionThread();
        thread.start();

        inviteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendToServerThread thread = new SendToServerThread(member_socket, "초대");
                thread.start();
                inviteBtn.setText("요청 중");
            }
        });

        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendToServerThread thread = new SendToServerThread(member_socket, "수락");
                thread.start();

//                Intent intent = new Intent(getApplicationContext(), GameRoomActivity.class);
//                intent.putExtra("player1", inviteId.getText());
//                intent.putExtra("player2", myId);
//                startActivity(intent);
//                finish();

//                try {
//                    isRunning = false;
//                    member_socket.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        });

    }

    @Override
    protected void onDestroy ()
    {
        super.onDestroy();
        Log.d("GameLobbyActivity", "onDestroy");
        try {
            isRunning = false;
            member_socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.d("GameLobbyActivity", "onPause");

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d("GameLobbyActivity", "onResume");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("GameLobbyActivity", "onStart");
    }

    // 서버접속 처리하는 스레드 클래스
    class ConnectionThread extends Thread {

        @Override
        public void run() {
            try {
                // 접속한다.
                final Socket socket = new Socket("115.68.216.237", 50002);
                member_socket=socket;
                // 미리 입력했던 닉네임을 서버로 전달한다.
                String nickName = myId;
                //user_nickname=nickName;     // 화자에 따라 말풍선을 바꿔주기위해
                // 스트림을 추출
                OutputStream os = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(os);
                // 닉네임을 송신한다.
                dos.writeUTF(nickName);

                // 접속 상태를 true로 셋팅한다.
                isConnect=true;
                // 메세지 수신을 위한 스레드 가동
                isRunning=true;
                MessageThread thread=new MessageThread(socket);
                thread.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //메세지 수신 스레드
    class MessageThread extends Thread {
        Socket socket;
        DataInputStream dis;

        public MessageThread(Socket socket) {
            try {
                this.socket = socket;
                InputStream is = socket.getInputStream();
                dis = new DataInputStream(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try{
                while (isRunning){
                    // 서버로부터 데이터를 수신받는다.
                    final String msg=dis.readUTF();
                    // 화면에 출력
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 메세지의 시작 이름이 내 닉네임과 일치하면
                            Log.d("MessageThread", msg);

                            if(msg.startsWith(myId)){
                                String[] split = msg.split(" : ");

                                //방장x
                                if(split[1].equals("수락")){
                                    Log.d("p1,p2,master", inviteId.getText().toString() + " / " + myId + " / " + master);
                                    Intent intent = new Intent(getApplicationContext(), UnityPlayerActivity.class);
                                    intent.putExtra("player1", inviteId.getText().toString());
                                    intent.putExtra("player2", myId);
                                    intent.putExtra("master", "false");
                                    startActivity(intent);
                                    finish();
                                }
                            }
                            else{
                                //접속 목록에 추가
                                if(msg.startsWith("서버")){
                                    Log.d("서버", msg);
                                    String[] split = msg.split(" : ");

                                    if(!split[1].equals(myId)){
                                        connectId.setText(split[1]);
                                        connectList.setVisibility(View.VISIBLE);
                                    }

                                    if(!split[2].equals(myId)){
                                        connectId.setText(split[2]);
                                        connectList.setVisibility(View.VISIBLE);
                                    }

                                }

                                //초대 목록에 추가
                                else{
                                    String[] split = msg.split(" : ");

                                    if(split[1].equals("초대")){
                                        inviteId.setText(split[0]);
                                        inviteList.setVisibility(View.VISIBLE);
                                    }
                                    //방장일때
                                    else if(split[1].equals("수락")){
                                        Intent intent = new Intent(getApplicationContext(), UnityPlayerActivity.class);
                                        intent.putExtra("player1", myId);
                                        intent.putExtra("player2", split[0]);
                                        intent.putExtra("master", "true");
                                        startActivity(intent);
                                        finish();
                                    }
                                }
                            }
                            //초대 목록에 추가
//                            if(!msg.startsWith(myId)){
//                                String[] split = msg.split(" : ");
//
//                                if(split[1].equals("초대")){
//                                    inviteId.setText(split[0]);
//                                    inviteList.setVisibility(View.VISIBLE);
//                                }
//
//                                else if(split[1].equals("수락")){
//                                    Intent intent = new Intent(getApplicationContext(), GameRoomActivity.class);
//                                    intent.putExtra("player1", myId);
//                                    intent.putExtra("player2", split[0]);
//                                    startActivity(intent);
//                                    finish();
//                                }
//                            }


                        }
                    });
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    // 서버에 데이터를 전달하는 스레드
    class SendToServerThread extends Thread{
        Socket socket;
        String msg;
        DataOutputStream dos;

        public SendToServerThread(Socket socket, String msg){
            try{
                this.socket=socket;
                this.msg=msg;
                OutputStream os=socket.getOutputStream();
                dos=new DataOutputStream(os);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try{
                dos.writeUTF(msg);

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
