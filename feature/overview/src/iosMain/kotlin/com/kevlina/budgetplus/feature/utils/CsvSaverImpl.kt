package com.kevlina.budgetplus.feature.utils

import com.kevlina.budgetplus.feature.overview.utils.CsvSaver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

@ContributesBinding(AppScope::class)
internal class CsvSaverImpl : CsvSaver {

    override suspend fun saveToDownload(fileName: String, csvText: String) {
        val tempDir = NSTemporaryDirectory()
        val fileUrl = NSURL.fileURLWithPath(tempDir).URLByAppendingPathComponent("$fileName.csv")!!

        withContext(Dispatchers.IO) {
            NSFileManager.defaultManager.createFileAtPath(
                path = fileUrl.path!!,
                contents = csvText.encodeToByteArray().toNSData(),
                attributes = null
            )
        }

        withContext(Dispatchers.Main) {
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: return@withContext

            val activityViewController = UIActivityViewController(
                activityItems = listOf(fileUrl),
                applicationActivities = null
            )

            activityViewController.popoverPresentationController?.let {
                it.sourceView = rootViewController.view
                it.sourceRect = rootViewController.view.bounds
                it.permittedArrowDirections = 0u
            }

            rootViewController.presentViewController(
                viewControllerToPresent = activityViewController,
                animated = true,
                completion = null
            )
        }
    }

    @OptIn(BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}