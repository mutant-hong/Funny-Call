package com.myhexaville.androidwebrtc.tutorial;

import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.myhexaville.androidwebrtc.R;
import com.myhexaville.androidwebrtc.app_rtc_sample.web_rtc.EglRenderer;
import com.myhexaville.androidwebrtc.databinding.ActivitySamplePeerConnectionBinding;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.myhexaville.androidwebrtc.app_rtc_sample.web_rtc.PeerConnectionClient.VIDEO_TRACK_ID;

/*
 * Shows how to use PeerConnection to connect clients and stream video using MediaStream
 * without any networking
 * */
public class MediaStreamActivity extends AppCompatActivity {
    private static final String TAG = "SamplePeerConnectionAct";
    public static final int VIDEO_RESOLUTION_WIDTH = 1280;
    public static final int VIDEO_RESOLUTION_HEIGHT = 720;
    public static final int FPS = 1;
    private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";

    public ActivitySamplePeerConnectionBinding binding;
    public static EglBase rootEglBase;
    private VideoTrack videoTrackFromCamera;
    private PeerConnectionFactory factory;
    private PeerConnection localPeerConnection, remotePeerConnection;


    ImageView imageView, imageView2;
    Rect r, r2;
    Bitmap mbitmap, mbitmap2;
    Canvas canvas, canvas2;
    Paint myPaint;

    private Handler handler, handler2;
    private HandlerThread handlerThread, handlerThread2;

