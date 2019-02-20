/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.myhexaville.androidwebrtc.app_rtc_sample.call;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.myhexaville.androidwebrtc.R;
import com.myhexaville.androidwebrtc.app_rtc_sample.web_rtc.AppRTCAudioManager;
import com.myhexaville.androidwebrtc.app_rtc_sample.web_rtc.AppRTCClient;
import com.myhexaville.androidwebrtc.app_rtc_sample.web_rtc.AppRTCClient.RoomConnectionParameters;
import com.myhexaville.androidwebrtc.app_rtc_sample.web_rtc.AppRTCClient.SignalingParameters;
import com.myhexaville.androidwebrtc.app_rtc_sample.web_rtc.PeerConnectionClient;
import com.myhexaville.androidwebrtc.app_rtc_sample.web_rtc.PeerConnectionClient.PeerConnectionParameters;
import com.myhexaville.androidwebrtc.app_rtc_sample.web_rtc.WebSocketRTCClient;
import com.myhexaville.androidwebrtc.databinding.ActivityCallBinding;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.CAPTURE_PERMISSION_REQUEST_CODE;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.EXTRA_ROOMID;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.LOCAL_HEIGHT_CONNECTED;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.LOCAL_HEIGHT_CONNECTING;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.LOCAL_WIDTH_CONNECTED;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.LOCAL_WIDTH_CONNECTING;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.LOCAL_X_CONNECTED;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.LOCAL_X_CONNECTING;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.LOCAL_Y_CONNECTED;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.LOCAL_Y_CONNECTING;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.REMOTE_HEIGHT;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.REMOTE_WIDTH;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.REMOTE_X;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.REMOTE_Y;
import static com.myhexaville.androidwebrtc.app_rtc_sample.util.Constants.STAT_CALLBACK_PERIOD;
import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL;
import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT;


/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class CallActivity extends AppCompatActivity
        implements AppRTCClient.SignalingEvents, PeerConnectionClient.PeerConnectionEvents, OnCallEvents {
    private static final String LOG_TAG = "CallActivity";

    private PeerConnectionClient peerConnectionClient;
    private AppRTCClient appRtcClient;
    private SignalingParameters signalingParameters;
    private AppRTCAudioManager audioManager;
    private EglBase rootEglBase;
    private final List<VideoRenderer.Callbacks> remoteRenderers = new ArrayList<>();
    private Toast logToast;
    private boolean activityRunning;

    private RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionParameters peerConnectionParameters;

    private boolean iceConnected;
    private boolean isError;
    private long callStartedTimeMs;
    private boolean micEnabled = true;

    private ActivityCallBinding binding;

    /////////face detect 추가
    private Handler handler, handler2;
    private HandlerThread handlerThread, handlerThread2;

    Rect r, r2;
    Bitmap mbitmap, mbitmap2;
    Canvas canvas, canvas2;
    Paint myPaint, myPaint2;
    FirebaseVisionPoint noisePos;


    SharedPreferences pref;
    String myId;

    // 서버 접속 여부를 판별하기 위한 변수
    boolean isConnect = false;
    ProgressDialog pro;
    Socket member_socket;
    String user_nickname;
    // 어플 종료시 스레드 중지를 위해...
    boolean isRunning=false;

    boolean local = false;
    boolean remote = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call);

        remoteRenderers.add(binding.remoteVideoView);

        // Create video renderers.
        rootEglBase = EglBase.create();
        binding.localVideoView.init(rootEglBase.getEglBaseContext(), null);
        binding.remoteVideoView.init(rootEglBase.getEglBaseContext(), null);

        binding.localVideoView.setZOrderMediaOverlay(true);
        binding.localVideoView.setEnableHardwareScaler(true);
        binding.remoteVideoView.setEnableHardwareScaler(true);

        //faceDetector 추가
        myPaint = new Paint();
        myPaint.setColor(Color.BLACK);
