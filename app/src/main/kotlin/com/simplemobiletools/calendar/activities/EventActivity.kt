package com.simplemobiletools.calendar.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.EditText
import com.simplemobiletools.calendar.*
import com.simplemobiletools.calendar.extensions.beVisibleIf
import com.simplemobiletools.calendar.extensions.updateWidget
import com.simplemobiletools.calendar.extensions.value
import com.simplemobiletools.calendar.fragments.DayFragment
import com.simplemobiletools.calendar.models.Event
import kotlinx.android.synthetic.main.activity_event.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

class EventActivity : SimpleActivity(), DBHelper.EventsListener {
    val HOUR_MINS = 60
    val DAY_MINS = 1440

    private var mWasReminderInit: Boolean = false
    private var mWasEndDateSet: Boolean = false
    private var mWasEndTimeSet: Boolean = false

    lateinit var mEventStartDateTime: DateTime
    lateinit var mEventEndDateTime: DateTime
    lateinit var mEvent: Event

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event)

        val intent = intent ?: return

        mWasReminderInit = false
        val eventId = intent.getIntExtra(Constants.EVENT_ID, 0)
        val event = DBHelper(applicationContext).getEvent(eventId)
        if (event != null) {
            mEvent = event
            setupEditEvent()
        } else {
            mEvent = Event()
            val dayCode = intent.getStringExtra(Constants.DAY_CODE)
            if (dayCode == null || dayCode.isEmpty())
                return

            setupNewEvent(dayCode)
        }

        updateStartDate()
        updateStartTime()
        updateEndDate()
        updateEndTime()
        setupReminder()
        setupRepetition()
        setupEndCheckbox()

        mWasEndDateSet = event != null
        mWasEndTimeSet = event != null

        event_start_date.setOnClickListener { setupStartDate() }
        event_start_time.setOnClickListener { setupStartTime() }
        event_end_date.setOnClickListener { setupEndDate() }
        event_end_time.setOnClickListener { setupEndTime() }

        event_end_checkbox.setOnCheckedChangeListener { compoundButton, isChecked -> endCheckboxChecked(isChecked) }

        event_reminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                reminderItemSelected()
            }
        }
    }

    private fun setupEditEvent() {
        title = resources.getString(R.string.edit_event)
        mEventStartDateTime = Formatter.getDateTimeFromTS(mEvent.startTS)
        mEventEndDateTime = Formatter.getDateTimeFromTS(mEvent.endTS)
        event_end_checkbox.isChecked = mEventStartDateTime != mEventEndDateTime
        endCheckboxChecked(event_end_checkbox.isChecked)
        event_title.setText(mEvent.title)
        event_description.setText(mEvent.description)
        hideKeyboard()
    }

    private fun setupNewEvent(dayCode: String) {
        title = resources.getString(R.string.new_event)
        mEventStartDateTime = Formatter.getDateTimeFromCode(dayCode).withZoneRetainFields(DateTimeZone.getDefault()).withHourOfDay(13)
        mEventEndDateTime = mEventStartDateTime
        event_reminder_other.setText(mConfig.lastOtherReminderMins.toString())
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow((currentFocus ?: View(this)).windowToken, 0)
    }

    private fun showKeyboard(et: EditText) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun setupReminder() {
        when (mEvent.reminderMinutes) {
            -1 -> event_reminder.setSelection(0)
            0 -> event_reminder.setSelection(1)
            else -> {
                event_reminder.setSelection(2)
                toggleCustomReminderVisibility(true)
                setupReminderPeriod()
            }
        }
    }

    private fun setupRepetition() {
        when (mEvent.repeatInterval) {
            Constants.DAY -> event_repetition.setSelection(1)
            Constants.WEEK -> event_repetition.setSelection(2)
            Constants.BIWEEK -> event_repetition.setSelection(3)
            Constants.MONTH -> event_repetition.setSelection(4)
            Constants.YEAR -> event_repetition.setSelection(5)
            else -> event_repetition.setSelection(0)
        }
    }

    private fun setupEndCheckbox() {
        event_end_checkbox.setTextColor(event_start_date.currentTextColor)
    }

    fun endCheckboxChecked(isChecked: Boolean) {
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
            event_reminder_other.requestFocus()
            showKeyboard(event_reminder_other)
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
            event_reminder_other_period.setSelection(2)
        } else if (mins % HOUR_MINS == 0) {
            value = mins / HOUR_MINS
            event_reminder_other_period.setSelection(1)
        } else {
            event_reminder_other_period.setSelection(0)
        }
        event_reminder_other.setText(value.toString())
    }

    fun toggleCustomReminderVisibility(show: Boolean) {
        event_reminder_other_period.beVisibleIf(show)
        event_reminder_other_val.beVisibleIf(show)
        event_reminder_other.beVisibleIf(show)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_event, menu)
        val item = menu.findItem(R.id.delete)
        if (mEvent.id == 0) {
            item.isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete -> {
                deleteEvent()
                true
            }
            R.id.save -> {
                saveEvent()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteEvent() {
        val intent = Intent()
        intent.putExtra(DayFragment.DELETED_ID, mEvent.id)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun saveEvent() {
        val newTitle = event_title.value
        if (newTitle.isEmpty()) {
            Utils.showToast(applicationContext, R.string.title_empty)
            event_title.requestFocus()
            return
        }

        val newStartTS = (mEventStartDateTime.millis / 1000).toInt()
        val newEndTS = (mEventEndDateTime.millis / 1000).toInt()

        if (event_end_checkbox.isChecked && newStartTS > newEndTS) {
            Utils.showToast(applicationContext, R.string.end_before_start)
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
            dbHelper.insert(mEvent)
        } else {
            dbHelper.update(mEvent)
        }

        saveLastReminderMins()
    }

    private fun saveLastReminderMins() {
        if (event_reminder.selectedItemPosition == 2) {
            mConfig.lastOtherReminderMins = getReminderMinutes()
        }
    }

    private fun getReminderMinutes(): Int {
        return when (event_reminder.selectedItemPosition) {
            0 -> -1
            1 -> 0
            else -> {
                val value = event_reminder_other.value
                if (value.isEmpty())
                    0

                val multiplier = when (event_reminder_other_period.selectedItemPosition) {
                    1 -> HOUR_MINS
                    2 -> DAY_MINS
                    else -> 1
                }
                Integer.valueOf(value) * multiplier
            }
        }
    }

    private fun getRepeatInterval(): Int {
        return when (event_repetition.selectedItemPosition) {
            1 -> Constants.DAY
            2 -> Constants.WEEK
            3 -> Constants.BIWEEK
            4 -> Constants.MONTH
            5 -> Constants.YEAR
            else -> 0
        }
    }

    private fun updateStartDate() {
        event_start_date.text = Formatter.getEventDate(applicationContext, mEventStartDateTime)
    }

    private fun updateStartTime() {
        event_start_time.text = Formatter.getEventTime(mEventStartDateTime)
    }

    private fun updateEndDate() {
        event_end_date.text = Formatter.getEventDate(applicationContext, mEventEndDateTime)
    }

    private fun updateEndTime() {
        event_end_time.text = Formatter.getEventTime(mEventEndDateTime)
    }

    fun setupStartDate() {
        DatePickerDialog(this, startDateSetListener, mEventStartDateTime.year, mEventStartDateTime.monthOfYear - 1,
                mEventStartDateTime.dayOfMonth).show()
    }

    fun setupStartTime() {
        TimePickerDialog(this, startTimeSetListener, mEventStartDateTime.hourOfDay, mEventStartDateTime.minuteOfHour, true).show()
    }

    fun setupEndDate() {
        DatePickerDialog(this, endDateSetListener, mEventEndDateTime.year, mEventEndDateTime.monthOfYear - 1,
                mEventEndDateTime.dayOfMonth).show()
    }

    fun setupEndTime() {
        TimePickerDialog(this, endTimeSetListener, mEventEndDateTime.hourOfDay, mEventEndDateTime.minuteOfHour, true).show()
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
            Utils.showToast(applicationContext, R.string.past_event_added)
        } else {
            Utils.showToast(applicationContext, R.string.event_added)
        }
        Utils.scheduleNotification(applicationContext, event)
        updateWidget()
        finish()
    }

    override fun eventUpdated(event: Event) {
        Utils.scheduleNotification(applicationContext, event)
        Utils.showToast(applicationContext, R.string.event_updated)
        updateWidget()
        finish()
    }

    override fun eventsDeleted(cnt: Int) {
        updateWidget()
    }

    override fun gotEvents(events: MutableList<Event>) {

    }
}
