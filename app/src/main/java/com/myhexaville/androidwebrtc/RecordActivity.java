package com.myhexaville.androidwebrtc;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.smp.soundtouchandroid.SoundStreamAduioRecorder;
import com.smp.soundtouchandroid.SoundTouch;

public class RecordActivity extends Activity {

    private static final String TAG = RecordActivity.class.getSimpleName();

    private SoundStreamAduioRecorder soundTouchRec;


    private Button recorderLayout;
    private Button playerLayout;


    private TextView pitchShow;
    private SeekBar pitchSeekBar;
    private Button pitchResetButton;

    private TextView tempoShow;
    private SeekBar tempoSeekBar;
    private Button tempoResetButton;

    private String lastRecordFile;

    private boolean isPlaying = false;
    private boolean finishEncodeDecode = false;
    private boolean needEncodeDecode = false;

    private TextView log;

    private SoundTouch soundTouch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);


        recorderLayout = (Button) findViewById(R.id.layout_recorder);

        recorderLayout.setOnTouchListener(recordTouchedListener);


        playerLayout = (Button) findViewById(R.id.layout_player);

        playerLayout.setOnClickListener(playListener);
        playerLayout.setClickable(false);

        pitchSeekBar = (SeekBar) findViewById(R.id.pitch_seek);
        pitchSeekBar.setOnSeekBarChangeListener(onPitchSeekBarListener);
        pitchResetButton = (Button) findViewById(R.id.button_reset_pitch);

        pitchResetButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                pitchSeekBar.setProgress(1000);
                onPitchSeekBarListener.onStartTrackingTouch(pitchSeekBar);
                onPitchSeekBarListener.onProgressChanged(pitchSeekBar, 1000, false);
                onPitchSeekBarListener.onStopTrackingTouch(pitchSeekBar);
            }
        });

        pitchShow = (TextView) findViewById(R.id.pitch_show);

        tempoSeekBar = (SeekBar) findViewById(R.id.tempo_seek);
        tempoSeekBar.setOnSeekBarChangeListener(onTempoSeekBarListener);
        tempoResetButton = (Button) findViewById(R.id.button_reset_tempo);
        tempoResetButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                tempoSeekBar.setProgress(5000);
                onTempoSeekBarListener.onStartTrackingTouch(tempoSeekBar);
                onTempoSeekBarListener.onProgressChanged(tempoSeekBar, 5000, false);
                onTempoSeekBarListener.onStopTrackingTouch(tempoSeekBar);
            }
        });

        tempoShow = (TextView) findViewById(R.id.tempo_show);

        soundTouch = new SoundTouch(0, 2, 1, 2, 1, 1);
        soundTouchRec = new SoundStreamAduioRecorder(this, soundTouch);

        log = (TextView) findViewById(R.id.log);
        log.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    protected void startEncodeDecode() {
        if (needEncodeDecode && !finishEncodeDecode && lastRecordFile != null) {
            soundTouchRec.stopRecord();
            log.setText("암호화 및 복호화 시작??\n" + log.getText());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerLayout.setClickable(false);

        isPlaying = false;

        soundTouchRec.stopRecord();
        soundTouchRec.stopPlay(false);
        log.setText("출구，모두 일시 중지\n\n" + log.getText());
    }

    private OnTouchListener recordTouchedListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    soundTouchRec.stopPlay(false);

                    isPlaying = false;

                    soundTouchRec.startRecord();
                    playerLayout.setClickable(false);
                    log.setText("녹음 시작\n" + log.getText());
                    recorderLayout.setText("Recording...");
                    break;
                case MotionEvent.ACTION_UP:
                    lastRecordFile = soundTouchRec.stopRecord();
                    playerLayout.setClickable(true);

                    log.setText("녹음 완료" + lastRecordFile + "\n" + log.getText());
                    recorderLayout.setText("Record");
                    finishEncodeDecode = false;
                    startEncodeDecode();
                    break;
            }
            return true;
        }
    };

    private OnClickListener playListener = new OnClickListener() {

        public void onClick(View v) {
            if (isPlaying) {
                log.setText("재생 중지\n" + log.getText());
                playerLayout.setText("Play");
                soundTouchRec.stopPlay();

            } else {
                log.setText("재생 시작" + getFileToPlay() + "\n" + log.getText());
                playerLayout.setText("stop");
                soundTouchRec.startPlay(getFileToPlay());

            }
            isPlaying = !isPlaying;
        }
    };

    private OnSeekBarChangeListener onPitchSeekBarListener = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            float pitch = (seekBar.getProgress() - 1000) / 100.0f;
            soundTouch.setPitchSemi(pitch);
            playerLayout.setClickable(true);
            log.setText("피치 수정\n" + log.getText());
            Log.d("pitch", ""+pitch);

            if (isPlaying) {
                soundTouchRec.stopPlay();
                soundTouchRec.startPlay(getFileToPlay());
                log.setText("재생 시작" + getFileToPlay() + "\n" + log.getText());

            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            playerLayout.setClickable(false);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {

            float pitch = (progress - 1000) / 100.0f;
            pitchShow.setText("톤: " + pitch);
        }
    };

    private OnSeekBarChangeListener onTempoSeekBarListener = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            float tempo = (seekBar.getProgress() - 5000) / 100.0f;
            soundTouch.setTempoChange(tempo);
            log.setText("수정속도\n" + log.getText());
            playerLayout.setClickable(true);
            if (isPlaying) {
                soundTouchRec.stopPlay();
                soundTouchRec.startPlay(getFileToPlay());
                log.setText("재생 시작" + getFileToPlay() + "\n" + log.getText());

            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            playerLayout.setClickable(false);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            float tempo = (progress - 5000) / 100.0f;
            tempoShow.setText("속도: " + tempo + "%");
        }
    };

    protected String getFileToPlay() {
        return lastRecordFile;
    }

    public void encodeDecodeFinished() {
        finishEncodeDecode = true;
        if (isPlaying) {
            soundTouchRec.stopPlay();
            RecordActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    log.setText("재생 시작" + getFileToPlay() + "\n" + log.getText());
                }
            });

            soundTouchRec.startPlay(getFileToPlay());
        }
    }
}