//        myPaint.setStyle(Paint.Style.STROKE);
//        myPaint.setStrokeWidth(5);

        myPaint2 = new Paint();
        myPaint2.setColor(Color.BLACK);
        myPaint2.setStrokeWidth(10);

        pref = getSharedPreferences("login", 0);
        myId = pref.getString("id","");

        ConnectionThread thread = new ConnectionThread();
        thread.start();

        //얼굴인식 시작
        binding.faceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //인식 시작
                if(!local){
                    local = true;
                    binding.faceBtn.setText("o");
                }
                //인식 그만
                else{
                    local = false;
                    binding.faceBtn.setText("x");
                }
            }
        });

        binding.localVideoView.addFrameListener(bitmap -> {

            if(local) {
                Log.d("localVideoView", "프레임추가");
                detectLocalFace(bitmap);
            }
        },0.05f);

//        binding.remoteVideoView.addFrameListener(bitmap -> {
//            Log.d("remoteVideoView", "프레임추가");
//            detectRemoteFace(bitmap);
//        }, 0.05f);

        updateVideoView();

        // Get Intent parameters.
        final Intent intent = getIntent();
        String roomId = intent.getStringExtra(EXTRA_ROOMID);
        Log.d(LOG_TAG, "Room ID: " + roomId);
        if (roomId == null || roomId.length() == 0) {
            logAndToast(getString(R.string.missing_url));
            Log.e(LOG_TAG, "Incorrect room ID in intent!");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // If capturing format is not specified for screencapture, use screen resolution.
        peerConnectionParameters = PeerConnectionParameters.createDefault();

        // Create connection client. Use DirectRTCClient if room name is an IP otherwise use the
        // standard WebSocketRTCClient.
        appRtcClient = new WebSocketRTCClient(this);

        // Create connection parameters.
        roomConnectionParameters = new RoomConnectionParameters("https://appr.tc", roomId, false);

        setupListeners();

        peerConnectionClient = PeerConnectionClient.getInstance();
        peerConnectionClient.createPeerConnectionFactory(this, peerConnectionParameters, this);

        startCall();
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
                user_nickname=nickName;     // 화자에 따라 말풍선을 바꿔주기위해
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
                            // 메세지의 시작 이름이 내 닉네임과 일치한다면 -> x
                            if(msg.startsWith(user_nickname)){
                                //tv.setBackgroundResource(R.drawable.me);
                                //tv.setBackgroundColor(Color.rgb(0,0,255));

                            }
                            //일치하지 않으면 좌표값 받아서 canvas
                            else{
                                //tv.setBackgroundResource(R.drawable.you);
                                //tv.setBackgroundColor(Color.rgb(255,0,0));

                                //그리는 동작
                                Toast.makeText(CallActivity.this, msg, Toast.LENGTH_SHORT).show();
                                Log.d("MessageThread", msg);

                                String[] split = msg.split(" : ");
                                // 1, 2 index 사용

                                Log.d("length", split.length + "");

                                if(split.length == 3){
                                    float noiseX = Float.parseFloat(split[1]);
                                    float noiseY = Float.parseFloat(split[2]);

                                    CallActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            try {
                                                mbitmap = Bitmap.createBitmap(1440, 2560, Bitmap.Config.ARGB_8888);

                                                canvas = new Canvas(mbitmap);
                                                canvas.drawColor(Color.TRANSPARENT);

                                                binding.remoteVideoImage.setImageBitmap(mbitmap);

                                                float x = 1440 / 36;
                                                float y = 2560 / 36;
                                                //canvas.drawRect(r.left * x, (r.top + 3) * y, r.right * x, (r.bottom - 3) * y, myPaint);
                                                canvas.drawArc((noiseX - 2)*x, (noiseY - 1)*y,
                                                        (noiseX + 2)*x, (noiseY + 1)*y, 0, 360, true, myPaint);

                                                float leftTopX = (noiseX - 5)*x;
                                                float leftTopY = (noiseY - 1)*y;
                                                float rightBotX = (noiseX + 5)*x;
                                                float rightBotY = (noiseY + 1)*y;

                                                canvas.drawLine(leftTopX, leftTopY, leftTopX - 300, leftTopY - 100, myPaint2);
                                                canvas.drawLine(leftTopX, (leftTopY + rightBotY) / 2, leftTopX - 300, (leftTopY + rightBotY) / 2, myPaint2);
                                                canvas.drawLine(leftTopX, rightBotY, leftTopX - 300, rightBotY + 100, myPaint2);

                                                canvas.drawLine(rightBotX, leftTopY, rightBotX + 300, leftTopY - 100, myPaint2);
                                                canvas.drawLine(rightBotX, (leftTopY + rightBotY) / 2, rightBotX + 300, (leftTopY + rightBotY) / 2, myPaint2);
                                                canvas.drawLine(rightBotX, rightBotY, rightBotX + 300, rightBotY + 100, myPaint2);

                                            }catch (Exception e){

                                            }



                                        }
                                    });
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
                // 서버로 데이터를 보낸다.
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

    //ml kit 사용
    public void detectFace(FirebaseVisionImage image) {

        Log.d("detectFace", "얼굴 인식 메소드 진입");
        // [START set_detector_options]

        //얼굴 인식 옵션
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .enableTracking()
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .build();
        // [END set_detector_options]

        // [START get_detector]

        //detector 생성
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);
        // [END get_detector]


        //얼굴 인식 시작
        Task<List<FirebaseVisionFace>> result =
                detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {

                    //성공 했을 때
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> faces) {

                        //imageView.setImageBitmap(bitmap);
                        Log.d("onSuccess", "얼굴 인식 처리");

                        for (FirebaseVisionFace face : faces) {

                            Rect bounds = face.getBoundingBox();
                            r = bounds;

                            FirebaseVisionFaceLandmark noise = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE);
                            if (noise != null) {
                                noisePos = noise.getPosition();
                            }
                            //Toast.makeText(MediaStreamActivity.this, "b : " + bounds.bottom + ", t : " + bounds.top + ", l : " + bounds.left + ", r : " + bounds.right, Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void detectLocalFace(Bitmap bitmap){

        Log.d("local", binding.localVideoView.getWidth() + ", " + binding.localVideoView.getHeight());
        Bitmap dstBitmap;
        if (bitmap.getWidth() >= bitmap.getHeight()) {
            dstBitmap = Bitmap.createBitmap(bitmap, bitmap.getWidth()/2 - bitmap.getHeight()/2, 0,
                    bitmap.getHeight(), bitmap.getHeight()
            );

            Log.d("resize", dstBitmap.getWidth() + ", " + dstBitmap.getHeight());

        } else {
            dstBitmap = Bitmap.createBitmap(bitmap, 0, bitmap.getHeight()/2 - bitmap.getWidth()/2,
                    bitmap.getWidth(), bitmap.getWidth()
            );
            Log.d("resize", dstBitmap.getWidth() + ", " + dstBitmap.getHeight());
        }

        runInBackground(() -> {

            Log.d("runInBackground", "addFrameListener -> runInBackground");
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(dstBitmap);
            detectFace(image);

//                    Paint myPaint = new Paint();
//                    myPaint.setColor(Color.RED);
//                    myPaint.setStyle(Paint.Style.STROKE);
//                    myPaint.setStrokeWidth(5);


            CallActivity.this.runOnUiThread(new Runnable() {

                public void run() {
                    mbitmap = Bitmap.createBitmap(1440, 2560, Bitmap.Config.ARGB_8888);

                    canvas = new Canvas(mbitmap);
                    canvas.drawColor(Color.TRANSPARENT);

                    binding.localVideoImage.setImageBitmap(mbitmap);


                    Log.d("dsds", binding.localVideoImage.getWidth() + ", " + binding.localVideoImage.getHeight());
                    Log.d("runOnUiThread", "runOnUiThread -> setImageBitmap");

                    try {
                        Log.d("faceRect", "l : "+ r.left + ", t : " + r.top + ", r : " + r.right + ", b : " + r.bottom);
                        Log.d("noise", noisePos.getX() + ", " + noisePos.getY() + ", " + noisePos.getZ());

                        //서버로 좌표 보내기
                        if(noisePos != null && isConnect == true){
                            String msg= noisePos.getX() + " : " + noisePos.getY();
                            // 송신 스레드 가동
                            SendToServerThread thread=new SendToServerThread(member_socket,msg);
                            thread.start();
                        }
                        float x = 1440 / 36;
                        float y = 2560 / 36;
                        //canvas.drawRect(r.left * x, (r.top + 3) * y, r.right * x, (r.bottom - 3) * y, myPaint);
                        canvas.drawArc((noisePos.getX() - 2)*x, (noisePos.getY() - 1)*y, (noisePos.getX() + 2)*x, (noisePos.getY() + 1)*y, 0, 360, true, myPaint);

                        float leftTopX = (noisePos.getX() - 5)*x;
                        float leftTopY = (noisePos.getY() - 1)*y;
                        float rightBotX = (noisePos.getX() + 5)*x;
                        float rightBotY = (noisePos.getY() + 1)*y;

                        canvas.drawLine(leftTopX, leftTopY, leftTopX - 300, leftTopY - 100, myPaint2);
                        canvas.drawLine(leftTopX, (leftTopY + rightBotY) / 2, leftTopX - 300, (leftTopY + rightBotY) / 2, myPaint2);
                        canvas.drawLine(leftTopX, rightBotY, leftTopX - 300, rightBotY + 100, myPaint2);

                        canvas.drawLine(rightBotX, leftTopY, rightBotX + 300, leftTopY - 100, myPaint2);
                        canvas.drawLine(rightBotX, (leftTopY + rightBotY) / 2, rightBotX + 300, (leftTopY + rightBotY) / 2, myPaint2);
                        canvas.drawLine(rightBotX, rightBotY, rightBotX + 300, rightBotY + 100, myPaint2);

                        //사각형 초기화
                        noisePos = null;
                        r = null;
                        //canvas.drawRect(50, 100, 150, 200, myPaint);
                        //binding.surfaceView.drawRect(r.left, r.top, r.right, r.bottom);
                    }catch (Exception e){

                    }
                    //canvas.drawColor(Color.TRANSPARENT);

                }

            });

        });

    }

    private void detectRemoteFace(Bitmap bitmap){

        Log.d("remote", binding.remoteVideoView.getWidth() + ", " + binding.remoteVideoView.getHeight());
        Bitmap dstBitmap;
        if (bitmap.getWidth() >= bitmap.getHeight()) {
            dstBitmap = Bitmap.createBitmap(bitmap, bitmap.getWidth()/2 - bitmap.getHeight()/2, 0,
                    bitmap.getHeight(), bitmap.getHeight()
            );

            Log.d("resize", dstBitmap.getWidth() + ", " + dstBitmap.getHeight());

        } else {
            dstBitmap = Bitmap.createBitmap(bitmap, 0, bitmap.getHeight()/2 - bitmap.getWidth()/2,
                    bitmap.getWidth(), bitmap.getWidth()
            );
            Log.d("resize", dstBitmap.getWidth() + ", " + dstBitmap.getHeight());
        }

        runInBackground(() -> {

            Log.d("runInBackground", "addFrameListener -> runInBackground");
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(dstBitmap);
            detectFace(image);

            CallActivity.this.runOnUiThread(new Runnable() {

                public void run() {
                    mbitmap = Bitmap.createBitmap(1440, 2560, Bitmap.Config.ARGB_8888);

                    canvas = new Canvas(mbitmap);
                    canvas.drawColor(Color.TRANSPARENT);

                    binding.remoteVideoImage.setImageBitmap(mbitmap);


                    Log.d("dsds", binding.remoteVideoImage.getWidth() + ", " + binding.remoteVideoImage.getHeight());
                    Log.d("runOnUiThread", "runOnUiThread -> setImageBitmap");

                    try {
                        Log.d("faceRect", "l : "+ r.left + ", t : " + r.top + ", r : " + r.right + ", b : " + r.bottom);
                        Log.d("noise", noisePos.getX() + ", " + noisePos.getY() + ", " + noisePos.getZ());
                        float x = 1440 / 36;
                        float y = 2560 / 36;

                        //코
                        canvas.drawArc((noisePos.getX() - 2)*x, (noisePos.getY() - 1)*y, (noisePos.getX() + 2)*x, (noisePos.getY() + 1)*y, 0, 360, true, myPaint);

                        float leftTopX = (noisePos.getX() - 5)*x;
                        float leftTopY = (noisePos.getY() - 1)*y;
                        float rightBotX = (noisePos.getX() + 5)*x;
                        float rightBotY = (noisePos.getY() + 1)*y;

                        //수염
                        canvas.drawLine(leftTopX, leftTopY, leftTopX - 300, leftTopY - 100, myPaint2);
                        canvas.drawLine(leftTopX, (leftTopY + rightBotY) / 2, leftTopX - 300, (leftTopY + rightBotY) / 2, myPaint2);
                        canvas.drawLine(leftTopX, rightBotY, leftTopX - 300, rightBotY + 100, myPaint2);

                        canvas.drawLine(rightBotX, leftTopY, rightBotX + 300, leftTopY - 100, myPaint2);
                        canvas.drawLine(rightBotX, (leftTopY + rightBotY) / 2, rightBotX + 300, (leftTopY + rightBotY) / 2, myPaint2);
                        canvas.drawLine(rightBotX, rightBotY, rightBotX + 300, rightBotY + 100, myPaint2);

                        //좌표 초기화
                        noisePos = null;
                        r = null;
                    }catch (Exception e){

                    }

                }

            });

        });

    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    private void setupListeners() {
        binding.buttonCallDisconnect.setOnClickListener(view -> onCallHangUp());

        binding.buttonCallSwitchCamera.setOnClickListener(view -> onCameraSwitch());

        binding.buttonCallToggleMic.setOnClickListener(view -> {
            boolean enabled = onToggleMic();
            binding.buttonCallToggleMic.setAlpha(enabled ? 1.0f : 0.3f);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE) {
            return;
        }
        startCall();
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this);
    }

    private boolean captureToTexture() {
        return true;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(LOG_TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(LOG_TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(LOG_TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(LOG_TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    // Activity interfaces
    @Override
    public void onPause() {
        super.onPause();

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            Log.e("onPause", e.toString());
        }

        activityRunning = false;
        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        activityRunning = true;
        // Video is not paused for screencapture. See onPause.
        if (peerConnectionClient != null) {
            peerConnectionClient.startVideoSource();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle args = getIntent().getExtras();
        if (args != null) {
            String contactName = args.getString(EXTRA_ROOMID);
            binding.contactNameCall.setText(contactName);
        }
        binding.captureFormatTextCall.setVisibility(View.GONE);
        binding.captureFormatSliderCall.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        disconnect();
        if (logToast != null) {
            logToast.cancel();
        }
        activityRunning = false;
        rootEglBase.release();

//        mbitmap.recycle();
        mbitmap = null;

        super.onDestroy();

        try{
            member_socket.close();
            isRunning=false;

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    // CallFragment.OnCallEvents interface implementation.
    @Override
    public void onCallHangUp() {
        disconnect();
    }

    @Override
    public void onCameraSwitch() {
        if (peerConnectionClient != null) {
            peerConnectionClient.switchCamera();
        }
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
        if (peerConnectionClient != null) {
            peerConnectionClient.changeCaptureFormat(width, height, framerate);
        }
    }

    @Override
    public boolean onToggleMic() {
        if (peerConnectionClient != null) {
            micEnabled = !micEnabled;
            peerConnectionClient.setAudioEnabled(micEnabled);
        }
        return micEnabled;
    }

    private void updateVideoView() {
        binding.remoteVideoLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
        binding.remoteVideoView.setScalingType(SCALE_ASPECT_FILL);
        binding.remoteVideoView.setMirror(true);

        if (iceConnected) {
            binding.localVideoLayout.setPosition(
                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED);
            binding.localVideoView.setScalingType(SCALE_ASPECT_FIT);
        } else {
            binding.localVideoLayout.setPosition(
                    LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING);
            binding.localVideoView.setScalingType(SCALE_ASPECT_FILL);
        }
        binding.localVideoView.setMirror(true);

        binding.localVideoView.requestLayout();
        binding.remoteVideoView.requestLayout();
    }

    private void startCall() {
        callStartedTimeMs = System.currentTimeMillis();

        // Start room connection.
        logAndToast(getString(R.string.connecting_to, roomConnectionParameters.roomUrl));
        appRtcClient.connectToRoom(roomConnectionParameters);

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(this);
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(LOG_TAG, "Starting the audio manager...");
        audioManager.start(this::onAudioManagerDevicesChanged);
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(LOG_TAG, "Call connected: delay=" + delta + "ms");
        if (peerConnectionClient == null || isError) {
            Log.w(LOG_TAG, "Call is connected in closed or error state");
            return;
        }
        // Update video view.
        updateVideoView();
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.d(LOG_TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        activityRunning = false;
        if (appRtcClient != null) {
            appRtcClient.disconnectFromRoom();
            appRtcClient = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        binding.localVideoView.release();
        binding.remoteVideoView.release();
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        if (iceConnected && !isError) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (!activityRunning) {
            Log.e(LOG_TAG, "Critical error: " + errorMessage);
            disconnect();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getText(R.string.channel_error_title))
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton(R.string.ok,
                            (dialog, id) -> {
                                dialog.cancel();
                                disconnect();
                            })
                    .create()
                    .show();
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(LOG_TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    private void reportError(final String description) {
        runOnUiThread(() -> {
            if (!isError) {
                isError = true;
                disconnectWithErrorMessage(description);
            }
        });
    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            Logging.d(LOG_TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            Logging.d(LOG_TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            reportError("Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    private void onConnectedToRoomInternal(final SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        signalingParameters = params;
        logAndToast("Creating peer connection, delay=" + delta + "ms");
        VideoCapturer videoCapturer = null;
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }
        peerConnectionClient.createPeerConnection(rootEglBase.getEglBaseContext(), binding.localVideoView,
                remoteRenderers, videoCapturer, signalingParameters);

        if (signalingParameters.initiator) {
            logAndToast("Creating OFFER...");
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();
        } else {
            if (params.offerSdp != null) {
                peerConnectionClient.setRemoteDescription(params.offerSdp);
                logAndToast("Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
            if (params.iceCandidates != null) {
                // Add remote ICE candidates from room.
                for (IceCandidate iceCandidate : params.iceCandidates) {
                    peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                }
            }
        }
    }

    @Override
    public void onConnectedToRoom(final SignalingParameters params) {
        runOnUiThread(() -> onConnectedToRoomInternal(params));
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(() -> {
            if (peerConnectionClient == null) {
                Log.e(LOG_TAG, "Received remote SDP for non-initilized peer connection.");
                return;
            }
            logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
            peerConnectionClient.setRemoteDescription(sdp);
            if (!signalingParameters.initiator) {
                logAndToast("Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        runOnUiThread(() -> {
            if (peerConnectionClient == null) {
                Log.e(LOG_TAG, "Received ICE candidate for a non-initialized peer connection.");
                return;
            }
            peerConnectionClient.addRemoteIceCandidate(candidate);
        });
    }

    @Override
    public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {
        runOnUiThread(() -> {
            if (peerConnectionClient == null) {
                Log.e(LOG_TAG, "Received ICE candidate removals for a non-initialized peer connection.");
                return;
            }
            peerConnectionClient.removeRemoteIceCandidates(candidates);
        });
    }

    @Override
    public void onChannelClose() {
        runOnUiThread(() -> {
            logAndToast("Remote end hung up; dropping PeerConnection");
            disconnect();
        });
    }

    @Override
    public void onChannelError(final String description) {
        reportError(description);
    }

    // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(() -> {
            if (appRtcClient != null) {
                logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
                if (signalingParameters.initiator) {
                    appRtcClient.sendOfferSdp(sdp);
                } else {
                    appRtcClient.sendAnswerSdp(sdp);
                }
            }
            if (peerConnectionParameters.videoMaxBitrate > 0) {
                Log.d(LOG_TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
            }
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        runOnUiThread(() -> {
            if (appRtcClient != null) {
                appRtcClient.sendLocalIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        runOnUiThread(() -> {
            if (appRtcClient != null) {
                appRtcClient.sendLocalIceCandidateRemovals(candidates);
            }
        });
    }

    @Override
    public void onIceConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(() -> {
            logAndToast("ICE connected, delay=" + delta + "ms");
            iceConnected = true;
            callConnected();
        });
    }

    @Override
    public void onIceDisconnected() {
        runOnUiThread(() -> {
            logAndToast("ICE disconnected");
            iceConnected = false;
            disconnect();
        });
    }

    @Override
    public void onPeerConnectionClosed() {
    }

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports) {
        runOnUiThread(() -> {
        });
    }

    @Override
    public void onPeerConnectionError(final String description) {
        reportError(description);
    }
}
