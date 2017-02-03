package com.simplemobiletools.calendar.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.dialogs.EventReminderDialog
import com.simplemobiletools.calendar.dialogs.EventRepeatIntervalDialog
import com.simplemobiletools.calendar.extensions.*
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.activity_event.*
import org.joda.time.DateTime

class EventActivity : SimpleActivity(), DBHelper.EventUpdateListener {
    private var mWasEndDateSet = false
    private var mWasEndTimeSet = false
    private var mReminderMinutes = 0
    private var mRepeatInterval = 0
    private var mDialogTheme = 0

    lateinit var mEventStartDateTime: DateTime
    lateinit var mEventEndDateTime: DateTime
    lateinit var mEvent: Event

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event)

        val intent = intent ?: return
        mDialogTheme = getAppropriateTheme()

        val eventId = intent.getIntExtra(EVENT_ID, 0)
        val event = DBHelper(applicationContext).getEvent(eventId)
        if (event != null) {
            mEvent = event
            setupEditEvent()
        } else {
            mEvent = Event()
            mReminderMinutes = config.defaultReminderMinutes
            val startTS = intent.getIntExtra(NEW_EVENT_START_TS, 0)
            if (startTS == 0)
                return

            setupNewEvent(Formatter.getDateTimeFromTS(startTS))
        }

        updateReminderText()
        updateRepetitionText()
        updateStartDate()
        updateStartTime()
        updateEndDate()
        updateEndTime()

        mWasEndDateSet = event != null
        mWasEndTimeSet = event != null

        event_start_date.setOnClickListener { setupStartDate() }
        event_start_time.setOnClickListener { setupStartTime() }
        event_end_date.setOnClickListener { setupEndDate() }
        event_end_time.setOnClickListener { setupEndTime() }

        event_all_day.setOnCheckedChangeListener { compoundButton, isChecked -> toggleAllDay(isChecked) }
        event_reminder.setOnClickListener { showReminderDialog() }
        event_repetition.setOnClickListener { showRepeatIntervalDialog() }

        updateTextColors(event_scrollview)
        updateIconColors()
    }

    private fun setupEditEvent() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        title = resources.getString(R.string.edit_event)
        mEventStartDateTime = Formatter.getDateTimeFromTS(mEvent.startTS)
        mEventEndDateTime = Formatter.getDateTimeFromTS(mEvent.endTS)
        event_title.setText(mEvent.title)
        event_description.setText(mEvent.description)
        mReminderMinutes = mEvent.reminderMinutes
        mRepeatInterval = mEvent.repeatInterval
    }

    private fun setupNewEvent(dateTime: DateTime) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        title = resources.getString(R.string.new_event)
        mEventStartDateTime = dateTime
        mEventEndDateTime = mEventStartDateTime.plusHours(1)
    }

    private fun showReminderDialog() {
        EventReminderDialog(this, mReminderMinutes) {
            mReminderMinutes = it
            updateReminderText()
        }
    }

    private fun showRepeatIntervalDialog() {
        EventRepeatIntervalDialog(this, mRepeatInterval) {
            mRepeatInterval = it
            updateRepetitionText()
        }
    }

    private fun updateReminderText() {
        event_reminder.text = getReminderText(mReminderMinutes)
    }

    private fun updateRepetitionText() {
        event_repetition.text = getRepetitionToString(mRepeatInterval)
    }

    private fun getRepetitionToString(seconds: Int) = getString(when (seconds) {
        DAY -> R.string.daily
        WEEK -> R.string.weekly
        BIWEEK -> R.string.biweekly
        MONTH -> R.string.monthly
        YEAR -> R.string.yearly
        else -> R.string.no_repetition
    })

    fun toggleAllDay(isChecked: Boolean) {
        event_start_time.beGoneIf(isChecked)
        event_end_time.beGoneIf(isChecked)
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

        if (newStartTS > newEndTS) {
            toast(R.string.end_before_start)
            return
        }

        val dbHelper = DBHelper(applicationContext, this)
        val newDescription = event_description.value
        mEvent.apply {
            startTS = newStartTS
            endTS = newEndTS
            title = newTitle
            description = newDescription
            reminderMinutes = mReminderMinutes
            repeatInterval = mRepeatInterval
        }

        if (mEvent.id == 0) {
            dbHelper.insert(mEvent) {}
        } else {
            dbHelper.update(mEvent)
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

    private fun updateIconColors() {
        event_time_image.setColorFilter(config.textColor, PorterDuff.Mode.SRC_IN)
        event_repetition_image.setColorFilter(config.textColor, PorterDuff.Mode.SRC_IN)
        event_reminder_image.setColorFilter(config.textColor, PorterDuff.Mode.SRC_IN)
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
