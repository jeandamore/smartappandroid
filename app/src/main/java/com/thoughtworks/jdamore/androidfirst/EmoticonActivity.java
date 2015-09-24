package com.thoughtworks.jdamore.androidfirst;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class EmoticonActivity extends Activity {

    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_FATIGUE_LEVEL = "0";

    public static FrameLayout currentLayout;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        switch(intent.getStringExtra(EXTRAS_FATIGUE_LEVEL)) {
            case "2":
                setContentView(R.layout.happy_face);
                currentLayout = (FrameLayout) findViewById(R.id.happy_face);
                break;
            case "3":
                setContentView(R.layout.normal_face);
                currentLayout = (FrameLayout) findViewById(R.id.normal_face);
                break;
            case "4":
                setContentView(R.layout.tired_face);
                currentLayout = (FrameLayout) findViewById(R.id.tired_face);
                break;
            default:
                finish();
                break;
        }

        currentLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                // action to do
                Log.d(TAG, "onTouch yay");
                finish();
                return true;//always return true to consume event
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

    }
}
