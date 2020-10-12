package com.sudhindra.deltaappdevproject.clients;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import androidx.core.content.FileProvider;

import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.models.Post;

import java.io.File;
import java.io.FileOutputStream;

public class ShareClient {

    public static void sharePost(Post post, String head, String body, ImageView imageView, Context context) {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.putExtra(android.content.Intent.EXTRA_TEXT, head + "\n" + body);
        if (post.isImagePost()) {
            try {
                Bitmap bitmap;
                bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                File file = new File(context.getExternalCacheDir(), post.getPostedTimeInMillis() + ".png");
                FileOutputStream outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file));
                intent.setType("image/png");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            intent.setType("text/plain");
        context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.sharePostTile)));
    }

    public static void shareMessageFile(String fileType, File file, Context context) {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file));
        intent.setType(fileType);
        context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.shareMessageFileTitle)));
    }
}
