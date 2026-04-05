package com.kevlina.budgetplus.feature.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.export_csv_success
import com.kevlina.budgetplus.core.common.ActivityProvider
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.feature.overview.utils.CsvSaver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provider
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.invoke
import java.io.File
import java.io.IOException

@ContributesBinding(AppScope::class)
internal class CsvSaverImpl(
    private val context: Context,
    @Named("app_package") private val appPackage: String,
    @Named("share_cache") private val shareCacheDir: Provider<File>,
    private val activityProvider: ActivityProvider,
    private val snackbarSender: SnackbarSender,
) : CsvSaver {

    private val contentResolver get() = context.contentResolver

    override suspend fun saveToDownload(fileName: String, csvText: String) = IO {
        val activity = activityProvider.activityFlow.filterNotNull().first()
        val cacheFile = File(shareCacheDir(), "$fileName.csv")
        val outputStream = contentResolver.openOutputStream(cacheFile.toUri())
            ?: throw IOException("Cannot open output stream from $cacheFile")

        outputStream.use { stream ->
            stream.write(csvText.toByteArray())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToDownload(cacheFile)
        } else {
            saveToDownloadPreQ(cacheFile)
        }
        snackbarSender.send(Res.string.export_csv_success)

        val fileUri = FileProvider.getUriForFile(context, "$appPackage.provider", cacheFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = CsvSaver.MimeType
        }
        activity.startActivity(Intent.createChooser(intent, null))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToDownload(cacheFile: File): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, cacheFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, CsvSaver.MimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val mediaUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Cannot resolve ContentValues. $contentValues")

        cacheFile.inputStream().use { input ->
            resolver.openOutputStream(mediaUri)
                ?.use { output -> input.copyTo(output) }
                ?: throw IOException("Cannot open output stream from $mediaUri")
        }

        return mediaUri
    }

    private fun saveToDownloadPreQ(cacheFile: File): Uri {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destFile = File(dir, cacheFile.name)

        cacheFile.copyTo(target = destFile, overwrite = true)
        return destFile.toUri()
    }
}