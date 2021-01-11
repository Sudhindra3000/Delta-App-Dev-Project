@file:JvmName("GsonUtil")

package com.sudhindra.deltaappdevproject.utils

import com.google.gson.Gson

private val gson = Gson()

/**
 * Extension Functions to Convert Objects to Strings and vice versa using Gson
 */

fun Any.toJson(): String = gson.toJson(this)

fun <T> String.fromJson(classOfT: Class<T>?): T = gson.fromJson(this, classOfT)
