package com.myhexaville.androidwebrtc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.smp.soundtouchandroid.SoundStreamAduioRecorder;
import com.smp.soundtouchandroid.SoundTouch;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RecordTestActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mSampleRate = 44100;
    private int mChannelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannelCount, mAudioFormat);

    public AudioRecord mAudioRecord = null;

    public Thread mRecordThread = null;
    public boolean isRecording = false;

    public AudioTrack mAudioTrack = null;
    public Thread mPlayThread = null;
    public boolean isPlaying = false;

    public Button mBtRecord = null;
    public Button mBtPlay = null;

    public String mFilePath = null;

    //////
    private SoundStreamAduioRecorder soundTouchRec;
    private SoundTouch soundTouch;

    //

    boolean isConnect = false;

    // 어플 종료시 스레드 중지를 위해...
    boolean isRunning=false;
    // 서버와 연결되어있는 소켓 객체
    Socket member_socket;
    // 사용자 닉네임( 내 닉넴과 일치하면 내가보낸 말풍선으로 설정 아니면 반대설정)
    String user_nickname;

    void doPermAudio()
    {
        int MY_PERMISSIONS_RECORD_AUDIO = 1;

        RecordTestActivity thisActivity = this;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(thisActivity,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_RECORD_AUDIO);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(thisActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    2);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    doPermAudio();
                }
            }
        });

        mBtRecord = (Button)findViewById(R.id.bt_record);
        mBtPlay = (Button)findViewById(R.id.bt_play);

        //mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelCount, mAudioFormat, mBufferSize);
        //mAudioRecord.startRecording();

        //mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, mChannelCount, mAudioFormat, mBufferSize, AudioTrack.MODE_STREAM);

