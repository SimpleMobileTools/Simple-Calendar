package com.simplemobiletools.calendar.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.support.v7.app.AlertDialog
import android.view.View
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.getAppropriateTheme
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.commons.extensions.isLollipopPlus
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.value
import kotlinx.android.synthetic.main.dialog_repeat_limit_type_picker.view.*
import org.joda.time.DateTime
import java.util.*

class RepeatLimitTypePickerDialog(val activity: Activity, var repeatLimit: Int, val startTS: Int, val callback: (repeatLimit: Int) -> Unit) :
        AlertDialog.Builder(activity) {
    lateinit var dialog: AlertDialog
    var view: View

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_repeat_limit_type_picker, null).apply {
            repeat_type_date.setOnClickListener { showRepetitionLimitDialog() }
            repeat_type_forever.setOnClickListener { callback(0); dialog.dismiss() }
            repeat_type_count.setOnClickListener { dialog_radio_view.check(R.id.repeat_type_x_times) }
        }

        view.dialog_radio_view.check(getCheckedItem())

        if (repeatLimit in 1..startTS)
            repeatLimit = startTS

        updateRepeatLimitText()

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialogInterface, i -> confirmRepetition() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this)
            activity.currentFocus?.clearFocus()
        }
    }

    private fun getCheckedItem(): Int {
        return if (repeatLimit > 0)
            R.id.repeat_type_till_date
        else if (repeatLimit < 0) {
            view.repeat_type_count.setText((-repeatLimit).toString())
            R.id.repeat_type_x_times
        } else
            R.id.repeat_type_forever
    }

    private fun updateRepeatLimitText() {
        if (repeatLimit <= 0)
            repeatLimit = (System.currentTimeMillis() / 1000).toInt()

        val repeatLimitDateTime = Formatter.getDateTimeFromTS(repeatLimit)
        view.repeat_type_date.text = Formatter.getFullDate(activity, repeatLimitDateTime)
    }

    private fun confirmRepetition() {
        when (view.dialog_radio_view.checkedRadioButtonId) {
            R.id.repeat_type_till_date -> callback(repeatLimit)
            R.id.repeat_type_forever -> callback(0)
            else -> {
                var count = view.repeat_type_count.value
                if (count.isEmpty())
                    count = "0"
                else
                    count = "-$count"
                callback(count.toInt())
            }
        }
        dialog.dismiss()
    }

    @SuppressLint("NewApi")
    private fun showRepetitionLimitDialog() {
        val now = (System.currentTimeMillis() / 1000).toInt()
        val repeatLimitDateTime = Formatter.getDateTimeFromTS(if (repeatLimit != 0) repeatLimit else now)
        val datepicker = DatePickerDialog(activity, activity.getAppropriateTheme(), repetitionLimitDateSetListener, repeatLimitDateTime.year,
                repeatLimitDateTime.monthOfYear - 1, repeatLimitDateTime.dayOfMonth)

        if (activity.isLollipopPlus()) {
            datepicker.datePicker.firstDayOfWeek = if (activity.config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        }

        datepicker.show()
    }

    private val repetitionLimitDateSetListener = DatePickerDialog.OnDateSetListener { v, year, monthOfYear, dayOfMonth ->
        val repeatLimitDateTime = DateTime().withDate(year, monthOfYear + 1, dayOfMonth).withTime(23, 59, 59, 0)
        if (repeatLimitDateTime.seconds() < startTS) {
            repeatLimit = 0
        } else {
            repeatLimit = repeatLimitDateTime.seconds()
        }
        callback(repeatLimit)
        dialog.dismiss()
    }
}
