@file:JvmName("ShareUtil")

package com.sudhindra.deltaappdevproject.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.sudhindra.deltaappdevproject.R
import com.sudhindra.deltaappdevproject.models.Post
import java.io.File
import java.io.FileOutputStream

/**
 * Utility Functions to Share a Post/MessageFile from Activity/Fragment
 */

fun Context.sharePost(post: Post, head: String, body: String, imageView: ImageView) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.putExtra(Intent.EXTRA_TEXT, "$head \n $body")
    if (post.isImagePost) {
        try {
            val bitmap: Bitmap = (imageView.drawable as BitmapDrawable).bitmap
            val file = File(externalCacheDir, post.postedTimeInMillis.toString() + ".jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", file))
            intent.type = "image/png"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else intent.type = "text/plain"
    startActivity(Intent.createChooser(intent, resources.getString(R.string.sharePostTile)))
}

fun Fragment.sharePost(post: Post, head: String, body: String, imageView: ImageView) =
        requireContext().sharePost(post, head, body, imageView)

fun Context.shareMessageFile(fileType: String?, file: File?) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", file!!))
    intent.type = fileType
    startActivity(Intent.createChooser(intent, resources.getString(R.string.shareMessageFileTitle)))
}
