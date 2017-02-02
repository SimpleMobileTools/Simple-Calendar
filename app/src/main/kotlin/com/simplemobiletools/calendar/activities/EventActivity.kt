package com.simplemobiletools.calendar.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.getAppropriateTheme
import com.simplemobiletools.calendar.extensions.scheduleNotification
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.activity_event.*
import org.joda.time.DateTime

class EventActivity : SimpleActivity(), DBHelper.EventUpdateListener {
    private var mWasReminderInit = false
    private var mWasEndDateSet = false
    private var mWasEndTimeSet = false
    private var mDialogTheme = 0

    lateinit var mEventStartDateTime: DateTime
    lateinit var mEventEndDateTime: DateTime
    lateinit var mEvent: Event

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event)

        val intent = intent ?: return
        mDialogTheme = getAppropriateTheme()

        mWasReminderInit = false
        val eventId = intent.getIntExtra(EVENT_ID, 0)
        val event = DBHelper(applicationContext).getEvent(eventId)
        if (event != null) {
            mEvent = event
            setupEditEvent()
            setupReminder()
        } else {
            mEvent = Event()
            val startTS = intent.getIntExtra(NEW_EVENT_START_TS, 0)
            if (startTS == 0)
                return

            setupNewEvent(Formatter.getDateTimeFromTS(startTS))
            setupDefaultReminderType()
        }

        updateStartDate()
        updateStartTime()
        updateEndDate()
        updateEndTime()
        setupRepetition()

        mWasEndDateSet = event != null
        mWasEndTimeSet = event != null

        event_start_date.setOnClickListener { setupStartDate() }
        event_start_time.setOnClickListener { setupStartTime() }
        event_end_date.setOnClickListener { setupEndDate() }
        event_end_time.setOnClickListener { setupEndTime() }

        event_end_checkbox.setOnCheckedChangeListener { compoundButton, isChecked -> endCheckboxChecked(isChecked) }
        event_all_day.setOnCheckedChangeListener { compoundButton, isChecked -> toggleAllDay(isChecked) }

        event_reminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                reminderItemSelected()
            }
        }

        updateTextColors(event_scrollview)
    }

    private fun setupEditEvent() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        title = resources.getString(R.string.edit_event)
        mEventStartDateTime = Formatter.getDateTimeFromTS(mEvent.startTS)
        mEventEndDateTime = Formatter.getDateTimeFromTS(mEvent.endTS)
        event_end_checkbox.isChecked = mEventStartDateTime != mEventEndDateTime
        endCheckboxChecked(event_end_checkbox.isChecked)
        event_title.setText(mEvent.title)
        event_description.setText(mEvent.description)
    }

    private fun setupNewEvent(dateTime: DateTime) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        title = resources.getString(R.string.new_event)
        mEventStartDateTime = dateTime
        mEventEndDateTime = mEventStartDateTime
        setupDefaultReminderType()
    }

    private fun setupDefaultReminderType() {
        val type = config.defaultReminderType
        toggleCustomReminderVisibility(type == REMINDER_CUSTOM)
        if (type == REMINDER_OFF) {
            event_reminder.setSelection(0)
        } else if (type == REMINDER_AT_START) {
            event_reminder.setSelection(1)
        } else {
            event_reminder.setSelection(2)
            setupDefaultReminderValue()
        }
    }

    private fun setupDefaultReminderValue() {
        val mins = config.defaultReminderMinutes
        var value = mins
        if (mins == 0) {
            custom_reminder_other_period.setSelection(0)
        } else if (mins % DAY_MINS == 0) {
            value = mins / DAY_MINS
            custom_reminder_other_period.setSelection(2)
        } else if (mins % HOUR_MINS == 0) {
            value = mins / HOUR_MINS
            custom_reminder_other_period.setSelection(1)
        } else {
            custom_reminder_other_period.setSelection(0)
        }
        custom_reminder_value.setText(value.toString())
    }

    private fun setupReminder() {
        when (mEvent.reminderMinutes) {
            REMINDER_OFF -> event_reminder.setSelection(0)
            REMINDER_AT_START -> event_reminder.setSelection(1)
            else -> {
                event_reminder.setSelection(2)
                toggleCustomReminderVisibility(true)
                setupReminderPeriod()
            }
        }
    }

    private fun setupRepetition() {
        event_repetition.setSelection(
                when (mEvent.repeatInterval) {
                    DAY -> 1
                    WEEK -> 2
                    BIWEEK -> 3
                    MONTH -> 4
                    YEAR -> 5
                    else -> 0
                }
        )
    }

    fun toggleAllDay(isChecked: Boolean) {
        event_start_time.beGoneIf(isChecked)
        event_end_checkbox.beGoneIf(isChecked)
        event_end_date.beGoneIf(isChecked || !event_end_checkbox.isChecked)
        event_end_time.beGoneIf(isChecked || !event_end_checkbox.isChecked)
    }

    fun endCheckboxChecked(isChecked: Boolean) {
        hideKeyboard()
        event_end_date.beVisibleIf(isChecked)
        event_end_time.beVisibleIf(isChecked)
    }

    fun reminderItemSelected() {
        if (!mWasReminderInit) {
            mWasReminderInit = true
            return
        }

        if (event_reminder.selectedItemPosition == event_reminder.count - 1) {
            toggleCustomReminderVisibility(true)
            custom_reminder_value.requestFocus()
            showKeyboard(custom_reminder_value)
        } else {
            hideKeyboard()
            toggleCustomReminderVisibility(false)
        }
    }

    private fun setupReminderPeriod() {
        val mins = mEvent.reminderMinutes
        var value = mins
        if (mins % DAY_MINS == 0) {
            value = mins / DAY_MINS
            custom_reminder_other_period.setSelection(2)
        } else if (mins % HOUR_MINS == 0) {
            value = mins / HOUR_MINS
            custom_reminder_other_period.setSelection(1)
        } else {
            custom_reminder_other_period.setSelection(0)
        }
        custom_reminder_value.setText(value.toString())
    }

    fun toggleCustomReminderVisibility(show: Boolean) {
        custom_reminder_holder.beVisibleIf(show)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_event, menu)
        val item = menu.findItem(R.id.cab_delete)
        if (mEvent.id == 0) {
            item.isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.cab_delete -> deleteEvent()
            R.id.save -> saveEvent()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun deleteEvent() {
        ConfirmationDialog(this) {
            DBHelper(applicationContext, this).deleteEvents(arrayOf(mEvent.id.toString()))
            finish()
        }
    }

    private fun saveEvent() {
        val newTitle = event_title.value
        if (newTitle.isEmpty()) {
            toast(R.string.title_empty)
            event_title.requestFocus()
            return
        }

        val newStartTS = mEventStartDateTime.seconds()
        val newEndTS = mEventEndDateTime.seconds()

        if (event_end_checkbox.isChecked && newStartTS > newEndTS) {
            toast(R.string.end_before_start)
            return
        }

        val dbHelper = DBHelper(applicationContext, this)
        val newDescription = event_description.value
        mEvent.apply {
            startTS = newStartTS
            endTS = if (event_end_checkbox.isChecked) newEndTS else newStartTS
            title = newTitle
            description = newDescription
            reminderMinutes = getReminderMinutes()
            repeatInterval = getRepeatInterval()
        }

        if (mEvent.id == 0) {
            dbHelper.insert(mEvent) {}
        } else {
            dbHelper.update(mEvent)
        }
    }

    private fun getReminderMinutes(): Int {
        return when (event_reminder.selectedItemPosition) {
            0 -> REMINDER_OFF
            1 -> REMINDER_AT_START
            else -> {
                val value = custom_reminder_value.value
                val multiplier = when (custom_reminder_other_period.selectedItemPosition) {
                    1 -> HOUR_MINS
                    2 -> DAY_MINS
                    else -> 1
                }
                Integer.valueOf(if (value.isEmpty()) "0" else value) * multiplier
            }
        }
    }

    private fun getRepeatInterval(): Int {
        return when (event_repetition.selectedItemPosition) {
            1 -> DAY
            2 -> WEEK
            3 -> BIWEEK
            4 -> MONTH
            5 -> YEAR
            else -> 0
        }
    }

    private fun updateStartDate() {
        event_start_date.text = Formatter.getDate(applicationContext, mEventStartDateTime)
    }

    private fun updateStartTime() {
        event_start_time.text = Formatter.getTime(this, mEventStartDateTime)
    }

    private fun updateEndDate() {
        event_end_date.text = Formatter.getDate(applicationContext, mEventEndDateTime)
    }

    private fun updateEndTime() {
        event_end_time.text = Formatter.getTime(this, mEventEndDateTime)
    }

    fun setupStartDate() {
        hideKeyboard()
        config.backgroundColor.getContrastColor()
        DatePickerDialog(this, mDialogTheme, startDateSetListener, mEventStartDateTime.year, mEventStartDateTime.monthOfYear - 1,
                mEventStartDateTime.dayOfMonth).show()
    }

    fun setupStartTime() {
        hideKeyboard()
        TimePickerDialog(this, mDialogTheme, startTimeSetListener, mEventStartDateTime.hourOfDay, mEventStartDateTime.minuteOfHour, true).show()
    }

    fun setupEndDate() {
        hideKeyboard()
        DatePickerDialog(this, mDialogTheme, endDateSetListener, mEventEndDateTime.year, mEventEndDateTime.monthOfYear - 1,
                mEventEndDateTime.dayOfMonth).show()
    }

    fun setupEndTime() {
        hideKeyboard()
        TimePickerDialog(this, mDialogTheme, endTimeSetListener, mEventEndDateTime.hourOfDay, mEventEndDateTime.minuteOfHour, true).show()
    }

    private val startDateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
        dateSet(year, monthOfYear, dayOfMonth, true)
        if (!mWasEndDateSet) {
            dateSet(year, monthOfYear, dayOfMonth, false)
        }
    }

    private val startTimeSetListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
        timeSet(hourOfDay, minute, true)
        if (!mWasEndTimeSet) {
            timeSet(hourOfDay, minute, false)
        }
    }

    private val endDateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth -> dateSet(year, monthOfYear, dayOfMonth, false) }

    private val endTimeSetListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute -> timeSet(hourOfDay, minute, false) }

    private fun dateSet(year: Int, month: Int, day: Int, isStart: Boolean) {
        if (isStart) {
            mEventStartDateTime = mEventStartDateTime.withDate(year, month + 1, day)
            updateStartDate()
        } else {
            mEventEndDateTime = mEventEndDateTime.withDate(year, month + 1, day)
            updateEndDate()
            mWasEndDateSet = true
        }
    }

    private fun timeSet(hours: Int, minutes: Int, isStart: Boolean) {
        if (isStart) {
            mEventStartDateTime = mEventStartDateTime.withHourOfDay(hours).withMinuteOfHour(minutes)
            updateStartTime()
        } else {
            mEventEndDateTime = mEventEndDateTime.withHourOfDay(hours).withMinuteOfHour(minutes)
            updateEndTime()
            mWasEndTimeSet = true
        }
    }

    override fun eventInserted(event: Event) {
        if (DateTime.now().isAfter(mEventStartDateTime.millis)) {
            toast(R.string.past_event_added)
        } else {
            toast(R.string.event_added)
        }
        scheduleNotification(event)
        finish()
    }

    override fun eventUpdated(event: Event) {
        scheduleNotification(event)
        toast(R.string.event_updated)
        finish()
    }

    override fun eventsDeleted(cnt: Int) {
    }

    override fun gotEvents(events: MutableList<Event>) {

    }
}
