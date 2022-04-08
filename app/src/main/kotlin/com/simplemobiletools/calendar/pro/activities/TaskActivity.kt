package com.simplemobiletools.calendar.pro.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.dialogs.SelectEventTypeDialog
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import kotlinx.android.synthetic.main.activity_task.*
import org.joda.time.DateTime
import java.util.*

class TaskActivity : SimpleActivity() {
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
        updateColors()
        val taskId = intent.getLongExtra(EVENT_ID, 0L)
        ensureBackgroundThread {
            val task = eventsDB.getTaskWithId(taskId)
            if (taskId != 0L && task == null) {
                hideKeyboard()
                finish()
                return@ensureBackgroundThread
            }

            runOnUiThread {
                if (!isDestroyed && !isFinishing) {
                    gotTask(savedInstanceState, task)
                }
            }
        }
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
        mEventTypeId = if (config.defaultEventTypeId == -1L) config.lastUsedLocalEventTypeId else config.defaultEventTypeId

        if (task != null) {
            mTask = task

            if (intent.getBooleanExtra(IS_DUPLICATE_INTENT, false)) {
                mTask.id = null
                updateActionBarTitle(getString(R.string.new_task))
            }

            if (savedInstanceState == null) {
                setupEditTask()
            }
        } else {
            mTask = Event(null)
            if (savedInstanceState == null) {
                setupNewTask()
            }
        }

        task_all_day.setOnCheckedChangeListener { compoundButton, isChecked -> toggleAllDay(isChecked) }
        task_all_day_holder.setOnClickListener {
            task_all_day.toggle()
        }

        task_date.setOnClickListener { setupDate() }
        task_time.setOnClickListener { setupTime() }
        event_type_holder.setOnClickListener { showEventTypeDialog() }

        if (savedInstanceState == null) {
            updateEventType()
            updateDateText()
            updateTimeText()
        }
    }

    private fun setupEditTask() {
        mTaskDateTime = Formatter.getDateTimeFromTS(mTask.startTS)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        updateActionBarTitle(getString(R.string.edit_task))

        mEventTypeId = mTask.eventType
        task_title.setText(mTask.title)
        task_description.setText(mTask.description)
        task_all_day.isChecked = mTask.getIsAllDay()
        toggleAllDay(mTask.getIsAllDay())
        setupMarkCompleteButton()
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

    private fun deleteTask() {
        ConfirmationDialog(this) {
            ensureBackgroundThread {
                eventsHelper.deleteEvent(mTask.id!!, false)

                runOnUiThread {
                    hideKeyboard()
                    finish()
                }
            }
        }
    }

    private fun duplicateTask() {
        // the activity has the singleTask launchMode to avoid some glitches, so finish it before relaunching
        hideKeyboard()
        finish()
        Intent(this, TaskActivity::class.java).apply {
            putExtra(EVENT_ID, mTask.id)
            putExtra(IS_DUPLICATE_INTENT, true)
            startActivity(this)
        }
    }

    private fun setupDate() {
        hideKeyboard()
        val datepicker = DatePickerDialog(
            this, getDatePickerDialogTheme(), dateSetListener, mTaskDateTime.year, mTaskDateTime.monthOfYear - 1, mTaskDateTime.dayOfMonth
        )

        datepicker.datePicker.firstDayOfWeek = if (config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        datepicker.show()
    }

    private fun setupTime() {
        hideKeyboard()
        TimePickerDialog(
            this, getTimePickerDialogTheme(), timeSetListener, mTaskDateTime.hourOfDay, mTaskDateTime.minuteOfHour, config.use24HourFormat
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

    private fun setupMarkCompleteButton() {
        toggle_mark_complete.setOnClickListener { toggleCompletion() }
        toggle_mark_complete.beVisibleIf(mTask.id != null)
        if (mTask.isTaskCompleted()) {
            toggle_mark_complete.background = ContextCompat.getDrawable(this, R.drawable.button_background_stroke)
            toggle_mark_complete.setText(R.string.mark_incomplete)
            toggle_mark_complete.setTextColor(getProperTextColor())
        } else {
            val markCompleteBgColor = if (isWhiteTheme()) {
                Color.WHITE
            } else {
                getProperPrimaryColor()
            }
            toggle_mark_complete.setTextColor(markCompleteBgColor.getContrastColor())
        }
    }

    private fun toggleCompletion() {
        if (mTask.isTaskCompleted()) {
            mTask.flags = mTask.flags.removeBit(FLAG_TASK_COMPLETED)
        } else {
            mTask.flags = mTask.flags or FLAG_TASK_COMPLETED
        }

        ensureBackgroundThread {
            eventsDB.updateTaskCompletion(mTask.id!!, mTask.flags)
            hideKeyboard()
            finish()
        }
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
                    event_type_color.setFillWithStroke(eventType.color, getProperBackgroundColor())
                }
            }
        }
    }

    private fun updateColors() {
        updateTextColors(task_scrollview)
        task_time_image.applyColorFilter(getProperTextColor())
        event_type_image.applyColorFilter(getProperTextColor())
    }
}
