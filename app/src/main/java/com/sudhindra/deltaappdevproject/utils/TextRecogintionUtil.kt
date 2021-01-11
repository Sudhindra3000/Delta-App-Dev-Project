@file:JvmName("TextRecognitionUtil")

package com.sudhindra.deltaappdevproject.utils

import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.sudhindra.deltaappdevproject.R
import java.util.*

/**
 * Utility Functions to Process Image for TextRecognition from Activity/Fragment
 */

private lateinit var vibrator: Vibrator
private lateinit var clipboardManager: ClipboardManager

private val textRecognizer = TextRecognition.getClient()
private lateinit var progressDialog: ProgressDialog

fun Context.processImage(imageView: ImageView) {
    vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    progressDialog = ProgressDialog(this, R.style.AppTheme_ProgressDialogTheme)
    progressDialog.setMessage("Processing...")
    progressDialog.setCancelable(false)
    progressDialog.setCanceledOnTouchOutside(false)
    progressDialog.show()
    val inputImage = InputImage.fromBitmap((imageView.drawable as BitmapDrawable).bitmap, 0)
    textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText: Text -> showRecognizedText(visionText) }
            .addOnFailureListener {
                it.printStackTrace()
                Toast.makeText(this, "Failed to Process Image", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
}

fun Fragment.processImage(imageView: ImageView) {
    requireContext().processImage(imageView)
}

private fun Context.showRecognizedText(visionText: Text) {
    progressDialog.dismiss()
    if (visionText.textBlocks.isEmpty()) {
        Toast.makeText(this, "No Text was Found", Toast.LENGTH_SHORT).show()
        return
    }
    val strings = ArrayList<String>()
    for (textBlock in visionText.textBlocks) strings.add(textBlock.text)
    MaterialAlertDialogBuilder(this, R.style.AppTheme_DialogTheme)
            .setTitle("Text Found")
            .setItems(strings.toTypedArray<CharSequence>()) { _: DialogInterface?, i: Int ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)) else vibrator.vibrate(60)
                val clip = ClipData.newPlainText("Copied Text", strings[i])
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(this, "Text Copied", Toast.LENGTH_SHORT).show()
            }.show()
}
