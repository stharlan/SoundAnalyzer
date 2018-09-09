package com.stuartharlan.apps.soundanalyzer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

    private MainView mainView = null;
    private AudioRecord recorder = null;
    private int SampleRate = 22050;
    private RecorderThread rproc = null;
    private byte[] Buffer = null;
    int bufferSize = 1024;

    public int GetSampleRate() { return this.SampleRate; }

    public void UpdateRecorderView(byte[] bufferData, float [] FftData) {
        long start = System.currentTimeMillis();
        if(bufferData != null) {
            if(this.mainView.CopyBufferData(bufferData, FftData) == true) {
                this.mainView.invalidate();
            }
        }
        //Log.d("MainActivity/UpdateReco", "" + (System.currentTimeMillis() - start));
    }

    private void StartAudio()
    {
        //Log.d("1", "starting audio");
        try {
            //int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
//                    AudioFormat.ENCODING_PCM_8BIT);
            this.recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    this.SampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_8BIT,
                    bufferSize);
            if(this.recorder != null) {
                this.Buffer = new byte[bufferSize];
                //this.mainView.SetLastError("recorder is ok");
                this.rproc = new RecorderThread(this, bufferSize, this.recorder);
                this.recorder.startRecording();
                this.rproc.start();
                this.mainView.SetBufferSize(bufferSize);
            } else {
                //this.mainView.SetLastError("recorder is null");
                this.rproc = null;
                this.recorder = null;
            }
        } catch (Exception ex) {
            //this.mainView.SetLastError(ex.getMessage());
            this.rproc = null;
            this.recorder = null;
        }
        this.mainView.invalidate();
    }

    private void StopAudio() {
        if(this.rproc != null) {
            //Log.d("1", "signalling thread");
            this.rproc.SetIsRecording(false);
        }
        if(this.recorder != null) {
            //Log.d("1", "stoppping and releasing recorder");
            this.recorder.stop();
            this.recorder.release();
            this.recorder = null;
            if(this.rproc != null) {
                try {
                    //Log.d("1", "waiting on thread to stop");
                    this.rproc.join();
                } catch (InterruptedException ie) {
                    //Log.d("1", ie.getMessage());
                }
                this.rproc = null;
            }
            this.mainView.SetBufferSize(0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.SampleRate = getIntent().getIntExtra("SampleRate", 22050);
        if(this.SampleRate == 11050) {
            // takes more time to get a full buffer, cut in half
            this.bufferSize = 512;
        } else if(this.SampleRate == 44100) {
            // fills buffer quicker, make larger
            this.bufferSize = 2048;
        }
        this.mainView = new MainView(this);
        setContentView(this.mainView);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        //Log.d("1", "resume");
        super.onResume();
        if(recorder == null) {
            StartAudio();
        }
    }

    @Override
    protected void onStart() {
        //Log.d("1", "start");
        super.onStart();
        StartAudio();
    }

    @Override
    protected void onStop() {
        //Log.d("1", "stop");
        super.onStop();
        StopAudio();
    }

    @Override
    protected void onPause() {
        //Log.d("1", "pause");
        super.onPause();
        StopAudio();
    }

    @Override
    protected void onDestroy() {
        //Log.d("1", "destroy");
        super.onDestroy();
    }


}
