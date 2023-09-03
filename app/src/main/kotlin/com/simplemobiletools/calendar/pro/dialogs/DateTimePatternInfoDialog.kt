package com.simplemobiletools.calendar.pro.dialogs

import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.databinding.DatetimePatternInfoLayoutBinding
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff

class DateTimePatternInfoDialog(activity: BaseSimpleActivity) {

    init {
        val view = DatetimePatternInfoLayoutBinding.inflate(activity.layoutInflater).root
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> { } }
            .apply {
                activity.setupDialogStuff(view, this)
            }
    }
}
