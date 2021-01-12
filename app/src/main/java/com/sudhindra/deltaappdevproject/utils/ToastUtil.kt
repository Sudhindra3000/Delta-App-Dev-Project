@file:JvmName("ToastUtil")

package com.sudhindra.deltaappdevproject.utils

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * Extension Functions to Make Toast Messages from Activity/Fragment
 */

fun Context.toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

fun Fragment.toast(text: String) = requireContext().toast(text)
