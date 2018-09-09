package com.stuartharlan.apps.soundanalyzer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

public class LaunchActivity extends AppCompatActivity {


    private Button StartButtonRef = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.StartButtonRef = (Button)findViewById(R.id.startButton);
        this.StartButtonRef.setEnabled(false);
        this.StartButtonRef.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                RadioGroup rg = (RadioGroup)findViewById(R.id.radioGroupSampleRate);
                int SelectedId = rg.getCheckedRadioButtonId();
                if(SelectedId == R.id.radio11025) {
                    intent.putExtra("SampleRate", (int) 11025);
                } else if(SelectedId == R.id.radio44100) {
                    intent.putExtra("SampleRate", (int) 44100);
                } else {
                    intent.putExtra("SampleRate", (int)22050);
                }
                startActivity(intent);
            }
        });
        //this.StartButtonRef.setOnClickListener(new View.OnClickListener() {
            //@Override
            //public void onClick(View view) {
                //
                //intent.putExtra("")
                //
        //    }
        //});
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.StartButtonRef.setEnabled(false);
        CheckAudioPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.StartButtonRef.setEnabled(false);
        CheckAudioPermissions();
    }

    private void CheckAudioPermissions()
    {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Snackbar.make(this.getWindow().getDecorView(),
                        "This app needs your permission to record audio.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        9801);
            }
        } else {
            this.StartButtonRef.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult (int requestCode,
        String[] permissions, int[] grantResults)
    {
        if(requestCode == 9801) {
            if(grantResults.length > 0) {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.StartButtonRef.setEnabled(true);
                }
            }
        }
    }
}
