package com.dsh.xtclean;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final String TARGET = "com.chint.inspect.mvptest";
    private static final int REQ_CODE = 4201;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView tvStatus;
    private TextView tvLog;
    private ScrollView logScroll;

    private final Shizuku.OnRequestPermissionResultListener permListener =
            (requestCode, grantResult) -> {
                if (requestCode != REQ_CODE) return;
                ui.post(() -> {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        log("已获得 Shizuku 授权");
                        doClear(true);
                    } else {
                        log("Shizuku 授权被拒绝，无法自动清理");
                    }
                    refreshStatus();
                });
            };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        Shizuku.addRequestPermissionResultListener(permListener);
        refreshStatus();
    }

    @Override
    protected void onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(permListener);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private int d(float v) {
        return (int) (getResources().getDisplayMetrics().density * v);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF4F6FA);
        root.setFitsSystemWindows(true);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(0xFF1565C0);
        header.setPadding(d(18), d(20), d(18), d(18));

        TextView t1 = new TextView(this);
        t1.setText("小泰换号助手");
        t1.setTextColor(0xFFFFFFFF);
        t1.setTextSize(20);
        t1.getPaint().setFakeBoldText(true);

        TextView t2 = new TextView(this);
        t2.setText("一键清掉「小泰智维」的本地登录数据，换别的账号登录");
        t2.setTextColor(0xCCFFFFFF);
        t2.setTextSize(11);
        t2.setPadding(0, d(4), 0, 0);

        header.addView(t1);
        header.addView(t2);
        root.addView(header);

        tvStatus = new TextView(this);
        tvStatus.setTextSize(13);
        tvStatus.setTextColor(0xFF37474F);
        tvStatus.setPadding(d(18), d(14), d(18), d(14));
        root.addView(tvStatus);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(d(14), 0, d(14), d(14));

        Button btnMain = mkButton("一键清理并打开小泰智维", true);
        btnMain.setOnClickListener(v -> onClearClicked());
        body.addView(btnMain, full());

        Button btnClearOnly = mkButton("只清理，不开 App", false);
        btnClearOnly.setOnClickListener(v -> {
            if (!ensurePermission()) return;
            doClear(false);
        });
        body.addView(btnClearOnly, full());

        Button btnOpen = mkButton("打开小泰智维", false);
        btnOpen.setOnClickListener(v -> launchTarget());
        body.addView(btnOpen, full());

        Button btnInfo = mkButton("应用信息（手动清除数据）", false);
        btnInfo.setOnClickListener(v -> openAppInfo());
        body.addView(btnInfo, full());

        TextView tip = new TextView(this);
        tip.setText("原理：安全退出只清了 App 的业务 token，正泰统一身份认证(SSO)凭证还留在本地，所以再点「内部登录」会自动登回原账号。清理本地数据后，SSO 凭证一起清掉，就能登录别的账号。");
        tip.setTextSize(11);
        tip.setTextColor(0xFF6B7280);
        tip.setLineSpacing(d(4), 1f);
        tip.setPadding(d(2), d(10), d(2), d(10));
        body.addView(tip);

        TextView logTitle = new TextView(this);
        logTitle.setText("运行日志");
        logTitle.setTextSize(12);
        logTitle.setTextColor(0xFF1565C0);
        logTitle.getPaint().setFakeBoldText(true);
        body.addView(logTitle);

        logScroll = new ScrollView(this);
        tvLog = new TextView(this);
        tvLog.setTextSize(11);
        tvLog.setTextColor(0xFF37474F);
        tvLog.setPadding(d(8), d(8), d(8), d(8));
        tvLog.setBackgroundColor(0xFFFFFFFF);
        logScroll.addView(tvLog);
        body.addView(logScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private LinearLayout.LayoutParams full() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = d(10);
        return lp;
    }

    private Button mkButton(String text, boolean primary) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTextColor(primary ? 0xFFFFFFFF : 0xFF1565C0);
        b.setBackgroundColor(primary ? 0xFF1565C0 : 0xFFFFFFFF);
        b.setPadding(d(6), d(14), d(6), d(14));
        b.setStateListAnimator(null);
        return b;
    }

    // ---------------- 状态 ----------------

    private boolean shizukuReady() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean shizukuGranted() {
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    private void refreshStatus() {
        String s;
        if (!shizukuReady()) {
            s = "Shizuku：未连接（请先打开 Shizuku 并启动服务）";
        } else if (shizukuGranted()) {
            s = "Shizuku：已连接 · 已授权，可以直接点下面的按钮";
        } else {
            s = "Shizuku：已连接 · 未授权（点按钮时会弹授权，允许即可）";
        }
        if (tvStatus != null) tvStatus.setText(s);
    }

    private boolean ensurePermission() {
        if (!shizukuReady()) {
            log("Shizuku 没在运行，读不到系统权限。");
            log("办法一：打开 Shizuku → 启动服务 → 回到这里再点。");
            log("办法二：点「应用信息」→ 存储 → 清除数据，效果一样。");
            toast("先启动 Shizuku，或用「应用信息」手动清");
            return false;
        }
        if (shizukuGranted()) return true;
        try {
            Shizuku.requestPermission(REQ_CODE);
            log("已弹出授权框，请点「允许」");
        } catch (Throwable t) {
            log("申请授权失败：" + t);
        }
        return false;
    }

    // ---------------- 清理 ----------------

    private void onClearClicked() {
        if (!ensurePermission()) return;
        doClear(true);
    }

    private void doClear(final boolean relaunch) {
        log("开始清理 " + TARGET + " …");
        new Thread(() -> {
            try {
                String out = sh("pm clear " + TARGET);
                log("pm clear 返回：" + out);
                if (out.contains("Success")) {
                    log("✔ 已清掉小泰智维全部本地数据（含 SSO 登录凭证）");
                    log("打开 App 后就是干净的登录页，可以直接登别的账号。");
                } else {
                    log("✘ 清理没成功，请点「应用信息」手动清除数据");
                }
                if (relaunch) {
                    ui.post(this::launchTarget);
                }
            } catch (Throwable t) {
                log("清理失败：" + t);
            }
        }).start();
    }

    private String sh(String cmd) throws Exception {
        Process p = newProcess(cmd);
        InputStream is = p.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        p.waitFor();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8).trim();
    }

    private static Process newProcess(String cmd) throws Exception {
        Class<?> c = Class.forName("rikka.shizuku.Shizuku");
        StringBuilder seen = new StringBuilder();
        for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
            if (!"newProcess".equals(m.getName())) continue;
            m.setAccessible(true);
            Class<?>[] pt = m.getParameterTypes();
            Object[] args = new Object[pt.length];
            for (int i = 0; i < pt.length; i++) {
                args[i] = (i == 0 && pt[i] == String[].class)
                        ? new String[]{"/system/bin/sh", "-c", cmd} : null;
            }
            try {
                Object r = m.invoke(null, args);
                if (r instanceof Process) return (Process) r;
            } catch (Throwable t) {
                seen.append(t).append(" | ");
            }
        }
        throw new NoSuchMethodException("newProcess not usable: " + seen);
    }

    private void launchTarget() {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(TARGET);
            if (i == null) {
                log("没有找到小泰智维，可能没装");
                return;
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable t) {
            log("打开失败：" + t);
        }
    }

    private void openAppInfo() {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + TARGET));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable t) {
            log("打开应用信息失败：" + t);
        }
    }

    // ---------------- 杂项 ----------------

    private void log(final String s) {
        ui.post(() -> {
            if (tvLog == null) return;
            tvLog.append(s + "\n");
            if (logScroll != null) {
                logScroll.post(() -> logScroll.fullScroll(ScrollView.FOCUS_DOWN));
            }
        });
    }

    private void toast(final String s) {
        ui.post(() -> Toast.makeText(this, s, Toast.LENGTH_SHORT).show());
    }
}
