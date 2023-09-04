package com.simplemobiletools.calendar.pro.dialogs

import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.databinding.DatetimePatternInfoLayoutBinding
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.viewBinding

class DateTimePatternInfoDialog(activity: BaseSimpleActivity) {
    val binding by activity.viewBinding(DatetimePatternInfoLayoutBinding::inflate)

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> { } }
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }
}
