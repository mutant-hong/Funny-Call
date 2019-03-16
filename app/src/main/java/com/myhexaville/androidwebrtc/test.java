package com.myhexaville.androidwebrtc;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class test extends Activity {
    private static String TAG = "AudioClient";

    // the server information
    private static final String SERVER = "192.168.0.3";//"115.68.216.237";
    private static final int PORT = 3000;

    // the audio recording options
    private static final int RECORDING_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_STEREO;//CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    // the button the user presses to send the audio stream to the server
    private Button sendAudioButton;

    // the audio recorder
    private AudioRecord recorder;
    private AudioTrack audioTrack;

    // the minimum buffer size needed for audio recording
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            RECORDING_RATE, CHANNEL, FORMAT);

    // are we currently sending audio data
    private boolean currentlySendingAudio = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testactivity);

        Log.i(TAG, "Creating the Audio Client with minimum buffer of "
                + BUFFER_SIZE + " bytes");

        // set up the button
        sendAudioButton = (Button) findViewById(R.id.btnStart);
        sendAudioButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        startStreamingAudio();
                        break;

                    case MotionEvent.ACTION_UP:
                        stopStreamingAudio();
                        break;
                }

                return false;
            }
        });
    }

    private void startStreamingAudio() {

        Log.i(TAG, "Starting the audio stream");
        currentlySendingAudio = true;
        startStreaming();
    }

    private void stopStreamingAudio() {

        Log.i(TAG, "Stopping the audio stream");
        currentlySendingAudio = false;
        recorder.release();
        //audioTrack.release();
    }

    private void startStreaming() {

        Log.i(TAG, "Starting the background thread to stream the audio data");

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    Log.d(TAG, "Creating the datagram socket");
                    DatagramSocket socket = new DatagramSocket();

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
                            RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 10);

                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 10, AudioTrack.MODE_STREAM);

                    Log.d(TAG, "AudioRecord recording...");
                    recorder.startRecording();
                    audioTrack.play();

                    while (currentlySendingAudio == true) {

                        Log.d("while", buffer.length + "");
                        // read the data into the buffer
                        int read = recorder.read(buffer, 0, buffer.length);

                        // place contents of buffer into the packet
                        packet = new DatagramPacket(buffer, read,
                                serverAddress, PORT);

                        // send the packet
                        socket.send(packet);

                        ////////test
                        buffer = new byte[BUFFER_SIZE];
                        packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);

                        audioTrack.write(packet.getData(), packet.getOffset(), packet.getLength());

                    }

                    Log.d(TAG, "AudioRecord finished recording");

                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e);
                }
            }
        });

        // start the thread
        streamThread.start();
    }
}