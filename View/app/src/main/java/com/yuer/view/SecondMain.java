package com.yuer.view;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SecondMain extends AppCompatActivity implements SocketServer.SocketCallback{
    private static final String TAG = "View";
    private Surface surface;
    private SurfaceView surfaceView;
    private MediaCodec mediaCodec;
    String url = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        Intent mainintents = getIntent();
        url = mainintents.getStringExtra("url");
        init();
    }

    private void init() {
        surfaceView = findViewById(R.id.sfv_play);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                surface = holder.getSurface();
                initSocket();
                initDecoder(surface);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }

    private void initDecoder(Surface surface) {
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC,720,1280);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,720*1280);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,20);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
            mediaCodec.configure(mediaFormat,surface,null,0);
            mediaCodec.start();
        } catch (IOException e) {
            Log.d(TAG,"initDecoder IOException ");
            e.printStackTrace();
        }
    }

    private void initSocket() {
        Log.d(TAG,"initSocket");
        SocketServer socketServer = new SocketServer();
        socketServer.setSocketCallback(this);
        //String url = "";
        socketServer.start(url);
    }

    @Override
    public void callBack(byte[] data) {
        Log.d(TAG,"mainActivity callBack");
        int index = mediaCodec.dequeueInputBuffer(100000);
        if (index >= 0){
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(data,0,data.length);
            mediaCodec.queueInputBuffer(index,0,data.length,System.currentTimeMillis(),0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,10000);
        while (outputBufferIndex > 0){
            mediaCodec.releaseOutputBuffer(outputBufferIndex,true);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,0);
        }
    }
}