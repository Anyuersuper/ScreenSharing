package com.yuer.share14.encode;
import com.yuer.share14.socket.SocketService;


import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CodecH265 extends Thread {

    private static final String TAG = "CastScreen_Codec";
    private int width = 720;
    private int height = 1280;
    private final String enCodeType = "video/hevc";
    private MediaCodec mediaCodec;
    private VirtualDisplay virtualDisplay;

    private MediaProjection mediaProjection;
    private SocketService socketService;

    private boolean play = true;
    private long timeOut = 10000;
    private byte[] vps_pps_sps;
    private final int NAL_I = 19;
    private final int NAL_VPS = 32;

    public CodecH265(SocketService socketService, MediaProjection mediaProjection) {
        this.socketService = socketService;
        this.mediaProjection = mediaProjection;
        Log.d(TAG, "CodecH265 初始化，分辨率: " + width + "x" + height);

        // 检查设备是否支持HEVC编码
        checkHevcSupport();
    }

    // 检查设备HEVC编码器支持情况
    private void checkHevcSupport() {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = codecList.getCodecInfos();
        List<String> supportedCodecs = new ArrayList<>();

        // 寻找支持HEVC编码的编码器
        for (MediaCodecInfo codecInfo : codecInfos) {
            if (codecInfo.isEncoder()) {
                String[] types = codecInfo.getSupportedTypes();
                for (String type : types) {
                    supportedCodecs.add(type);
                    if (type.equalsIgnoreCase(enCodeType)) {
                        Log.d(TAG, "找到HEVC编码器: " + codecInfo.getName());
                    }
                }
            }
        }

        Log.d(TAG, "设备支持的编码类型: " + supportedCodecs);

        if (!supportedCodecs.contains(enCodeType)) {
            Log.e(TAG, "警告：该设备可能不支持HEVC编码！将尝试使用其他方式初始化编码器");
        }
    }

    public void startEncode() {
        try {
            Log.d(TAG, "开始配置编码器");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(enCodeType, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            // 添加一些关键的编码参数，提高兼容性
            mediaFormat.setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);

            Log.d(TAG, "创建H.265编码器");
            try {
                // 尝试使用指定mime类型创建编码器
                mediaCodec = MediaCodec.createEncoderByType(enCodeType);
            } catch (IOException e) {
                Log.e(TAG, "创建HEVC编码器失败，尝试使用AVC(H.264):", e);
                // 如果HEVC不可用，尝试使用H.264
                mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            }

            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            Log.d(TAG, "创建输入Surface");
            Surface surface = mediaCodec.createInputSurface();

            Log.d(TAG, "创建VirtualDisplay");
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "screen",
                    width,
                    height,
                    1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    surface,
                    null,
                    null
            );
            Log.d(TAG, "编码器配置完成，开始编码线程");
            this.start();
        } catch (IOException e) {
            Log.e(TAG, "初始化编码器失败", e);
        } catch (Exception e) {
            Log.e(TAG, "配置编码器时出现异常", e);
        }
    }

    @Override
    public void run() {
        try {
            Log.d(TAG, "开始编码线程");
            mediaCodec.start();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            long startTime = System.currentTimeMillis();
            int frameCount = 0;

            while (play) {
                try {
                    int outPutBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, timeOut);
                    if (outPutBufferId >= 0) {
                        frameCount++;
                        if (frameCount % 100 == 0) {
                            long now = System.currentTimeMillis();
                            Log.d(TAG, "已编码" + frameCount + "帧，平均帧率: " +
                                    (frameCount * 1000.0 / (now - startTime)) + " fps");
                        }

                        ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outPutBufferId);
                        if (byteBuffer != null) {
                            reEncode(byteBuffer, bufferInfo);
                        } else {
                            Log.w(TAG, "获取到的ByteBuffer为null");
                        }
                        mediaCodec.releaseOutputBuffer(outPutBufferId, false);
                    } else if (outPutBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        Log.d(TAG, "编码器输出格式已更改: " + mediaCodec.getOutputFormat());
                    } else if (outPutBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // 超时，继续尝试
                    } else {
                        Log.w(TAG, "未知的编码器返回值: " + outPutBufferId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "编码过程中出现错误，尝试恢复", e);
                    // 短暂休眠避免频繁错误
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        // 忽略
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "编码过程中出现严重异常", e);
        }
    }

    private void reEncode(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        try {
            int offSet = 4;
            if (byteBuffer.get(2) == 0x01) {
                offSet = 3;
            }

            // 计算出当前帧的类型
            int type = (byteBuffer.get(offSet) & 0x7E) >> 1;
            if (type == NAL_VPS) {
                // 保存vps sps pps信息
                Log.d(TAG, "收到VPS/SPS/PPS帧，大小: " + bufferInfo.size + " 字节");
                vps_pps_sps = new byte[bufferInfo.size];
                byteBuffer.get(vps_pps_sps);
            } else if (type == NAL_I) {
                // 将保存的vps sps pps添加到I帧前
                Log.d(TAG, "收到I帧，大小: " + bufferInfo.size + " 字节");
                final byte[] bytes = new byte[bufferInfo.size];
                byteBuffer.get(bytes);

                if (vps_pps_sps != null) {
                    byte[] newBytes = new byte[vps_pps_sps.length + bytes.length];
                    System.arraycopy(vps_pps_sps, 0, newBytes, 0, vps_pps_sps.length);
                    System.arraycopy(bytes, 0, newBytes, vps_pps_sps.length, bytes.length);
                    socketService.sendData(newBytes);
                    Log.d(TAG, "发送带VPS/SPS/PPS的I帧，总大小: " + newBytes.length + " 字节");
                } else {
                    Log.w(TAG, "VPS/SPS/PPS数据为空，只发送I帧数据");
                    socketService.sendData(bytes);
                }
            } else {
                // P帧或其他类型帧
                byte[] bytes = new byte[bufferInfo.size];
                byteBuffer.get(bytes);
                socketService.sendData(bytes);
            }
        } catch (Exception e) {
            Log.e(TAG, "处理编码数据时出现异常", e);
        }
    }

    public void stopEncode() {
        Log.d(TAG, "停止编码");
        play = false;

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
            Log.d(TAG, "已释放VirtualDisplay");
        }

        if (mediaCodec != null) {
            try {
                mediaCodec.stop();
                mediaCodec.release();
                Log.d(TAG, "已释放MediaCodec");
            } catch (Exception e) {
                Log.e(TAG, "释放MediaCodec时出现异常", e);
            }
            mediaCodec = null;
        }
    }
}
