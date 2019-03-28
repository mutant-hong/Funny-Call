package com.myhexaville.androidwebrtc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class GameRoomActivity extends AppCompatActivity {

    TextView player1, player2;
    Button go;

    Socket member_socket;
    SharedPreferences pref;
    String myId;

    boolean isConnect = false;
    boolean isRunning=false;

    boolean master = false;
    boolean ready = false;

    String player1Id;
    String player2Id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_room);

        player1 = (TextView)findViewById(R.id.player1);
        player2 = (TextView)findViewById(R.id.player2);

        go = (Button)findViewById(R.id.go);

        ConnectionThread thread = new ConnectionThread();
        thread.start();

        pref = getSharedPreferences("login", 0);
        myId = pref.getString("id","");

        Intent intent = getIntent();

        player1Id = intent.getStringExtra("player1");
        player2Id = intent.getStringExtra("player2");

        player1.setText(player1Id + " (방장)");
        player2.setText(player2Id);

        if(player1Id.equals(myId)) {
            go.setText("시작");
            go.setBackgroundColor(Color.LTGRAY);
            go.setEnabled(false);

            //방장으로 설정
            master = true;
        }
        else{
            go.setText("준비");
            master = false;
        }

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //방장 일때 클릭 -> 게임 시작
                if(master){
                    SendToServerThread thread = new SendToServerThread(member_socket, "시작");
                    thread.start();
                }
                else{
                    String msg = "";

                    if(ready) {
                        msg = "준비취소";
                        go.setText("준비");
                        ready = false;
                    }
                    else {
                        msg = "준비완료";
                        go.setText("준비취소");
                        ready = true;
                    }

                    SendToServerThread thread = new SendToServerThread(member_socket, msg);
                    thread.start();
                }
            }
        });

    }

    // 서버접속 처리하는 스레드 클래스
    class ConnectionThread extends Thread {

        @Override
        public void run() {
            try {
                // 접속한다.
                final Socket socket = new Socket("115.68.216.237", 50001);
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
                            if(msg.startsWith(myId)){
                                String[] split = msg.split(" : ");

                                if(split[1].equals("시작")){
                                    Intent intent = new Intent(getApplicationContext(), UnityPlayerActivity.class);
                                    intent.putExtra("player1", player1Id);
                                    intent.putExtra("player2", player2Id);
                                    intent.putExtra("master", Boolean.toString(master));
                                    startActivity(intent);

                                    try {
                                        isRunning = false;
                                        member_socket.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            //다르고 서버도 아닐경우
                            else{
                                if(!msg.startsWith("서버")) {
                                    String[] split = msg.split(" : ");

                                    if (split[1].equals("준비완료")) {
                                        Toast.makeText(GameRoomActivity.this, "준비완료", Toast.LENGTH_SHORT).show();
                                        go.setBackgroundColor(Color.GREEN);
                                        go.setEnabled(true);

                                    }

                                    if (split[1].equals("준비취소")) {
                                        Toast.makeText(GameRoomActivity.this, "준비취소", Toast.LENGTH_SHORT).show();
                                        go.setBackgroundColor(Color.LTGRAY);
                                        go.setEnabled(false);
                                    }

                                    if(split[1].equals("시작")){
                                        Intent intent = new Intent(getApplicationContext(), UnityPlayerActivity.class);
                                        intent.putExtra("player1", player1Id);
                                        intent.putExtra("player2", player2Id);
                                        intent.putExtra("master", Boolean.toString(master));
                                        startActivity(intent);

                                        try {
                                            isRunning = false;
                                            member_socket.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                            }
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
