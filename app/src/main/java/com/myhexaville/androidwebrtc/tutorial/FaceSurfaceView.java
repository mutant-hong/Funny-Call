package com.myhexaville.androidwebrtc.tutorial;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class FaceSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    Context mContext;
    SurfaceHolder mHolder;
    RenderingThread mRThread;
    Bitmap mBitmap;

    public FaceSurfaceView(Context context) {
        super(context);

        mContext = context;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mRThread = new FaceSurfaceView.RenderingThread();

    }

    public void addBitmap(Bitmap bitmap){
        this.mBitmap = bitmap;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mRThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            mRThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class RenderingThread extends Thread {

        public RenderingThread() {
            Log.d("RenderingThread", "RenderingThread()");
        }

        public void run() {
            Log.d("RenderingThread", "run()");
            Canvas canvas = null;
            while (true) {
                canvas = mHolder.lockCanvas();
                try {
                    synchronized (mHolder) {
                        canvas.drawBitmap(mBitmap, 0, 0, null);
                    }
                } finally {
                    if (canvas == null) return;
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    } // RenderingThread
}
