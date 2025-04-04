package com.yuer.share14;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtil {

    private static final String TAG = "CastScreen_Permission";

    // 仅保留真正需要的权限
    public static String[] storagePermissions = {
            Manifest.permission.RECORD_AUDIO
    };

    // 简化的权限检查方法
    public static void checkPermission(Context context, String[] permissions, int requestCode) {
        // 仅检查基本权限，不再主动检查和请求MANAGE_EXTERNAL_STORAGE权限
        // 因为这个权限对屏幕录制不是绝对必要的
        if (!hasPermission(context, permissions)) {
            ActivityCompat.requestPermissions((Activity) context, permissions, requestCode);
        }
    }

    public static boolean hasPermission(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "权限未授予: " + permission);
                return false;
            }
        }
        return true;
    }

    // 此方法保留，但由于我们不再自动请求此权限，仅作为可选工具
    public static void requestManageExternalStoragePermission(Context context) {
        try {
            Log.d(TAG, "请求'管理所有文件'权限");
            Toast.makeText(context, "请授予'管理所有文件'权限以继续使用屏幕录制功能", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            ((Activity)context).startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "请求'管理所有文件'权限失败，尝试使用通用设置页面", e);
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            ((Activity)context).startActivity(intent);
        }
    }
}
