package com.myhexaville.androidwebrtc.tutorial;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.myhexaville.androidwebrtc.R;
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

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static android.graphics.ImageFormat.NV21;
import static com.myhexaville.androidwebrtc.app_rtc_sample.web_rtc.PeerConnectionClient.VIDEO_TRACK_ID;

/*
* Shows how to use PeerConnection to connect clients and stream video using MediaStream
* without any networking
* */
public class MediaStreamActivity extends AppCompatActivity implements VideoRenderer.Callbacks {
    private static final String TAG = "SamplePeerConnectionAct";
    public static final int VIDEO_RESOLUTION_WIDTH = 1280;
    public static final int VIDEO_RESOLUTION_HEIGHT = 720;
    public static final int FPS = 30;
    private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";

    public static ActivitySamplePeerConnectionBinding binding;
    public static EglBase rootEglBase;
    private VideoTrack videoTrackFromCamera;
    private PeerConnectionFactory factory;
    private PeerConnection localPeerConnection, remotePeerConnection;

    ///
    EasyrtcSingleFrameCapturer.BitmapListener gotFrameListener;
    //EasyrtcSingleFrameCapturer.
    ImageView imageView;
    ///

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_peer_connection);

        imageView = (ImageView)findViewById(R.id.image);

        gotFrameListener = theBitmap -> {
            Log.e(TAG, "got bitmap!");

            /*
            ImageView imageView = findViewById(R.id.object_preview);
            imageView.setImageBitmap(theBitmap);

            imageView.setVisibility(View.VISIBLE);
            */

            imageView.setImageBitmap(theBitmap);

            /*
            FaceDetector detector = new FaceDetector.Builder(getApplicationContext())
                    .setTrackingEnabled(false)
                    .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .build();

            Detector<Face> safeDetector = new SafeFaceDetector(detector);

            Frame frame = new Frame.Builder().setBitmap(theBitmap).build();
            SparseArray<Face> faces = safeDetector.detect(frame);
*/
            //FaceView overlay = (FaceView) findViewById(R.id.faceView);
            //overlay.setContent(theBitmap, faces);

            //safeDetector.release();


            ////////////////////////////////

/*
            int mWidth = theBitmap.getWidth();
            int mHeight = theBitmap.getHeight();

            int[] mIntArray = new int[mWidth * mHeight];

            // Copy pixel data from the Bitmap into the 'intArray' array
            theBitmap.getPixels(mIntArray, 0, mWidth, 0, 0, mWidth, mHeight);

            byte [] yuv = new byte[mWidth*mHeight*3/2];

            // Call to encoding function : convert intArray to Yuv Binary data
            //encodeYUV420SP(yuv, mIntArray, mWidth, mHeight);
*/
        };

        Log.d("ddddd","test");

        initializeSurfaceViews();

        initializePeerConnectionFactory();

        createVideoTrackFromCameraAndShowIt();

        initializePeerConnections();

        startStreamingVideo();
    }

    public void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
                U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
                V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
                }

                index ++;
            }
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

        //binding.surfaceView3.init(rootEglBase.getEglBaseContext(), null);
        //binding.surfaceView3.setEnableHardwareScaler(true);
        //binding.surfaceView3.setMirror(true);
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

        //VideoRenderer v = new VideoRenderer(binding.surfaceView);

        //SurfaceViewRenderer surfaceViewRenderer = new SurfaceViewRenderer(getApplicationContext());


        videoTrackFromCamera = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrackFromCamera.setEnabled(true);
        videoTrackFromCamera.addRenderer(new VideoRenderer(binding.surfaceView));

        boolean enable = videoTrackFromCamera.enabled();
        String id = videoTrackFromCamera.id();
        String kind = videoTrackFromCamera.kind();
        String state = videoTrackFromCamera.state().toString();

        Log.d("enabled()",Boolean.toString(videoTrackFromCamera.enabled()));
        Log.d("id()",videoTrackFromCamera.id());
        Log.d("kind()",videoTrackFromCamera.kind());
        Log.d("state()", videoTrackFromCamera.state().toString());

        //VideoRenderer.Callbacks
        //VideoRenderer v = new VideoRenderer();

        //VideoTrack videoTrack =
        //renderFrame(new VideoRenderer.I420Frame());

        //YuvFrame yuvFrame = new YuvFrame(VideoRenderer.I420Frame, PROCESSING_NONE, 0);

        //VideoRenderer videoRenderer;

        //VideoFileRenderer
