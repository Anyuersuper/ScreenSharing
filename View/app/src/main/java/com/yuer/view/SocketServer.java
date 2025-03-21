package com.yuer.view;


import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

public class SocketServer {

    private final String TAG = "View";
    private SocketClient socketClient;
    private SocketCallback socketCallback;

    public void setSocketCallback(SocketCallback socketCallback){
        this.socketCallback = socketCallback;
    }

    public void start(String url){
        try {
            //URI uri = new URI("ws://10.70.122.79:11006");
            url = "ws://" + url;
            URI uri = new URI(url);
            socketClient = new SocketClient(uri);
            socketClient.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG,"error:"+e.toString());
            e.printStackTrace();
        }
    }

    private class SocketClient extends WebSocketClient{

        public SocketClient(URI serverUri) {
            super(serverUri);
            Log.d(TAG,"new SocketClient");
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            Log.d(TAG,"SocketClient onOpen");
        }

        @Override
        public void onMessage(String message) {
            Log.d(TAG,"onMessage");
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            Log.d(TAG,"onMessage");
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            socketCallback.callBack(buf);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.d(TAG,"onClose ="+reason);
        }

        @Override
        public void onError(Exception ex) {
            Log.d(TAG,"onerror ="+ex.toString());
        }
    }

    public interface SocketCallback{
        void callBack(byte[] data);
    }
}
