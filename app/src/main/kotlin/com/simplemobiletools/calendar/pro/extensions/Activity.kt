package com.simplemobiletools.calendar.pro.extensions

import android.app.Activity
import com.simplemobiletools.calendar.pro.BuildConfig
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.dialogs.CustomEventRepeatIntervalDialog
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.RadioItem
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

fun BaseSimpleActivity.shareEvents(ids: List<Long>) {
    ensureBackgroundThread {
        val file = getTempFile()
        if (file == null) {
            toast(R.string.unknown_error_occurred)
            return@ensureBackgroundThread
        }

        val events = eventsDB.getEventsWithIds(ids) as ArrayList<Event>
        getFileOutputStream(file.toFileDirItem(this), true) {
            IcsExporter().exportEvents(this, it, events, false) {
                if (it == IcsExporter.ExportResult.EXPORT_OK) {
                    sharePathIntent(file.absolutePath, BuildConfig.APPLICATION_ID)
                }
            }
        }
    }
}

fun BaseSimpleActivity.getTempFile(): File? {
    val folder = File(cacheDir, "events")
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, "events.ics")
}

fun Activity.showEventRepeatIntervalDialog(curSeconds: Int, callback: (minutes: Int) -> Unit) {
    hideKeyboard()
    val seconds = TreeSet<Int>()
    seconds.apply {
        add(0)
        add(DAY)
        add(WEEK)
        add(MONTH)
        add(YEAR)
        add(curSeconds)
    }

    val items = ArrayList<RadioItem>(seconds.size + 1)
    seconds.mapIndexedTo(items) { index, value ->
        RadioItem(index, getRepetitionText(value), value)
    }

    var selectedIndex = 0
    seconds.forEachIndexed { index, value ->
        if (value == curSeconds)
            selectedIndex = index
    }

    items.add(RadioItem(-1, getString(R.string.custom)))

    RadioGroupDialog(this, items, selectedIndex) {
        if (it == -1) {
            CustomEventRepeatIntervalDialog(this) {
                callback(it)
            }
        } else {
            callback(it as Int)
        }
    }
}
