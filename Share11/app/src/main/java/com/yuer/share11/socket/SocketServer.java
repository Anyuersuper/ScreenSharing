package com.yuer.share11.socket;

import android.util.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class SocketServer extends WebSocketServer {
    private final String TAG = "CastScreen_Socket";

    // 使用线程安全的集合存储所有连接的客户端
    private final ConcurrentHashMap<String, WebSocket> clients = new ConcurrentHashMap<>();
    private int dataPacketsCount = 0;
    private boolean hasStarted = false;

    public SocketServer(InetSocketAddress inetSocketAddress){
        super(inetSocketAddress);
        setConnectionLostTimeout(30); // 30秒超时
        Log.d(TAG, "创建服务器，地址: " + inetSocketAddress.getHostString() + ":" + inetSocketAddress.getPort());
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String clientId = conn.getRemoteSocketAddress().toString();
        Log.d(TAG, "客户端连接: " + clientId);

        // 添加客户端到连接集合
        clients.put(clientId, conn);
        Log.d(TAG, "当前连接客户端数: " + clients.size());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String clientId = conn.getRemoteSocketAddress().toString();
        Log.d(TAG, "连接关闭: " + clientId + " 原因: " + reason);

        // 从集合中移除客户端
        clients.remove(clientId);
        Log.d(TAG, "当前连接客户端数: " + clients.size());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String clientId = conn.getRemoteSocketAddress().toString();
        Log.d(TAG, "收到客户端 " + clientId + " 文本消息: " + message);

        // 简单的控制协议
        if ("ping".equalsIgnoreCase(message)) {
            conn.send("pong");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        String clientId = conn != null ? conn.getRemoteSocketAddress().toString() : "未知客户端";
        Log.e(TAG, "客户端 " + clientId + " 发生错误: " + ex.getMessage(), ex);

        if (conn != null && clients.containsKey(clientId)) {
            // 移除出错的客户端连接
            clients.remove(clientId);
            try {
                conn.close();
            } catch (Exception e) {
                Log.e(TAG, "关闭出错连接时发生异常", e);
            }
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "服务器启动成功，监听端口: " + getPort());
        hasStarted = true;
    }

    public void sendData(byte[] bytes){
        dataPacketsCount++;
        if (dataPacketsCount % 100 == 0) {
            Log.d(TAG, "已发送 " + dataPacketsCount + " 个数据包");
        }

        if (clients.isEmpty()) {
            if (dataPacketsCount % 50 == 0) {  // 减少日志频率
                Log.w(TAG, "无法发送数据，没有连接的客户端");
            }
            return;
        }

        // 向所有连接的客户端广播数据
        int sentCount = 0;
        for (WebSocket socket : clients.values()) {
            try {
                if (socket != null && socket.isOpen()) {
                    socket.send(bytes);
                    sentCount++;
                }
            } catch (Exception e) {
                Log.e(TAG, "发送数据到客户端时出错", e);
            }
        }

        if (dataPacketsCount % 100 == 0) {
            Log.d(TAG, "成功发送到 " + sentCount + " 个客户端");
        }
    }

    // 修改为自定义关闭方法，不使用继承的close方法
    public void closeAllConnections() {
        Log.d(TAG, "关闭WebSocket服务器");

        // 关闭所有客户端连接
        for (WebSocket socket : clients.values()) {
            try {
                if (socket != null && socket.isOpen()) {
                    socket.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "关闭客户端连接时出错", e);
            }
        }

        clients.clear();
        hasStarted = false;

        // 调用父类的stop方法而不是close
        try {
            this.stop();
            Log.d(TAG, "WebSocketServer成功停止");
        } catch (Exception e) {
            Log.e(TAG, "停止WebSocketServer时出错", e);
        }
    }
}

