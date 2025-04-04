package com.yuer.view;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SecondMain extends AppCompatActivity implements SocketServer.SocketCallback{
    private static final String TAG = "View";
    private Surface surface;
    private SurfaceView surfaceView;
    private MediaCodec mediaCodec;
    String url = null;
    
    // 视频尺寸，可以根据实际情况调整
    private int videoWidth = 720;
    private int videoHeight = 1280;
    
    // 屏幕尺寸
    private int screenWidth;
    private int screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        Intent mainintents = getIntent();
        url = mainintents.getStringExtra("url");
        
        // 获取屏幕尺寸
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        
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
                // 当Surface大小变化时调整布局
                adjustSurfaceLayout();
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (mediaCodec != null) {
                    try {
                        mediaCodec.stop();
                        mediaCodec.release();
                        mediaCodec = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    
    // 调整SurfaceView大小以适应视频比例
    private void adjustSurfaceLayout() {
        float videoRatio = (float) videoWidth / videoHeight;
        float screenRatio = (float) screenWidth / screenHeight;
        
        ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
        
        if (videoRatio > screenRatio) {
            // 视频比屏幕更宽，以宽度为基准
            layoutParams.width = screenWidth;
            layoutParams.height = (int) (screenWidth / videoRatio);
        } else {
            // 视频比屏幕更高，以高度为基准
            layoutParams.height = screenHeight;
            layoutParams.width = (int) (screenHeight * videoRatio);
        }
        
        surfaceView.setLayoutParams(layoutParams);
    }

    private void initDecoder(Surface surface) {
        try {
            // 创建H.265(HEVC)解码器
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            
            // 配置MediaFormat
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, videoWidth, videoHeight);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoWidth * videoHeight);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            
            // 使用更通用的颜色格式
            // MediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodec.COLOR_FormatYUV420Flexible);
            
            // 配置解码器
            mediaCodec.configure(mediaFormat, surface, null, 0);
            mediaCodec.start();
            
            // 调整SurfaceView布局
            adjustSurfaceLayout();
        } catch (IOException e) {
            Log.d(TAG, "initDecoder IOException: " + e.getMessage());
            e.printStackTrace();
            
            // 尝试回退到AVC解码器
            try {
                Log.d(TAG, "Trying AVC/H.264 decoder as fallback");
                mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoWidth * videoHeight);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                // mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodec.COLOR_FormatYUV420Flexible);
                mediaCodec.configure(mediaFormat, surface, null, 0);
                mediaCodec.start();
            } catch (IOException e2) {
                Log.e(TAG, "Both HEVC and AVC decoders failed to initialize: " + e2.getMessage());
                e2.printStackTrace();
            }
        }
    }

    private void initSocket() {
        Log.d(TAG,"initSocket");
        SocketServer socketServer = new SocketServer();
        socketServer.setSocketCallback(this);
        //String url = "";
        socketServer.start(url);
    }
    
    // 当接收到视频格式信息时可以更新视频尺寸（如果服务端有提供）
    public void updateVideoSize(int width, int height) {
        if (width > 0 && height > 0) {
            videoWidth = width;
            videoHeight = height;
            runOnUiThread(this::adjustSurfaceLayout);
        }
    }

    @Override
    public void callBack(byte[] data) {
        if (mediaCodec == null) {
            Log.e(TAG, "mediaCodec is null, cannot process video data");
            return;
        }
        
        try {
            int index = mediaCodec.dequeueInputBuffer(100000);
            if (index >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
                inputBuffer.clear();
                inputBuffer.put(data, 0, data.length);
                mediaCodec.queueInputBuffer(index, 0, data.length, System.currentTimeMillis(), 0);
            }
            
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            
            // 处理所有可用的输出缓冲区
            while (outputBufferIndex >= 0) {
                mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
            
            // 处理特殊情况
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d(TAG, "Output format changed");
                MediaFormat newFormat = mediaCodec.getOutputFormat();
                // 如果格式改变包含新的尺寸信息，更新尺寸
                if (newFormat.containsKey(MediaFormat.KEY_WIDTH) && newFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
                    int width = newFormat.getInteger(MediaFormat.KEY_WIDTH);
                    int height = newFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    updateVideoSize(width, height);
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                Log.d(TAG, "Output buffers changed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing video data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaCodec != null) {
            try {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}