/*
        VideoRenderer.I420Frame i420Frame;


        int width = VIDEO_RESOLUTION_WIDTH;
        int height = VIDEO_RESOLUTION_HEIGHT;
        int stride_y = 16 + ((width-1)/16)*16;
        int stride_uv = 16 + ((stride_y/2-1)/16)*16;
        ByteBuffer[] byteBuffers = {};

        byteBuffers[0] = ByteBuffer.allocate(3);
        byteBuffers[1] = ByteBuffer.allocate(3);
        byteBuffers[2] = ByteBuffer.allocate(3);
*/



        //renderFrame(new VideoRenderer.I420Frame(0,0,0,));

        //renderFrame(new VideoRenderer.I420Frame(width, height, 0, new int[]{stride_y, stride_uv, stride_uv}, byteBuffers, 0));
    }

    private void initializePeerConnections() {
        localPeerConnection = createPeerConnection(factory, true);
        remotePeerConnection = createPeerConnection(factory, false);
    }

    private void startStreamingVideo() {
        MediaStream mediaStream = factory.createLocalMediaStream("ARDAMS");
        //new MediaStream(nativeCreateLocalMediaStream(this.nativeFactory, label));
        //this.nativeFactory = nativeCreatePeerConnectionFactory(options);

        /*


    public MediaStream(long nativeStream) {
        this.nativeStream = nativeStream;
    }
         */

        //mediaStream.videoTracks.

        mediaStream.addTrack(videoTrackFromCamera);

        //EasyrtcSingleFrameCapturer.BitmapListener gotFrameListener = new EasyrtcSingleFrameCapturer.BitmapListener() {

/*
        MediaStream stream = contextManager.getStream();
        EasyrtcSingleFrameCapturer.toBitmap(this, stream, gotFrameListener);
*/

        int i = mediaStream.videoTracks.size();


        EasyrtcSingleFrameCapturer.toBitmap(this, mediaStream, gotFrameListener);




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
        //return false;
    }

    ////////////


    ////////////
    @Override
    public void renderFrame(final VideoRenderer.I420Frame i420Frame) {
        YuvImage yuvImage = i420ToYuvImage(i420Frame.yuvPlanes, i420Frame.yuvStrides, i420Frame.width, i420Frame.height);

        // Set image data (YUV N21 format) -- NOT working. The commented bitmap line works.
        //Frame frame = new Frame.Builder().setImageData(ByteBuffer.wrap(yuvImage.getYuvData()), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getYuvFormat()).build();
        //Frame frame = new Frame.Builder().setBitmap(yuvImage).build();
        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(true)
                //.setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();


        Frame frame = new Frame.Builder().setImageData(ByteBuffer.wrap(yuvImage.getYuvData()), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getYuvFormat()).setRotation(Frame.ROTATION_270).build();

        // Detect faces
        SparseArray<Face> faces = detector.detect(frame);

        if (!detector.isOperational()) {
            Log.e(TAG, "Detector is not operational!");
        }

        if (faces.size() > 0) {
            Log.i("yuv", "Smiling %: " + faces.valueAt(0).getIsSmilingProbability());
        }

        detector.release();
        Log.i("yuv", "Faces detected: " + faces.size());
    }

    private YuvImage i420ToYuvImage(ByteBuffer[] yuvPlanes, int[] yuvStrides, int width, int height) {
        if (yuvStrides[0] != width) {
            return fastI420ToYuvImage(yuvPlanes, yuvStrides, width, height);
        }
        if (yuvStrides[1] != width / 2) {
            return fastI420ToYuvImage(yuvPlanes, yuvStrides, width, height);
        }
        if (yuvStrides[2] != width / 2) {
            return fastI420ToYuvImage(yuvPlanes, yuvStrides, width, height);
        }

        byte[] bytes = new byte[yuvStrides[0] * height +
                yuvStrides[1] * height / 2 +
                yuvStrides[2] * height / 2];
        ByteBuffer tmp = ByteBuffer.wrap(bytes, 0, width * height);
        copyPlane(yuvPlanes[0], tmp);

        byte[] tmpBytes = new byte[width / 2 * height / 2];
        tmp = ByteBuffer.wrap(tmpBytes, 0, width / 2 * height / 2);

        copyPlane(yuvPlanes[2], tmp);
        for (int row = 0 ; row < height / 2 ; row++) {
            for (int col = 0 ; col < width / 2 ; col++) {
                bytes[width * height + row * width + col * 2]
                        = tmpBytes[row * width / 2 + col];
            }
        }
        copyPlane(yuvPlanes[1], tmp);
        for (int row = 0 ; row < height / 2 ; row++) {
            for (int col = 0 ; col < width / 2 ; col++) {
                bytes[width * height + row * width + col * 2 + 1] =
                        tmpBytes[row * width / 2 + col];
            }
        }
        return new YuvImage(bytes, NV21, width, height, null);
    }

    private YuvImage fastI420ToYuvImage(ByteBuffer[] yuvPlanes,
                                        int[] yuvStrides,
                                        int width,
                                        int height) {
        byte[] bytes = new byte[width * height * 3 / 2];
        int i = 0;
        for (int row = 0 ; row < height ; row++) {
            for (int col = 0 ; col < width ; col++) {
                bytes[i++] = yuvPlanes[0].get(col + row * yuvStrides[0]);
            }
        }
        for (int row = 0 ; row < height / 2 ; row++) {
            for (int col = 0 ; col < width / 2; col++) {
                bytes[i++] = yuvPlanes[2].get(col + row * yuvStrides[2]);
                bytes[i++] = yuvPlanes[1].get(col + row * yuvStrides[1]);
            }
        }
        return new YuvImage(bytes, NV21, width, height, null);
    }

    private void copyPlane(ByteBuffer src, ByteBuffer dst) {
        src.position(0).limit(src.capacity());
        dst.put(src);
        dst.position(0).limit(dst.capacity());
    }


    // Copy the bytes out of |src| and into |dst|, ignoring and overwriting