    //opencv 사용할때
/*
    private Mat matInput;
    private Mat matResult;

    public native long loadCascade(String cascadeFileName );
    public native void detect(long cascadeClassifier_face, long cascadeClassifier_eye, long matAddrInput, long matAddrResult);
    public long cascadeClassifier_face = 0;
    public long cascadeClassifier_eye = 0;

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");

        if(!OpenCVLoader.initDebug())
            Log.d(TAG, "not loaded");
        else
            Log.d(TAG,"success");
    }

    private void copyFile(String filename) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;

        AssetManager assetManager = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d( TAG, "copyFile :: 다음 경로로 파일복사 "+ pathDir);
            inputStream = assetManager.open(filename);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 "+e.toString() );
        }

    }

    private void read_cascade_file(){
        copyFile("haarcascade_frontalface_alt.xml");
        copyFile("haarcascade_eye_tree_eyeglasses.xml");

        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_face = loadCascade( "haarcascade_frontalface_alt.xml");
        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_eye = loadCascade( "haarcascade_eye_tree_eyeglasses.xml");
    }
*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_peer_connection);

        imageView = (ImageView)findViewById(R.id.image);
        imageView2 = (ImageView)findViewById(R.id.image2);

        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();

        int width = dm.widthPixels;

        int height = dm.heightPixels;

        Log.d("size", width + ", " + height);

        myPaint = new Paint();
        myPaint.setColor(Color.RED);
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setStrokeWidth(5);

        initializeSurfaceViews();

        initializePeerConnectionFactory();

        createVideoTrackFromCameraAndShowIt();

        initializePeerConnections();

        startStreamingVideo();


    }

    //안드로이드 FaceDetector 사용

//    Bitmap mBitmap;
//    FaceDetector.Face[] mFace;
//    FaceDetector mFaceDetector;
//
//    int mMaxFace = 5;
//    int mFaceCount;
//    float mEyeDistance;
//
//    public void detectFace(Bitmap bitmap) {
//        //int i = 0;
//        mBitmap = bitmap;
//        // 최대 얼굴 인식 데이터 저장소 생성
//        mFace = new FaceDetector.Face[mMaxFace];
//        // 얼굴 인식 클래스 생성
//        mFaceDetector = new FaceDetector( bitmap.getWidth(), bitmap.getHeight(), mFace.length);
//        // bitmap 에서 인식된 얼굴 갯수를 mFaceCount에 반환
//        // mFace에 인식된 얼굴 데이터를 저장
//        mFaceCount = mFaceDetector.findFaces(bitmap, mFace);
//        Log.d("facecnt", Integer.toString(mFaceCount));
//
//        if(mFaceCount == 1)
//            Toast.makeText(this, "얼굴 인식", Toast.LENGTH_SHORT).show();
//
//        Paint myPaint = new Paint();
//        myPaint.setColor(Color.RED);
//        myPaint.setStyle(Paint.Style.STROKE);
//        myPaint.setStrokeWidth(5);
//        Canvas canvas = new Canvas(bitmap);
//
//        for(int i=0; i < mFaceCount; i++) {
//            FaceDetector.Face face = mFace[i];
//            PointF myMidPoint = new PointF();
//            face.getMidPoint(myMidPoint);
//            mEyeDistance = face.eyesDistance();
//            canvas.drawRect(
//                    (int)(myMidPoint.x - mEyeDistance),
//                    (int)(myMidPoint.y - mEyeDistance),
//                    (int)(myMidPoint.x + mEyeDistance),
//                    (int)(myMidPoint.y + mEyeDistance),
//                    myPaint);
//        }
//    }

    //opencv로 얼굴 인식
//    public void detectFace(Bitmap bitmap) {
//
//        matInput = new Mat (bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8U, new Scalar(4));
//
//        Utils.bitmapToMat(bitmap, matInput);
//
//        if ( matResult == null )
//            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
//
//        //Core.flip(matInput, matInput, 1);
//
//        detect(cascadeClassifier_face,cascadeClassifier_eye, matInput.getNativeObjAddr(),
//                matResult.getNativeObjAddr());
//
//        Utils.matToBitmap(matResult, bitmap);
//
//        imageView.setImageBitmap(bitmap);
//    }


    //ml kit 사용
    public void detectFace(FirebaseVisionImage image) {

        Log.d("detectFace", "얼굴 인식 메소드 진입");
        // [START set_detector_options]

        //얼굴 인식 옵션
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .enableTracking()
                        //.setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
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

                        //Toast.makeText(MediaStreamActivity.this, "b : " + bounds.bottom + ", t : " + bounds.top + ", l : " + bounds.left + ", r : " + bounds.right, Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });
    }

    //remote view에서 인식
    public void detectFace2(FirebaseVisionImage image) {

        Log.d("detectFace", "얼굴 인식 메소드 진입");
        // [START set_detector_options]

        //얼굴 인식 옵션
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .enableTracking()
                        //.setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
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
                            r2 = bounds;

                            //Toast.makeText(MediaStreamActivity.this, "b : " + bounds.bottom + ", t : " + bounds.top + ", l : " + bounds.left + ", r : " + bounds.right, Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    //bitmap 말고 bytebuffer 이용
    private void imageFromBuffer(ByteBuffer buffer, int rotation) {
        // [START set_metadata]
        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setWidth(480)   // 480x360 is typically sufficient for
                .setHeight(360)  // image recognition
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setRotation(rotation)
                .build();
        // [END set_metadata]
        // [START image_from_buffer]
        FirebaseVisionImage image = FirebaseVisionImage.fromByteBuffer(buffer, metadata);

        //detectFace(image);
        // [END image_from_buffer]
    }

    //카메라 id값 얻기
    private String getFrontFacingCameraId(CameraManager cManager) {
        try {
            String cameraId;
            int cameraOrientation;
            CameraCharacteristics characteristics;
            for (int i = 0; i < cManager.getCameraIdList().length; i++) {
                cameraId = cManager.getCameraIdList()[i];
                characteristics = cManager.getCameraCharacteristics(cameraId);
                cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId;
                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        Log.d("onResume", "onResume");
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        handlerThread2 = new HandlerThread("inference2");
        handlerThread2.start();
        handler2 = new Handler(handlerThread2.getLooper());
    }

    protected synchronized void runInBackground(final Runnable r) {
        Log.d("runInBackground", "runInBackground");
        if (handler != null) {
            handler.post(r);
        }
    }

    protected synchronized void runInBackground2(final Runnable r) {
        Log.d("runInBackground2", "runInBackground2");
        if (handler2 != null) {
            handler2.post(r);
        }
    }


    private void initializeSurfaceViews() {
        rootEglBase = EglBase.create();
        binding.surfaceView.init(rootEglBase.getEglBaseContext(), null);
        binding.surfaceView.setEnableHardwareScaler(true);
        binding.surfaceView.setMirror(true);

        binding.surfaceView2.init(rootEglBase.getEglBaseContext(), null);
        binding.surfaceView2.setEnableHardwareScaler(true);
        binding.surfaceView2.setMirror(true);



        //bytebuffer 사용 시, 카메라 rotation 계산

//        VisionImage visionImage = new VisionImage();
//        int rotation = -1;
//        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
//        try {
//            rotation = visionImage.getRotationCompensation(getFrontFacingCameraId(manager), this, getApplicationContext());
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//
//
//        int finalRotation = rotation;

        //surfaceView에서 프레임 추가될 때마다, bitmap or bytebuffer 가져오기
        binding.surfaceView.addFrameListener(new EglRenderer.FrameListener() {
            @Override
            public void onFrame(Bitmap bitmap) {
                Log.d("addFrameListener", "프레임 추가");

                //bytebuffer 사용 시
                //imageFromBuffer(byteBuffer, finalRotation);

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


                    MediaStreamActivity.this.runOnUiThread(new Runnable() {

                        public void run() {

                            mbitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);

                            canvas = new Canvas(mbitmap);
                            canvas.drawColor(Color.TRANSPARENT);
                            //imageView.setImageBitmap(dstBitmap);
                            imageView.setImageBitmap(mbitmap);
                            Log.d("runOnUiThread", "runOnUiThread -> setImageBitmap");

                            try {
                                Log.d("faceRect", "l : "+ r.left + ", t : " + r.top + ", r : " + r.right + ", b : " + r.bottom);

                                float x = 1280 / 72;
                                float y = 720/ 72;
                                canvas.drawRect(r.left * x, r.top * y, r.right * x, r.bottom * y, myPaint);
                                //사각형 초기화
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
        },0.1f);

        /*
        binding.surfaceView2.addFrameListener(new EglRenderer.FrameListener() {
            @Override
            public void onFrame(Bitmap bitmap) {
                Log.d("addFrameListener", "프레임 추가");

                Bitmap dstBitmap;
                if (bitmap.getWidth() >= bitmap.getHeight()) {
                    dstBitmap = Bitmap.createBitmap(bitmap, bitmap.getWidth()/2 - bitmap.getHeight()/2, 0,
                            bitmap.getHeight(), bitmap.getHeight()
                    );

                    Log.d("resize2", dstBitmap.getWidth() + ", " + dstBitmap.getHeight());

                } else {
                    dstBitmap = Bitmap.createBitmap(bitmap, 0, bitmap.getHeight()/2 - bitmap.getWidth()/2,
                            bitmap.getWidth(), bitmap.getWidth()
                    );
                    Log.d("resize2", dstBitmap.getWidth() + ", " + dstBitmap.getHeight());
                }

                runInBackground2(() -> {

                    Log.d("runInBackground", "addFrameListener -> runInBackground");
                    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(dstBitmap);
                    detectFace2(image);

                    MediaStreamActivity.this.runOnUiThread(new Runnable() {

                        public void run() {

                            mbitmap2 = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);

                            canvas2 = new Canvas(mbitmap2);
                            canvas2.drawColor(Color.TRANSPARENT);

                            imageView2.setImageBitmap(mbitmap2);
                            Log.d("runOnUiThread", "runOnUiThread -> setImageBitmap");

                            try {
                                Log.d("faceRect", "l : "+ r2.left + ", t : " + r2.top + ", r : " + r2.right + ", b : " + r2.bottom);

                                float x = 1280 / 72;
                                float y = 720/ 72;
                                canvas2.drawRect(r2.left * x, r2.top * y, r2.right * x, r2.bottom * y, myPaint);
                                //사각형 초기화
                                r2 = null;

                            }catch (Exception e){

                            }

                        }

                    });

                });

            }
        },0.1f);
*/
    }

    private void initializePeerConnectionFactory() {
        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
        factory = new PeerConnectionFactory(null);
        factory.setVideoHwAccelerationOptions(rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
    }

    private void createVideoTrackFromCameraAndShowIt() {
        VideoCapturer videoCapturer = createVideoCapturer();
        VideoSource videoSource = factory.createVideoSource(videoCapturer);
        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);

        videoTrackFromCamera = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrackFromCamera.setEnabled(true);
        videoTrackFromCamera.addRenderer(new VideoRenderer(binding.surfaceView));
    }

    private void initializePeerConnections() {
        localPeerConnection = createPeerConnection(factory, true);
        remotePeerConnection = createPeerConnection(factory, false);
    }

    private void startStreamingVideo() {
        MediaStream mediaStream = factory.createLocalMediaStream("ARDAMS");
        mediaStream.addTrack(videoTrackFromCamera);

        localPeerConnection.addStream(mediaStream);

        MediaConstraints sdpMediaConstraints = new MediaConstraints();

        localPeerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "onCreateSuccess: ");
                localPeerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                remotePeerConnection.setRemoteDescription(new SimpleSdpObserver(), sessionDescription);

                remotePeerConnection.createAnswer(new SimpleSdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        localPeerConnection.setRemoteDescription(new SimpleSdpObserver(), sessionDescription);
                        remotePeerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                    }
                }, sdpMediaConstraints);
            }
        }, sdpMediaConstraints);
    }

    private PeerConnection createPeerConnection(PeerConnectionFactory factory, boolean isLocal) {
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(new ArrayList<>());
        MediaConstraints pcConstraints = new MediaConstraints();

        PeerConnection.Observer pcObserver = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "onSignalingChange: ");
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: ");
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(TAG, "onIceConnectionReceivingChange: ");
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "onIceGatheringChange: ");
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "onIceCandidate: " + isLocal);
                if (isLocal) {
                    remotePeerConnection.addIceCandidate(iceCandidate);
                } else {
                    localPeerConnection.addIceCandidate(iceCandidate);
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(TAG, "onIceCandidatesRemoved: ");
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "onAddStream: " + mediaStream.videoTracks.size());
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                remoteVideoTrack.setEnabled(true);
                remoteVideoTrack.addRenderer(new VideoRenderer(binding.surfaceView2));

            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "onRemoveStream: ");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(TAG, "onDataChannel: ");
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded: ");
            }
        };

        return factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver);
    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;

        if (useCamera2()) {
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));

        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
        }

        Log.d("VideoCapturer", videoCapturer.toString());
        return videoCapturer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    /*
     * Read more about Camera2 here
     * https://developer.android.com/reference/android/hardware/camera2/package-summary.html
     * */

    private boolean useCamera2() {

        return Camera2Enumerator.isSupported(this);
    }


    class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private int data = 0;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {
            data = params[0];
            return null;// decodeSampledBitmapFromResource(getResources(), data, 100, 100));
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
}