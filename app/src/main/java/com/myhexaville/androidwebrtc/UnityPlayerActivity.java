package com.myhexaville.androidwebrtc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import com.unity3d.player.UnityPlayer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class UnityPlayerActivity extends Activity
{
    protected UnityPlayer mUnityPlayer; // don't change the name of this variable; referenced from native code
    Socket member_socket;
    SharedPreferences pref;
    String myId;
    boolean isConnect = false;
    boolean isRunning=false;

    String player1Id;
    String player2Id;

    // Setup activity layout
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        mUnityPlayer = new UnityPlayer(this);
        setContentView(mUnityPlayer);
        mUnityPlayer.requestFocus();

        Intent intent = getIntent();
        player1Id = intent.getStringExtra("player1");
        Log.d("player1", player1Id);
        player2Id = intent.getStringExtra("player2");
        Log.d("player2", player2Id);
        String master = intent.getStringExtra("master");
        Log.d("master", master);
        mUnityPlayer.UnitySendMessage("life", "OnMessageReceived", master + "/" + player1Id + "/" + player2Id);

        pref = getSharedPreferences("login", 0);
        myId = pref.getString("id","");

        ConnectionThread thread = new ConnectionThread();
        thread.start();
    }

    @Override protected void onNewIntent(Intent intent)
    {
        // To support deep linking, we need to make sure that the client can get access to
        // the last sent intent. The clients access this through a JNI api that allows them
        // to get the intent set on launch. To update that after launch we have to manually
        // replace the intent with the one caught here.
        setIntent(intent);
    }

    // Quit Unity
    @Override protected void onDestroy ()
    {
        mUnityPlayer.quit();
        super.onDestroy();
        Log.d("UnityPlayerActivity", "onDestroy");
        try{
            member_socket.close();
            isRunning=false;

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    // Pause Unity
    @Override protected void onPause()
    {
        super.onPause();
        mUnityPlayer.pause();
        Log.d("UnityPlayerActivity", "onPause");
    }

    // Resume Unity
    @Override protected void onResume()
    {
        super.onResume();
        mUnityPlayer.resume();
        Log.d("UnityPlayerActivity", "onResume");
    }

    @Override protected void onStart()
    {
        super.onStart();
        mUnityPlayer.start();
        Log.d("UnityPlayerActivity", "onStart");
        mUnityPlayer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

//                SendToServerThread thread = new SendToServerThread(member_socket, Float.toString(event.getX()));
//                thread.start();
                switch (event.getAction()){
                    case MotionEvent .ACTION_DOWN :
                        SendToServerThread thread = new SendToServerThread(member_socket, Float.toString(event.getX()));
                        thread.start();
                        //mUnityPlayer.UnitySendMessage("Player", "OnMessageReceived", Float.toString(event.getX()));
                        //Log.d("unity", + event.getX() + " / " + event.getY());

                }

                return false;
            }
        });
    }

    @Override protected void onStop()
    {
        super.onStop();
        mUnityPlayer.stop();
        Log.d("UnityPlayerActivity", "onStop");
    }

    // Low Memory Unity
    @Override public void onLowMemory()
    {
        super.onLowMemory();
        mUnityPlayer.lowMemory();
    }

    // Trim Memory Unity
    @Override public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_RUNNING_CRITICAL)
        {
            mUnityPlayer.lowMemory();
        }
    }

    // This ensures the layout will be correct.
    @Override public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mUnityPlayer.configurationChanged(newConfig);
    }

    // Notify Unity of the focus change.
    @Override public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
    }

    // For some reason the multiple keyevent type is not supported by the ndk.
    // Force event injection by overriding dispatchKeyEvent().
    @Override public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
            return mUnityPlayer.injectEvent(event);
        return super.dispatchKeyEvent(event);
    }

    // Pass any events not handled by (unfocused) views straight to UnityPlayer
    @Override public boolean onKeyUp(int keyCode, KeyEvent event)     { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event)   { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onTouchEvent(MotionEvent event)          { return mUnityPlayer.injectEvent(event); }
    /*API12*/ public boolean onGenericMotionEvent(MotionEvent event)  { return mUnityPlayer.injectEvent(event); }



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


                // ProgressDialog 를 제거한다.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //pro.dismiss();
                        // 접속 상태를 true로 셋팅한다.
                        isConnect=true;
                        // 메세지 수신을 위한 스레드 가동
                        isRunning=true;
                        MessageThread thread=new MessageThread(socket);
                        thread.start();
                    }
                });

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
                            // 메세지의 시작 이름이 내 닉네임과 일치하지 않으면
                            if(msg.startsWith(myId)){
                                String[] split = msg.split(" : ");
                                mUnityPlayer.UnitySendMessage("Player", "OnMessageReceived", split[1]);
                            }

                            else {
                                if(!msg.startsWith("서버")) {
                                    String[] split = msg.split(" : ");
                                    mUnityPlayer.UnitySendMessage("Player2", "OnMessageReceived", split[1]);
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

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //전송확인
                        Log.d("좌표", msg);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
