package com.simplemobiletools.calendar.pro.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.dialogs.SelectEventTypeDialog
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.eventTypesDB
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import kotlinx.android.synthetic.main.activity_task.*
import org.joda.time.DateTime
import java.util.*

class TaskActivity : SimpleActivity() {
    private var mDialogTheme = 0
    private var mEventTypeId = REGULAR_EVENT_TYPE_ID
    private lateinit var mTaskDateTime: DateTime
    private lateinit var mTask: Event

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        if (checkAppSideloading()) {
            return
        }

        val intent = intent ?: return
        mDialogTheme = getDialogTheme()
        updateColors()
        val taskId = intent.getLongExtra(TASK_ID, 0L)
        gotTask(savedInstanceState, null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_task, menu)
        if (::mTask.isInitialized) {
            menu.findItem(R.id.delete).isVisible = mTask.id != null
            menu.findItem(R.id.duplicate).isVisible = mTask.id != null
        }

        updateMenuItemColors(menu, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save -> saveTask()
            R.id.delete -> deleteTask()
            R.id.duplicate -> duplicateTask()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!::mTask.isInitialized) {
            return
        }

        outState.apply {
            putSerializable(TASK, mTask)
            putLong(START_TS, mTaskDateTime.seconds())
            putLong(EVENT_TYPE_ID, mEventTypeId)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (!savedInstanceState.containsKey(START_TS)) {
            hideKeyboard()
            finish()
            return
        }

        savedInstanceState.apply {
            mTask = getSerializable(TASK) as Event
            mTaskDateTime = Formatter.getDateTimeFromTS(getLong(START_TS))
            mEventTypeId = getLong(EVENT_TYPE_ID)
        }

        updateEventType()
        updateDateText()
        updateTimeText()
    }

    private fun gotTask(savedInstanceState: Bundle?, task: Event?) {
        if (task != null) {
            mTask = task
        } else {
            mTask = Event(null)
        }

        mEventTypeId = if (config.defaultEventTypeId == -1L) config.lastUsedLocalEventTypeId else config.defaultEventTypeId

        task_all_day.setOnCheckedChangeListener { compoundButton, isChecked -> toggleAllDay(isChecked) }
        task_all_day_holder.setOnClickListener {
            task_all_day.toggle()
        }

        task_date.setOnClickListener { setupDate() }
        task_time.setOnClickListener { setupTime() }
        event_type_holder.setOnClickListener { showEventTypeDialog() }

        setupNewTask()

        if (savedInstanceState == null) {
            updateEventType()
            updateDateText()
            updateTimeText()
        }
    }

    private fun setupNewTask() {
        val startTS = intent.getLongExtra(NEW_EVENT_START_TS, 0L)
        val dateTime = Formatter.getDateTimeFromTS(startTS)
        mTaskDateTime = dateTime

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        task_title.requestFocus()
        updateActionBarTitle(getString(R.string.new_task))
    }

    private fun saveTask() {
        val newTitle = task_title.value
        if (newTitle.isEmpty()) {
            toast(R.string.title_empty)
            runOnUiThread {
                task_title.requestFocus()
            }
            return
        }

        config.lastUsedLocalEventTypeId = mEventTypeId
        mTask.apply {
            startTS = mTaskDateTime.withSecondOfMinute(0).withMillisOfSecond(0).seconds()
            endTS = startTS
            title = newTitle
            description = task_description.value
            flags = mTask.flags.addBitIf(task_all_day.isChecked, FLAG_ALL_DAY)
            lastUpdated = System.currentTimeMillis()
            eventType = mEventTypeId
            type = TYPE_TASK
        }

        ensureBackgroundThread {
            EventsHelper(this).insertTask(mTask) {
                hideKeyboard()
                finish()
            }
        }
    }

    private fun deleteTask() {}

    private fun duplicateTask() {}

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

    private fun showEventTypeDialog() {
        hideKeyboard()
        SelectEventTypeDialog(this, mEventTypeId, false, true, false, true) {
            mEventTypeId = it.id!!
            updateEventType()
        }
    }

    private fun updateEventType() {
        ensureBackgroundThread {
            val eventType = eventTypesDB.getEventTypeWithId(mEventTypeId)
            if (eventType != null) {
                runOnUiThread {
                    event_type.text = eventType.title
                    event_type_color.setFillWithStroke(eventType.color, config.backgroundColor)
                }
            }
        }
    }

    private fun updateColors() {
        updateTextColors(task_scrollview)
        task_time_image.applyColorFilter(config.textColor)
        event_type_image.applyColorFilter(config.textColor)
    }
}
