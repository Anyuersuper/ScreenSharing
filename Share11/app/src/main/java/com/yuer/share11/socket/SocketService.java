package com.yuer.share11.socket;

import android.media.projection.MediaProjection;
import android.util.Log;
import com.yuer.share11.encode.CodecH265;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketService {

    private static final String TAG = "CastScreen_Service";
    private int port = 9837;
    private CodecH265 codecH265;
    private SocketServer webSocketServer;
    private AtomicBoolean isServiceRunning = new AtomicBoolean(false);

    public SocketService(){
        try {
            String ipAddress = getLocalIpAddress();
            Log.d(TAG, "本机IP地址: " + ipAddress);

            // 创建并绑定指定端口的WebSocket服务器
            webSocketServer = new SocketServer(new InetSocketAddress(ipAddress, port));
            Log.d(TAG, "SocketServer创建成功，地址: " + ipAddress + ":" + port);
        } catch (Exception e) {
            Log.e(TAG, "创建SocketServer失败", e);
        }
    }

    // 获取本机IP地址
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                // 跳过禁用的、环回的或虚拟的接口
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                    continue;
                }

                Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && !address.isLinkLocalAddress() && address.isSiteLocalAddress()) {
                        String ip = address.getHostAddress();
                        Log.d(TAG, "找到可用IP: " + ip + ", 接口: " + ni.getDisplayName());
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取IP地址失败", e);
        }
        Log.w(TAG, "未找到合适的IP地址，使用0.0.0.0（所有接口）");
        return "0.0.0.0"; // 使用所有接口，而不是回环
    }

    public void start(MediaProjection mediaProjection){
        if (isServiceRunning.get()) {
            Log.w(TAG, "服务已在运行，忽略启动请求");
            return;
        }

        try {
            Log.d(TAG, "启动SocketServer");
            if (webSocketServer != null) {
                // 以守护线程方式启动WebSocket服务器
                webSocketServer.setReuseAddr(true);

                // 使用try-catch启动服务器，如果启动成功则继续
                try {
                    webSocketServer.start();
                    Log.d(TAG, "WebSocket服务器启动命令已发送");

                    // 给服务器一些时间启动
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // 忽略
                    }

                    Log.d(TAG, "WebSocket服务器已启动，监听端口: " + port);
                } catch (Exception e) {
                    Log.e(TAG, "WebSocket服务器启动失败", e);
                    return;
                }
            } else {
                Log.e(TAG, "WebSocket服务器对象为空，无法启动");
                return;
            }

            Log.d(TAG, "创建并启动编码器");
            codecH265 = new CodecH265(this, mediaProjection);
            codecH265.startEncode();
            Log.d(TAG, "服务启动完成");
            isServiceRunning.set(true);
        } catch (Exception e) {
            Log.e(TAG, "启动服务失败", e);
        }
    }

    public void close(){
        if (!isServiceRunning.get()) {
            Log.w(TAG, "服务未运行，忽略关闭请求");
            return;
        }

        try {
            Log.d(TAG, "关闭服务");
            isServiceRunning.set(false);

            if (codecH265 != null) {
                codecH265.stopEncode();
                Log.d(TAG, "编码器已停止");
            }

            if (webSocketServer != null) {
                try {
                    webSocketServer.stop();
                    Log.d(TAG, "WebSocketServer已停止");
                } catch (Exception e) {
                    Log.e(TAG, "停止WebSocketServer时出错", e);
                }

                try {
                    // 使用新的方法关闭WebSocketServer
                    webSocketServer.closeAllConnections();
                    Log.d(TAG, "WebSocketServer连接已关闭");
                } catch (Exception e) {
                    Log.e(TAG, "关闭WebSocketServer连接时出错", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "关闭服务时出现异常", e);
        }
    }

    public void sendData(byte[] bytes){
        if (!isServiceRunning.get()) {
            Log.w(TAG, "服务未运行，忽略数据发送");
            return;
        }

        if (webSocketServer != null) {
            webSocketServer.sendData(bytes);
        } else {
            Log.e(TAG, "无法发送数据，WebSocketServer未初始化");
        }
    }
}
