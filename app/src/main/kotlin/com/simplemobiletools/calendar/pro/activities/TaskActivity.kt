package com.simplemobiletools.calendar.pro.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.dialogs.ReminderWarningDialog
import com.simplemobiletools.calendar.pro.dialogs.SelectEventTypeDialog
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.Reminder
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
    private var mIsAllDayEvent = false

    private var mReminder1Minutes = REMINDER_OFF
    private var mReminder2Minutes = REMINDER_OFF
    private var mReminder3Minutes = REMINDER_OFF
    private var mReminder1Type = REMINDER_NOTIFICATION
    private var mReminder2Type = REMINDER_NOTIFICATION
    private var mReminder3Type = REMINDER_NOTIFICATION

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

            putInt(REMINDER_1_MINUTES, mReminder1Minutes)
            putInt(REMINDER_2_MINUTES, mReminder2Minutes)
            putInt(REMINDER_3_MINUTES, mReminder3Minutes)
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

            mReminder1Minutes = getInt(REMINDER_1_MINUTES)
            mReminder2Minutes = getInt(REMINDER_2_MINUTES)
            mReminder3Minutes = getInt(REMINDER_3_MINUTES)
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
            config.apply {
                mReminder1Minutes = if (usePreviousEventReminders && lastEventReminderMinutes1 >= -1) lastEventReminderMinutes1 else defaultReminder1
                mReminder2Minutes = if (usePreviousEventReminders && lastEventReminderMinutes2 >= -1) lastEventReminderMinutes2 else defaultReminder2
                mReminder3Minutes = if (usePreviousEventReminders && lastEventReminderMinutes3 >= -1) lastEventReminderMinutes3 else defaultReminder3
            }

            if (savedInstanceState == null) {
                setupNewTask()
            }
        }

        task_all_day.setOnCheckedChangeListener { _, isChecked -> toggleAllDay(isChecked) }
        task_all_day_holder.setOnClickListener {
            task_all_day.toggle()
        }

        task_date.setOnClickListener { setupDate() }
        task_time.setOnClickListener { setupTime() }
        event_type_holder.setOnClickListener { showEventTypeDialog() }

        event_reminder_1.setOnClickListener {
            handleNotificationAvailability {
                if (config.wasAlarmWarningShown) {
                    showReminder1Dialog()
                } else {
                    ReminderWarningDialog(this) {
                        config.wasAlarmWarningShown = true
                        showReminder1Dialog()
                    }
                }
            }
        }

        event_reminder_2.setOnClickListener { showReminder2Dialog() }
        event_reminder_3.setOnClickListener { showReminder3Dialog() }

        if (savedInstanceState == null) {
            updateEventType()
            updateDateText()
            updateTimeText()
            updateReminderTexts()
        }
    }

    private fun setupEditTask() {
        mTaskDateTime = Formatter.getDateTimeFromTS(mTask.startTS)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        updateActionBarTitle(getString(R.string.edit_task))

        mEventTypeId = mTask.eventType
        mReminder1Minutes = mTask.reminder1Minutes
        mReminder2Minutes = mTask.reminder2Minutes
        mReminder3Minutes = mTask.reminder3Minutes
        mReminder1Type = mTask.reminder1Type
        mReminder2Type = mTask.reminder2Type
        mReminder3Type = mTask.reminder3Type

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


        val reminders = getReminders()
        if (!task_all_day.isChecked) {
            if ((reminders.getOrNull(2)?.minutes ?: 0) < -1) {
                reminders.removeAt(2)
            }

            if ((reminders.getOrNull(1)?.minutes ?: 0) < -1) {
                reminders.removeAt(1)
            }

            if ((reminders.getOrNull(0)?.minutes ?: 0) < -1) {
                reminders.removeAt(0)
            }
        }

        val reminder1 = reminders.getOrNull(0) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)
        val reminder2 = reminders.getOrNull(1) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)
        val reminder3 = reminders.getOrNull(2) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)

        config.apply {
            if (usePreviousEventReminders) {
                lastEventReminderMinutes1 = reminder1.minutes
                lastEventReminderMinutes2 = reminder2.minutes
                lastEventReminderMinutes3 = reminder3.minutes
            }
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

            reminder1Minutes = mReminder1Minutes
            reminder1Type = mReminder1Type
            reminder2Minutes = mReminder2Minutes
            reminder2Type = mReminder2Type
            reminder3Minutes = mReminder3Minutes
            reminder3Type = mReminder3Type
        }

        ensureBackgroundThread {
            EventsHelper(this).insertTask(mTask, true) {
                hideKeyboard()

                if (DateTime.now().isAfter(mTaskDateTime.millis)) {
                    if (mTask.repeatInterval == 0 && mTask.getReminders().any { it.type == REMINDER_NOTIFICATION }) {
                        notifyEvent(mTask)
                    }
                }

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
        val datePicker = DatePickerDialog(
            this, getDatePickerDialogTheme(), dateSetListener, mTaskDateTime.year, mTaskDateTime.monthOfYear - 1, mTaskDateTime.dayOfMonth
        )

        datePicker.datePicker.firstDayOfWeek = if (config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        datePicker.show()
    }

    private fun setupTime() {
        hideKeyboard()
        TimePickerDialog(
            this, getTimePickerDialogTheme(), timeSetListener, mTaskDateTime.hourOfDay, mTaskDateTime.minuteOfHour, config.use24HourFormat
        ).show()
    }

    private val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
        dateSet(year, monthOfYear, dayOfMonth)
    }

    private val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
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

    private fun updateReminderTexts() {
        updateReminder1Text()
        updateReminder2Text()
        updateReminder3Text()
    }

    private fun updateReminder1Text() {
        event_reminder_1.text = getFormattedMinutes(mReminder1Minutes)
    }

    private fun updateReminder2Text() {
        event_reminder_2.apply {
            beGoneIf(event_reminder_2.isGone() && mReminder1Minutes == REMINDER_OFF)
            if (mReminder2Minutes == REMINDER_OFF) {
                text = resources.getString(R.string.add_another_reminder)
                alpha = 0.4f
            } else {
                text = getFormattedMinutes(mReminder2Minutes)
                alpha = 1f
            }
        }
    }

    private fun updateReminder3Text() {
        event_reminder_3.apply {
            beGoneIf(event_reminder_3.isGone() && (mReminder2Minutes == REMINDER_OFF || mReminder1Minutes == REMINDER_OFF))
            if (mReminder3Minutes == REMINDER_OFF) {
                text = resources.getString(R.string.add_another_reminder)
                alpha = 0.4f
            } else {
                text = getFormattedMinutes(mReminder3Minutes)
                alpha = 1f
            }
        }
    }

    private fun handleNotificationAvailability(callback: () -> Unit) {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            callback()
        } else {
            ConfirmationDialog(this, messageId = R.string.notifications_disabled, positive = R.string.ok, negative = 0) {
                callback()
            }
        }
    }

    private fun showReminder1Dialog() {
        showPickSecondsDialogHelper(mReminder1Minutes, showDuringDayOption = mIsAllDayEvent) {
            mReminder1Minutes = if (it == -1 || it == 0) it else it / 60
            updateReminderTexts()
        }
    }

    private fun showReminder2Dialog() {
        showPickSecondsDialogHelper(mReminder2Minutes, showDuringDayOption = mIsAllDayEvent) {
            mReminder2Minutes = if (it == -1 || it == 0) it else it / 60
            updateReminderTexts()
        }
    }

    private fun showReminder3Dialog() {
        showPickSecondsDialogHelper(mReminder3Minutes, showDuringDayOption = mIsAllDayEvent) {
            mReminder3Minutes = if (it == -1 || it == 0) it else it / 60
            updateReminderTexts()
        }
    }

    private fun getReminders(): ArrayList<Reminder> {
        var reminders = arrayListOf(
            Reminder(mReminder1Minutes, mReminder1Type),
            Reminder(mReminder2Minutes, mReminder2Type),
            Reminder(mReminder3Minutes, mReminder3Type)
        )
        reminders = reminders.filter { it.minutes != REMINDER_OFF }.sortedBy { it.minutes }.toMutableList() as ArrayList<Reminder>
        return reminders
    }

    private fun showEventTypeDialog() {
        hideKeyboard()
        SelectEventTypeDialog(
            activity = this,
            currEventType = mEventTypeId,
            showCalDAVCalendars = false,
            showNewEventTypeOption = true,
            addLastUsedOneAsFirstOption = false,
            showOnlyWritable = true
        ) {
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
        val textColor = getProperTextColor()
        arrayOf(
            task_time_image, event_reminder_image, event_type_image
        ).forEach {
            it.applyColorFilter(textColor)
        }
    }
}
