package com.simplemobiletools.calendar.activities

import android.os.Bundle
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.dialogs.CustomEventReminderDialog
import com.simplemobiletools.calendar.extensions.getReminderText
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.models.RadioItem
import java.util.TreeSet
import kotlin.collections.ArrayList

open class SimpleActivity : BaseSimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun showEventReminderDialog(curMinutes: Int, callback: (minutes: Int) -> Unit) {
        val minutes = TreeSet<Int>()
        minutes.apply {
            add(-1)
            add(0)
            add(10)
            add(30)
            add(curMinutes)
        }

        val items = ArrayList<RadioItem>(minutes.size + 1)
        minutes.mapIndexedTo(items, {
            index, value ->
            RadioItem(index, getReminderText(value), value)
        })

        var selectedIndex = 0
        minutes.forEachIndexed { index, value ->
            if (value == curMinutes)
                selectedIndex = index
        }

        items.add(RadioItem(-2, getString(R.string.custom)))

        RadioGroupDialog(this, items, selectedIndex) {
            if (it == -2) {
                CustomEventReminderDialog(this) {
                    callback(it)
                }
            } else {
                callback(it as Int)
            }
        }
    }
}
