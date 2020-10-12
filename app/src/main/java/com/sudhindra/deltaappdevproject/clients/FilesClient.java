package com.sudhindra.deltaappdevproject.clients;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;

import androidx.core.content.FileProvider;

import com.sudhindra.deltaappdevproject.activities.ChatActivity;
import com.sudhindra.deltaappdevproject.activities.NewChatActivity;
import com.sudhindra.deltaappdevproject.models.Message;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

public class FilesClient {

    public static final int FROM_NEW_CHAT = 0, FROM_CHAT = 2;
    public static final String MESSAGE_FILES_DIR = "messageFiles";

    private static File getMessageFilesDir(Context context) {
        File dir = new File(context.getFilesDir(), MESSAGE_FILES_DIR);
        dir.mkdirs();
        return dir;
    }

    public static File getMessageFilesDirForChatChannel(Context context, String channelId) {
        File chatChannelFilesDir = new File(getMessageFilesDir(context), channelId);
        chatChannelFilesDir.mkdirs();
        return chatChannelFilesDir;
    }

    public static File getMessageFile(File channelFilesDir, Message message) {
        File fileDir = new File(channelFilesDir, String.valueOf(message.getSentTimeInMillis()));
        fileDir.mkdirs();
        return new File(fileDir, message.getFileName());
    }

    public static void deleteMessageFile(File channelFilesDir, Message message) {
        File fileDir = new File(channelFilesDir, String.valueOf(message.getSentTimeInMillis()));
        fileDir.mkdirs();
        File file = new File(fileDir, message.getFileName());
        if (file.exists())
            file.delete();
    }

    public static void deleteMessageFilesInChatChannel(String channelId, Context context) throws IOException {
        File dir = new File(getMessageFilesDir(context), channelId);
        if (dir.exists())
            FileUtils.cleanDirectory(dir);
    }

    public static void deleteAllMessageFiles(Context context) throws IOException {
        File dir = new File(context.getFilesDir(), MESSAGE_FILES_DIR);
        if (dir.exists())
            FileUtils.cleanDirectory(dir);
    }

    public static Uri getUriForFile(File file, Context context) {
        return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
    }

    public static String getFileName(Uri uri, Context context) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst())
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1)
                result = result.substring(cut + 1);
        }
        return result;
    }

    public static String getFileType(Uri uri, Context context) {
        return context.getContentResolver().getType(uri);
    }

    public static void copyUriToFile(Uri uri, File file, Context context) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024 * 1024]; // or other buffer size
            int read;

            while ((read = inputStream.read(buffer)) != -1)
                outputStream.write(buffer, 0, read);

            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class FileCopyTask extends AsyncTask<Uri, Void, Void> {

        private WeakReference<Context> contextWeakReference;
        private WeakReference<Activity> activityWeakReference;
        private int from;
        private File file;

        public FileCopyTask(Context context, Activity activity, int from, File file) {
            contextWeakReference = new WeakReference<>(context);
            activityWeakReference = new WeakReference<>(activity);
            this.from = from;
            this.file = file;
        }

        @Override
        protected Void doInBackground(Uri... uris) {
            Context context = contextWeakReference.get();
            FilesClient.copyUriToFile(uris[0], file, context);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (from == FROM_NEW_CHAT) {
                NewChatActivity newChatActivity = (NewChatActivity) activityWeakReference.get();
                newChatActivity.onFileCopyTaskComplete();
            } else if (from == FROM_CHAT) {
                ChatActivity chatActivity = (ChatActivity) activityWeakReference.get();
                chatActivity.onFileCopyTaskComplete();
            }
        }
    }
}
