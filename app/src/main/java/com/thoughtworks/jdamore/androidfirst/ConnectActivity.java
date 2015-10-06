package com.thoughtworks.jdamore.androidfirst;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ConnectActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setUpConnectView();
    }

    public void setUpConnectView() {
        Typeface futuraFont = Typeface.createFromAsset(getAssets(),
                "fonts/Futura.ttc");
        Typeface myriadFont = Typeface.createFromAsset(getAssets(), "fonts/MyriadPro_Regular.otf");
        setContentView(R.layout.connect);
        final Button button = (Button) findViewById(R.id.connectButton);
        TextView smartCapTextView = (TextView) findViewById(R.id.smartCapText);
        smartCapTextView.setTypeface(futuraFont);
        final Intent intent = new Intent(this, DeviceScanActivity.class);
        button.setTypeface(myriadFont);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(intent);
            }
        });
    }
}
