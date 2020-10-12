package com.sudhindra.deltaappdevproject.clients;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sudhindra.deltaappdevproject.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ExternalStorageClient {

    public static final int WRITE_EXTERNAL_STORAGE_REQUEST = 67;

    public static void checkWriteExternalStoragePermission(Activity activity, ImageView imageView, String fileName, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Storage Permission")
                        .setMessage(context.getResources().getString(R.string.savePhotoRequest))
                        .setNegativeButton("Not now", (dialog, which) -> {
                        })
                        .setPositiveButton("Continue", (dialog, which) -> activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST))
                        .show();
            } else saveImageToStorage(imageView, fileName, context);
        } else saveImageToStorage(imageView, fileName, context);
    }

    private static void saveImageToStorage(ImageView imageView, String fileName, Context context) {
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

        File filePath = Environment.getExternalStorageDirectory();
        File dir = new File(filePath.getAbsolutePath() + "/" + context.getResources().getString(R.string.app_name) + "/");
        dir.mkdirs();
        File file = new File(dir, "IMG_" + fileName + ".jpg");
        try {
            OutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            Toast.makeText(context, "Image saved", Toast.LENGTH_SHORT).show();
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
