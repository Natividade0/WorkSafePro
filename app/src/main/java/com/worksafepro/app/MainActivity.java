package com.worksafepro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.rgb(8, 17, 31));
        TextView title = new TextView(this);
        title.setText("WorkSafePro");
        title.setTextColor(Color.rgb(0, 200, 150));
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        TextView msg = new TextView(this);
        msg.setText("Tela inicial nativa funcionando.");
        msg.setTextColor(Color.WHITE);
        msg.setTextSize(18);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, 30, 0, 0);
        layout.addView(title);
        layout.addView(msg);
        setContentView(layout);
    }
}
