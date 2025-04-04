package com.yuer.share14;
import com.yuer.share14.socket.SocketService;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CastScreen";
    private int port = 9837;
    private int permissionRequestCode = 100;
    private int captureRequestCode = 1;

    private TextView statusTextView;
    private Button startButton;
    private ProgressBar progressBar;

    private MediaProjectionManager mediaProjectionManager;
    private SocketService socketService;

    // 用于防止重复点击
    private boolean isProcessingRequest = false;
    // 用于设置IP地址提示
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: 应用启动");

        // 初始化UI控件
        statusTextView = findViewById(R.id.tv_status);
        startButton = findViewById(R.id.btn_start);
        progressBar = findViewById(R.id.progress_bar);
        mainHandler = new Handler(Looper.getMainLooper());

        init();
    }

    private void init() {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Log.d(TAG, "MediaProjectionManager 初始化完成");
    }

    public void onClick(View view) {
        if (isProcessingRequest) {
            Log.d(TAG, "请求处理中，忽略点击");
            return;
        }

        int id = view.getId();
        if (id == R.id.btn_start) {
            Log.d(TAG, "开始按钮被点击");
            isProcessingRequest = true;
            requestRecordingPermission();
        }
    }

    // 直接请求录屏权限，不再先检查存储权限等
    private void requestRecordingPermission() {
        try {
            showStatus("正在请求屏幕录制权限...");
            setLoading(true);

            // 首先确保我们有录音权限（屏幕录制通常需要此权限）
            if (!PermissionUtil.hasPermission(this, PermissionUtil.storagePermissions)) {
                PermissionUtil.checkPermission(this, PermissionUtil.storagePermissions, permissionRequestCode);
                return;
            }

            // 直接请求屏幕录制权限
            Intent intent = mediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, captureRequestCode);
        } catch (Exception e) {
            Log.e(TAG, "请求录屏权限失败", e);
            showStatus("请求录屏权限失败，请重试");
            setLoading(false);
            isProcessingRequest = false;
            Toast.makeText(this, "请求录屏权限失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "权限请求结果返回: requestCode=" + requestCode);
        if (requestCode == permissionRequestCode) {
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                Log.d(TAG, "权限 " + permissions[i] + " 结果: " +
                        (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "已授予" : "被拒绝"));
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                }
            }

            if (allGranted) {
                // 权限已授予，继续请求屏幕录制权限
                requestRecordingPermission();
            } else {
                Log.e(TAG, "部分权限被拒绝，无法继续");
                showStatus("部分权限被拒绝，无法开始屏幕投射");
                setLoading(false);
                isProcessingRequest = false;
                Toast.makeText(this, "请授予录音权限以使用屏幕投射功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (resultCode != RESULT_OK){
            Log.e(TAG, "用户拒绝了屏幕录制请求");
            showStatus("屏幕录制权限被拒绝");
            setLoading(false);
            isProcessingRequest = false;
            return;
        }
        if(requestCode == this.captureRequestCode){
            Log.d(TAG, "屏幕录制请求已批准，开始投射");
            showStatus("屏幕录制开始，等待客户端连接...");
            startCast(resultCode, data);
        }
    }

    private void startCast(int resultCode, Intent data){
        try {
            MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection == null){
                Log.e(TAG, "获取MediaProjection失败");
                showStatus("初始化录屏失败");
                setLoading(false);
                isProcessingRequest = false;
                return;
            }
            Log.d(TAG, "成功获取MediaProjection，开始创建SocketService");
            socketService = new SocketService();

            // 显示启动中状态
            final String startingMsg = "正在启动服务...";
            showStatus(startingMsg);

            // 启动服务
            socketService.start(mediaProjection);

            // 延迟获取IP地址并显示
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 获取本机IP地址作为连接提示
                        String ipAddressHint = "本机连接地址: " + getLocalIpAddress() + ":" + port;
                        showStatus("屏幕投射已开始，等待客户端连接...\n\n" + ipAddressHint);
                    } catch (Exception e) {
                        showStatus("屏幕投射已开始，等待客户端连接...");
                    }
                    setLoading(false);
                    isProcessingRequest = false;
                }
            }, 1000);

            Log.d(TAG, "SocketService启动完成");
        } catch (Exception e) {
            Log.e(TAG, "启动录屏失败", e);
            showStatus("启动录屏失败: " + e.getMessage());
            setLoading(false);
            isProcessingRequest = false;
        }
    }

    // 获取本机IP地址的简化版本
    private String getLocalIpAddress() {
        try {
            java.net.NetworkInterface intf = java.net.NetworkInterface.getByName("wlan0");
            if (intf != null) {
                java.util.Enumeration<java.net.InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    java.net.InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() &&
                            inetAddress.getAddress().length == 4) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取IP地址失败", e);
        }
        return "无法获取IP";
    }

    // 显示当前状态
    private void showStatus(String status) {
        if (statusTextView != null) {
            statusTextView.setText(status);
        }
    }

    // 设置加载状态
    private void setLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (startButton != null) {
            startButton.setEnabled(!isLoading);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: 应用销毁");
        super.onDestroy();
        if (socketService != null){
            socketService.close();
        }

        // 清理Handler
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}