// positon & limit in both buffers.
//** copied from org/webrtc/VideoRenderer.java **//

    /*
    private static void copyPlane(ByteBuffer src, ByteBuffer dst) {
        src.position(0).limit(src.capacity());
        dst.put(src);
        dst.position(0).limit(dst.capacity());
    }

    public static android.graphics.YuvImage ConvertTo(org.webrtc.VideoRenderer.I420Frame src, int imageFormat) {
        switch (imageFormat) {
            default:
                return null;

            case android.graphics.ImageFormat.YV12: {
                byte[] bytes = new byte[src.yuvStrides[0]*src.height +
                        src.yuvStrides[1]*src.height/2 +
                        src.yuvStrides[2]*src.height/2];
                ByteBuffer tmp = ByteBuffer.wrap(bytes, 0, src.yuvStrides[0]*src.height);
                copyPlane(src.yuvPlanes[0], tmp);
                tmp = ByteBuffer.wrap(bytes, src.yuvStrides[0]*src.height, src.yuvStrides[2]*src.height/2);
                copyPlane(src.yuvPlanes[2], tmp);
                tmp = ByteBuffer.wrap(bytes, src.yuvStrides[0]*src.height+src.yuvStrides[2]*src.height/2, src.yuvStrides[1]*src.height/2);
                copyPlane(src.yuvPlanes[1], tmp);
                int[] strides = src.yuvStrides.clone();
                return new YuvImage(bytes, imageFormat, src.width, src.height, strides);
            }

            case NV21: {
                if (src.yuvStrides[0] != src.width)
                    return convertLineByLine(src);
                if (src.yuvStrides[1] != src.width/2)
                    return convertLineByLine(src);
                if (src.yuvStrides[2] != src.width/2)
                    return convertLineByLine(src);

                byte[] bytes = new byte[src.yuvStrides[0]*src.height +
                        src.yuvStrides[1]*src.height/2 +
                        src.yuvStrides[2]*src.height/2];
                ByteBuffer tmp = ByteBuffer.wrap(bytes, 0, src.width*src.height);
                copyPlane(src.yuvPlanes[0], tmp);

                byte[] tmparray = new byte[src.width/2*src.height/2];
                tmp = ByteBuffer.wrap(tmparray, 0, src.width/2*src.height/2);

                copyPlane(src.yuvPlanes[2], tmp);
                for (int row=0; row<src.height/2; row++) {
                    for (int col=0; col<src.width/2; col++) {
                        bytes[src.width*src.height + row*src.width + col*2] = tmparray[row*src.width/2 + col];
                    }
                }
                copyPlane(src.yuvPlanes[1], tmp);
                for (int row=0; row<src.height/2; row++) {
                    for (int col=0; col<src.width/2; col++) {
                        bytes[src.width*src.height + row*src.width + col*2+1] = tmparray[row*src.width/2 + col];
                    }
                }
                return new YuvImage(bytes, imageFormat, src.width, src.height, null);
            }
        }
    }

    public static android.graphics.YuvImage convertLineByLine(org.webrtc.VideoRenderer.I420Frame src) {
        byte[] bytes = new byte[src.width*src.height*3/2];
        int i=0;
        for (int row=0; row<src.height; row++) {
            for (int col=0; col<src.width; col++) {
                bytes[i++] = src.yuvPlanes[0][col+row*src.yuvStrides[0]];

            }
        }
        for (int row=0; row<src.height/2; row++) {
            for (int col=0; col<src.width/2; col++) {
                bytes[i++] = src.yuvPlanes[2][col+row*src.yuvStrides[2]];
                bytes[i++] = src.yuvPlanes[1][col+row*src.yuvStrides[1]];
            }
        }
        return new YuvImage(bytes, NV21, src.width, src.height, null);

    }
}
*/


}