package com.angel.sentryhouse.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun createImageUri(context: Context): Uri {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File.createTempFile("IMG_${timestamp}_", ".jpg", context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}
