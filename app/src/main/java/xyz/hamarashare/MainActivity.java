package xyz.hamarashare;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 48, 32, 32);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("Hamara Share");
        title.setTextSize(28);
        root.addView(title);
        TextView note = new TextView(this);
        note.setText("\nTCP proxy prototype • port 8888\nKeep Hamara Tunnel connected. Start your Android hotspot separately.");
        note.setTextSize(16);
        root.addView(note);
        status = new TextView(this);
        status.setText("\nStatus: stopped");
        status.setTextSize(18);
        root.addView(status);
        Button start = new Button(this);
        start.setText("Start proxy");
        start.setOnClickListener(v -> {
            startService(new Intent(this, ProxyService.class).setAction("START"));
            status.setText("Status: starting on port 8888");
        });
        root.addView(start);
        Button stop = new Button(this);
        stop.setText("Stop proxy");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, ProxyService.class));
            status.setText("Status: stopped");
        });
        root.addView(stop);
        TextView warning = new TextView(this);
        warning.setText("\nIMPORTANT: This provides a proxy, not transparent hotspot routing. A TV that cannot configure a proxy will not automatically use it. It does not guarantee UDP sharing.");
        warning.setTextSize(14);
        root.addView(warning);
        setContentView(root);
    }
}
