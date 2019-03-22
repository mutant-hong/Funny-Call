package com.myhexaville.androidwebrtc;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import com.smp.soundtouchandroid.SoundTouch;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AudioConnection {

    private static String TAG = "AudioConnection";

    // the server information
    private static final String SERVER = "115.68.216.237";//"172.30.1.10";
    private static final int PORT = 3000;

    DatagramSocket socket;

    boolean udpConnect = false;
    public boolean receiver = false;
    public boolean sender = false;

    // the audio recording options
    private static final int RECORDING_RATE = 8000;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_STEREO;//CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    // the audio recorder, audioTrack
    private AudioRecord recorder;
    private AudioTrack audioTrack;

    // the minimum buffer size needed for audio recording
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            RECORDING_RATE, CHANNEL, FORMAT);

    SoundTouch soundTouch;

    public AudioConnection(SoundTouch soundTouch){
        this.soundTouch = soundTouch;

        UdpConnection udpConnection = new UdpConnection();
        udpConnection.start();

    }

    public void stopStreamingAudio() {

        Log.i(TAG, "Stopping the audio stream");
        sender = false;

        recorder.release();
        //audioTrack.release();
    }

    public void startStreaming() {
        Log.i(TAG, "Starting the background thread to stream the audio data");

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Log.d(TAG, "Creating the buffer of size " + BUFFER_SIZE);
                    byte[] buffer = new byte[BUFFER_SIZE];

                    Log.d(TAG, "Connecting to " + SERVER + ":" + PORT);
                    final InetAddress serverAddress = InetAddress
                            .getByName(SERVER);
                    Log.d(TAG, "Connected to " + SERVER + ":" + PORT);

                    Log.d(TAG, "Creating the reuseable DatagramPacket");
                    DatagramPacket packet;

                    Log.d(TAG, "Creating the AudioRecord");
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 3);


                    Log.d(TAG, "AudioRecord recording...");
                    recorder.startRecording();

                    while (sender) {

                        //Log.d("while", buffer.length + "");
                        // read the data into the buffer
                        int read = recorder.read(buffer, 0, buffer.length);

                        soundTouch.putBytes(buffer);

                        int len = soundTouch.getBytes(buffer);

                        // place contents of buffer into the packet
                        packet = new DatagramPacket(buffer, len,
                                serverAddress, PORT);

                        // send the packet
                        socket.send(packet);
                    }

                    Log.d(TAG, "AudioRecord finished recording");

                }catch (Exception e){

                }
            }
        });

        // start the thread
        streamThread.start();

    }

    class UdpConnection extends Thread{

        @Override
        public void run(){
            try {
                Log.d(TAG, "Creating the datagram socket");
                socket = new DatagramSocket();

                Log.d(TAG, "Connecting to " + SERVER + ":" + PORT);
                final InetAddress serverAddress = InetAddress.getByName(SERVER);
                Log.d(TAG, "Connected to " + SERVER + ":" + PORT);

                Log.d(TAG, "Creating the reuseable DatagramPacket");
                //DatagramPacket packet = new DatagramPacket(userName.getBytes(), userName.getBytes().length, serverAddress, PORT);

                //socket.send(packet);

                udpConnect = true;
                receiver = true;

                UdpReceiver udpReceiver = new UdpReceiver();
                udpReceiver.start();

                Log.i(TAG, "Starting the audio stream");
                sender = true;

                startStreaming();

            }catch (Exception e){

            }

        }
    }

    class UdpReceiver extends Thread{

        @Override
        public void run(){
            try {

                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 3, AudioTrack.MODE_STREAM);

                Log.d(TAG, "audioTrack playing...");

                int len = 0;

                audioTrack.play();

                while (receiver){
                    byte[] buffer = new byte[BUFFER_SIZE];

                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    Log.d("받음", packet.getData().toString());

                    //soundTouch.putBytes(packet.getData());

                    //len = soundTouch.getBytes(packet.getData());

                    audioTrack.write(packet.getData(), packet.getOffset(), packet.getLength());
                }
            }catch (Exception e){

            }

        }
    }
}
