package com.simplemobiletools.calendar.helpers

import android.content.Context
import com.simplemobiletools.calendar.helpers.IcsExporter.ExportResult.*

class IcsExporter {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    var eventsExported = 0
    var eventsFailed = 0

    fun exportEvents(context: Context, path: String): ExportResult {

        return if (eventsExported == 0) {
            EXPORT_FAIL
        } else if (eventsFailed > 0) {
            EXPORT_PARTIAL
        } else {
            EXPORT_OK
        }
    }
}
