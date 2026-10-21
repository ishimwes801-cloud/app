package xyz.hamarashare;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ProxyService extends Service {
    private volatile boolean running = false;
    private ServerSocket server;
    private ExecutorService pool;
    private static final int PORT = 8888;
    private static final String CHANNEL = "hamara_share";

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Hamara Share", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        Notification n = new Notification.Builder(this, CHANNEL)
            .setContentTitle("Hamara Share is running")
            .setContentText("TCP proxy listening on port 8888")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .build();
        startForeground(1, n);
    }
    @Override public int onStartCommand(Intent i, int flags, int startId) {
        if (!running) {
            running = true;
            pool = Executors.newCachedThreadPool();
            pool.execute(() -> acceptLoop());
        }
        return START_STICKY;
    }
    private void acceptLoop() {
        try {
            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress("0.0.0.0", PORT));
            while (running) {
                Socket client = server.accept();
                pool.execute(() -> handle(client));
            }
        } catch (Exception e) {
            android.util.Log.e("HamaraShare", "Proxy listener failed", e);
        }
    }
    private void handle(Socket client) {
        try (Socket c = client;
             BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
             OutputStream out = c.getOutputStream()) {
            c.setSoTimeout(15000);
            String line = in.readLine();
            if (line == null) return;
            String[] parts = line.split(" ");
            if (parts.length < 2) { reply(out, "400 Bad Request"); return; }
            String method = parts[0];
            String target = parts[1];
            while (true) {
                String h = in.readLine();
                if (h == null || h.isEmpty()) break;
            }
            if (!"CONNECT".equalsIgnoreCase(method)) {
                reply(out, "501 Only HTTPS CONNECT is supported");
                return;
            }
            String host = target;
            int port = 443;
            int colon = target.lastIndexOf(':');
            if (colon > 0) {
                host = target.substring(0, colon);
                try { port = Integer.parseInt(target.substring(colon + 1)); } catch (NumberFormatException ignored) {}
            }
            try (Socket remote = new Socket()) {
                remote.connect(new InetSocketAddress(host, port), 12000);
                remote.setSoTimeout(0);
                out.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes("UTF-8"));
                out.flush();
                Thread a = new Thread(() -> pipeQuiet(c, remote));
                Thread b = new Thread(() -> pipeQuiet(remote, c));
                a.start(); b.start(); a.join(); b.join();
            }
        } catch (Exception e) {
            android.util.Log.w("HamaraShare", "Client failed: " + e.getMessage());
        }
    }
    private void pipeQuiet(Socket from, Socket to) {
        try {
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) { out.write(buf, 0, n); out.flush(); }
        } catch (Exception ignored) {}
        try { to.shutdownOutput(); } catch (Exception ignored) {}
    }
    private void reply(OutputStream out, String status) throws IOException {
        out.write(("HTTP/1.1 " + status + "\r\nConnection: close\r\nContent-Length: 0\r\n\r\n").getBytes("UTF-8"));
        out.flush();
    }
    @Override public void onDestroy() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        if (pool != null) pool.shutdownNow();
        stopForeground(true);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) { return null; }
}
