package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.app.DatePickerDialog
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.getNowSeconds
import com.simplemobiletools.commons.extensions.getDialogTheme
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.value
import kotlinx.android.synthetic.main.dialog_repeat_limit_type_picker.view.*
import org.joda.time.DateTime
import java.util.*

class RepeatLimitTypePickerDialog(val activity: Activity, var repeatLimit: Long, val startTS: Long, val callback: (repeatLimit: Long) -> Unit) {
    lateinit var dialog: AlertDialog
    var view: View

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_repeat_limit_type_picker, null).apply {
            repeat_type_date.setOnClickListener { showRepetitionLimitDialog() }
            repeat_type_count.setOnClickListener { dialog_radio_view.check(R.id.repeat_type_x_times) }
            repeat_type_forever.setOnClickListener {
                callback(0)
                dialog.dismiss()
            }
        }

        view.dialog_radio_view.check(getCheckedItem())

        if (repeatLimit in 1..startTS) {
            repeatLimit = startTS
        }

        updateRepeatLimitText()

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok) { dialogInterface, i -> confirmRepetition() }
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this) {
                        activity.currentFocus?.clearFocus()
                    }
                }
    }

    private fun getCheckedItem() = when {
        repeatLimit > 0 -> R.id.repeat_type_till_date
        repeatLimit < 0 -> {
            view.repeat_type_count.setText((-repeatLimit).toString())
            R.id.repeat_type_x_times
        }
        else -> R.id.repeat_type_forever
    }

    private fun updateRepeatLimitText() {
        if (repeatLimit <= 0)
            repeatLimit = getNowSeconds()

        val repeatLimitDateTime = Formatter.getDateTimeFromTS(repeatLimit)
        view.repeat_type_date.text = Formatter.getFullDate(activity, repeatLimitDateTime)
    }

    private fun confirmRepetition() {
        when (view.dialog_radio_view.checkedRadioButtonId) {
            R.id.repeat_type_till_date -> callback(repeatLimit)
            R.id.repeat_type_forever -> callback(0)
            else -> {
                var count = view.repeat_type_count.value
                count = if (count.isEmpty()) {
                    "0"
                } else {
                    "-$count"
                }
                callback(count.toLong())
            }
        }
        dialog.dismiss()
    }

    private fun showRepetitionLimitDialog() {
        val repeatLimitDateTime = Formatter.getDateTimeFromTS(if (repeatLimit != 0L) repeatLimit else getNowSeconds())
        val datepicker = DatePickerDialog(activity, activity.getDialogTheme(), repetitionLimitDateSetListener, repeatLimitDateTime.year,
                repeatLimitDateTime.monthOfYear - 1, repeatLimitDateTime.dayOfMonth)

        datepicker.datePicker.firstDayOfWeek = if (activity.config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        datepicker.show()
    }

    private val repetitionLimitDateSetListener = DatePickerDialog.OnDateSetListener { v, year, monthOfYear, dayOfMonth ->
        val repeatLimitDateTime = DateTime().withDate(year, monthOfYear + 1, dayOfMonth).withTime(23, 59, 59, 0)
        repeatLimit = if (repeatLimitDateTime.seconds() < startTS) {
            0
        } else {
            repeatLimitDateTime.seconds()
        }
        callback(repeatLimit)
        dialog.dismiss()
    }
}
