package com.myhexaville.androidwebrtc.app_rtc_sample.call;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class LocalCat extends View {

    public LocalCat(Context context) {
        super(context);
    }

    public LocalCat(Context context, AttributeSet att){
        super(context, att);
    }

    public LocalCat(Context context, AttributeSet att, int ref){
        super(context, att, ref);
    }

    float noiseX = 0;
    float noiseY = 0;

    float x = 360 / 36;
    float y = 640 / 36;

    @Override
    public void onDraw(Canvas canvas){

        try {
            Log.d("localCat", ""+noiseX + " / " + noiseY);
            if(noiseX != 0 && noiseY != 0) {
                Paint catNose = new Paint();
                catNose.setColor(Color.BLACK);
                catNose.setAntiAlias(true);

                Paint catMustache = new Paint();
                catMustache.setColor(Color.BLACK);
                catMustache.setStrokeWidth(2.5f);
                catMustache.setAntiAlias(true);

                Log.d("faceXY", noiseX + ", " + noiseY);
                canvas.drawArc((noiseX - 2) * x, (noiseY - 1) * y,
                        (noiseX + 2) * x, (noiseY + 1) * y, 0, 360, true, catNose);

                float leftTopX = (noiseX - 5) * x;
                float leftTopY = (noiseY - 1) * y;
                float rightBotX = (noiseX + 5) * x;
                float rightBotY = (noiseY + 1) * y;

                canvas.drawLine(leftTopX, leftTopY, leftTopX - (300/4), leftTopY - (100/4), catMustache);
                canvas.drawLine(leftTopX, (leftTopY + rightBotY) / 2, leftTopX - (300/4), (leftTopY + rightBotY) / 2, catMustache);
                canvas.drawLine(leftTopX, rightBotY, leftTopX - (300/4), rightBotY + (100/4), catMustache);

                canvas.drawLine(rightBotX, leftTopY, rightBotX + (300/4), leftTopY - (100/4), catMustache);
                canvas.drawLine(rightBotX, (leftTopY + rightBotY) / 2, rightBotX + (300/4), (leftTopY + rightBotY) / 2, catMustache);
                canvas.drawLine(rightBotX, rightBotY, rightBotX + (300/4), rightBotY + (100/4), catMustache);
            }
        }catch (Exception e){

        }
    }
}
