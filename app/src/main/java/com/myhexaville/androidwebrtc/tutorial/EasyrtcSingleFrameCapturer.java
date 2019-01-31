package com.myhexaville.androidwebrtc.tutorial;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Created by eric on 11/04/17.
 */

public class EasyrtcSingleFrameCapturer {

    public interface BitmapListener {
        void gotBitmap(Bitmap theBitmap);
    }

    private static boolean firstTimeOnly = true;


    // the below pixelBuffer code is based on from
    // https://github.com/CyberAgent/android-gpuimage/blob/master/library/src/jp/co/cyberagent/android/gpuimage/PixelBuffer.java
    //
    class PixelBuffer implements org.webrtc.VideoRenderer.Callbacks {
        final static String TAG = "PixelBuffer";
        final static boolean LIST_CONFIGS = false;

        int mWidth, mHeight;
        EGL10 mEGL;
        EGLDisplay mEGLDisplay;
        boolean gotFrame = false;
        String mThreadOwner;
        BitmapListener listener;
        android.app.Activity activity;


        public PixelBuffer(android.app.Activity activity, BitmapListener listener) {
            this.listener = listener;
            this.activity = activity;
        }


        private static final String VERTEX_SHADER_STRING =
                "varying vec2 interp_tc;\n"
                        + "attribute vec4 in_pos;\n"
                        + "attribute vec4 in_tc;\n"
                        + "\n"
                        + "uniform mat4 texMatrix;\n"
                        + "\n"
                        + "void main() {\n"
                        + "    gl_Position = in_pos;\n"
                        + "    interp_tc = (texMatrix * in_tc).xy;\n"
                        + "}\n";


        @Override
        public void renderFrame(final org.webrtc.VideoRenderer.I420Frame i420Frame) {
            Log.d(TAG, "entered renderFrame");
            //
            // we only want to grab a single frame but our method may get called
            // a few times before we're done.
            //
            if (gotFrame || i420Frame.width == 0 || i420Frame.height == 0) {
                Log.d(TAG, "Already got frame so taking honourable exit");
                org.webrtc.VideoRenderer.renderFrameDone(i420Frame);
                return;
            }
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    int width = i420Frame.width;
                    int height = i420Frame.height;
                    Log.d(TAG, "about to call initWithSize");
                    initWithSize(width, height);
                    Bitmap bitmap = toBitmap(i420Frame);
                    org.webrtc.VideoRenderer.renderFrameDone(i420Frame);
                    gotFrame = true;
                    listener.gotBitmap(bitmap);
                    destroy();
                }
            });


            ///////////
/*
            YuvImage yuvImage = i420ToYuvImage(i420Frame.yuvPlanes, i420Frame.yuvStrides, i420Frame.width, i420Frame.height);

            Context context = activity.getApplicationContext();
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
*/
            //////////
        }
