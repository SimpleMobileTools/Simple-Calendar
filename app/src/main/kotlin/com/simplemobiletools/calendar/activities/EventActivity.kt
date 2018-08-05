package com.simplemobiletools.calendar.activities

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.dialogs.*
import com.simplemobiletools.calendar.extensions.*
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.models.CalDAVCalendar
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import kotlinx.android.synthetic.main.activity_event.*
import org.joda.time.DateTime
import java.util.*
import java.util.regex.Pattern

class EventActivity : SimpleActivity() {
    private val LAT_LON_PATTERN = "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)([,;])\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"
    private val START_TS = "START_TS"
    private val END_TS = "END_TS"
    private val REMINDER_1_MINUTES = "REMINDER_1_MINUTES"
    private val REMINDER_2_MINUTES = "REMINDER_2_MINUTES"
    private val REMINDER_3_MINUTES = "REMINDER_3_MINUTES"
    private val REPEAT_INTERVAL = "REPEAT_INTERVAL"
    private val REPEAT_LIMIT = "REPEAT_LIMIT"
    private val REPEAT_RULE = "REPEAT_RULE"
    private val EVENT_TYPE_ID = "EVENT_TYPE_ID"
    private val EVENT_CALENDAR_ID = "EVENT_CALENDAR_ID"

    private var mReminder1Minutes = 0
    private var mReminder2Minutes = 0
    private var mReminder3Minutes = 0
    private var mRepeatInterval = 0
    private var mRepeatLimit = 0
    private var mRepeatRule = 0
    private var mEventTypeId = DBHelper.REGULAR_EVENT_TYPE_ID
    private var mDialogTheme = 0
    private var mEventOccurrenceTS = 0
    private var mEventCalendarId = STORED_LOCALLY_ONLY
    private var wasActivityInitialized = false

