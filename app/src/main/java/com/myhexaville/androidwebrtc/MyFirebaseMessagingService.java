package com.myhexaville.androidwebrtc;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;
import com.myhexaville.androidwebrtc.app_rtc_sample.call.CallActivity;
import com.myhexaville.androidwebrtc.app_rtc_sample.main.AppRTCMainActivity;

import java.text.SimpleDateFormat;


public class MyFirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    private static final String TAG = "FirebaseMsgService";

    CallDataDB callDataDB;
    SQLiteDatabase database;

    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {



        // 이거 추가 하면
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE );
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wakeLock = pm.newWakeLock( PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG" );
        wakeLock.acquire(3000);

        //추가한것
        //sendNotification(remoteMessage.getData().get("message"));

        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        if(title.equals("친구요청")){
            sendNotificationAddFriends(title, body);
        }
        else{
            if(body.equals("수신거부")){
                refuseCall(title);
            }
            else if(body.equals("발신취소")){
                cancleCall(title);
            }
            else {
                //sendNotification(title, body);
                sendCallingActivity(title, body);
            }
        }

    }

    //전화온 화면으로 이동
    private void sendCallingActivity(String title, String messageBody){
        Intent intent = new Intent(this, CallingActivity.class);
        intent.putExtra("friendId", title);
        intent.putExtra("roomId", messageBody);
        intent.putExtra("caller", "false");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    //수신 거부
    private void refuseCall(String friendId){
//        Intent intent = new Intent(this, AppRTCMainActivity.class);
//        intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);
        CallActivity.Call.finish();
//
//        SimpleDateFormat dataformat = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분");
//        String date = dataformat.format(System.currentTimeMillis());
//
//        callDataDB = new CallDataDB(this, CallDataDB.tableName, null, 1);
//        database = callDataDB.getWritableDatabase();
//
//        callDataDB.insertData(database, friendId + "수신거부", date, 0, "true");
    }

    //발신취소
    private void cancleCall(String friendId){
        CallingActivity.Calling.finish();

        SimpleDateFormat dataformat = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분");
        String date = dataformat.format(System.currentTimeMillis());

        callDataDB = new CallDataDB(this, CallDataDB.tableName, null, 1);
        database = callDataDB.getWritableDatabase();

        callDataDB.insertData(database, friendId, date, 0, "false", "true");
    }

    //영상 통화 알림
    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, AppRTCMainActivity.class);
        intent.putExtra("friendId", title);
        intent.putExtra("caller", "false");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent);

        //notificationBuilder.addAction(R.drawable.disconnect, "disconnect", null);
        //notificationBuilder.addAction(R.drawable.call, "call", pendingIntent);



        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = notificationBuilder.build();
        notification.flags |= Notification.FLAG_INSISTENT;
        //notification.flags |= Notification.FLAG_ONGOING_EVENT;

        notificationManager.notify(0 /* ID of notification */, notification);
    }

    //친구 요청 알림
    private void sendNotificationAddFriends(String title, String messageBody) {
        Intent intent = new Intent(this, FriendsActivity.class);
        intent.putExtra("친구요청", messageBody);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                //.setSound(defaultSoundUri)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = notificationBuilder.build();
        //notification.flags |= Notification.FLAG_INSISTENT;

        notificationManager.notify(0 /* ID of notification */, notification);
    }

}