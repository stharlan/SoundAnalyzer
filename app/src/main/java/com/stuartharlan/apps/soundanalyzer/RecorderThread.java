package com.stuartharlan.apps.soundanalyzer;

import android.app.Activity;
import android.media.AudioRecord;
import android.util.Log;
import org.jtransforms.fft.FloatFFT_1D;

/**
 * Created by stuar on 10/1/2017.
 */

public class RecorderThread extends Thread {

    private MainActivity activity = null;
    private AudioRecord audioRecorder = null;
    private byte [] buffer = null;
    private float[] fBuffer = null;
    FloatFFT_1D fft = null;
    private boolean IsRecording = false;
    private int counter = 0;
    private int ReadResult = 0;
    private String ErrorMessage = null;

    public void SetIsRecording(boolean value) { this.IsRecording = value; }

    public RecorderThread(MainActivity vActivity, int vBufferSize, AudioRecord vAudioRecord)
    {
        this.buffer = new byte[vBufferSize];
        this.fBuffer = new float[vBufferSize * 2];
        this.fft = new FloatFFT_1D(vBufferSize);
        this.activity = vActivity;
        this.audioRecorder = vAudioRecord;
        this.IsRecording = true;
    }

    // draw is 1 to 10 millis
    // proc is 1 to 2 millis

    @Override
    public void run() {
        try {
            long start;
            long gotBuffer;
            long end;
            while (this.IsRecording == true) {
                start = System.currentTimeMillis();
                ReadResult = this.audioRecorder.read(this.buffer, 0, this.buffer.length, AudioRecord.READ_BLOCKING);
                gotBuffer = System.currentTimeMillis();
                if(ReadResult > 0 && this.buffer != null) {

                    for(int i=0; i<this.buffer.length; i++) {
                        this.fBuffer[i*2] = (float)this.buffer[i];
                        this.fBuffer[(i*2)+1] = 0.0f;
                    }
                    fft.complexForward(this.fBuffer);

                    this.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        activity.UpdateRecorderView(buffer, fBuffer);
                        }
                    });
                }
                end = System.currentTimeMillis();
                //Log.d("RecorderThread/run/wait", "" + (gotBuffer - start));
                //Log.d("RecorderThread/run/proc", "" + (end - gotBuffer));
                if(end - start < 100) {
                    //Log.d("RecorderThread/run/proc", "sleeping");
                    Thread.sleep(end - start);
                }
            }
        } catch (Exception e) {
            //this.ErrorMessage = e.getMessage();
            //this.activity.runOnUiThread(new Runnable() {
                //@Override
                //public void run() {
                    //activity.UpdateRecorderView(null);
            //    }
            //});
            //this.audioRecorder = null;
            //this.buffer = null;
        }
    }
}