//        soundTouch = new SoundTouch(0, 2, 1, 2, 1, 1);
//        soundTouchRec = new SoundStreamAduioRecorder(this, soundTouch);
//
//        if (isConnect == false) {   //접속전
//            ConnectionThread thread = new ConnectionThread();
//            thread.start();
//        }

    }

    public void onRecord(View view) {
        if(isRecording == true) {
            isRecording = false;
            mBtRecord.setText("Record");
        }
        else {
            isRecording = true;
            mBtRecord.setText("Stop");

            if(mAudioRecord == null) {
                mAudioRecord =  new AudioRecord(mAudioSource, mSampleRate, mChannelCount, mAudioFormat, mBufferSize);
                mAudioRecord.startRecording();
            }
            if(mAudioTrack == null) {
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, mChannelCount, mAudioFormat, mBufferSize, AudioTrack.MODE_STREAM);
            }
            //mRecordThread.start();

            RecordThread recordThread = new RecordThread();
            recordThread.start();


        }

    }

    public void onPlay(View view) {
        if(isPlaying == true) {
            isPlaying = false;
            mBtPlay.setText("Play");
            //soundTouchRec.stopPlay();
        }
        else {
            isPlaying = true;
            mBtPlay.setText("Stop");

            if(mAudioTrack == null) {
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, mChannelCount, mAudioFormat, mBufferSize, AudioTrack.MODE_STREAM);
            }
            //mPlayThread.start();
            PlayThread playThread = new PlayThread();
            playThread.start();

            //soundTouchRec.startPlay(mFilePath);
        }

    }

    class RecordThread extends Thread{

//        Socket socket;
//        DataOutputStream dos;
//
//        public RecordThread(Socket socket){
//            try{
//                this.socket=socket;
//                OutputStream os=socket.getOutputStream();
//                dos=new DataOutputStream(os);
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }

        @Override
        public void run() {
            int cnt = 0;
            byte[] readData = new byte[mBufferSize];
            Log.d("readData Size1", readData.length + "");
//            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() +"/record.pcm";
//            Log.d("filepath", mFilePath);
//            FileOutputStream fos = null;
//            try {
//                fos = new FileOutputStream(mFilePath);
//            } catch(FileNotFoundException e) {
//                e.printStackTrace();
//            }
            Log.d(TAG, "mBufferSize is " + mBufferSize);


//            try {
//                dos.writeInt(mBufferSize);
//
//            } catch(Exception e) {
//                e.printStackTrace();
//            }

            //mAudioTrack.play();
            while(isRecording) {
                int ret = mAudioRecord.read(readData, 0, mBufferSize);
                Log.d(TAG, "read bytes is " + ret);
                Log.d("readData Size2", readData.length + "");


//                try {
//                    //fos.write(readData, 0, mBufferSize);
//
//                    //dos.write(readData);
//                    dos.write(readData, 0, mBufferSize);
//
//                    //dos.writeInt(ret);
//                    //dos.writeUTF("녹음 전송"+cnt);
//                    cnt++;
//                }catch (IOException e){
//                    e.printStackTrace();
//                }


                //mAudioTrack.write(readData, 0, mBufferSize);
                Log.d("readData Size3", readData.length + "");
//                SendToServerThread sendToServerThread = new SendToServerThread(member_socket, readData, ret);
//                sendToServerThread.start();

            }

            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;

//            mAudioTrack.stop();
////            mAudioTrack.release();
////            mAudioTrack = null;

//            try {
//                //dos.close();
////                fos.close();
////                File f1 = new File(mFilePath); // The location of your PCM file
////                File f2 = new File("/sdcard/Download/record.wav"); // The location where you want your WAV file
////                rawToWave(f1, f2);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    class PlayThread extends Thread{
        @Override
        public void run() {
            byte[] writeData = new byte[mBufferSize];
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(mFilePath);
            }catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            DataInputStream dis = new DataInputStream(fis);
            mAudioTrack.play();

            while(isPlaying) {
                try {
                    int ret = dis.read(writeData, 0, mBufferSize);
                    if (ret <= 0) {
                        (RecordTestActivity.this).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                isPlaying = false;
                                mBtPlay.setText("Play");
                            }
                        });

                        break;
                    }
                    mAudioTrack.write(writeData, 0, ret);
                }catch (IOException e) {
                    e.printStackTrace();
                }

            }
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;

            try {
                dis.close();
                fis.close();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ConnectionThread extends Thread {

        @Override
        public void run() {
            try {
                Log.d("ConnectionThread", "ConnectionThread");
                // 접속한다.
                final Socket socket = new Socket("192.168.0.231", 50001);
                member_socket=socket;
                // 미리 입력했던 닉네임을 서버로 전달한다.
                String nickName = "sam";
                user_nickname=nickName;     // 화자에 따라 말풍선을 바꿔주기위해

//                byte[] readData = new byte[mBufferSize];
//                int ret = mAudioRecord.read(readData, 0, mBufferSize);

                // 스트림을 추출
                OutputStream os = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(os);
                // 닉네임을 송신한다.
                dos.writeUTF(nickName);
                //dos.write(ret);

                // ProgressDialog 를 제거한다.
                // 접속 상태를 true로 셋팅한다.
                isConnect=true;
                // 메세지 수신을 위한 스레드 가동
                isRunning=true;
                MessageThread thread=new MessageThread(socket);
                thread.start();
                if(mAudioTrack == null) {
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, mChannelCount, mAudioFormat, mBufferSize, AudioTrack.MODE_STREAM);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //수신 스레드
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
            Log.d("MessageThread","start");
            try{


                mAudioTrack.play();

                Log.d("MessageThread","" + isRunning);
                while (isRunning){
                    // 서버로부터 데이터를 수신받는다.
//                    final String msg=dis.readUTF();
//                    // 화면에 출력
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            // 메세지의 시작 이름이 내 닉네임과 일치한다면
//                            if(msg.startsWith(user_nickname)){
//                                Log.d("my msg", msg);
//
//                            }
//                            else{
//                                Log.d("friend msg", msg);
//                                //String[] split = msg.split(" : ");
//                            }
//
//                            mAudioTrack.play();
//
//                        }
//                    });
                    try {
                        int size = dis.readInt();
                        byte[] writeData = new byte[size];
                        //int ret = dis.read(writeData, 0, mBufferSize);
                        int ret = dis.read(writeData, 0, size);

                        mAudioTrack.write(writeData, 0, ret);
                        Log.d("read Record", ret + "");
                    }catch (IOException e) {
                        e.printStackTrace();
                    }

//                    mAudioTrack.stop();
//                    mAudioTrack.release();
//                    mAudioTrack = null;

//                    try {
//                        dis.close();
//
//                    }catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    // 서버에 데이터를 전달하는 스레드
    class SendToServerThread extends Thread{
        Socket socket;
        byte[] readData;
        int ret;
        DataOutputStream dos;

        public SendToServerThread(Socket socket, byte[] readData, int ret){
            try{
                this.socket=socket;
                this.readData = readData.clone();
                this.ret = ret;
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
                //dos.writeUTF(msg);
                Log.d(TAG, "send bytes is " + ret);
                dos.write(readData, 0, ret);

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    class FileSender extends Thread {
        Socket socket;
        DataOutputStream dos;
        FileInputStream fis;
        BufferedInputStream bis;
        String filename;
        int control = 0;
        public FileSender(Socket socket,String filestr) {
            this.socket = socket;
            this.filename = filestr;
            try {
                // 데이터 전송용 스트림 생성
                dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {

                String fName = filename;

                // 파일 내용을 읽으면서 전송
                File f = new File(fName);
                fis = new FileInputStream(f);
                bis = new BufferedInputStream(fis);

                int len;
                int size = 4096;
                byte[] data = new byte[size];
                while ((len = bis.read(data)) != -1) {
                    control++;
                    if(control % 10000 == 0)
                    {
                        System.out.println("전송중..." + control/10000);
                    }
                    dos.write(data, 0, len);
                }

                dos.flush();
                dos.close();
                bis.close();
                fis.close();
                System.out.println("완료");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, 44100); // sample rate
            writeInt(output, mSampleRate * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length/2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        } catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }
    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }
}
