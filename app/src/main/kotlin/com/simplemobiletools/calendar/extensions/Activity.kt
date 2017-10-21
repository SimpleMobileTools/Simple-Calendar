package com.simplemobiletools.calendar.extensions

import android.app.Activity
import android.content.Intent
import android.support.v4.content.FileProvider
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.helpers.IcsExporter
import com.simplemobiletools.commons.extensions.toast
import java.io.File

fun SimpleActivity.shareEvents(ids: List<Int>) {
    val file = getTempFile()
    if (file == null) {
        toast(R.string.unknown_error_occurred)
        return
    }

    val events = dbHelper.getEventsWithIds(ids)
    IcsExporter().exportEvents(this, file, events) {
        if (it == IcsExporter.ExportResult.EXPORT_OK) {
            val uri = FileProvider.getUriForFile(this, "com.simplemobiletools.calendar.fileprovider", file)
            val shareTitle = resources.getString(R.string.share_via)
            Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(uri, contentResolver.getType(uri))
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/calendar"

                if (resolveActivity(packageManager) != null) {
                    startActivity(Intent.createChooser(this, shareTitle))
                } else {
                    toast(R.string.no_app_for_ics)
                }
            }
        }
    }
}

fun Activity.getTempFile(): File? {
    val folder = File(cacheDir, "events")
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, "events.ics")
}
