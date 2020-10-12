package com.sudhindra.deltaappdevproject.clients;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.sudhindra.deltaappdevproject.R;

import java.util.ArrayList;

public class TextRecognitionClient {

    private static final String TAG = "TextRecognitionClient";
    private static Vibrator vibrator;
    private static ClipboardManager clipboardManager;
    private static TextRecognizer textRecognizer = TextRecognition.getClient();
    private static ProgressDialog textRecognitionProgressDialog;

    public static void processImage(Context context, ImageView imageView) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        textRecognitionProgressDialog = new ProgressDialog(context, R.style.AppTheme_ProgressDialogTheme);
        textRecognitionProgressDialog.setMessage("Processing...");
        textRecognitionProgressDialog.setCancelable(false);
        textRecognitionProgressDialog.setCanceledOnTouchOutside(false);
        textRecognitionProgressDialog.show();
        InputImage inputImage = InputImage.fromBitmap(((BitmapDrawable) imageView.getDrawable()).getBitmap(), 0);
        textRecognizer.process(inputImage)
                .addOnSuccessListener(visionText -> showRecognizedText(context, visionText))
                .addOnFailureListener(e -> {
                    Log.i(TAG, "processImage: " + e.getMessage());
                    Toast.makeText(context, "Failed to Process Image", Toast.LENGTH_SHORT).show();
                    textRecognitionProgressDialog.dismiss();
                });
    }

    private static void showRecognizedText(Context context, Text visionText) {
        textRecognitionProgressDialog.dismiss();
        if (visionText.getTextBlocks().isEmpty()) {
            Toast.makeText(context, "No Text was Found", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<String> strings = new ArrayList<>();
        for (Text.TextBlock textBlock : visionText.getTextBlocks())
            strings.add(textBlock.getText());
        new MaterialAlertDialogBuilder(context, R.style.AppTheme_DialogTheme)
                .setTitle("Text Found")
                .setItems(strings.toArray(new CharSequence[0]), (dialogInterface, i) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
                    else
                        vibrator.vibrate(60);
                    ClipData clip = ClipData.newPlainText("Copied Text", strings.get(i));
                    clipboardManager.setPrimaryClip(clip);
                    Toast.makeText(context, "Text Copied", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
