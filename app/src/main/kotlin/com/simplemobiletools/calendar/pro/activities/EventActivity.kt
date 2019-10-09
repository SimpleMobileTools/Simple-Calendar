package com.simplemobiletools.calendar.pro.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.adapters.AutoCompleteTextViewAdapter
import com.simplemobiletools.calendar.pro.dialogs.*
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.models.*
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.views.MyAutoCompleteTextView
import kotlinx.android.synthetic.main.activity_event.*
import kotlinx.android.synthetic.main.activity_event.view.*
import kotlinx.android.synthetic.main.item_attendee.view.*
import org.joda.time.DateTime
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class EventActivity : SimpleActivity() {
    private val LAT_LON_PATTERN = "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)([,;])\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"
    private val EVENT = "EVENT"
    private val START_TS = "START_TS"
    private val END_TS = "END_TS"
    private val REMINDER_1_MINUTES = "REMINDER_1_MINUTES"
    private val REMINDER_2_MINUTES = "REMINDER_2_MINUTES"
    private val REMINDER_3_MINUTES = "REMINDER_3_MINUTES"
    private val REMINDER_1_TYPE = "REMINDER_1_TYPE"
    private val REMINDER_2_TYPE = "REMINDER_2_TYPE"
    private val REMINDER_3_TYPE = "REMINDER_3_TYPE"
    private val REPEAT_INTERVAL = "REPEAT_INTERVAL"
    private val REPEAT_LIMIT = "REPEAT_LIMIT"
    private val REPEAT_RULE = "REPEAT_RULE"
    private val ATTENDEES = "ATTENDEES"
    private val EVENT_TYPE_ID = "EVENT_TYPE_ID"
    private val EVENT_CALENDAR_ID = "EVENT_CALENDAR_ID"

    private var mReminder1Minutes = REMINDER_OFF
    private var mReminder2Minutes = REMINDER_OFF
    private var mReminder3Minutes = REMINDER_OFF
    private var mReminder1Type = REMINDER_NOTIFICATION
    private var mReminder2Type = REMINDER_NOTIFICATION
    private var mReminder3Type = REMINDER_NOTIFICATION
    private var mRepeatInterval = 0
    private var mRepeatLimit = 0L
    private var mRepeatRule = 0
    private var mEventTypeId = REGULAR_EVENT_TYPE_ID
    private var mDialogTheme = 0
    private var mEventOccurrenceTS = 0L
    private var mEventCalendarId = STORED_LOCALLY_ONLY
    private var mWasActivityInitialized = false
    private var mWasContactsPermissionChecked = false
    private var mAttendees = ArrayList<Attendee>()
    private var mAttendeeAutoCompleteViews = ArrayList<MyAutoCompleteTextView>()
    private var mAvailableContacts = ArrayList<Attendee>()
    private var mSelectedContacts = ArrayList<Attendee>()
    private var mStoredEventTypes = ArrayList<EventType>()

    private lateinit var mAttendeePlaceholder: Drawable
    private lateinit var mEventStartDateTime: DateTime
    private lateinit var mEventEndDateTime: DateTime
    private lateinit var mEvent: Event

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event)

        if (checkAppSideloading()) {
            return
        }

        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_cross_vector)
        val intent = intent ?: return
        mDialogTheme = getDialogTheme()
        mWasContactsPermissionChecked = hasPermission(PERMISSION_READ_CONTACTS)
        mAttendeePlaceholder = resources.getDrawable(R.drawable.attendee_circular_background)
        (mAttendeePlaceholder as LayerDrawable).findDrawableByLayerId(R.id.attendee_circular_background).applyColorFilter(config.primaryColor)

        val eventId = intent.getLongExtra(EVENT_ID, 0L)
        ensureBackgroundThread {
            mStoredEventTypes = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
            val event = eventsDB.getEventWithId(eventId)
            if (eventId != 0L && event == null) {
                finish()
                return@ensureBackgroundThread
            }

            val localEventType = mStoredEventTypes.firstOrNull { it.id == config.lastUsedLocalEventTypeId }
            runOnUiThread {
                gotEvent(savedInstanceState, localEventType, event)
            }
        }
    }

    private fun gotEvent(savedInstanceState: Bundle?, localEventType: EventType?, event: Event?) {
        if (localEventType == null || localEventType.caldavCalendarId != 0) {
            config.lastUsedLocalEventTypeId = REGULAR_EVENT_TYPE_ID
        }

        mEventTypeId = if (config.defaultEventTypeId == -1L) config.lastUsedLocalEventTypeId else config.defaultEventTypeId

        if (event != null) {
            mEvent = event
            mEventOccurrenceTS = intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L)
            if (savedInstanceState == null) {
                setupEditEvent()
            }

            if (intent.getBooleanExtra(IS_DUPLICATE_INTENT, false)) {
                mEvent.id = null
            } else {
                cancelNotification(mEvent.id!!)
            }
        } else {
            mEvent = Event(null)
            config.apply {
                mReminder1Minutes = if (usePreviousEventReminders) lastEventReminderMinutes1 else defaultReminder1
                mReminder2Minutes = if (usePreviousEventReminders) lastEventReminderMinutes2 else defaultReminder2
                mReminder3Minutes = if (usePreviousEventReminders) lastEventReminderMinutes3 else defaultReminder3
            }

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
            handleNotificationAvailability {
                if (config.wasAlarmWarningShown) {
                    showReminder1Dialog()
                } else {
                    ConfirmationDialog(this, messageId = R.string.reminder_warning, positive = R.string.ok, negative = 0) {
                        config.wasAlarmWarningShown = true
                        showReminder1Dialog()
                    }
                }
            }
        }

        event_reminder_2.setOnClickListener { showReminder2Dialog() }
        event_reminder_3.setOnClickListener { showReminder3Dialog() }

        event_reminder_1_type.setOnClickListener {
            showReminderTypePicker(mReminder1Type) {
                mReminder1Type = it
                updateReminderTypeImage(event_reminder_1_type, Reminder(mReminder1Minutes, mReminder1Type))
            }
        }

        event_reminder_2_type.setOnClickListener {
            showReminderTypePicker(mReminder2Type) {
                mReminder2Type = it
                updateReminderTypeImage(event_reminder_2_type, Reminder(mReminder2Minutes, mReminder2Type))
            }
        }

        event_reminder_3_type.setOnClickListener {
            showReminderTypePicker(mReminder3Type) {
                mReminder3Type = it
                updateReminderTypeImage(event_reminder_3_type, Reminder(mReminder3Minutes, mReminder3Type))
            }
        }

        event_type_holder.setOnClickListener { showEventTypeDialog() }
        event_all_day.apply {
            isChecked = mEvent.flags and FLAG_ALL_DAY != 0
            jumpDrawablesToCurrentState()
        }

        updateTextColors(event_scrollview)
        updateIconColors()
        mWasActivityInitialized = true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_event, menu)
        if (mWasActivityInitialized) {
            menu.findItem(R.id.delete).isVisible = mEvent.id != null
            menu.findItem(R.id.share).isVisible = mEvent.id != null
            menu.findItem(R.id.duplicate).isVisible = mEvent.id != null
        }
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save -> saveCurrentEvent()
            R.id.delete -> deleteEvent()
            R.id.duplicate -> duplicateEvent()
            R.id.share -> shareEvent()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!mWasActivityInitialized) {
            return
        }

        outState.apply {
            putSerializable(EVENT, mEvent)
            putLong(START_TS, mEventStartDateTime.seconds())
            putLong(END_TS, mEventEndDateTime.seconds())

            putInt(REMINDER_1_MINUTES, mReminder1Minutes)
            putInt(REMINDER_2_MINUTES, mReminder2Minutes)
            putInt(REMINDER_3_MINUTES, mReminder3Minutes)

            putInt(REMINDER_1_TYPE, mReminder1Type)
            putInt(REMINDER_2_TYPE, mReminder2Type)
            putInt(REMINDER_3_TYPE, mReminder3Type)

            putInt(REPEAT_INTERVAL, mRepeatInterval)
            putInt(REPEAT_RULE, mRepeatRule)
            putLong(REPEAT_LIMIT, mRepeatLimit)

            putString(ATTENDEES, getAllAttendees(false))

            putLong(EVENT_TYPE_ID, mEventTypeId)
            putInt(EVENT_CALENDAR_ID, mEventCalendarId)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (!savedInstanceState.containsKey(START_TS)) {
            finish()
            return
        }

        savedInstanceState.apply {
            mEvent = getSerializable(EVENT) as Event
            mEventStartDateTime = Formatter.getDateTimeFromTS(getLong(START_TS))
            mEventEndDateTime = Formatter.getDateTimeFromTS(getLong(END_TS))

            mReminder1Minutes = getInt(REMINDER_1_MINUTES)
            mReminder2Minutes = getInt(REMINDER_2_MINUTES)
            mReminder3Minutes = getInt(REMINDER_3_MINUTES)

            mReminder1Type = getInt(REMINDER_1_TYPE)
            mReminder2Type = getInt(REMINDER_2_TYPE)
            mReminder3Type = getInt(REMINDER_3_TYPE)

            mRepeatInterval = getInt(REPEAT_INTERVAL)
            mRepeatRule = getInt(REPEAT_RULE)
            mRepeatLimit = getLong(REPEAT_LIMIT)

            mAttendees = Gson().fromJson<ArrayList<Attendee>>(getString(ATTENDEES), object : TypeToken<List<Attendee>>() {}.type)
                    ?: ArrayList()

            mEventTypeId = getLong(EVENT_TYPE_ID)
            mEventCalendarId = getInt(EVENT_CALENDAR_ID)
        }

        checkRepeatTexts(mRepeatInterval)
        checkRepeatRule()
        updateTexts()
        updateEventType()
        updateCalDAVCalendar()
        checkAttendees()
    }

    private fun updateTexts() {
        updateRepetitionText()
        checkReminderTexts()
        updateStartTexts()
        updateEndTexts()
        updateAttendeesVisibility()
    }

    private fun setupEditEvent() {
        val realStart = if (mEventOccurrenceTS == 0L) mEvent.startTS else mEventOccurrenceTS
        val duration = mEvent.endTS - mEvent.startTS
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        updateActionBarTitle(getString(R.string.edit_event))
        mEventStartDateTime = Formatter.getDateTimeFromTS(realStart)
        mEventEndDateTime = Formatter.getDateTimeFromTS(realStart + duration)
        event_title.setText(mEvent.title)
        event_location.setText(mEvent.location)
        event_description.setText(mEvent.description)

        mReminder1Minutes = mEvent.reminder1Minutes
        mReminder2Minutes = mEvent.reminder2Minutes
        mReminder3Minutes = mEvent.reminder3Minutes
        mReminder1Type = mEvent.reminder1Type
        mReminder2Type = mEvent.reminder2Type
        mReminder3Type = mEvent.reminder3Type
        mRepeatInterval = mEvent.repeatInterval
        mRepeatLimit = mEvent.repeatLimit
        mRepeatRule = mEvent.repeatRule
        mEventTypeId = mEvent.eventType
        mEventCalendarId = mEvent.getCalDAVCalendarId()
        mAttendees = Gson().fromJson<ArrayList<Attendee>>(mEvent.attendees, object : TypeToken<List<Attendee>>() {}.type) ?: ArrayList()
        checkRepeatTexts(mRepeatInterval)
        checkAttendees()
    }

    private fun setupNewEvent() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        event_title.requestFocus()
        updateActionBarTitle(getString(R.string.new_event))
        if (config.defaultEventTypeId != -1L) {
            config.lastUsedCaldavCalendarId = mStoredEventTypes.firstOrNull { it.id == config.defaultEventTypeId }?.caldavCalendarId ?: 0
        }

        val isLastCaldavCalendarOK = config.caldavSync && config.getSyncedCalendarIdsAsList().contains(config.lastUsedCaldavCalendarId)
        mEventCalendarId = if (isLastCaldavCalendarOK) config.lastUsedCaldavCalendarId else STORED_LOCALLY_ONLY

        if (intent.action == Intent.ACTION_EDIT || intent.action == Intent.ACTION_INSERT) {
            val startTS = intent.getLongExtra("beginTime", System.currentTimeMillis()) / 1000L
            mEventStartDateTime = Formatter.getDateTimeFromTS(startTS)

            val endTS = intent.getLongExtra("endTime", System.currentTimeMillis()) / 1000L
            mEventEndDateTime = Formatter.getDateTimeFromTS(endTS)

            if (intent.getBooleanExtra("allDay", false)) {
                mEvent.flags = mEvent.flags or FLAG_ALL_DAY
                event_all_day.isChecked = true
                toggleAllDay(true)
            }

            event_title.setText(intent.getStringExtra("title"))
            event_location.setText(intent.getStringExtra("eventLocation"))
            event_description.setText(intent.getStringExtra("description"))
            if (event_description.value.isNotEmpty()) {
                event_description.movementMethod = LinkMovementMethod.getInstance()
            }
        } else {
            val startTS = intent.getLongExtra(NEW_EVENT_START_TS, 0L)
            val dateTime = Formatter.getDateTimeFromTS(startTS)
            mEventStartDateTime = dateTime

            val addMinutes = if (intent.getBooleanExtra(NEW_EVENT_SET_HOUR_DURATION, false)) {
                60
            } else {
                config.defaultDuration
            }
            mEventEndDateTime = mEventStartDateTime.plusMinutes(addMinutes)
        }

        checkAttendees()
    }

    private fun checkAttendees() {
        ensureBackgroundThread {
            fillAvailableContacts()
            runOnUiThread {
                updateAttendees()
            }
        }
    }

    private fun handleNotificationAvailability(callback: () -> Unit) {
        if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            callback()
        } else {
            ConfirmationDialog(this, messageId = R.string.notifications_disabled, positive = R.string.ok, negative = 0) {
                callback()
            }
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

    private fun setRepeatLimit(limit: Long) {
        mRepeatLimit = limit
        checkRepetitionLimitText()
    }

    private fun checkRepetitionLimitText() {
        event_repetition_limit.text = when {
            mRepeatLimit == 0L -> {
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
        SelectEventTypeDialog(this, mEventTypeId, false, true, false, true) {
            mEventTypeId = it.id!!
            updateEventType()
        }
    }

    private fun checkReminderTexts() {
        updateReminder1Text()
        updateReminder2Text()
        updateReminder3Text()
        updateReminderTypeImages()
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

    private fun showReminderTypePicker(currentValue: Int, callback: (Int) -> Unit) {
        val items = arrayListOf(
                RadioItem(REMINDER_NOTIFICATION, getString(R.string.notification)),
                RadioItem(REMINDER_EMAIL, getString(R.string.email))
        )
        RadioGroupDialog(this, items, currentValue) {
            callback(it as Int)
        }
    }

    private fun updateReminderTypeImages() {
        updateReminderTypeImage(event_reminder_1_type, Reminder(mReminder1Minutes, mReminder1Type))
        updateReminderTypeImage(event_reminder_2_type, Reminder(mReminder2Minutes, mReminder2Type))
        updateReminderTypeImage(event_reminder_3_type, Reminder(mReminder3Minutes, mReminder3Type))
    }

    private fun updateAttendeesVisibility() {
        val isSyncedEvent = mEventCalendarId != STORED_LOCALLY_ONLY
        event_attendees_image.beVisibleIf(isSyncedEvent)
        event_attendees_holder.beVisibleIf(isSyncedEvent)
        event_attendees_divider.beVisibleIf(isSyncedEvent)
    }

    private fun updateReminderTypeImage(view: ImageView, reminder: Reminder) {
        view.beVisibleIf(reminder.minutes != REMINDER_OFF && mEventCalendarId != STORED_LOCALLY_ONLY)
        val drawable = if (reminder.type == REMINDER_NOTIFICATION) R.drawable.ic_bell_vector else R.drawable.ic_email_vector
        val icon = resources.getColoredDrawableWithColor(drawable, config.textColor)
        view.setImageDrawable(icon)
    }

    private fun updateRepetitionText() {
        event_repetition.text = getRepetitionText(mRepeatInterval)
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

    private fun updateCalDAVCalendar() {
        if (config.caldavSync) {
            event_caldav_calendar_image.beVisible()
            event_caldav_calendar_holder.beVisible()
            event_caldav_calendar_divider.beVisible()

            val calendars = calDAVHelper.getCalDAVCalendars("", true).filter {
                it.canWrite() && config.getSyncedCalendarIdsAsList().contains(it.id)
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
                    updateReminderTypeImages()
                    updateAttendeesVisibility()
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
        event_caldav_calendar_color.beGoneIf(currentCalendar == null)

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

            ensureBackgroundThread {
                val calendarColor = eventsHelper.getEventTypeWithCalDAVCalendarId(currentCalendar.id)?.color
                        ?: currentCalendar.color

                runOnUiThread {
                    event_caldav_calendar_color.setFillWithStroke(calendarColor, config.backgroundColor)
                    event_caldav_calendar_name.apply {
                        text = currentCalendar.displayName
                        setPadding(paddingLeft, paddingTop, paddingRight, resources.getDimension(R.dimen.tiny_margin).toInt())
                    }

                    event_caldav_calendar_holder.apply {
                        setPadding(paddingLeft, 0, paddingRight, 0)
                    }
                }
            }
        }
    }

    private fun resetTime() {
        if (mEventEndDateTime.isBefore(mEventStartDateTime) &&
                mEventStartDateTime.dayOfMonth() == mEventEndDateTime.dayOfMonth() &&
                mEventStartDateTime.monthOfYear() == mEventEndDateTime.monthOfYear()) {

            mEventEndDateTime = mEventEndDateTime.withTime(mEventStartDateTime.hourOfDay, mEventStartDateTime.minuteOfHour, mEventStartDateTime.secondOfMinute, 0)
            updateEndTimeText()
            checkStartEndValidity()
        }
    }

    private fun toggleAllDay(isChecked: Boolean) {
        hideKeyboard()
        event_start_time.beGoneIf(isChecked)
        event_end_time.beGoneIf(isChecked)
        resetTime()
    }

    private fun shareEvent() {
        shareEvents(arrayListOf(mEvent.id!!))
    }

    private fun deleteEvent() {
        if (mEvent.id == null) {
            return
        }

        DeleteEventDialog(this, arrayListOf(mEvent.id!!), mEvent.repeatInterval > 0) {
            ensureBackgroundThread {
                when (it) {
                    DELETE_SELECTED_OCCURRENCE -> eventsHelper.addEventRepetitionException(mEvent.id!!, mEventOccurrenceTS, true)
                    DELETE_FUTURE_OCCURRENCES -> eventsHelper.addEventRepeatLimit(mEvent.id!!, mEventOccurrenceTS)
                    DELETE_ALL_OCCURRENCES -> eventsHelper.deleteEvent(mEvent.id!!, true)
                }
                runOnUiThread {
                    finish()
                }
            }
        }
    }

    private fun duplicateEvent() {
        // the activity has the singleTask launchMode to avoid some glitches, so finish it before relaunching
        finish()
        Intent(this, EventActivity::class.java).apply {
            putExtra(EVENT_ID, mEvent.id)
            putExtra(EVENT_OCCURRENCE_TS, mEventOccurrenceTS)
            putExtra(IS_DUPLICATE_INTENT, true)
            startActivity(this)
        }
    }

    private fun saveCurrentEvent() {
        ensureBackgroundThread {
            saveEvent()
        }
    }

    private fun saveEvent() {
        val newTitle = event_title.value
        if (newTitle.isEmpty()) {
            toast(R.string.title_empty)
            runOnUiThread {
                event_title.requestFocus()
            }
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
        val newImportId = if (mEvent.id != null) mEvent.importId else UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis().toString()

        val newEventType = if (!config.caldavSync || config.lastUsedCaldavCalendarId == 0 || mEventCalendarId == STORED_LOCALLY_ONLY) {
            mEventTypeId
        } else {
            calDAVHelper.getCalDAVCalendars("", true).firstOrNull { it.id == mEventCalendarId }?.apply {
                if (!canWrite()) {
                    runOnUiThread {
                        toast(R.string.insufficient_permissions)
                    }
                    return
                }
            }

            eventsHelper.getEventTypeWithCalDAVCalendarId(mEventCalendarId)?.id ?: config.lastUsedLocalEventTypeId
        }

        val newSource = if (!config.caldavSync || mEventCalendarId == STORED_LOCALLY_ONLY) {
            config.lastUsedLocalEventTypeId = newEventType
            SOURCE_SIMPLE_CALENDAR
        } else {
            "$CALDAV-$mEventCalendarId"
        }

        var reminders = arrayListOf(
                Reminder(mReminder1Minutes, mReminder1Type),
                Reminder(mReminder2Minutes, mReminder2Type),
                Reminder(mReminder3Minutes, mReminder3Type)
        )
        reminders = reminders.filter { it.minutes != REMINDER_OFF }.sortedBy { it.minutes }.toMutableList() as ArrayList<Reminder>

        val reminder1 = reminders.getOrNull(0) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)
        val reminder2 = reminders.getOrNull(1) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)
        val reminder3 = reminders.getOrNull(2) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)

        mReminder1Type = if (mEventCalendarId == STORED_LOCALLY_ONLY) REMINDER_NOTIFICATION else reminder1.type
        mReminder2Type = if (mEventCalendarId == STORED_LOCALLY_ONLY) REMINDER_NOTIFICATION else reminder2.type
        mReminder3Type = if (mEventCalendarId == STORED_LOCALLY_ONLY) REMINDER_NOTIFICATION else reminder3.type

        config.apply {
            if (usePreviousEventReminders) {
                lastEventReminderMinutes1 = reminder1.minutes
                lastEventReminderMinutes2 = reminder2.minutes
                lastEventReminderMinutes3 = reminder3.minutes
            }
        }

        mEvent.apply {
            startTS = newStartTS
            endTS = newEndTS
            title = newTitle
            description = event_description.value
            reminder1Minutes = reminder1.minutes
            reminder2Minutes = reminder2.minutes
            reminder3Minutes = reminder3.minutes
            reminder1Type = mReminder1Type
            reminder2Type = mReminder2Type
            reminder3Type = mReminder3Type
            repeatInterval = mRepeatInterval
            importId = newImportId
            flags = mEvent.flags.addBitIf(event_all_day.isChecked, FLAG_ALL_DAY)
            repeatLimit = if (repeatInterval == 0) 0 else mRepeatLimit
            repeatRule = mRepeatRule
            attendees = if (mEventCalendarId == STORED_LOCALLY_ONLY) "" else getAllAttendees(true)
            eventType = newEventType
            lastUpdated = System.currentTimeMillis()
            source = newSource
            location = event_location.value
        }

        // recreate the event if it was moved in a different CalDAV calendar
        if (mEvent.id != null && oldSource != newSource) {
            eventsHelper.deleteEvent(mEvent.id!!, true)
            mEvent.id = null
        }

        storeEvent(wasRepeatable)
    }

    private fun storeEvent(wasRepeatable: Boolean) {
        if (mEvent.id == null || mEvent.id == null) {
            eventsHelper.insertEvent(mEvent, true, true) {
                if (DateTime.now().isAfter(mEventStartDateTime.millis)) {
                    if (mEvent.repeatInterval == 0 && mEvent.getReminders().any { it.type == REMINDER_NOTIFICATION }) {
                        notifyEvent(mEvent)
                    }
                }

                finish()
            }
        } else {
            if (mRepeatInterval > 0 && wasRepeatable) {
                runOnUiThread {
                    showEditRepeatingEventDialog()
                }
            } else {
                eventsHelper.updateEvent(mEvent, true, true) {
                    finish()
                }
            }
        }
    }

    private fun showEditRepeatingEventDialog() {
        EditRepeatingEventDialog(this) {
            if (it) {
                ensureBackgroundThread {
                    eventsHelper.updateEvent(mEvent, true, true) {
                        finish()
                    }
                }
            } else {
                ensureBackgroundThread {
                    eventsHelper.addEventRepetitionException(mEvent.id!!, mEventOccurrenceTS, true)
                    mEvent.apply {
                        parentId = id!!.toLong()
                        id = null
                        repeatRule = 0
                        repeatInterval = 0
                        repeatLimit = 0
                    }

                    eventsHelper.insertEvent(mEvent, true, true) {
                        finish()
                    }
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

    private fun setupStartDate() {
        hideKeyboard()
        config.backgroundColor.getContrastColor()
        val datepicker = DatePickerDialog(this, mDialogTheme, startDateSetListener, mEventStartDateTime.year, mEventStartDateTime.monthOfYear - 1,
                mEventStartDateTime.dayOfMonth)

        datepicker.datePicker.firstDayOfWeek = if (config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        datepicker.show()
    }

    private fun setupStartTime() {
        hideKeyboard()
        TimePickerDialog(this, mDialogTheme, startTimeSetListener, mEventStartDateTime.hourOfDay, mEventStartDateTime.minuteOfHour, config.use24HourFormat).show()
    }

    private fun setupEndDate() {
        hideKeyboard()
        val datepicker = DatePickerDialog(this, mDialogTheme, endDateSetListener, mEventEndDateTime.year, mEventEndDateTime.monthOfYear - 1,
                mEventEndDateTime.dayOfMonth)

        datepicker.datePicker.firstDayOfWeek = if (config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
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

            mEventEndDateTime = mEventStartDateTime.plusSeconds(diff.toInt())
            updateEndTexts()
        } else {
            mEventEndDateTime = mEventEndDateTime.withDate(year, month + 1, day)
            updateEndDateText()
        }
    }

    private fun timeSet(hours: Int, minutes: Int, isStart: Boolean) {
        try {
            if (isStart) {
                val diff = mEventEndDateTime.seconds() - mEventStartDateTime.seconds()

                mEventStartDateTime = mEventStartDateTime.withHourOfDay(hours).withMinuteOfHour(minutes)
                updateStartTimeText()

                mEventEndDateTime = mEventStartDateTime.plusSeconds(diff.toInt())
                updateEndTexts()
            } else {
                mEventEndDateTime = mEventEndDateTime.withHourOfDay(hours).withMinuteOfHour(minutes)
                updateEndTimeText()
            }
        } catch (e: Exception) {
            timeSet(hours + 1, minutes, isStart)
            return
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

    private fun fillAvailableContacts() {
        mAvailableContacts = getEmails()

        val names = getNames()
        mAvailableContacts.forEach {
            val contactId = it.contactId
            val contact = names.firstOrNull { it.contactId == contactId }
            val name = contact?.name
            if (name != null) {
                it.name = name
            }

            val photoUri = contact?.photoUri
            if (photoUri != null) {
                it.photoUri = photoUri
            }
        }
    }

    private fun updateAttendees() {
        val currentCalendar = calDAVHelper.getCalDAVCalendars("", true).firstOrNull { it.id == mEventCalendarId }
        mAttendees.forEach {
            it.isMe = it.email == currentCalendar?.accountName
        }

        mAttendees.sortWith(compareBy<Attendee>
        { it.isMe }.thenBy
        { it.status == CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED }.thenBy
        { it.status == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED }.thenBy
        { it.status == CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE }.thenBy
        { it.status })
        mAttendees.reverse()

        mAttendees.forEach {
            val attendee = it
            val deviceContact = mAvailableContacts.firstOrNull { it.email.isNotEmpty() && it.email == attendee.email && it.photoUri.isNotEmpty() }
            if (deviceContact != null) {
                attendee.photoUri = deviceContact.photoUri
            }
            addAttendee(attendee)
        }
        addAttendee()

        val imageHeight = event_repetition_image.height
        if (imageHeight > 0) {
            event_attendees_image.layoutParams.height = imageHeight
        } else {
            event_repetition_image.onGlobalLayout {
                event_attendees_image.layoutParams.height = event_repetition_image.height
            }
        }
    }

    private fun addAttendee(attendee: Attendee? = null) {
        val attendeeHolder = layoutInflater.inflate(R.layout.item_attendee, event_attendees_holder, false) as RelativeLayout
        val autoCompleteView = attendeeHolder.event_attendee
        val selectedAttendeeHolder = attendeeHolder.event_contact_attendee
        val selectedAttendeeDismiss = attendeeHolder.event_contact_dismiss

        mAttendeeAutoCompleteViews.add(autoCompleteView)
        autoCompleteView.onTextChangeListener {
            if (mWasContactsPermissionChecked) {
                checkNewAttendeeField()
            } else {
                handlePermission(PERMISSION_READ_CONTACTS) {
                    checkNewAttendeeField()
                    mWasContactsPermissionChecked = true
                }
            }
        }

        event_attendees_holder.addView(attendeeHolder)

        val textColor = config.textColor
        autoCompleteView.setColors(textColor, getAdjustedPrimaryColor(), config.backgroundColor)
        selectedAttendeeHolder.event_contact_name.setColors(textColor, getAdjustedPrimaryColor(), config.backgroundColor)
        selectedAttendeeHolder.event_contact_me_status.setColors(textColor, getAdjustedPrimaryColor(), config.backgroundColor)
        selectedAttendeeDismiss.applyColorFilter(textColor)

        selectedAttendeeDismiss.setOnClickListener {
            attendeeHolder.beGone()
            mSelectedContacts = mSelectedContacts.filter { it.toString() != selectedAttendeeDismiss.tag }.toMutableList() as ArrayList<Attendee>
        }

        val adapter = AutoCompleteTextViewAdapter(this, mAvailableContacts)
        autoCompleteView.setAdapter(adapter)
        autoCompleteView.imeOptions = EditorInfo.IME_ACTION_NEXT
        autoCompleteView.setOnItemClickListener { parent, view, position, id ->
            val currAttendees = (autoCompleteView.adapter as AutoCompleteTextViewAdapter).resultList
            val selectedAttendee = currAttendees[position]
            addSelectedAttendee(selectedAttendee, autoCompleteView, selectedAttendeeHolder)
        }

        if (attendee != null) {
            addSelectedAttendee(attendee, autoCompleteView, selectedAttendeeHolder)
        }
    }

    private fun addSelectedAttendee(attendee: Attendee, autoCompleteView: MyAutoCompleteTextView, selectedAttendeeHolder: RelativeLayout) {
        mSelectedContacts.add(attendee)

        autoCompleteView.beGone()
        autoCompleteView.focusSearch(View.FOCUS_DOWN)?.requestFocus()

        selectedAttendeeHolder.apply {
            beVisible()

            val attendeeStatusBackground = resources.getDrawable(R.drawable.attendee_status_circular_background)
            (attendeeStatusBackground as LayerDrawable).findDrawableByLayerId(R.id.attendee_status_circular_background).applyColorFilter(config.backgroundColor)
            event_contact_status_image.apply {
                background = attendeeStatusBackground
                setImageDrawable(getAttendeeStatusImage(attendee))
                beVisibleIf(attendee.showStatusImage())
            }

            event_contact_image.apply {
                attendee.updateImage(applicationContext, this, mAttendeePlaceholder)
                beVisible()
            }

            event_contact_dismiss.apply {
                tag = attendee.toString()
                beGoneIf(attendee.isMe)
            }

            event_contact_name.text = if (attendee.isMe) getString(R.string.my_status) else attendee.getPublicName()
            if (attendee.isMe) {
                (event_contact_name.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.START_OF, event_contact_me_status.id)
            }

            if (attendee.isMe) {
                updateAttendeeMe(this, attendee)
            }

            event_contact_me_status.apply {
                beVisibleIf(attendee.isMe)
            }

            if (attendee.isMe) {
                event_contact_attendee.setOnClickListener {
                    val items = arrayListOf(
                            RadioItem(CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED, getString(R.string.going)),
                            RadioItem(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED, getString(R.string.not_going)),
                            RadioItem(CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE, getString(R.string.maybe_going))
                    )

                    RadioGroupDialog(this@EventActivity, items, attendee.status) {
                        attendee.status = it as Int
                        updateAttendeeMe(this, attendee)
                    }
                }
            }
        }
    }

    private fun getAttendeeStatusImage(attendee: Attendee): Drawable {
        return resources.getDrawable(when (attendee.status) {
            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED -> R.drawable.ic_check_green
            CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED -> R.drawable.ic_cross_red
            else -> R.drawable.ic_question_yellow
        })
    }

    private fun updateAttendeeMe(holder: RelativeLayout, attendee: Attendee) {
        holder.apply {
            event_contact_me_status.text = getString(when (attendee.status) {
                CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED -> R.string.going
                CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED -> R.string.not_going
                CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE -> R.string.maybe_going
                else -> R.string.invited
            })

            event_contact_status_image.apply {
                beVisibleIf(attendee.showStatusImage())
                setImageDrawable(getAttendeeStatusImage(attendee))
            }

            mAttendees.firstOrNull { it.isMe }?.status = attendee.status
        }
    }

    private fun checkNewAttendeeField() {
        if (mAttendeeAutoCompleteViews.none { it.isVisible() && it.value.isEmpty() }) {
            addAttendee()
        }
    }

    private fun getAllAttendees(isSavingEvent: Boolean): String {
        var attendees = ArrayList<Attendee>()
        mSelectedContacts.forEach {
            attendees.add(it)
        }

        val customEmails = mAttendeeAutoCompleteViews.filter { it.isVisible() }.map { it.value }.filter { it.isNotEmpty() }.toMutableList() as ArrayList<String>
        customEmails.mapTo(attendees) {
            Attendee(0, "", it, CalendarContract.Attendees.ATTENDEE_STATUS_INVITED, "", false, CalendarContract.Attendees.RELATIONSHIP_NONE)
        }
        attendees = attendees.distinctBy { it.email }.toMutableList() as ArrayList<Attendee>

        if (mEvent.id == null && isSavingEvent && attendees.isNotEmpty()) {
            val currentCalendar = calDAVHelper.getCalDAVCalendars("", true).firstOrNull { it.id == mEventCalendarId }
            mAvailableContacts.firstOrNull { it.email == currentCalendar?.accountName }?.apply {
                attendees = attendees.filter { it.email != currentCalendar?.accountName }.toMutableList() as ArrayList<Attendee>
                status = CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
                relationship = CalendarContract.Attendees.RELATIONSHIP_ORGANIZER
                attendees.add(this)
            }
        }

        return Gson().toJson(attendees)
    }

    private fun getNames(): List<Attendee> {
        val contacts = ArrayList<Attendee>()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.StructuredName.PREFIX,
                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                ContactsContract.CommonDataKinds.StructuredName.SUFFIX,
                ContactsContract.CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI)

        val selection = "${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.CONTACT_ID)
                    val prefix = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX) ?: ""
                    val firstName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME) ?: ""
                    val middleName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME) ?: ""
                    val surname = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME) ?: ""
                    val suffix = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX) ?: ""
                    val photoUri = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI) ?: ""

                    val names = arrayListOf(prefix, firstName, middleName, surname, suffix).filter { it.trim().isNotEmpty() }
                    val fullName = TextUtils.join("", names)
                    if (fullName.isNotEmpty() || photoUri.isNotEmpty()) {
                        val contact = Attendee(id, fullName, "", CalendarContract.Attendees.ATTENDEE_STATUS_NONE, photoUri, false, CalendarContract.Attendees.RELATIONSHIP_NONE)
                        contacts.add(contact)
                    }
                } while (cursor.moveToNext())
            }
        } catch (ignored: Exception) {
        } finally {
            cursor?.close()
        }
        return contacts
    }

    private fun getEmails(): ArrayList<Attendee> {
        val contacts = ArrayList<Attendee>()
        val uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.DATA
        )

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, null, null, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.CONTACT_ID)
                    val email = cursor.getStringValue(ContactsContract.CommonDataKinds.Email.DATA) ?: continue
                    val contact = Attendee(id, "", email, CalendarContract.Attendees.ATTENDEE_STATUS_NONE, "", false, CalendarContract.Attendees.RELATIONSHIP_NONE)
                    contacts.add(contact)
                } while (cursor.moveToNext())
            }
        } catch (ignored: Exception) {
        } finally {
            cursor?.close()
        }
        return contacts
    }

    private fun updateIconColors() {
        val textColor = config.textColor
        event_time_image.applyColorFilter(textColor)
        event_repetition_image.applyColorFilter(textColor)
        event_reminder_image.applyColorFilter(textColor)
        event_type_image.applyColorFilter(textColor)
        event_caldav_calendar_image.applyColorFilter(textColor)
        event_show_on_map.applyColorFilter(getAdjustedPrimaryColor())
        event_reminder_1_type.applyColorFilter(textColor)
        event_reminder_2_type.applyColorFilter(textColor)
        event_reminder_3_type.applyColorFilter(textColor)
        event_attendees_image.applyColorFilter(textColor)
    }
}
