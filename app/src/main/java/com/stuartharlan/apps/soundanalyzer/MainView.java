package com.stuartharlan.apps.soundanalyzer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by stuar on 10/1/2017.
 */

public class MainView extends View {

    private static final int FFT_FULL = 1;
    private static final int FFT_LOW = 2;
    private static final int FFT_MID = 3;
    private static final int FFT_HIGH = 4;

    private static final int VIS_BOTH = 1;
    private static final int VIS_FFT = 2;
    private static final int VIS_WAVEFORM = 3;

    private Paint paint = null;
    private int BufferSize = 0;
    private byte[] AudioData = null;
    private int[] AudioDataInt = null;
    private float[] FftData = null;
    private Bitmap localBitmap = null;
    private Bitmap tempBitmap = null;
    private Canvas localCanvas = null;
    private int FftType = FFT_FULL;
    private int[] MaxFrequencies = null;
    private int MaxFreqIndex = 0;
    private int MaxFrequencyAvg = 0;
    private int VisType = VIS_BOTH;
    private int TouchedBin = -1;
    private boolean IsPaused = false;
    private int SampleRate = 22050;
    private int Multiplier = 1;

    private float tx = -1;
    private float ty = -1;

    public MainView(Context context) {
        super(context);
        this.paint = new Paint();
        this.MaxFrequencies = new int[25];
        MainActivity mainActivity = (MainActivity)context;
        this.SampleRate = mainActivity.GetSampleRate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int maskedAction = MotionEventCompat.getActionMasked(event);
        if(maskedAction == MotionEvent.ACTION_DOWN) {
            float w = localBitmap.getWidth();
            float h = localBitmap.getHeight();
            float ex = event.getX();
            float ey = event.getY();
            if(ex > w - 210.0f && ex < w - 10.0f && ey > 10.0f && ey < 110.0f) {
                this.FftType++;
                if(this.FftType > FFT_HIGH) this.FftType = FFT_FULL;
            } else if(ex > w - 420.0f && ex < w - 220.0f && ey > 10.0f && ey < 110.0f) {
                this.VisType++;
                if (this.VisType > VIS_WAVEFORM) this.VisType = VIS_BOTH;
            } else if(ex > w - 630.0f && ex < w - 430.0f && ey > 10.0f && ey < 110.0f) {
                this.IsPaused = !this.IsPaused;
            } else if(ex > w - 870.0f && ex < w - 670.0f && ey > 10.0f && ey < 110.0f) {
                Multiplier++;
                if(Multiplier > 5) Multiplier = 1;
            } else {
                this.tx = ex;
                this.ty = ey;
            }
            return true;
        } else if(maskedAction == MotionEvent.ACTION_MOVE) {
            this.tx = event.getX();
            this.ty = event.getY();
            return true;
        }
        return false;
    }

    public void SetBufferSize(int value) {
        this.BufferSize = value;
        this.AudioData = new byte[value];
        this.AudioDataInt = new int[value];
        this.FftData = new float[value];
    }

