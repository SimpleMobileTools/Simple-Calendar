package com.simplemobiletools.calendar.pro.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.WindowManager
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.TASK_ID
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.activity_task.*
import org.joda.time.DateTime
import java.util.*

class TaskActivity : SimpleActivity() {
    private var mDialogTheme = 0
    private lateinit var mTaskDateTime: DateTime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        if (checkAppSideloading()) {
            return
        }

        val intent = intent ?: return
        mDialogTheme = getDialogTheme()
        val taskId = intent.getLongExtra(TASK_ID, 0L)
        mTaskDateTime = DateTime.now()
        updateColors()
        gotTask()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu, true)
        return true
    }

    private fun gotTask() {
        task_all_day.setOnCheckedChangeListener { compoundButton, isChecked -> toggleAllDay(isChecked) }
        task_all_day_holder.setOnClickListener {
            task_all_day.toggle()
        }

        task_date.setOnClickListener { setupDate() }
        task_time.setOnClickListener { setupTime() }

        setupNewTask()
        updateDateText()
        updateTimeText()
    }

    private fun setupNewTask() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        task_title.requestFocus()
        updateActionBarTitle(getString(R.string.new_task))
    }

    private fun setupDate() {
        hideKeyboard()
        val datepicker = DatePickerDialog(
            this, mDialogTheme, dateSetListener, mTaskDateTime.year, mTaskDateTime.monthOfYear - 1, mTaskDateTime.dayOfMonth
        )

        datepicker.datePicker.firstDayOfWeek = if (config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        datepicker.show()
    }

    private fun setupTime() {
        hideKeyboard()
        TimePickerDialog(
            this, mDialogTheme, timeSetListener, mTaskDateTime.hourOfDay, mTaskDateTime.minuteOfHour, config.use24HourFormat
        ).show()
    }

    private val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
        dateSet(year, monthOfYear, dayOfMonth)
    }

    private val timeSetListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
        timeSet(hourOfDay, minute)
    }

    private fun dateSet(year: Int, month: Int, day: Int) {
        mTaskDateTime = mTaskDateTime.withDate(year, month + 1, day)
        updateDateText()
    }

    private fun timeSet(hours: Int, minutes: Int) {
        mTaskDateTime = mTaskDateTime.withHourOfDay(hours).withMinuteOfHour(minutes)
        updateTimeText()
    }

    private fun updateDateText() {
        task_date.text = Formatter.getDate(this, mTaskDateTime)
    }

    private fun updateTimeText() {
        task_time.text = Formatter.getTime(this, mTaskDateTime)
    }

    private fun toggleAllDay(isChecked: Boolean) {
        hideKeyboard()
        task_time.beGoneIf(isChecked)
    }

    private fun updateColors() {
        updateTextColors(task_scrollview)
        task_time_image.applyColorFilter(config.textColor)
    }
}
