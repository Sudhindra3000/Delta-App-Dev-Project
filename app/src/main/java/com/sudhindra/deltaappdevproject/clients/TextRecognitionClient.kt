package com.sudhindra.deltaappdevproject.clients

import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.sudhindra.deltaappdevproject.R
import java.util.*

class TextRecognitionClient(val context: Context) {

    private var vibrator: Vibrator? = null
    private var clipboardManager: ClipboardManager? = null
    private val textRecognizer = TextRecognition.getClient()
    private var textRecognitionProgressDialog: ProgressDialog? = null

    fun processImage(imageView: ImageView) {
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        textRecognitionProgressDialog = ProgressDialog(context, R.style.AppTheme_ProgressDialogTheme)
        textRecognitionProgressDialog!!.setMessage("Processing...")
        textRecognitionProgressDialog!!.setCancelable(false)
        textRecognitionProgressDialog!!.setCanceledOnTouchOutside(false)
        textRecognitionProgressDialog!!.show()
        val inputImage = InputImage.fromBitmap((imageView.drawable as BitmapDrawable).bitmap, 0)
        textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText: Text -> showRecognizedText(visionText) }
                .addOnFailureListener { e: Exception ->
                    Log.i(TAG, "processImage: " + e.message)
                    Toast.makeText(context, "Failed to Process Image", Toast.LENGTH_SHORT).show()
                    textRecognitionProgressDialog!!.dismiss()
                }
    }

    private fun showRecognizedText(visionText: Text) {
        textRecognitionProgressDialog!!.dismiss()
        if (visionText.textBlocks.isEmpty()) {
            Toast.makeText(context, "No Text was Found", Toast.LENGTH_SHORT).show()
            return
        }
        val strings = ArrayList<String>()
        for (textBlock in visionText.textBlocks) strings.add(textBlock.text)
        MaterialAlertDialogBuilder(context, R.style.AppTheme_DialogTheme)
                .setTitle("Text Found")
                .setItems(strings.toTypedArray<CharSequence>()) { dialogInterface: DialogInterface?, i: Int ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator!!.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)) else vibrator!!.vibrate(60)
                    val clip = ClipData.newPlainText("Copied Text", strings[i])
                    clipboardManager!!.setPrimaryClip(clip)
                    Toast.makeText(context, "Text Copied", Toast.LENGTH_SHORT).show()
                }
                .show()
    }

    companion object {
        private val TAG = "TextRecognitionClient"
    }
}