    public boolean CopyBufferData(byte[] data, float [] fdata) {

        if(this.AudioData.length < 1) return false;
        if(this.IsPaused == true) return false;

        // copy the audio data
        System.arraycopy(data, 0, this.AudioData, 0, data.length);

        // only copy half the data from fft
        System.arraycopy(fdata, 0, this.FftData, 0, fdata.length / 2);

        for(int p=0; p<this.AudioData.length; p++) {
            this.AudioDataInt[p] = ((int)(this.AudioData[p] & 0xFF) - 128) * Multiplier;
            if(this.AudioDataInt[p] > 127) this.AudioDataInt[p] = 127;
            if(this.AudioDataInt[p] < -128) this.AudioDataInt[p] = -128;
        }

        // calculate the magnitudes and max
        double [] fvals = new double[this.AudioData.length];
        double maxVal = Double.MIN_VALUE;
        int maxBin = 1;
        double rp = 0.0;
        double ip = 0.0;
        for(int i=1; i<(this.AudioData.length / 2) && i < this.localCanvas .getWidth(); i++) {
            rp = this.FftData[i * 2];
            ip = this.FftData[(i * 2) + 1];
            fvals[i] = Math.sqrt((rp * rp) + (ip * ip));
            //fvals[i] = this.FftData[i*2];
            if(fvals[i] > maxVal) {
                maxVal = fvals[i];
                maxBin = i;
            }
        }
        double logMaxVal = Math.log10(maxVal);

        // calculate the peak freq
        this.MaxFrequencies[this.MaxFreqIndex] = (int)((float)maxBin * (float)SampleRate / (float)this.AudioData.length);
        this.MaxFreqIndex = this.MaxFreqIndex + 1;
        if(this.MaxFreqIndex >= this.MaxFrequencies.length) this.MaxFreqIndex = 0;
        this.MaxFrequencyAvg = 0;
        for(int i=0; i<this.MaxFrequencies.length; i++) {
            this.MaxFrequencyAvg += this.MaxFrequencies[i];
        }
        this.MaxFrequencyAvg /= this.MaxFrequencies.length;

        //Bitmap tempBmp = Bitmap.createBitmap(this.localBitmap);
        Rect src = new Rect();
        src.set(9, 0, this.tempBitmap.getWidth() - 1, this.tempBitmap.getHeight());
        Rect dest = new Rect();
        dest.set(10, 0, this.tempBitmap.getWidth(), this.tempBitmap.getHeight());

        // set canvas to temp bitmap
        this.localCanvas.setBitmap(this.tempBitmap);

        // draw from localBitmap to tempBitmap
        this.localCanvas.drawBitmap(this.localBitmap, src, dest, null);

        // switch local and temp
        Bitmap trash = this.localBitmap;
        this.localBitmap = this.tempBitmap;
        this.tempBitmap = trash;
        trash = null;

        // set canvas to localBitmap
        this.localCanvas.setBitmap(this.localBitmap);

        float factor = 1.0f;
        int offset = 0;
        if(FftType == FFT_FULL) {
            factor = ((float) this.AudioData.length / 2.0f) / (float) this.localBitmap.getHeight();
        } else {
            factor = ((float) this.AudioData.length / 6.0f) / (float) this.localBitmap.getHeight();
            if(FftType == FFT_MID) {
                offset = this.AudioData.length / 6;
            } else if(FftType == FFT_HIGH) {
                offset = this.AudioData.length / 3;
            }
        }

        // translate y to bin
        if(ty > -1) {
            this.TouchedBin = (int) ((float) ty * factor) + offset;
            if(this.TouchedBin < 1) this.TouchedBin = 1;
            if(this.TouchedBin >= (this.AudioData.length / 2)) this.TouchedBin = this.AudioData.length / 2;
        }

        //for(int i=1; i<(this.AudioData.length / 2) && i < this.localCanvas.getHeight(); i++) {
        int clr = 0;
        for(int i=1; i<this.localCanvas.getHeight(); i++) {
            int index = (int)((float)i * factor) + offset;
            if(index < (this.AudioData.length / 2)) {
                //int clr = (int) ((Math.log10(fvals[index]) / logMaxVal) * 255.0);
                clr = 0;
                clr = (int) ((fvals[index] / maxVal) * 255.0);
                paint.setARGB(255, clr, 0, 0);
                //this.localCanvas.drawLine(i, this.localCanvas.getHeight(), i, this.localCanvas.getHeight() - 10, paint);
                this.localCanvas.drawLine(0, i, 10, i, paint);
                //this.localCanvas.drawLine(0, i, 1, i, paint);
                //this.localCanvas.drawPoint(0, i, paint);
            }
        }
        // END FFT

        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if(this.localBitmap != null) this.localBitmap.recycle();
        if(this.tempBitmap != null) this.tempBitmap.recycle();
        this.localBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        this.tempBitmap = Bitmap.createBitmap(this.localBitmap);
        this.localCanvas = new Canvas(this.localBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {

        long start = System.currentTimeMillis();

        super.onDraw(canvas);
        paint.setColor(Color.BLACK);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);

        // draw the fft
        if(VisType == VIS_BOTH || VisType == VIS_FFT) {
            canvas.drawBitmap(this.localBitmap, 0, 0, null);
        }

        paint.setColor(Color.YELLOW);
        paint.setTextSize(48);
        //canvas.drawText("SoundAnalyzer 1.0: Running...", 2, 48, paint);
        //canvas.drawText("Buffer Size = " + this.BufferSize, 2, 2 * 48, paint);
        //canvas.drawText(this.LastError, 2, 3 * 48, paint);
        //canvas.drawText("read result = " + this.ReadResult, 2, 4 * 48, paint);
        //canvas.drawText("w " + canvas.getWidth() + "; h " + canvas.getHeight(), 2, 48, paint);

        if(VisType == VIS_BOTH || VisType == VIS_WAVEFORM) {
            // scale the width to show entire buffer, 2048 buffer
            float hScale = (float) this.BufferSize / (float) canvas.getWidth();
            float halfCanvas = canvas.getHeight() / 2;
            float vScale = (float) halfCanvas / 128.0f;
            int limit = this.AudioData.length;

            float aValue = 0;
            for (int i = 0; i < canvas.getWidth(); i++) {
                int index = (int) ((float) i * hScale);
                if (index < limit) {
                    //aValue = (float)this.AudioData[index] * (float)Multiplier;
                    //if(aValue > 127.0f) aValue = 127.0f;
                    //if(aValue < -127.0f) aValue = -127.0f;

//                    aValue = (float)this.AudioData[index];
//                    if (aValue > 0.0f) {
//                        // positive is 127 (signal) to 0 (no signal)
//                        canvas.drawLine((float) i, aValue * vScale, (float) i, halfCanvas, paint);
//                    } else {
//                        // negative is
//                        canvas.drawLine((float) i, halfCanvas, (float) i, halfCanvas + ((aValue * vScale) + halfCanvas), paint);
//                    }

                    canvas.drawLine((float)i, halfCanvas, (float)i, halfCanvas + (this.AudioDataInt[index] * vScale), paint);

                }
            }
            canvas.drawLine(0, halfCanvas, canvas.getWidth(), halfCanvas, paint);
        }

        int w = canvas.getWidth();
        int h = canvas.getHeight();

        if((VisType == VIS_BOTH || VisType == VIS_FFT) && tx > -1 && ty > -1) {
            int freq = (int) ((float) this.TouchedBin * (float) this.SampleRate / (float) this.AudioData.length);
            String fstr = freq + " Hz";
            float tlen = paint.measureText(fstr);
            paint.setARGB(255, 127, 127, 255);
            canvas.drawText(fstr, w - tlen - 10, h - 108, paint);

            paint.setARGB(127, 127, 127, 255);
            paint.setStrokeWidth(16);
            canvas.drawLine(0, ty, w, ty, paint);
            paint.setStrokeWidth(1);
            //canvas.drawLine(tx, 0, tx, h, paint);
            //canvas.drawCircle(tx, ty, 20, paint);
        }

        // fft button
        paint.setColor(Color.YELLOW);
        canvas.drawRect(w - 210, 10, w - 10, 110, paint);

        paint.setColor(Color.BLACK);
        if(FftType == FFT_FULL) {
            canvas.drawText("FULL", w - 190, 80, paint);
        } else if(FftType == FFT_HIGH) {
            canvas.drawText("HIGH", w - 190, 80, paint);
        } else if(FftType == FFT_MID) {
            canvas.drawText("MID", w - 190, 80, paint);
        } else if(FftType == FFT_LOW) {
            canvas.drawText("LOW", w - 190, 80, paint);
        }

        // vis button
        paint.setColor(Color.YELLOW);
        canvas.drawRect(w - 420, 10, w - 220, 110, paint);

        paint.setColor(Color.BLACK);
        if(VisType == VIS_BOTH) {
            canvas.drawText("BOTH", w - 400, 80, paint);
        } else if(VisType == VIS_FFT) {
            canvas.drawText("FFT", w - 400, 80, paint);
        } else if(VisType == VIS_WAVEFORM) {
            canvas.drawText("WAVE", w - 400, 80, paint);
        }

        // pause button
        paint.setColor(Color.YELLOW);
        canvas.drawRect(w - 660, 10, w - 430, 110, paint);
        paint.setColor(Color.BLACK);
        if(IsPaused == true) {
            canvas.drawText("RESUME", w - 640, 80, paint);
        } else {
            canvas.drawText("PAUSE", w - 640, 80, paint);
        }

        // mult button
        paint.setColor(Color.YELLOW);
        canvas.drawRect(w - 870, 10, w - 670, 110, paint);
        paint.setColor(Color.BLACK);
        canvas.drawText(Multiplier + "x", w - 850, 80, paint);

        paint.setColor(Color.YELLOW);
        String fstr = this.MaxFrequencyAvg + " Hz";
        float tlen = paint.measureText(fstr);
        canvas.drawText(fstr, w - tlen - 10, h - 58, paint);

        //Log.d("MainView/onDraw", "" + (System.currentTimeMillis() - start));
    }
}