/*
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
        */
        /////////////

        private int buildARGB(int r, int g, int b) {
            return (0xff << 24) |(r << 16) | (g << 8) | b;
        }

        private Bitmap toBitmap(org.webrtc.VideoRenderer.I420Frame frame) {

            if (!frame.yuvFrame) {

                //EglBase eglBase = EglStuff.getEglBase();
                EglBase eglBase = MediaStreamActivity.rootEglBase;
                //EglBase eglBase = EglStuff.getEglBase();
                if(firstTimeOnly) {
                    eglBase.createDummyPbufferSurface();
                    firstTimeOnly = false;
                }
                eglBase.makeCurrent();
                TextureToRGB textureToRGB = new TextureToRGB();
                int numPixels = mWidth *mHeight;
                final int bytesPerPixel = 4;
                ByteBuffer framebuffer = ByteBuffer.allocateDirect(numPixels*bytesPerPixel);

                final float frameAspectRatio = (float) frame.rotatedWidth() / (float) frame.rotatedHeight();

                final float[] rotatedSamplingMatrix =
                        RendererCommon.rotateTextureMatrix(frame.samplingMatrix, frame.rotationDegree);
                final float[] layoutMatrix = RendererCommon.getLayoutMatrix(
                        false, frameAspectRatio, (float) mWidth / mHeight);
                final float[] texMatrix = RendererCommon.multiplyMatrices(rotatedSamplingMatrix, layoutMatrix);

                textureToRGB.convert(framebuffer, mWidth, mHeight, frame.textureId, texMatrix);

                byte [] frameBytes = framebuffer.array();
                int [] dataARGB = new int[numPixels];
                for(int i = 0, j = 0; j < numPixels; i+=bytesPerPixel, j++) {
                    //
                    // data order in frameBytes is red, green, blue, alpha, red, green, ....
                    //
                    dataARGB[j] = buildARGB(frameBytes[i] & 0xff,frameBytes[i+1] &0xff,frameBytes[i+2] &0xff);
                }

                Bitmap bitmap = Bitmap.createBitmap(dataARGB, mWidth, mHeight, Bitmap.Config.ARGB_8888);
                return bitmap;
            }
            else {
                return null;
            }
        }

        private void initWithSize(final int width, final int height) {
            mWidth = width;
            mHeight = height;

            // Record thread owner of OpenGL context
            mThreadOwner = Thread.currentThread().getName();
        }


        public void destroy() {
        }


        private int getConfigAttrib(final EGLConfig config, final int attribute) {
            int[] value = new int[1];
            return mEGL.eglGetConfigAttrib(mEGLDisplay, config,
                    attribute, value) ? value[0] : 0;
        }


    }


    final private static String TAG = "frameCapturer";
    org.webrtc.VideoRenderer renderer;

    private  EasyrtcSingleFrameCapturer(final android.app.Activity activity, MediaStream mediaStream, final BitmapListener gotFrameListener) {
        if( mediaStream.videoTracks.size() == 0) {
            Log.e(TAG, "No video track to capture from");
            return;
        }

        //final VideoTrack videoTrack = mediaStream.videoTracks.getFirst();

        VideoTrack videoTrack;

        final LinkedList<VideoTrack> videoTracks = mediaStream.videoTracks;
        for(int i=0; i<videoTracks.size(); i++) {
            videoTrack = videoTracks.get(i);
            VideoTrack finalVideoTrack = videoTrack;


            final PixelBuffer vg = new PixelBuffer(activity, bitmap -> activity.runOnUiThread(() -> {
                //finalVideoTrack.removeRenderer(renderer);

                try {
                    gotFrameListener.gotBitmap(bitmap);
                } catch (Exception e1) {
                    Log.e(TAG, "Exception in gotBitmap callback:" + e1.getMessage());
                    e1.printStackTrace(System.err);
                }
            }));

            renderer = new org.webrtc.VideoRenderer(vg);
            videoTrack.addRenderer(renderer);
            /*
            Context context = activity.getApplicationContext();
            FaceDetector detector = new FaceDetector.Builder(context)
                    .setTrackingEnabled(true)
                    //.setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                    .build();


            Frame frame = new Frame.Builder().setImageData(ByteBuffer.wrap(yuvImage.getYuvData()), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getYuvFormat()).setRotation(Frame.ROTATION_270).build();

            // Detect faces
            SparseArray<Face> faces = detector.detect(frame);




            renderer = new VideoRenderer(MediaStreamActivity.binding.surfaceView3);
            videoTrack.addRenderer(renderer);
            */
        }
    }

    /**
     * This constructor builds an object which captures a frame from mediastream to a Bitmap.
     * @param mediaStream The input media mediaStream.
     * @param gotFrameListener A callback which will receive the Bitmap.
     */
    public static void toBitmap(android.app.Activity activity, MediaStream mediaStream, final BitmapListener gotFrameListener) {
        new EasyrtcSingleFrameCapturer(activity, mediaStream, gotFrameListener);
    }

    /**
     * This method captures a frame from the supplied media stream to a jpeg file written to the supplied outputStream.
     * @param mediaStream  the source media stream
     * @param quality the quality of the jpeq 0 to 100.
     * @param outputStream the output stream the jpeg file will be written to.
     * @param done a runnable that will be invoked when the outputstream has been written to.
     * @return The frame capturer. You should keep a reference to the frameCapturer until the done object is invoked.
     */
    public static void toOutputStream(android.app.Activity activity, MediaStream mediaStream, final int quality, final java.io.OutputStream outputStream, final Runnable done) {
        BitmapListener gotFrameListener = new BitmapListener() {

            @Override
            public void gotBitmap(Bitmap theBitmap) {
                theBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                try {
                    done.run();
                } catch( Exception e1) {
                    Log.e(TAG, "Exception in toOutputStream done callback:" + e1.getMessage());
                    e1.printStackTrace(System.err);
                }

            }
        };
        toBitmap(activity, mediaStream, gotFrameListener);
    }

    /**
     * This method captures a frame from the supplied mediastream to a dataurl written to a StringBuilder.
     * @param mediaStream  the source media stream
     * @param quality the quality of the jpeq 0 to 100.
     * @param output a StringBuilder which will be the recipient of the dataurl.
     * @param done a runnable that will be invoked when the dataurl is built.
     * @return The frame capturer. You should keep a reference to the frameCapturer until the done object is invoked.
     */
    public static void toDataUrl(android.app.Activity activity, MediaStream mediaStream, final int quality, final StringBuilder output, final Runnable done) {

        final java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        Runnable convertToUrl = new Runnable() {

            @Override
            public void run() {
                output.append("data:image/jpeg;base64,");
                output.append(Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT));
                try {
                    done.run();
                } catch( Exception e1) {
                    Log.e(TAG, "Exception in toDataUrl done callback:" + e1.getMessage());
                    e1.printStackTrace(System.err);
                }
            }
        };
        toOutputStream(activity, mediaStream, quality, outputStream, convertToUrl);
    }
}