    lateinit var mEventStartDateTime: DateTime
    lateinit var mEventEndDateTime: DateTime
    lateinit var mEvent: Event

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event)

        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_cross)
        val intent = intent ?: return
        mDialogTheme = getDialogTheme()

        val eventId = intent.getIntExtra(EVENT_ID, 0)
        val event = dbHelper.getEventWithId(eventId)

        if (eventId != 0 && event == null) {
            finish()
            return
        }

        val localEventType = dbHelper.getEventType(config.lastUsedLocalEventTypeId)
        if (localEventType == null || localEventType.caldavCalendarId != 0) {
            config.lastUsedLocalEventTypeId = DBHelper.REGULAR_EVENT_TYPE_ID
        }

        mEventTypeId = config.lastUsedLocalEventTypeId

        if (event != null) {
            mEvent = event
            mEventOccurrenceTS = intent.getIntExtra(EVENT_OCCURRENCE_TS, 0)
            if (savedInstanceState == null) {
                setupEditEvent()
            }

            if (intent.getBooleanExtra(IS_DUPLICATE_INTENT, false)) {
                mEvent.id = 0
            }
        } else {
            mEvent = Event()
            mReminder1Minutes = config.defaultReminderMinutes
            mReminder2Minutes = config.defaultReminderMinutes3
            mReminder3Minutes = config.defaultReminderMinutes2

            if (savedInstanceState == null) {
                setupNewEvent()
            }
        }

        if (savedInstanceState == null) {
            updateTexts()
            updateEventType()
            updateCalDAVCalendar()
        }

        event_show_on_map.setOnClickListener { showOnMap() }
        event_start_date.setOnClickListener { setupStartDate() }
        event_start_time.setOnClickListener { setupStartTime() }
        event_end_date.setOnClickListener { setupEndDate() }
        event_end_time.setOnClickListener { setupEndTime() }

        event_all_day.setOnCheckedChangeListener { compoundButton, isChecked -> toggleAllDay(isChecked) }
        event_repetition.setOnClickListener { showRepeatIntervalDialog() }
        event_repetition_rule_holder.setOnClickListener { showRepetitionRuleDialog() }
        event_repetition_limit_holder.setOnClickListener { showRepetitionTypePicker() }

        event_reminder_1.setOnClickListener {
            if (config.wasAlarmWarningShown) {
                showReminder1Dialog()
            } else {
                ConfirmationDialog(this, messageId = R.string.reminder_warning, positive = R.string.ok, negative = 0) {
                    config.wasAlarmWarningShown = true
                    showReminder1Dialog()
                }
            }
        }

        event_reminder_2.setOnClickListener { showReminder2Dialog() }
        event_reminder_3.setOnClickListener { showReminder3Dialog() }

        event_type_holder.setOnClickListener { showEventTypeDialog() }

        if (mEvent.flags and FLAG_ALL_DAY != 0)
            event_all_day.toggle()

        updateTextColors(event_scrollview)
        updateIconColors()
        wasActivityInitialized = true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_event, menu)
        if (wasActivityInitialized) {
            menu.findItem(R.id.delete).isVisible = mEvent.id != 0
            menu.findItem(R.id.share).isVisible = mEvent.id != 0
            menu.findItem(R.id.duplicate).isVisible = mEvent.id != 0
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save -> saveEvent()
            R.id.delete -> deleteEvent()
            R.id.duplicate -> duplicateEvent()
            R.id.share -> shareEvent()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(START_TS, mEventStartDateTime.seconds())
        outState.putInt(END_TS, mEventEndDateTime.seconds())

        outState.putInt(REMINDER_1_MINUTES, mReminder1Minutes)
        outState.putInt(REMINDER_2_MINUTES, mReminder2Minutes)
        outState.putInt(REMINDER_3_MINUTES, mReminder3Minutes)

        outState.putInt(REPEAT_INTERVAL, mRepeatInterval)
        outState.putInt(REPEAT_LIMIT, mRepeatLimit)
        outState.putInt(REPEAT_RULE, mRepeatRule)

        outState.putInt(EVENT_TYPE_ID, mEventTypeId)
        outState.putInt(EVENT_CALENDAR_ID, mEventCalendarId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.apply {
            mEventStartDateTime = Formatter.getDateTimeFromTS(getInt(START_TS))
            mEventEndDateTime = Formatter.getDateTimeFromTS(getInt(END_TS))

            mReminder1Minutes = getInt(REMINDER_1_MINUTES)
            mReminder2Minutes = getInt(REMINDER_2_MINUTES)
            mReminder3Minutes = getInt(REMINDER_3_MINUTES)

            mRepeatInterval = getInt(REPEAT_INTERVAL)
            mRepeatLimit = getInt(REPEAT_LIMIT)
            mRepeatRule = getInt(REPEAT_RULE)

            mEventTypeId = getInt(EVENT_TYPE_ID)
            mEventCalendarId = getInt(EVENT_CALENDAR_ID)
        }

        checkRepeatTexts(mRepeatInterval)
        checkRepeatRule()
        updateTexts()
        updateEventType()
        updateCalDAVCalendar()
    }

    private fun updateTexts() {
        updateRepetitionText()
        checkReminderTexts()
        updateStartTexts()
        updateEndTexts()
    }

    private fun setupEditEvent() {
        val realStart = if (mEventOccurrenceTS == 0) mEvent.startTS else mEventOccurrenceTS
        val duration = mEvent.endTS - mEvent.startTS
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        updateActionBarTitle(getString(R.string.edit_event))
        mEventStartDateTime = Formatter.getDateTimeFromTS(realStart)
        mEventEndDateTime = Formatter.getDateTimeFromTS(realStart + duration)
        event_title.setText(mEvent.title)
        event_location.setText(mEvent.location)
        event_description.setText(mEvent.description)
        if (event_description.value.isNotEmpty()) {
            event_description.movementMethod = LinkMovementMethod.getInstance()
        }

        mReminder1Minutes = mEvent.reminder1Minutes
        mReminder2Minutes = mEvent.reminder2Minutes
        mReminder3Minutes = mEvent.reminder3Minutes
        mRepeatInterval = mEvent.repeatInterval
        mRepeatLimit = mEvent.repeatLimit
        mRepeatRule = mEvent.repeatRule
        mEventTypeId = mEvent.eventType
        mEventCalendarId = mEvent.getCalDAVCalendarId()
        checkRepeatTexts(mRepeatInterval)
    }

    private fun setupNewEvent() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        updateActionBarTitle(getString(R.string.new_event))
        val isLastCaldavCalendarOK = config.caldavSync && config.getSyncedCalendarIdsAsList().contains(config.lastUsedCaldavCalendarId.toString())
        mEventCalendarId = if (isLastCaldavCalendarOK) config.lastUsedCaldavCalendarId else STORED_LOCALLY_ONLY

        if (intent.action == Intent.ACTION_EDIT || intent.action == Intent.ACTION_INSERT) {
            val startTS = (intent.getLongExtra("beginTime", System.currentTimeMillis()) / 1000).toInt()
            mEventStartDateTime = Formatter.getDateTimeFromTS(startTS)

            val endTS = (intent.getLongExtra("endTime", System.currentTimeMillis()) / 1000).toInt()
            mEventEndDateTime = Formatter.getDateTimeFromTS(endTS)

            event_title.setText(intent.getStringExtra("title"))
            event_location.setText(intent.getStringExtra("eventLocation"))
            event_description.setText(intent.getStringExtra("description"))
            if (event_description.value.isNotEmpty()) {
                event_description.movementMethod = LinkMovementMethod.getInstance()
            }
        } else {
            val startTS = intent.getIntExtra(NEW_EVENT_START_TS, 0)
            val dateTime = Formatter.getDateTimeFromTS(startTS)
            mEventStartDateTime = dateTime

            val addHours = if (intent.getBooleanExtra(NEW_EVENT_SET_HOUR_DURATION, false)) 1 else 0
            mEventEndDateTime = mEventStartDateTime.plusHours(addHours)
        }
    }

    private fun showReminder1Dialog() {
        showPickSecondsDialogHelper(mReminder1Minutes) {
            mReminder1Minutes = if (it <= 0) it else it / 60
            checkReminderTexts()
        }
    }

    private fun showReminder2Dialog() {
        showPickSecondsDialogHelper(mReminder2Minutes) {
            mReminder2Minutes = if (it <= 0) it else it / 60
            checkReminderTexts()
        }
    }

    private fun showReminder3Dialog() {
        showPickSecondsDialogHelper(mReminder3Minutes) {
            mReminder3Minutes = if (it <= 0) it else it / 60
            checkReminderTexts()
        }
    }

    private fun showRepeatIntervalDialog() {
        showEventRepeatIntervalDialog(mRepeatInterval) {
            setRepeatInterval(it)
        }
    }

    private fun setRepeatInterval(interval: Int) {
        mRepeatInterval = interval
        updateRepetitionText()
        checkRepeatTexts(interval)

        when {
            mRepeatInterval.isXWeeklyRepetition() -> setRepeatRule(Math.pow(2.0, (mEventStartDateTime.dayOfWeek - 1).toDouble()).toInt())
            mRepeatInterval.isXMonthlyRepetition() -> setRepeatRule(REPEAT_SAME_DAY)
            mRepeatInterval.isXYearlyRepetition() -> setRepeatRule(REPEAT_SAME_DAY)
        }
    }

    private fun checkRepeatTexts(limit: Int) {
        event_repetition_limit_holder.beGoneIf(limit == 0)
        checkRepetitionLimitText()

        event_repetition_rule_holder.beVisibleIf(mRepeatInterval.isXWeeklyRepetition() || mRepeatInterval.isXMonthlyRepetition() || mRepeatInterval.isXYearlyRepetition())
        checkRepetitionRuleText()
    }

    private fun showRepetitionTypePicker() {
        hideKeyboard()
        RepeatLimitTypePickerDialog(this, mRepeatLimit, mEventStartDateTime.seconds()) {
            setRepeatLimit(it)
        }
    }

    private fun setRepeatLimit(limit: Int) {
        mRepeatLimit = limit
        checkRepetitionLimitText()
    }

    private fun checkRepetitionLimitText() {
        event_repetition_limit.text = when {
            mRepeatLimit == 0 -> {
                event_repetition_limit_label.text = getString(R.string.repeat)
                resources.getString(R.string.forever)
            }
            mRepeatLimit > 0 -> {
                event_repetition_limit_label.text = getString(R.string.repeat_till)
                val repeatLimitDateTime = Formatter.getDateTimeFromTS(mRepeatLimit)
                Formatter.getFullDate(applicationContext, repeatLimitDateTime)
            }
            else -> {
                event_repetition_limit_label.text = getString(R.string.repeat)
                "${-mRepeatLimit} ${getString(R.string.times)}"
            }
        }
    }

    private fun showRepetitionRuleDialog() {
        hideKeyboard()
        when {
            mRepeatInterval.isXWeeklyRepetition() -> RepeatRuleWeeklyDialog(this, mRepeatRule) {
                setRepeatRule(it)
            }
            mRepeatInterval.isXMonthlyRepetition() -> {
                val items = getAvailableMonthlyRepetitionRules()
                RadioGroupDialog(this, items, mRepeatRule) {
                    setRepeatRule(it as Int)
                }
            }
            mRepeatInterval.isXYearlyRepetition() -> {
                val items = getAvailableYearlyRepetitionRules()
                RadioGroupDialog(this, items, mRepeatRule) {
                    setRepeatRule(it as Int)
                }
            }
        }
    }

    private fun getAvailableMonthlyRepetitionRules(): ArrayList<RadioItem> {
        val items = arrayListOf(RadioItem(REPEAT_SAME_DAY, getString(R.string.repeat_on_the_same_day_monthly)))

        // split Every Last Sunday and Every Fourth Sunday of the month, if the month has 4 sundays
        if (isLastWeekDayOfMonth()) {
            val order = (mEventStartDateTime.dayOfMonth - 1) / 7 + 1
            if (order == 4) {
                items.add(RadioItem(REPEAT_ORDER_WEEKDAY, getRepeatXthDayString(true, REPEAT_ORDER_WEEKDAY)))
                items.add(RadioItem(REPEAT_ORDER_WEEKDAY_USE_LAST, getRepeatXthDayString(true, REPEAT_ORDER_WEEKDAY_USE_LAST)))
            } else if (order == 5) {
                items.add(RadioItem(REPEAT_ORDER_WEEKDAY_USE_LAST, getRepeatXthDayString(true, REPEAT_ORDER_WEEKDAY_USE_LAST)))
            }
        } else {
            items.add(RadioItem(REPEAT_ORDER_WEEKDAY, getRepeatXthDayString(true, REPEAT_ORDER_WEEKDAY)))
        }

        if (isLastDayOfTheMonth()) {
            items.add(RadioItem(REPEAT_LAST_DAY, getString(R.string.repeat_on_the_last_day_monthly)))
        }
        return items
    }

    private fun getAvailableYearlyRepetitionRules(): ArrayList<RadioItem> {
        val items = arrayListOf(RadioItem(REPEAT_SAME_DAY, getString(R.string.repeat_on_the_same_day_yearly)))

        if (isLastWeekDayOfMonth()) {
            val order = (mEventStartDateTime.dayOfMonth - 1) / 7 + 1
            if (order == 4) {
                items.add(RadioItem(REPEAT_ORDER_WEEKDAY, getRepeatXthDayInMonthString(true, REPEAT_ORDER_WEEKDAY)))
                items.add(RadioItem(REPEAT_ORDER_WEEKDAY_USE_LAST, getRepeatXthDayInMonthString(true, REPEAT_ORDER_WEEKDAY_USE_LAST)))
            } else if (order == 5) {
                items.add(RadioItem(REPEAT_ORDER_WEEKDAY_USE_LAST, getRepeatXthDayInMonthString(true, REPEAT_ORDER_WEEKDAY_USE_LAST)))
            }
        } else {
            items.add(RadioItem(REPEAT_ORDER_WEEKDAY, getRepeatXthDayInMonthString(true, REPEAT_ORDER_WEEKDAY)))
        }

        return items
    }

    private fun isLastDayOfTheMonth() = mEventStartDateTime.dayOfMonth == mEventStartDateTime.dayOfMonth().withMaximumValue().dayOfMonth

    private fun isLastWeekDayOfMonth() = mEventStartDateTime.monthOfYear != mEventStartDateTime.plusDays(7).monthOfYear

    private fun getRepeatXthDayString(includeBase: Boolean, repeatRule: Int): String {
        val dayOfWeek = mEventStartDateTime.dayOfWeek
        val base = getBaseString(dayOfWeek)
        val order = getOrderString(repeatRule)
        val dayString = getDayString(dayOfWeek)
        return if (includeBase) {
            "$base $order $dayString"
        } else {
            val everyString = getString(if (isMaleGender(mEventStartDateTime.dayOfWeek)) R.string.every_m else R.string.every_f)
            "$everyString $order $dayString"
        }
    }

    private fun getBaseString(day: Int): String {
        return getString(if (isMaleGender(day)) {
            R.string.repeat_every_m
        } else {
            R.string.repeat_every_f
        })
    }

    private fun isMaleGender(day: Int) = day == 1 || day == 2 || day == 4 || day == 5

    private fun getOrderString(repeatRule: Int): String {
        val dayOfMonth = mEventStartDateTime.dayOfMonth
        var order = (dayOfMonth - 1) / 7 + 1
        if (order == 4 && isLastWeekDayOfMonth() && repeatRule == REPEAT_ORDER_WEEKDAY_USE_LAST) {
            order = -1
        }

        val isMale = isMaleGender(mEventStartDateTime.dayOfWeek)
        return getString(when (order) {
            1 -> if (isMale) R.string.first_m else R.string.first_f
            2 -> if (isMale) R.string.second_m else R.string.second_f
            3 -> if (isMale) R.string.third_m else R.string.third_f
            4 -> if (isMale) R.string.fourth_m else R.string.fourth_f
            else -> if (isMale) R.string.last_m else R.string.last_f
        })
    }

    private fun getDayString(day: Int): String {
        return getString(when (day) {
            1 -> R.string.monday_alt
            2 -> R.string.tuesday_alt
            3 -> R.string.wednesday_alt
            4 -> R.string.thursday_alt
            5 -> R.string.friday_alt
            6 -> R.string.saturday_alt
            else -> R.string.sunday_alt
        })
    }

    private fun getRepeatXthDayInMonthString(includeBase: Boolean, repeatRule: Int): String {
        val weekDayString = getRepeatXthDayString(includeBase, repeatRule)
        val monthString = resources.getStringArray(R.array.in_months)[mEventStartDateTime.monthOfYear - 1]
        return "$weekDayString $monthString"
    }

    private fun setRepeatRule(rule: Int) {
        mRepeatRule = rule
        checkRepetitionRuleText()
        if (rule == 0) {
            setRepeatInterval(0)
        }
    }

    private fun checkRepetitionRuleText() {
        when {
            mRepeatInterval.isXWeeklyRepetition() -> {
                event_repetition_rule.text = if (mRepeatRule == EVERY_DAY_BIT) getString(R.string.every_day) else getSelectedDaysString(mRepeatRule)
            }
            mRepeatInterval.isXMonthlyRepetition() -> {
                val repeatString = if (mRepeatRule == REPEAT_ORDER_WEEKDAY_USE_LAST || mRepeatRule == REPEAT_ORDER_WEEKDAY)
                    R.string.repeat else R.string.repeat_on

                event_repetition_rule_label.text = getString(repeatString)
                event_repetition_rule.text = getMonthlyRepetitionRuleText()
            }
            mRepeatInterval.isXYearlyRepetition() -> {
                val repeatString = if (mRepeatRule == REPEAT_ORDER_WEEKDAY_USE_LAST || mRepeatRule == REPEAT_ORDER_WEEKDAY)
                    R.string.repeat else R.string.repeat_on

                event_repetition_rule_label.text = getString(repeatString)
                event_repetition_rule.text = getYearlyRepetitionRuleText()
            }
        }
    }

    private fun getMonthlyRepetitionRuleText() = when (mRepeatRule) {
        REPEAT_SAME_DAY -> getString(R.string.the_same_day)
        REPEAT_LAST_DAY -> getString(R.string.the_last_day)
        else -> getRepeatXthDayString(false, mRepeatRule)
    }

    private fun getYearlyRepetitionRuleText() = when (mRepeatRule) {
        REPEAT_SAME_DAY -> getString(R.string.the_same_day)
        else -> getRepeatXthDayInMonthString(false, mRepeatRule)
    }

    private fun showEventTypeDialog() {
        hideKeyboard()
        SelectEventTypeDialog(this, mEventTypeId, false) {
            mEventTypeId = it.id
            updateEventType()
        }
    }

    private fun checkReminderTexts() {
        updateReminder1Text()
        updateReminder2Text()
        updateReminder3Text()
    }

    private fun updateReminder1Text() {
        event_reminder_1.text = getFormattedMinutes(mReminder1Minutes)
        if (mReminder1Minutes == REMINDER_OFF) {
            mReminder2Minutes = REMINDER_OFF
            mReminder3Minutes = REMINDER_OFF
        }
    }

    private fun updateReminder2Text() {
        event_reminder_2.apply {
            beGoneIf(mReminder1Minutes == REMINDER_OFF)
            if (mReminder2Minutes == REMINDER_OFF) {
                text = resources.getString(R.string.add_another_reminder)
                alpha = 0.4f
                mReminder3Minutes = REMINDER_OFF
            } else {
                text = getFormattedMinutes(mReminder2Minutes)
                alpha = 1f
            }
        }
    }

    private fun updateReminder3Text() {
        event_reminder_3.apply {
            beGoneIf(mReminder2Minutes == REMINDER_OFF || mReminder1Minutes == REMINDER_OFF)
            if (mReminder3Minutes == REMINDER_OFF) {
                text = resources.getString(R.string.add_another_reminder)
                alpha = 0.4f
            } else {
                text = getFormattedMinutes(mReminder3Minutes)
                alpha = 1f
            }
        }
    }

    private fun updateRepetitionText() {
        event_repetition.text = getRepetitionText(mRepeatInterval)
    }

    private fun updateEventType() {
        val eventType = dbHelper.getEventType(mEventTypeId)
        if (eventType != null) {
            event_type.text = eventType.title
            event_type_color.setFillWithStroke(eventType.color, config.backgroundColor)
        }
    }

    private fun updateCalDAVCalendar() {
        if (config.caldavSync) {
            event_caldav_calendar_image.beVisible()
            event_caldav_calendar_holder.beVisible()
            event_caldav_calendar_divider.beVisible()

            val calendars = CalDAVHandler(applicationContext).getCalDAVCalendars(this).filter {
                it.canWrite() && config.getSyncedCalendarIdsAsList().contains(it.id.toString())
            }
            updateCurrentCalendarInfo(if (mEventCalendarId == STORED_LOCALLY_ONLY) null else getCalendarWithId(calendars, getCalendarId()))

            event_caldav_calendar_holder.setOnClickListener {
                hideKeyboard()
                SelectEventCalendarDialog(this, calendars, mEventCalendarId) {
                    if (mEventCalendarId != STORED_LOCALLY_ONLY && it == STORED_LOCALLY_ONLY) {
                        mEventTypeId = config.lastUsedLocalEventTypeId
                        updateEventType()
                    }
                    mEventCalendarId = it
                    config.lastUsedCaldavCalendarId = it
                    updateCurrentCalendarInfo(getCalendarWithId(calendars, it))
                }
            }
        } else {
            updateCurrentCalendarInfo(null)
        }
    }

    private fun getCalendarId() = if (mEvent.source == SOURCE_SIMPLE_CALENDAR) config.lastUsedCaldavCalendarId else mEvent.getCalDAVCalendarId()

    private fun getCalendarWithId(calendars: List<CalDAVCalendar>, calendarId: Int): CalDAVCalendar? =
            calendars.firstOrNull { it.id == calendarId }

    private fun updateCurrentCalendarInfo(currentCalendar: CalDAVCalendar?) {
        event_type_image.beVisibleIf(currentCalendar == null)
        event_type_holder.beVisibleIf(currentCalendar == null)
        event_caldav_calendar_divider.beVisibleIf(currentCalendar == null)
        event_caldav_calendar_email.beGoneIf(currentCalendar == null)

        if (currentCalendar == null) {
            mEventCalendarId = STORED_LOCALLY_ONLY
            val mediumMargin = resources.getDimension(R.dimen.medium_margin).toInt()
            event_caldav_calendar_name.apply {
                text = getString(R.string.store_locally_only)
                setPadding(paddingLeft, paddingTop, paddingRight, mediumMargin)
            }

            event_caldav_calendar_holder.apply {
                setPadding(paddingLeft, mediumMargin, paddingRight, mediumMargin)
            }
        } else {
            event_caldav_calendar_email.text = currentCalendar.accountName
            event_caldav_calendar_name.apply {
                text = currentCalendar.displayName
                setPadding(paddingLeft, paddingTop, paddingRight, resources.getDimension(R.dimen.tiny_margin).toInt())
            }

            event_caldav_calendar_holder.apply {
                setPadding(paddingLeft, 0, paddingRight, 0)
            }
        }
    }

    private fun toggleAllDay(isChecked: Boolean) {
        hideKeyboard()
        event_start_time.beGoneIf(isChecked)
        event_end_time.beGoneIf(isChecked)
    }

    private fun shareEvent() {
        shareEvents(arrayListOf(mEvent.id))
    }

    private fun deleteEvent() {
        DeleteEventDialog(this, arrayListOf(mEvent.id), mEvent.repeatInterval > 0) {
            when (it) {
                DELETE_SELECTED_OCCURRENCE -> dbHelper.addEventRepeatException(mEvent.id, mEventOccurrenceTS, true)
                DELETE_FUTURE_OCCURRENCES -> dbHelper.addEventRepeatLimit(mEvent.id, mEventOccurrenceTS)
                DELETE_ALL_OCCURRENCES -> dbHelper.deleteEvents(arrayOf(mEvent.id.toString()), true)
            }
            finish()
        }
    }

    private fun duplicateEvent() {
        Intent(this, EventActivity::class.java).apply {
            putExtra(EVENT_ID, mEvent.id)
            putExtra(EVENT_OCCURRENCE_TS, mEventOccurrenceTS)
            putExtra(IS_DUPLICATE_INTENT, true)
            startActivity(this)
        }
        finish()
    }

    private fun saveEvent() {
        val newTitle = event_title.value
        if (newTitle.isEmpty()) {
            toast(R.string.title_empty)
            event_title.requestFocus()
            return
        }

        val newStartTS = mEventStartDateTime.withSecondOfMinute(0).withMillisOfSecond(0).seconds()
        val newEndTS = mEventEndDateTime.withSecondOfMinute(0).withMillisOfSecond(0).seconds()

        if (newStartTS > newEndTS) {
            toast(R.string.end_before_start)
            return
        }

        val wasRepeatable = mEvent.repeatInterval > 0
        val oldSource = mEvent.source
        val newImportId = if (mEvent.id != 0) mEvent.importId else UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis().toString()

        val newEventType = if (!config.caldavSync || config.lastUsedCaldavCalendarId == 0 || mEventCalendarId == STORED_LOCALLY_ONLY) {
            mEventTypeId
        } else {
            dbHelper.getEventTypeWithCalDAVCalendarId(config.lastUsedCaldavCalendarId)?.id ?: config.lastUsedLocalEventTypeId
        }

        val newSource = if (!config.caldavSync || config.lastUsedCaldavCalendarId == 0 || mEventCalendarId == STORED_LOCALLY_ONLY) {
            config.lastUsedLocalEventTypeId = newEventType
            SOURCE_SIMPLE_CALENDAR
        } else {
            "$CALDAV-${config.lastUsedCaldavCalendarId}"
        }

        val reminders = sortedSetOf(mReminder1Minutes, mReminder2Minutes, mReminder3Minutes).filter { it != REMINDER_OFF }
        val reminder1 = reminders.getOrElse(0) { REMINDER_OFF }
        val reminder2 = reminders.getOrElse(1) { REMINDER_OFF }
        val reminder3 = reminders.getOrElse(2) { REMINDER_OFF }

        config.apply {
            defaultReminderMinutes = reminder1
            defaultReminderMinutes2 = reminder2
            defaultReminderMinutes3 = reminder3
        }

        mEvent.apply {
            startTS = newStartTS
            endTS = newEndTS
            title = newTitle
            description = event_description.value
            reminder1Minutes = reminder1
            reminder2Minutes = reminder2
            reminder3Minutes = reminder3
            repeatInterval = mRepeatInterval
            importId = newImportId
            flags = if (event_all_day.isChecked) (mEvent.flags.addBit(FLAG_ALL_DAY)) else (mEvent.flags.removeBit(FLAG_ALL_DAY))
            repeatLimit = if (repeatInterval == 0) 0 else mRepeatLimit
            repeatRule = mRepeatRule
            eventType = newEventType
            offset = getCurrentOffset()
            isDstIncluded = TimeZone.getDefault().inDaylightTime(Date())
            lastUpdated = System.currentTimeMillis()
            source = newSource
            location = event_location.value
        }

        // recreate the event if it was moved in a different CalDAV calendar
        if (mEvent.id != 0 && oldSource != newSource) {
            dbHelper.deleteEvents(arrayOf(mEvent.id.toString()), true)
            mEvent.id = 0
        }

        storeEvent(wasRepeatable)
    }

    private fun storeEvent(wasRepeatable: Boolean) {
        if (mEvent.id == 0) {
            dbHelper.insert(mEvent, true, this) {
                if (DateTime.now().isAfter(mEventStartDateTime.millis)) {
                    if (mEvent.repeatInterval == 0 && mEvent.getReminders().isNotEmpty()) {
                        notifyEvent(mEvent)
                    }
                }

                finish()
            }
        } else {
            if (mRepeatInterval > 0 && wasRepeatable) {
                EditRepeatingEventDialog(this) {
                    if (it) {
                        dbHelper.update(mEvent, true, this) {
                            finish()
                        }
                    } else {
                        dbHelper.addEventRepeatException(mEvent.id, mEventOccurrenceTS, true)
                        mEvent.apply {
                            parentId = id
                            id = 0
                            repeatRule = 0
                            repeatInterval = 0
                            repeatLimit = 0
                        }

                        dbHelper.insert(mEvent, true, this) {
                            finish()
                        }
                    }
                }
            } else {
                dbHelper.update(mEvent, true, this) {
                    finish()
                }
            }
        }
    }

    private fun updateStartTexts() {
        updateStartDateText()
        updateStartTimeText()
    }

    private fun updateStartDateText() {
        event_start_date.text = Formatter.getDate(applicationContext, mEventStartDateTime)
        checkStartEndValidity()
    }

    private fun updateStartTimeText() {
        event_start_time.text = Formatter.getTime(this, mEventStartDateTime)
        checkStartEndValidity()
    }

    private fun updateEndTexts() {
        updateEndDateText()
        updateEndTimeText()
    }

    private fun updateEndDateText() {
        event_end_date.text = Formatter.getDate(applicationContext, mEventEndDateTime)
        checkStartEndValidity()
    }

    private fun updateEndTimeText() {
        event_end_time.text = Formatter.getTime(this, mEventEndDateTime)
        checkStartEndValidity()
    }

    private fun checkStartEndValidity() {
        val textColor = if (mEventStartDateTime.isAfter(mEventEndDateTime)) resources.getColor(R.color.red_text) else config.textColor
        event_end_date.setTextColor(textColor)
        event_end_time.setTextColor(textColor)
    }

    private fun showOnMap() {
        if (event_location.value.isEmpty()) {
            toast(R.string.please_fill_location)
            return
        }

        val pattern = Pattern.compile(LAT_LON_PATTERN)
        val locationValue = event_location.value
        val uri = if (pattern.matcher(locationValue).find()) {
            val delimiter = if (locationValue.contains(';')) ";" else ","
            val parts = locationValue.split(delimiter)
            val latitude = parts.first()
            val longitude = parts.last()
            Uri.parse("geo:$latitude,$longitude")
        } else {
            val location = Uri.encode(locationValue)
            Uri.parse("geo:0,0?q=$location")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            toast(R.string.no_app_found)
        }
    }

    @SuppressLint("NewApi")
    private fun setupStartDate() {
        hideKeyboard()
        config.backgroundColor.getContrastColor()
        val datepicker = DatePickerDialog(this, mDialogTheme, startDateSetListener, mEventStartDateTime.year, mEventStartDateTime.monthOfYear - 1,
                mEventStartDateTime.dayOfMonth)

        if (isLollipopPlus()) {
            datepicker.datePicker.firstDayOfWeek = if (config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        }

        datepicker.show()
    }

    private fun setupStartTime() {
        hideKeyboard()
        TimePickerDialog(this, mDialogTheme, startTimeSetListener, mEventStartDateTime.hourOfDay, mEventStartDateTime.minuteOfHour, config.use24HourFormat).show()
    }

    @SuppressLint("NewApi")
    private fun setupEndDate() {
        hideKeyboard()
        val datepicker = DatePickerDialog(this, mDialogTheme, endDateSetListener, mEventEndDateTime.year, mEventEndDateTime.monthOfYear - 1,
                mEventEndDateTime.dayOfMonth)

        if (isLollipopPlus()) {
            datepicker.datePicker.firstDayOfWeek = if (config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        }

        datepicker.show()
    }

    private fun setupEndTime() {
        hideKeyboard()
        TimePickerDialog(this, mDialogTheme, endTimeSetListener, mEventEndDateTime.hourOfDay, mEventEndDateTime.minuteOfHour, config.use24HourFormat).show()
    }

    private val startDateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
        dateSet(year, monthOfYear, dayOfMonth, true)
    }

    private val startTimeSetListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
        timeSet(hourOfDay, minute, true)
    }

    private val endDateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth -> dateSet(year, monthOfYear, dayOfMonth, false) }

    private val endTimeSetListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute -> timeSet(hourOfDay, minute, false) }

    private fun dateSet(year: Int, month: Int, day: Int, isStart: Boolean) {
        if (isStart) {
            val diff = mEventEndDateTime.seconds() - mEventStartDateTime.seconds()

            mEventStartDateTime = mEventStartDateTime.withDate(year, month + 1, day)
            updateStartDateText()
            checkRepeatRule()

            mEventEndDateTime = mEventStartDateTime.plusSeconds(diff)
            updateEndTexts()
        } else {
            mEventEndDateTime = mEventEndDateTime.withDate(year, month + 1, day)
            updateEndDateText()
        }
    }

    private fun timeSet(hours: Int, minutes: Int, isStart: Boolean) {
        if (isStart) {
            val diff = mEventEndDateTime.seconds() - mEventStartDateTime.seconds()

            mEventStartDateTime = mEventStartDateTime.withHourOfDay(hours).withMinuteOfHour(minutes)
            updateStartTimeText()

            mEventEndDateTime = mEventStartDateTime.plusSeconds(diff)
            updateEndTexts()
        } else {
            mEventEndDateTime = mEventEndDateTime.withHourOfDay(hours).withMinuteOfHour(minutes)
            updateEndTimeText()
        }
    }

    private fun checkRepeatRule() {
        if (mRepeatInterval.isXWeeklyRepetition()) {
            val day = mRepeatRule
            if (day == MONDAY_BIT || day == TUESDAY_BIT || day == WEDNESDAY_BIT || day == THURSDAY_BIT || day == FRIDAY_BIT || day == SATURDAY_BIT || day == SUNDAY_BIT) {
                setRepeatRule(Math.pow(2.0, (mEventStartDateTime.dayOfWeek - 1).toDouble()).toInt())
            }
        } else if (mRepeatInterval.isXMonthlyRepetition() || mRepeatInterval.isXYearlyRepetition()) {
            if (mRepeatRule == REPEAT_LAST_DAY && !isLastDayOfTheMonth()) {
                mRepeatRule = REPEAT_SAME_DAY
            }
            checkRepetitionRuleText()
        }
    }

    private fun updateIconColors() {
        val textColor = config.textColor
        event_time_image.applyColorFilter(textColor)
        event_repetition_image.applyColorFilter(textColor)
        event_reminder_image.applyColorFilter(textColor)
        event_type_image.applyColorFilter(textColor)
        event_caldav_calendar_image.applyColorFilter(textColor)
        event_show_on_map.applyColorFilter(getAdjustedPrimaryColor())
    }
}
