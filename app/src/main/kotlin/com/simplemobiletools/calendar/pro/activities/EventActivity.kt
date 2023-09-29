package com.simplemobiletools.calendar.pro.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Colors
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.RelativeLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.adapters.AutoCompleteTextViewAdapter
import com.simplemobiletools.calendar.pro.databinding.ActivityEventBinding
import com.simplemobiletools.calendar.pro.databinding.ItemAttendeeBinding
import com.simplemobiletools.calendar.pro.dialogs.*
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.models.*
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.dialogs.ConfirmationAdvancedDialog
import com.simplemobiletools.commons.dialogs.PermissionRequiredDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.views.MyAutoCompleteTextView
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.TimeZone
import java.util.regex.Pattern

class EventActivity : SimpleActivity() {
    private val LAT_LON_PATTERN = "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)([,;])\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"
    private val SELECT_TIME_ZONE_INTENT = 1

    private var mIsAllDayEvent = false
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
    private var mEventOccurrenceTS = 0L
    private var mLastSavePromptTS = 0L
    private var mEventCalendarId = STORED_LOCALLY_ONLY
    private var mWasContactsPermissionChecked = false
    private var mWasCalendarChanged = false
    private var mAttendees = ArrayList<Attendee>()
    private var mAttendeeAutoCompleteViews = ArrayList<MyAutoCompleteTextView>()
    private var mAvailableContacts = ArrayList<Attendee>()
    private var mSelectedContacts = ArrayList<Attendee>()
    private var mAvailability = Attendees.AVAILABILITY_BUSY
    private var mStoredEventTypes = ArrayList<EventType>()
    private var mOriginalTimeZone = DateTimeZone.getDefault().id
    private var mOriginalStartTS = 0L
    private var mOriginalEndTS = 0L
    private var mIsNewEvent = true
    private var mEventColor = 0

    private lateinit var mEventStartDateTime: DateTime
    private lateinit var mEventEndDateTime: DateTime
    private lateinit var mEvent: Event

    private val binding by viewBinding(ActivityEventBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()
        refreshMenuItems()

        if (checkAppSideloading()) {
            return
        }

        updateMaterialActivityViews(binding.eventCoordinator, binding.eventHolder, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(binding.eventNestedScrollview, binding.eventToolbar)

        val intent = intent ?: return
        mWasContactsPermissionChecked = hasPermission(PERMISSION_READ_CONTACTS)

        val eventId = intent.getLongExtra(EVENT_ID, 0L)
        ensureBackgroundThread {
            mStoredEventTypes = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
            val event = eventsDB.getEventWithId(eventId)
            if (eventId != 0L && event == null) {
                hideKeyboard()
                finish()
                return@ensureBackgroundThread
            }

            val localEventType = mStoredEventTypes.firstOrNull { it.id == config.lastUsedLocalEventTypeId }
            runOnUiThread {
                if (!isDestroyed && !isFinishing) {
                    gotEvent(savedInstanceState, localEventType, event)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.eventToolbar, NavigationIcon.Arrow)
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - mLastSavePromptTS > SAVE_DISCARD_PROMPT_INTERVAL && isEventChanged()) {
            mLastSavePromptTS = System.currentTimeMillis()
            ConfirmationAdvancedDialog(
                activity = this,
                message = "",
                messageId = com.simplemobiletools.commons.R.string.save_before_closing,
                positive = com.simplemobiletools.commons.R.string.save,
                negative = com.simplemobiletools.commons.R.string.discard
            ) {
                if (it) {
                    saveCurrentEvent()
                } else {
                    super.onBackPressed()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!::mEvent.isInitialized) {
            return
        }

        outState.apply {
            putSerializable(EVENT, mEvent)
            putLong(START_TS, mEventStartDateTime.seconds())
            putLong(END_TS, mEventEndDateTime.seconds())
            putString(TIME_ZONE, mEvent.timeZone)

            putInt(REMINDER_1_MINUTES, mReminder1Minutes)
            putInt(REMINDER_2_MINUTES, mReminder2Minutes)
            putInt(REMINDER_3_MINUTES, mReminder3Minutes)

            putInt(REMINDER_1_TYPE, mReminder1Type)
            putInt(REMINDER_2_TYPE, mReminder2Type)
            putInt(REMINDER_3_TYPE, mReminder3Type)

            putInt(REPEAT_INTERVAL, mRepeatInterval)
            putInt(REPEAT_RULE, mRepeatRule)
            putLong(REPEAT_LIMIT, mRepeatLimit)

            putString(ATTENDEES, Gson().toJson(getAllAttendees(false)))

            putInt(AVAILABILITY, mAvailability)
            putInt(EVENT_COLOR, mEventColor)

            putLong(EVENT_TYPE_ID, mEventTypeId)
            putInt(EVENT_CALENDAR_ID, mEventCalendarId)
            putBoolean(IS_NEW_EVENT, mIsNewEvent)
            putLong(ORIGINAL_START_TS, mOriginalStartTS)
            putLong(ORIGINAL_END_TS, mOriginalEndTS)
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
            mEvent = getSerializable(EVENT) as Event
            mEventStartDateTime = Formatter.getDateTimeFromTS(getLong(START_TS))
            mEventEndDateTime = Formatter.getDateTimeFromTS(getLong(END_TS))
            mEvent.timeZone = getString(TIME_ZONE) ?: TimeZone.getDefault().id

            mReminder1Minutes = getInt(REMINDER_1_MINUTES)
            mReminder2Minutes = getInt(REMINDER_2_MINUTES)
            mReminder3Minutes = getInt(REMINDER_3_MINUTES)

            mReminder1Type = getInt(REMINDER_1_TYPE)
            mReminder2Type = getInt(REMINDER_2_TYPE)
            mReminder3Type = getInt(REMINDER_3_TYPE)

            mAvailability = getInt(AVAILABILITY)
            mEventColor = getInt(EVENT_COLOR)

            mRepeatInterval = getInt(REPEAT_INTERVAL)
            mRepeatRule = getInt(REPEAT_RULE)
            mRepeatLimit = getLong(REPEAT_LIMIT)

            val token = object : TypeToken<List<Attendee>>() {}.type
            mAttendees = Gson().fromJson<ArrayList<Attendee>>(getString(ATTENDEES), token) ?: ArrayList()

            mEventTypeId = getLong(EVENT_TYPE_ID)
            mEventCalendarId = getInt(EVENT_CALENDAR_ID)
            mIsNewEvent = getBoolean(IS_NEW_EVENT)
            mOriginalStartTS = getLong(ORIGINAL_START_TS)
            mOriginalEndTS = getLong(ORIGINAL_END_TS)
        }

        checkRepeatTexts(mRepeatInterval)
        checkRepeatRule()
        updateTexts()
        updateEventType()
        updateCalDAVCalendar()
        checkAttendees()
        updateActionBarTitle()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == SELECT_TIME_ZONE_INTENT && resultCode == Activity.RESULT_OK && resultData?.hasExtra(TIME_ZONE) == true) {
            val timeZone = resultData.getSerializableExtra(TIME_ZONE) as MyTimeZone
            mEvent.timeZone = timeZone.zoneName
            updateTimeZoneText()
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun gotEvent(savedInstanceState: Bundle?, localEventType: EventType?, event: Event?) = binding.apply {
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
                eventToolbar.title = getString(R.string.new_event)
            } else {
                cancelNotification(mEvent.id!!)
            }
        } else {
            mEvent = Event(null)
            config.apply {
                mReminder1Minutes = if (usePreviousEventReminders && lastEventReminderMinutes1 >= -1) lastEventReminderMinutes1 else defaultReminder1
                mReminder2Minutes = if (usePreviousEventReminders && lastEventReminderMinutes2 >= -1) lastEventReminderMinutes2 else defaultReminder2
                mReminder3Minutes = if (usePreviousEventReminders && lastEventReminderMinutes3 >= -1) lastEventReminderMinutes3 else defaultReminder3
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

        eventShowOnMap.setOnClickListener { showOnMap() }
        eventStartDate.setOnClickListener { setupStartDate() }
        eventStartTime.setOnClickListener { setupStartTime() }
        eventEndDate.setOnClickListener { setupEndDate() }
        eventEndTime.setOnClickListener { setupEndTime() }
        eventTimeZone.setOnClickListener { setupTimeZone() }

        eventAllDay.setOnCheckedChangeListener { _, isChecked -> toggleAllDay(isChecked) }
        eventRepetition.setOnClickListener { showRepeatIntervalDialog() }
        eventRepetitionRuleHolder.setOnClickListener { showRepetitionRuleDialog() }
        eventRepetitionLimitHolder.setOnClickListener { showRepetitionTypePicker() }

        eventReminder1.setOnClickListener {
            handleNotificationAvailability {
                if (config.wasAlarmWarningShown) {
                    showReminder1Dialog()
                } else {
                    ReminderWarningDialog(this@EventActivity) {
                        config.wasAlarmWarningShown = true
                        showReminder1Dialog()
                    }
                }
            }
        }

        eventReminder2.setOnClickListener { showReminder2Dialog() }
        eventReminder3.setOnClickListener { showReminder3Dialog() }

        eventReminder1Type.setOnClickListener {
            showReminderTypePicker(mReminder1Type) {
                mReminder1Type = it
                updateReminderTypeImage(eventReminder1Type, Reminder(mReminder1Minutes, mReminder1Type))
            }
        }

        eventReminder2Type.setOnClickListener {
            showReminderTypePicker(mReminder2Type) {
                mReminder2Type = it
                updateReminderTypeImage(eventReminder2Type, Reminder(mReminder2Minutes, mReminder2Type))
            }
        }

        eventReminder3Type.setOnClickListener {
            showReminderTypePicker(mReminder3Type) {
                mReminder3Type = it
                updateReminderTypeImage(eventReminder3Type, Reminder(mReminder3Minutes, mReminder3Type))
            }
        }

        eventAvailability.setOnClickListener {
            showAvailabilityPicker(mAvailability) {
                mAvailability = it
                updateAvailabilityText()
                updateAvailabilityImage()
            }
        }

        eventTypeHolder.setOnClickListener { showEventTypeDialog() }
        eventAllDay.apply {
            isChecked = mEvent.getIsAllDay()
            jumpDrawablesToCurrentState()
        }

        eventAllDayHolder.setOnClickListener {
            eventAllDay.toggle()
        }

        eventColorHolder.setOnClickListener {
            showEventColorDialog()
        }

        updateTextColors(eventNestedScrollview)
        updateIconColors()
        refreshMenuItems()
        showOrHideTimeZone()
    }

    private fun refreshMenuItems() {
        if (::mEvent.isInitialized) {
            binding.eventToolbar.menu.apply {
                findItem(R.id.delete).isVisible = mEvent.id != null
                findItem(R.id.share).isVisible = mEvent.id != null
                findItem(R.id.duplicate).isVisible = mEvent.id != null
            }
        }
    }

    private fun setupOptionsMenu() {
        binding.eventToolbar.setOnMenuItemClickListener { menuItem ->
            if (!::mEvent.isInitialized) {
                return@setOnMenuItemClickListener true
            }

            when (menuItem.itemId) {
                R.id.save -> saveCurrentEvent()
                R.id.delete -> deleteEvent()
                R.id.duplicate -> duplicateEvent()
                R.id.share -> shareEvent()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun getStartEndTimes(): Pair<Long, Long> {
        if (mIsAllDayEvent) {
            val newStartTS = mEventStartDateTime.withTimeAtStartOfDay().seconds()
            val newEndTS = mEventEndDateTime.withTimeAtStartOfDay().withHourOfDay(12).seconds()
            return Pair(newStartTS, newEndTS)
        } else {
            val offset = if (!config.allowChangingTimeZones || mEvent.getTimeZoneString().equals(mOriginalTimeZone, true)) {
                0
            } else {
                val original = mOriginalTimeZone.ifEmpty { DateTimeZone.getDefault().id }
                val millis = System.currentTimeMillis()
                val newOffset = DateTimeZone.forID(mEvent.getTimeZoneString()).getOffset(millis)
                val oldOffset = DateTimeZone.forID(original).getOffset(millis)
                (newOffset - oldOffset) / 1000L
            }

            val newStartTS = mEventStartDateTime.seconds() - offset
            val newEndTS = mEventEndDateTime.seconds() - offset
            return Pair(newStartTS, newEndTS)
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

    private fun isEventChanged(): Boolean {
        if (!::mEvent.isInitialized) {
            return false
        }

        var newStartTS: Long
        var newEndTS: Long
        getStartEndTimes().apply {
            newStartTS = first
            newEndTS = second
        }

        val hasTimeChanged = if (mOriginalStartTS == 0L) {
            mEvent.startTS != newStartTS || mEvent.endTS != newEndTS
        } else {
            mOriginalStartTS != newStartTS || mOriginalEndTS != newEndTS
        }

        val reminders = getReminders()
        if (binding.eventTitle.text.toString() != mEvent.title ||
            binding.eventLocation.text.toString() != mEvent.location ||
            binding.eventDescription.text.toString() != mEvent.description ||
            binding.eventTimeZone.text != mEvent.getTimeZoneString() ||
            reminders != mEvent.getReminders() ||
            mRepeatInterval != mEvent.repeatInterval ||
            mRepeatRule != mEvent.repeatRule ||
            mEventTypeId != mEvent.eventType ||
            mWasCalendarChanged ||
            mIsAllDayEvent != mEvent.getIsAllDay() ||
            mEventColor != mEvent.color ||
            hasTimeChanged
        ) {
            return true
        }

        return false
    }

    private fun updateTexts() {
        updateRepetitionText()
        checkReminderTexts()
        updateStartTexts()
        updateEndTexts()
        updateTimeZoneText()
        updateCalDAVVisibility()
        updateAvailabilityText()
        updateAvailabilityImage()
    }

    private fun setupEditEvent() {
        mIsNewEvent = false
        val realStart = if (mEventOccurrenceTS == 0L) mEvent.startTS else mEventOccurrenceTS
        val duration = mEvent.endTS - mEvent.startTS
        mOriginalStartTS = realStart
        mOriginalEndTS = realStart + duration

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        binding.eventToolbar.title = getString(R.string.edit_event)
        mOriginalTimeZone = mEvent.timeZone
        if (config.allowChangingTimeZones) {
            try {
                mEventStartDateTime = Formatter.getDateTimeFromTS(realStart).withZone(DateTimeZone.forID(mOriginalTimeZone))
                mEventEndDateTime = Formatter.getDateTimeFromTS(realStart + duration).withZone(DateTimeZone.forID(mOriginalTimeZone))
            } catch (e: Exception) {
                showErrorToast(e)
                mEventStartDateTime = Formatter.getDateTimeFromTS(realStart)
                mEventEndDateTime = Formatter.getDateTimeFromTS(realStart + duration)
            }
        } else {
            mEventStartDateTime = Formatter.getDateTimeFromTS(realStart)
            mEventEndDateTime = Formatter.getDateTimeFromTS(realStart + duration)
        }

        binding.eventTitle.setText(mEvent.title)
        binding.eventLocation.setText(mEvent.location)
        binding.eventDescription.setText(mEvent.description)

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
        mAvailability = mEvent.availability
        mEventColor = mEvent.color

        mAttendees = mEvent.attendees.toMutableList() as ArrayList<Attendee>

        checkRepeatTexts(mRepeatInterval)
        checkAttendees()
    }

    private fun setupNewEvent() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        binding.eventTitle.requestFocus()
        binding.eventToolbar.title = getString(R.string.new_event)
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
                binding.eventAllDay.isChecked = true
                toggleAllDay(true)
            }

            binding.eventTitle.setText(intent.getStringExtra("title"))
            binding.eventLocation.setText(intent.getStringExtra("eventLocation"))
            binding.eventDescription.setText(intent.getStringExtra("description"))
            if (binding.eventDescription.value.isNotEmpty()) {
                binding.eventDescription.movementMethod = LinkMovementMethod.getInstance()
            }
        } else {
            val startTS = intent.getLongExtra(NEW_EVENT_START_TS, 0L)
            val dateTime = Formatter.getDateTimeFromTS(startTS)
            mEventStartDateTime = dateTime

            val addMinutes = if (intent.getBooleanExtra(NEW_EVENT_SET_HOUR_DURATION, false)) {
                // if an event is created at 23:00 on the weekly view, make it end on 23:59 by default to avoid spanning across multiple days
                if (mEventStartDateTime.hourOfDay == 23) {
                    59
                } else {
                    60
                }
            } else {
                config.defaultDuration
            }
            mEventEndDateTime = mEventStartDateTime.plusMinutes(addMinutes)
        }
        addDefValuesToNewEvent()
        checkAttendees()
    }

    private fun addDefValuesToNewEvent() {
        var newStartTS: Long
        var newEndTS: Long
        getStartEndTimes().apply {
            newStartTS = first
            newEndTS = second
        }

        mEvent.apply {
            startTS = newStartTS
            endTS = newEndTS
            reminder1Minutes = mReminder1Minutes
            reminder1Type = mReminder1Type
            reminder2Minutes = mReminder2Minutes
            reminder2Type = mReminder2Type
            reminder3Minutes = mReminder3Minutes
            reminder3Type = mReminder3Type
            eventType = mEventTypeId
        }
    }

    private fun checkAttendees() {
        ensureBackgroundThread {
            fillAvailableContacts()
            updateAttendees()
        }
    }

    private fun showReminder1Dialog() {
        showPickSecondsDialogHelper(mReminder1Minutes, showDuringDayOption = mIsAllDayEvent) {
            mReminder1Minutes = if (it == -1 || it == 0) it else it / 60
            checkReminderTexts()
        }
    }

    private fun showReminder2Dialog() {
        showPickSecondsDialogHelper(mReminder2Minutes, showDuringDayOption = mIsAllDayEvent) {
            mReminder2Minutes = if (it == -1 || it == 0) it else it / 60
            checkReminderTexts()
        }
    }

    private fun showReminder3Dialog() {
        showPickSecondsDialogHelper(mReminder3Minutes, showDuringDayOption = mIsAllDayEvent) {
            mReminder3Minutes = if (it == -1 || it == 0) it else it / 60
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
        binding.eventRepetitionLimitHolder.beGoneIf(limit == 0)
        checkRepetitionLimitText()

        binding.eventRepetitionRuleHolder.beVisibleIf(mRepeatInterval.isXWeeklyRepetition() || mRepeatInterval.isXMonthlyRepetition() || mRepeatInterval.isXYearlyRepetition())
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
        binding.eventRepetitionLimit.text = when {
            mRepeatLimit == 0L -> {
                binding.eventRepetitionLimitLabel.text = getString(R.string.repeat)
                resources.getString(R.string.forever)
            }

            mRepeatLimit > 0 -> {
                binding.eventRepetitionLimitLabel.text = getString(R.string.repeat_till)
                val repeatLimitDateTime = Formatter.getDateTimeFromTS(mRepeatLimit)
                Formatter.getFullDate(this, repeatLimitDateTime)
            }

            else -> {
                binding.eventRepetitionLimitLabel.text = getString(R.string.repeat)
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

        items.add(RadioItem(REPEAT_ORDER_WEEKDAY, getRepeatXthDayString(true, REPEAT_ORDER_WEEKDAY)))
        if (isLastWeekDayOfMonth()) {
            items.add(RadioItem(REPEAT_ORDER_WEEKDAY_USE_LAST, getRepeatXthDayString(true, REPEAT_ORDER_WEEKDAY_USE_LAST)))
        }

        if (isLastDayOfTheMonth()) {
            items.add(RadioItem(REPEAT_LAST_DAY, getString(R.string.repeat_on_the_last_day_monthly)))
        }
        return items
    }

    private fun getAvailableYearlyRepetitionRules(): ArrayList<RadioItem> {
        val items = arrayListOf(RadioItem(REPEAT_SAME_DAY, getString(R.string.repeat_on_the_same_day_yearly)))

        items.add(RadioItem(REPEAT_ORDER_WEEKDAY, getRepeatXthDayInMonthString(true, REPEAT_ORDER_WEEKDAY)))
        if (isLastWeekDayOfMonth()) {
            items.add(RadioItem(REPEAT_ORDER_WEEKDAY_USE_LAST, getRepeatXthDayInMonthString(true, REPEAT_ORDER_WEEKDAY_USE_LAST)))
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
        return getString(
            if (isMaleGender(day)) {
                R.string.repeat_every_m
            } else {
                R.string.repeat_every_f
            }
        )
    }

    private fun isMaleGender(day: Int) = day == 1 || day == 2 || day == 4 || day == 5

    private fun getOrderString(repeatRule: Int): String {
        val dayOfMonth = mEventStartDateTime.dayOfMonth
        var order = (dayOfMonth - 1) / 7 + 1
        if (isLastWeekDayOfMonth() && repeatRule == REPEAT_ORDER_WEEKDAY_USE_LAST) {
            order = -1
        }

        val isMale = isMaleGender(mEventStartDateTime.dayOfWeek)
        return getString(
            when (order) {
                1 -> if (isMale) R.string.first_m else R.string.first_f
                2 -> if (isMale) R.string.second_m else R.string.second_f
                3 -> if (isMale) R.string.third_m else R.string.third_f
                4 -> if (isMale) R.string.fourth_m else R.string.fourth_f
                5 -> if (isMale) R.string.fifth_m else R.string.fifth_f
                else -> if (isMale) R.string.last_m else R.string.last_f
            }
        )
    }

    private fun getDayString(day: Int): String {
        return getString(
            when (day) {
                1 -> R.string.monday_alt
                2 -> R.string.tuesday_alt
                3 -> R.string.wednesday_alt
                4 -> R.string.thursday_alt
                5 -> R.string.friday_alt
                6 -> R.string.saturday_alt
                else -> R.string.sunday_alt
            }
        )
    }

    private fun getRepeatXthDayInMonthString(includeBase: Boolean, repeatRule: Int): String {
        val weekDayString = getRepeatXthDayString(includeBase, repeatRule)
        val monthString = resources.getStringArray(com.simplemobiletools.commons.R.array.in_months)[mEventStartDateTime.monthOfYear - 1]
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
                binding.eventRepetitionRule.text = if (mRepeatRule == EVERY_DAY_BIT) {
                    getString(com.simplemobiletools.commons.R.string.every_day)
                } else {
                    getShortDaysFromBitmask(mRepeatRule)
                }
            }

            mRepeatInterval.isXMonthlyRepetition() -> {
                val repeatString = if (mRepeatRule == REPEAT_ORDER_WEEKDAY_USE_LAST || mRepeatRule == REPEAT_ORDER_WEEKDAY)
                    R.string.repeat else R.string.repeat_on

                binding.eventRepetitionRuleLabel.text = getString(repeatString)
                binding.eventRepetitionRule.text = getMonthlyRepetitionRuleText()
            }

            mRepeatInterval.isXYearlyRepetition() -> {
                val repeatString = if (mRepeatRule == REPEAT_ORDER_WEEKDAY_USE_LAST || mRepeatRule == REPEAT_ORDER_WEEKDAY)
                    R.string.repeat else R.string.repeat_on

                binding.eventRepetitionRuleLabel.text = getString(repeatString)
                binding.eventRepetitionRule.text = getYearlyRepetitionRuleText()
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

        SelectEventTypeDialog(
            activity = this,
            currEventType = mEventTypeId,
            showCalDAVCalendars = false,
            showNewEventTypeOption = true,
            addLastUsedOneAsFirstOption = false,
            showOnlyWritable = true,
            showManageEventTypes = true
        ) {
            mEventTypeId = it.id!!
            updateEventType()
        }
    }

    private fun showEventColorDialog() {
        hideKeyboard()
        ensureBackgroundThread {
            val isLocalEvent = mEventCalendarId == STORED_LOCALLY_ONLY
            if (isLocalEvent) {
                showCustomEventColorDialog()
            } else {
                showCalDAVEventColorDialog()
            }
        }
    }

    private fun showCustomEventColorDialog() {
        val eventType = eventTypesDB.getEventTypeWithId(mEventTypeId)!!
        val currentColor = if (mEventColor == 0) {
            eventType.color
        } else {
            mEventColor
        }

        runOnUiThread {
            ColorPickerDialog(activity = this, color = currentColor, addDefaultColorButton = true) { wasPositivePressed, newColor ->
                if (wasPositivePressed) {
                    gotNewEventColor(newColor, currentColor, eventType.color)
                }
            }
        }
    }

    private fun showCalDAVEventColorDialog() {
        val eventType = eventsHelper.getEventTypeWithCalDAVCalendarId(calendarId = mEventCalendarId)!!
        val eventColors = getEventColors(eventType)
        val currentColor = if (mEventColor == 0) {
            eventType.color
        } else {
            mEventColor
        }

        runOnUiThread {
            SelectEventColorDialog(activity = this, colors = eventColors, currentColor = currentColor) { newColor ->
                gotNewEventColor(newColor, currentColor, eventType.color)
            }
        }
    }

    private fun gotNewEventColor(newColor: Int, currentColor: Int, defaultColor: Int) {
        if (newColor != currentColor) {
            mEventColor = newColor
            updateEventColorInfo(defaultColor = defaultColor)
        }
    }

    private fun checkReminderTexts() {
        updateReminder1Text()
        updateReminder2Text()
        updateReminder3Text()
        updateReminderTypeImages()
    }

    private fun updateReminder1Text() {
        binding.eventReminder1.text = getFormattedMinutes(mReminder1Minutes)
    }

    private fun updateReminder2Text() {
        binding.eventReminder2.apply {
            beGoneIf(isGone() && mReminder1Minutes == REMINDER_OFF)
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
        binding.eventReminder3.apply {
            beGoneIf(isGone() && (mReminder2Minutes == REMINDER_OFF || mReminder1Minutes == REMINDER_OFF))
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
            RadioItem(REMINDER_NOTIFICATION, getString(com.simplemobiletools.commons.R.string.notification)),
            RadioItem(REMINDER_EMAIL, getString(com.simplemobiletools.commons.R.string.email))
        )
        RadioGroupDialog(this, items, currentValue) {
            callback(it as Int)
        }
    }

    private fun showAvailabilityPicker(currentValue: Int, callback: (Int) -> Unit) {
        val items = arrayListOf(
            RadioItem(Attendees.AVAILABILITY_BUSY, getString(R.string.status_busy)),
            RadioItem(Attendees.AVAILABILITY_FREE, getString(R.string.status_free))
        )
        RadioGroupDialog(this, items, currentValue) {
            callback(it as Int)
        }
    }

    private fun updateReminderTypeImages() {
        updateReminderTypeImage(binding.eventReminder1Type, Reminder(mReminder1Minutes, mReminder1Type))
        updateReminderTypeImage(binding.eventReminder2Type, Reminder(mReminder2Minutes, mReminder2Type))
        updateReminderTypeImage(binding.eventReminder3Type, Reminder(mReminder3Minutes, mReminder3Type))
    }

    private fun updateCalDAVVisibility() {
        val isSyncedEvent = mEventCalendarId != STORED_LOCALLY_ONLY
        binding.eventAttendeesImage.beVisibleIf(isSyncedEvent)
        binding.eventAttendeesHolder.beVisibleIf(isSyncedEvent)
        binding.eventAttendeesDivider.beVisibleIf(isSyncedEvent)
        binding.eventAvailabilityDivider.beVisibleIf(isSyncedEvent)
        binding.eventAvailabilityImage.beVisibleIf(isSyncedEvent)
        binding.eventAvailability.beVisibleIf(isSyncedEvent)
    }

    private fun updateReminderTypeImage(view: ImageView, reminder: Reminder) {
        view.beVisibleIf(reminder.minutes != REMINDER_OFF && mEventCalendarId != STORED_LOCALLY_ONLY)
        val drawable = if (reminder.type == REMINDER_NOTIFICATION) {
            com.simplemobiletools.commons.R.drawable.ic_bell_vector
        } else {
            com.simplemobiletools.commons.R.drawable.ic_mail_vector
        }

        val icon = resources.getColoredDrawableWithColor(drawable, getProperTextColor())
        view.setImageDrawable(icon)
    }

    private fun updateAvailabilityImage() {
        val drawable = if (mAvailability == Attendees.AVAILABILITY_FREE) R.drawable.ic_event_available_vector else R.drawable.ic_event_busy_vector
        val icon = resources.getColoredDrawableWithColor(drawable, getProperTextColor())
        binding.eventAvailabilityImage.setImageDrawable(icon)
    }

    private fun updateAvailabilityText() {
        binding.eventAvailability.text = if (mAvailability == Attendees.AVAILABILITY_FREE) getString(R.string.status_free) else getString(R.string.status_busy)
    }

    private fun updateRepetitionText() {
        binding.eventRepetition.text = getRepetitionText(mRepeatInterval)
    }

    private fun updateEventType() {
        ensureBackgroundThread {
            val eventType = eventTypesDB.getEventTypeWithId(mEventTypeId)
            if (eventType != null) {
                runOnUiThread {
                    binding.eventType.text = eventType.title
                    updateEventColorInfo(eventType.color)
                }
            }
        }
    }

    private fun updateCalDAVCalendar() {
        if (config.caldavSync) {
            binding.eventCaldavCalendarImage.beVisible()
            binding.eventCaldavCalendarHolder.beVisible()
            binding.eventCaldavCalendarDivider.beVisible()

            val calendars = calDAVHelper.getCalDAVCalendars("", true).filter {
                it.canWrite() && config.getSyncedCalendarIdsAsList().contains(it.id)
            }
            updateCurrentCalendarInfo(if (mEventCalendarId == STORED_LOCALLY_ONLY) null else getCalendarWithId(calendars, getCalendarId()))

            binding.eventCaldavCalendarHolder.setOnClickListener {
                hideKeyboard()
                SelectEventCalendarDialog(this, calendars, mEventCalendarId) {
                    if (mEventCalendarId != STORED_LOCALLY_ONLY && it == STORED_LOCALLY_ONLY) {
                        mEventTypeId = config.lastUsedLocalEventTypeId
                        updateEventType()
                    }
                    mWasCalendarChanged = true
                    mEventCalendarId = it
                    config.lastUsedCaldavCalendarId = it
                    updateCurrentCalendarInfo(getCalendarWithId(calendars, it))
                    updateReminderTypeImages()
                    updateCalDAVVisibility()
                    updateAvailabilityText()
                    updateAvailabilityImage()
                }
            }
        } else {
            updateCurrentCalendarInfo(null)
        }
    }

    private fun getCalendarId() = if (mEvent.source == SOURCE_SIMPLE_CALENDAR) config.lastUsedCaldavCalendarId else mEvent.getCalDAVCalendarId()

    private fun getCalendarWithId(calendars: List<CalDAVCalendar>, calendarId: Int) = calendars.firstOrNull { it.id == calendarId }

    private fun updateCurrentCalendarInfo(currentCalendar: CalDAVCalendar?) = binding.apply {
        eventTypeImage.beVisibleIf(currentCalendar == null)
        eventTypeHolder.beVisibleIf(currentCalendar == null)
        eventCaldavCalendarDivider.beVisibleIf(currentCalendar == null)
        eventCaldavCalendarEmail.beGoneIf(currentCalendar == null)

        if (currentCalendar == null) {
            mEventCalendarId = STORED_LOCALLY_ONLY
            val mediumMargin = resources.getDimension(com.simplemobiletools.commons.R.dimen.medium_margin).toInt()
            eventCaldavCalendarName.apply {
                text = getString(R.string.store_locally_only)
                setPadding(paddingLeft, paddingTop, paddingRight, mediumMargin)
            }

            eventCaldavCalendarHolder.apply {
                setPadding(paddingLeft, mediumMargin, paddingRight, mediumMargin)
            }

            ensureBackgroundThread {
                val eventType = eventTypesDB.getEventTypeWithId(mEventTypeId)
                runOnUiThread {
                    eventColorImage.beVisibleIf(eventType != null)
                    eventColorHolder.beVisibleIf(eventType != null)
                    eventColorDivider.beVisibleIf(eventType != null)
                    if (eventType != null) {
                        updateEventColorInfo(eventType.color)
                    }
                }
            }
        } else {
            eventCaldavCalendarEmail.text = currentCalendar.accountName

            ensureBackgroundThread {
                val eventType = eventsHelper.getEventTypeWithCalDAVCalendarId(currentCalendar.id)
                val calendarColor = eventType?.color ?: currentCalendar.color
                val canCustomizeColors = if (eventType != null) {
                    getEventColors(eventType).isNotEmpty()
                } else {
                    false
                }

                runOnUiThread {
                    eventCaldavCalendarName.apply {
                        text = currentCalendar.displayName
                        setPadding(paddingLeft, paddingTop, paddingRight, resources.getDimension(com.simplemobiletools.commons.R.dimen.tiny_margin).toInt())
                    }

                    eventCaldavCalendarHolder.apply {
                        setPadding(paddingLeft, 0, paddingRight, 0)
                    }

                    eventColorImage.beVisibleIf(canCustomizeColors)
                    eventColorHolder.beVisibleIf(canCustomizeColors)
                    eventColorDivider.beVisibleIf(canCustomizeColors)
                    if (canCustomizeColors) {
                        updateEventColorInfo(calendarColor)
                    }
                }
            }
        }
    }

    private fun updateEventColorInfo(defaultColor: Int) {
        val eventColor = if (mEventColor == 0) {
            defaultColor
        } else {
            mEventColor
        }
        binding.eventColor.setFillWithStroke(eventColor, getProperBackgroundColor())
    }

    private fun getEventColors(eventType: EventType): IntArray {
        return calDAVHelper.getAvailableCalDAVCalendarColors(eventType, Colors.TYPE_EVENT).keys.toIntArray()
    }

    private fun resetTime() {
        if (mEventEndDateTime.isBefore(mEventStartDateTime) &&
            mEventStartDateTime.dayOfMonth() == mEventEndDateTime.dayOfMonth() &&
            mEventStartDateTime.monthOfYear() == mEventEndDateTime.monthOfYear()
        ) {

            mEventEndDateTime =
                mEventEndDateTime.withTime(mEventStartDateTime.hourOfDay, mEventStartDateTime.minuteOfHour, mEventStartDateTime.secondOfMinute, 0)
            updateEndTimeText()
            checkStartEndValidity()
        }
    }

    private fun toggleAllDay(isAllDay: Boolean) {
        hideKeyboard()
        mIsAllDayEvent = isAllDay
        binding.eventStartTime.beGoneIf(isAllDay)
        binding.eventEndTime.beGoneIf(isAllDay)
        updateTimeZoneText()
        showOrHideTimeZone()
        resetTime()
    }

    private fun showOrHideTimeZone() {
        val allowChangingTimeZones = config.allowChangingTimeZones && !mIsAllDayEvent
        binding.eventTimeZoneDivider.beVisibleIf(allowChangingTimeZones)
        binding.eventTimeZoneImage.beVisibleIf(allowChangingTimeZones)
        binding.eventTimeZone.beVisibleIf(allowChangingTimeZones)
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
                    DELETE_SELECTED_OCCURRENCE -> eventsHelper.deleteRepeatingEventOccurrence(mEvent.id!!, mEventOccurrenceTS, true)
                    DELETE_FUTURE_OCCURRENCES -> eventsHelper.addEventRepeatLimit(mEvent.id!!, mEventOccurrenceTS)
                    DELETE_ALL_OCCURRENCES -> eventsHelper.deleteEvent(mEvent.id!!, true)
                }

                runOnUiThread {
                    hideKeyboard()
                    finish()
                }
            }
        }
    }

    private fun duplicateEvent() {
        // the activity has the singleTask launchMode to avoid some glitches, so finish it before relaunching
        hideKeyboard()
        finish()
        Intent(this, EventActivity::class.java).apply {
            putExtra(EVENT_ID, mEvent.id)
            putExtra(EVENT_OCCURRENCE_TS, mEventOccurrenceTS)
            putExtra(IS_DUPLICATE_INTENT, true)
            startActivity(this)
        }
    }

    private fun saveCurrentEvent() {
        if (config.wasAlarmWarningShown || (mReminder1Minutes == REMINDER_OFF && mReminder2Minutes == REMINDER_OFF && mReminder3Minutes == REMINDER_OFF)) {
            ensureBackgroundThread {
                saveEvent()
            }
        } else {
            ReminderWarningDialog(this) {
                config.wasAlarmWarningShown = true
                ensureBackgroundThread {
                    saveEvent()
                }
            }
        }
    }

    private fun saveEvent() {
        val newTitle = binding.eventTitle.value
        if (newTitle.isEmpty()) {
            toast(R.string.title_empty)
            runOnUiThread {
                binding.eventTitle.requestFocus()
            }
            return
        }

        var newStartTS: Long
        var newEndTS: Long
        getStartEndTimes().apply {
            newStartTS = first
            newEndTS = second
        }

        if (newStartTS > newEndTS) {
            toast(R.string.end_before_start)
            return
        }

        val wasRepeatable = mEvent.repeatInterval > 0
        val oldSource = mEvent.source
        val newImportId = if (mEvent.id != null) {
            mEvent.importId
        } else {
            generateImportId()
        }

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

        val reminders = getReminders()
        if (!binding.eventAllDay.isChecked) {
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
            description = binding.eventDescription.value
            reminder1Minutes = reminder1.minutes
            reminder2Minutes = reminder2.minutes
            reminder3Minutes = reminder3.minutes
            reminder1Type = mReminder1Type
            reminder2Type = mReminder2Type
            reminder3Type = mReminder3Type
            repeatInterval = mRepeatInterval
            importId = newImportId
            timeZone = if (mIsAllDayEvent || timeZone.isEmpty()) DateTimeZone.getDefault().id else timeZone
            flags = mEvent.flags.addBitIf(binding.eventAllDay.isChecked, FLAG_ALL_DAY)
            repeatLimit = if (repeatInterval == 0) 0 else mRepeatLimit
            repeatRule = mRepeatRule
            attendees = if (mEventCalendarId == STORED_LOCALLY_ONLY) emptyList() else getAllAttendees(true)
            eventType = newEventType
            lastUpdated = System.currentTimeMillis()
            source = newSource
            location = binding.eventLocation.value
            availability = mAvailability
            color = mEventColor
        }

        // recreate the event if it was moved in a different CalDAV calendar
        if (mEvent.id != null && oldSource != newSource && oldSource != SOURCE_IMPORTED_ICS) {
            if (mRepeatInterval > 0 && wasRepeatable) {
                eventsHelper.applyOriginalStartEndTimes(mEvent, mOriginalStartTS, mOriginalEndTS)
            }
            eventsHelper.deleteEvent(mEvent.id!!, true)
            mEvent.id = null
        }

        if (mEvent.getReminders().isNotEmpty()) {
            handleNotificationPermission { granted ->
                if (granted) {
                    ensureBackgroundThread {
                        storeEvent(wasRepeatable)
                    }
                } else {
                    PermissionRequiredDialog(this, com.simplemobiletools.commons.R.string.allow_notifications_reminders, { openNotificationSettings() })
                }
            }
        } else {
            storeEvent(wasRepeatable)
        }
    }

    private fun storeEvent(wasRepeatable: Boolean) {
        if (mEvent.id == null) {
            eventsHelper.insertEvent(mEvent, addToCalDAV = true, showToasts = true) {
                hideKeyboard()

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
                hideKeyboard()
                eventsHelper.updateEvent(mEvent, updateAtCalDAV = true, showToasts = true) {
                    finish()
                }
            }
        }
    }

    private fun showEditRepeatingEventDialog() {
        EditRepeatingEventDialog(this) {
            hideKeyboard()
            if (it == null) {
                return@EditRepeatingEventDialog
            }
            when (it) {
                EDIT_SELECTED_OCCURRENCE -> {
                    eventsHelper.editSelectedOccurrence(mEvent, true) {
                        finish()
                    }
                }

                EDIT_FUTURE_OCCURRENCES -> {
                    eventsHelper.editFutureOccurrences(mEvent, mEventOccurrenceTS, true) {
                        finish()
                    }
                }

                EDIT_ALL_OCCURRENCES -> {
                    eventsHelper.editAllOccurrences(mEvent, mOriginalStartTS, mOriginalEndTS, true) {
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
        binding.eventStartDate.text = Formatter.getDate(this, mEventStartDateTime)
        checkStartEndValidity()
    }

    private fun updateStartTimeText() {
        binding.eventStartTime.text = Formatter.getTime(this, mEventStartDateTime)
        checkStartEndValidity()
    }

    private fun updateEndTexts() {
        updateEndDateText()
        updateEndTimeText()
    }

    private fun updateEndDateText() {
        binding.eventEndDate.text = Formatter.getDate(this, mEventEndDateTime)
        checkStartEndValidity()
    }

    private fun updateEndTimeText() {
        binding.eventEndTime.text = Formatter.getTime(this, mEventEndDateTime)
        checkStartEndValidity()
    }

    private fun updateTimeZoneText() {
        binding.eventTimeZone.text = mEvent.getTimeZoneString()
    }

    private fun checkStartEndValidity() {
        val textColor = if (mEventStartDateTime.isAfter(mEventEndDateTime)) resources.getColor(R.color.red_text) else getProperTextColor()
        binding.eventEndDate.setTextColor(textColor)
        binding.eventEndTime.setTextColor(textColor)
    }

    private fun showOnMap() {
        if (binding.eventLocation.value.isEmpty()) {
            toast(R.string.please_fill_location)
            return
        }

        val pattern = Pattern.compile(LAT_LON_PATTERN)
        val locationValue = binding.eventLocation.value
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
        launchActivityIntent(intent)
    }

    private fun setupStartDate() {
        hideKeyboard()
        val datePicker = DatePickerDialog(
            this, getDatePickerDialogTheme(), startDateSetListener, mEventStartDateTime.year, mEventStartDateTime.monthOfYear - 1,
            mEventStartDateTime.dayOfMonth
        )

        datePicker.datePicker.firstDayOfWeek = getJavaDayOfWeekFromJoda(config.firstDayOfWeek)
        datePicker.show()
    }

    private fun setupStartTime() {
        hideKeyboard()
        if (config.isUsingSystemTheme) {
            val timeFormat = if (config.use24HourFormat) {
                TimeFormat.CLOCK_24H
            } else {
                TimeFormat.CLOCK_12H
            }

            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(timeFormat)
                .setHour(mEventStartDateTime.hourOfDay)
                .setMinute(mEventStartDateTime.minuteOfHour)
                .setInputMode(INPUT_MODE_CLOCK)
                .build()

            timePicker.addOnPositiveButtonClickListener {
                timeSet(timePicker.hour, timePicker.minute, true)
            }

            timePicker.show(supportFragmentManager, "")
        } else {
            TimePickerDialog(
                this,
                getTimePickerDialogTheme(),
                startTimeSetListener,
                mEventStartDateTime.hourOfDay,
                mEventStartDateTime.minuteOfHour,
                config.use24HourFormat
            ).show()
        }
    }

    private fun setupEndDate() {
        hideKeyboard()
        val datePicker = DatePickerDialog(
            this, getDatePickerDialogTheme(), endDateSetListener, mEventEndDateTime.year, mEventEndDateTime.monthOfYear - 1,
            mEventEndDateTime.dayOfMonth
        )

        datePicker.datePicker.firstDayOfWeek = getJavaDayOfWeekFromJoda(config.firstDayOfWeek)
        datePicker.show()
    }

    private fun setupEndTime() {
        hideKeyboard()
        if (config.isUsingSystemTheme) {
            val timeFormat = if (config.use24HourFormat) {
                TimeFormat.CLOCK_24H
            } else {
                TimeFormat.CLOCK_12H
            }

            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(timeFormat)
                .setHour(mEventEndDateTime.hourOfDay)
                .setMinute(mEventEndDateTime.minuteOfHour)
                .setInputMode(INPUT_MODE_CLOCK)
                .build()

            timePicker.addOnPositiveButtonClickListener {
                timeSet(timePicker.hour, timePicker.minute, false)
            }

            timePicker.show(supportFragmentManager, "")
        } else {
            TimePickerDialog(
                this,
                getTimePickerDialogTheme(),
                endTimeSetListener,
                mEventEndDateTime.hourOfDay,
                mEventEndDateTime.minuteOfHour,
                config.use24HourFormat
            ).show()
        }
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

    private fun setupTimeZone() {
        hideKeyboard()
        Intent(this, SelectTimeZoneActivity::class.java).apply {
            putExtra(CURRENT_TIME_ZONE, mEvent.getTimeZoneString())
            startActivityForResult(this, SELECT_TIME_ZONE_INTENT)
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
            it.isMe = it.email == currentCalendar?.ownerName
        }

        mAttendees.sortWith(compareBy<Attendee>
        { it.isMe }.thenBy
        { it.status == Attendees.ATTENDEE_STATUS_ACCEPTED }.thenBy
        { it.status == Attendees.ATTENDEE_STATUS_DECLINED }.thenBy
        { it.status == Attendees.ATTENDEE_STATUS_TENTATIVE }.thenBy
        { it.status })
        mAttendees.reverse()

        runOnUiThread {
            mAttendees.forEach {
                val attendee = it
                val deviceContact = mAvailableContacts.firstOrNull { it.email.isNotEmpty() && it.email == attendee.email && it.photoUri.isNotEmpty() }
                if (deviceContact != null) {
                    attendee.photoUri = deviceContact.photoUri
                }
                addAttendee(attendee)
            }
            addAttendee()

            binding.apply {
                val imageHeight = eventRepetitionImage.height
                if (imageHeight > 0) {
                    eventAttendeesImage.layoutParams.height = imageHeight
                } else {
                    eventRepetitionImage.onGlobalLayout {
                        eventAttendeesImage.layoutParams.height = eventRepetitionImage.height
                    }
                }
            }
        }
    }

    private fun addAttendee(attendee: Attendee? = null) {
        val attendeeHolder = ItemAttendeeBinding.inflate(layoutInflater, binding.eventAttendeesHolder, false)
        val autoCompleteView = attendeeHolder.eventAttendee
        val selectedAttendeeDismiss = attendeeHolder.eventContactDismiss

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

        binding.eventAttendeesHolder.addView(attendeeHolder.root)

        val textColor = getProperTextColor()
        val backgroundColor = getProperBackgroundColor()
        val primaryColor = getProperPrimaryColor()
        autoCompleteView.setColors(textColor, primaryColor, backgroundColor)
        attendeeHolder.eventContactName.setColors(textColor, primaryColor, backgroundColor)
        attendeeHolder.eventContactMeStatus.setColors(textColor, primaryColor, backgroundColor)
        selectedAttendeeDismiss.applyColorFilter(textColor)

        selectedAttendeeDismiss.setOnClickListener {
            attendeeHolder.root.beGone()
            mSelectedContacts = mSelectedContacts.filter { it.toString() != selectedAttendeeDismiss.tag }.toMutableList() as ArrayList<Attendee>
        }

        val adapter = AutoCompleteTextViewAdapter(this, mAvailableContacts)
        autoCompleteView.setAdapter(adapter)
        autoCompleteView.imeOptions = EditorInfo.IME_ACTION_NEXT
        autoCompleteView.setOnItemClickListener { parent, view, position, id ->
            val currAttendees = (autoCompleteView.adapter as AutoCompleteTextViewAdapter).resultList
            val selectedAttendee = currAttendees[position]
            addSelectedAttendee(selectedAttendee, autoCompleteView, attendeeHolder)
        }

        if (attendee != null) {
            addSelectedAttendee(attendee, autoCompleteView, attendeeHolder)
        }
    }

    private fun addSelectedAttendee(attendee: Attendee, autoCompleteView: MyAutoCompleteTextView, attendeeHolder: ItemAttendeeBinding) {
        mSelectedContacts.add(attendee)

        autoCompleteView.beGone()
        autoCompleteView.focusSearch(View.FOCUS_DOWN)?.requestFocus()

        attendeeHolder.apply {
            eventContactAttendee.beVisible()

            val attendeeStatusBackground = resources.getDrawable(R.drawable.attendee_status_circular_background)
            (attendeeStatusBackground as LayerDrawable).findDrawableByLayerId(R.id.attendee_status_circular_background)
                .applyColorFilter(getProperBackgroundColor())
            eventContactStatusImage.apply {
                background = attendeeStatusBackground
                setImageDrawable(getAttendeeStatusImage(attendee))
                beVisibleIf(attendee.showStatusImage())
            }

            eventContactName.text = if (attendee.isMe) getString(R.string.my_status) else attendee.getPublicName()
            if (attendee.isMe) {
                (eventContactName.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.START_OF, eventContactMeStatus.id)
            }

            val placeholder = BitmapDrawable(resources, SimpleContactsHelper(this@EventActivity).getContactLetterIcon(eventContactName.value))
            eventContactImage.apply {
                attendee.updateImage(this@EventActivity, this, placeholder)
                beVisible()
            }

            eventContactDismiss.apply {
                tag = attendee.toString()
                beGoneIf(attendee.isMe)
            }

            if (attendee.isMe) {
                updateAttendeeMe(this, attendee)
            }

            eventContactMeStatus.apply {
                beVisibleIf(attendee.isMe)
            }

            if (attendee.isMe) {
                eventContactAttendee.setOnClickListener {
                    val items = arrayListOf(
                        RadioItem(Attendees.ATTENDEE_STATUS_ACCEPTED, getString(R.string.going)),
                        RadioItem(Attendees.ATTENDEE_STATUS_DECLINED, getString(R.string.not_going)),
                        RadioItem(Attendees.ATTENDEE_STATUS_TENTATIVE, getString(R.string.maybe_going))
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
        return resources.getDrawable(
            when (attendee.status) {
                Attendees.ATTENDEE_STATUS_ACCEPTED -> R.drawable.ic_check_green
                Attendees.ATTENDEE_STATUS_DECLINED -> R.drawable.ic_cross_red
                else -> R.drawable.ic_question_yellow
            }
        )
    }

    private fun updateAttendeeMe(holder: ItemAttendeeBinding, attendee: Attendee) {
        holder.apply {
            eventContactMeStatus.text = getString(
                when (attendee.status) {
                    Attendees.ATTENDEE_STATUS_ACCEPTED -> R.string.going
                    Attendees.ATTENDEE_STATUS_DECLINED -> R.string.not_going
                    Attendees.ATTENDEE_STATUS_TENTATIVE -> R.string.maybe_going
                    else -> R.string.invited
                }
            )

            eventContactStatusImage.apply {
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

    private fun getAllAttendees(isSavingEvent: Boolean): ArrayList<Attendee> {
        var attendees = ArrayList<Attendee>()
        mSelectedContacts.forEach {
            attendees.add(it)
        }

        val customEmails = mAttendeeAutoCompleteViews.filter { it.isVisible() }.map { it.value }.filter { it.isNotEmpty() }.toMutableList() as ArrayList<String>
        customEmails.mapTo(attendees) {
            Attendee(0, "", it, Attendees.ATTENDEE_STATUS_INVITED, "", false, Attendees.RELATIONSHIP_NONE)
        }
        attendees = attendees.distinctBy { it.email }.toMutableList() as ArrayList<Attendee>

        if (mEvent.id == null && isSavingEvent && attendees.isNotEmpty()) {
            val currentCalendar = calDAVHelper.getCalDAVCalendars("", true).firstOrNull { it.id == mEventCalendarId }
            mAvailableContacts.firstOrNull { it.email == currentCalendar?.ownerName }?.apply {
                attendees = attendees.filter { it.email != currentCalendar?.ownerName }.toMutableList() as ArrayList<Attendee>
                status = Attendees.ATTENDEE_STATUS_ACCEPTED
                relationship = Attendees.RELATIONSHIP_ORGANIZER
                attendees.add(this)
            }
        }

        return attendees
    }

    private fun getNames(): List<Attendee> {
        val contacts = ArrayList<Attendee>()
        val uri = Data.CONTENT_URI
        val projection = arrayOf(
            Data.CONTACT_ID,
            StructuredName.PREFIX,
            StructuredName.GIVEN_NAME,
            StructuredName.MIDDLE_NAME,
            StructuredName.FAMILY_NAME,
            StructuredName.SUFFIX,
            StructuredName.PHOTO_THUMBNAIL_URI
        )

        val selection = "${Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(StructuredName.CONTENT_ITEM_TYPE)

        queryCursor(uri, projection, selection, selectionArgs) { cursor ->
            val id = cursor.getIntValue(Data.CONTACT_ID)
            val prefix = cursor.getStringValue(StructuredName.PREFIX) ?: ""
            val firstName = cursor.getStringValue(StructuredName.GIVEN_NAME) ?: ""
            val middleName = cursor.getStringValue(StructuredName.MIDDLE_NAME) ?: ""
            val surname = cursor.getStringValue(StructuredName.FAMILY_NAME) ?: ""
            val suffix = cursor.getStringValue(StructuredName.SUFFIX) ?: ""
            val photoUri = cursor.getStringValue(StructuredName.PHOTO_THUMBNAIL_URI) ?: ""

            val names = arrayListOf(prefix, firstName, middleName, surname, suffix).filter { it.trim().isNotEmpty() }
            val fullName = TextUtils.join(" ", names).trim()
            if (fullName.isNotEmpty() || photoUri.isNotEmpty()) {
                val contact = Attendee(id, fullName, "", Attendees.ATTENDEE_STATUS_NONE, photoUri, false, Attendees.RELATIONSHIP_NONE)
                contacts.add(contact)
            }
        }
        return contacts
    }

    private fun getEmails(): ArrayList<Attendee> {
        val contacts = ArrayList<Attendee>()
        val uri = CommonDataKinds.Email.CONTENT_URI
        val projection = arrayOf(
            Data.CONTACT_ID,
            CommonDataKinds.Email.DATA
        )

        queryCursor(uri, projection) { cursor ->
            val id = cursor.getIntValue(Data.CONTACT_ID)
            val email = cursor.getStringValue(CommonDataKinds.Email.DATA) ?: return@queryCursor
            val contact = Attendee(id, "", email, Attendees.ATTENDEE_STATUS_NONE, "", false, Attendees.RELATIONSHIP_NONE)
            contacts.add(contact)
        }

        return contacts
    }

    private fun updateIconColors() = binding.apply {
        eventShowOnMap.applyColorFilter(getProperPrimaryColor())
        val textColor = getProperTextColor()
        arrayOf(
            eventTimeImage, eventTimeZoneImage, eventRepetitionImage, eventReminderImage, eventTypeImage, eventCaldavCalendarImage,
            eventReminder1Type, eventReminder2Type, eventReminder3Type, eventAttendeesImage, eventAvailabilityImage, eventColorImage
        ).forEach {
            it.applyColorFilter(textColor)
        }
    }

    private fun updateActionBarTitle() {
        binding.eventToolbar.title = if (mIsNewEvent) {
            getString(R.string.new_event)
        } else {
            getString(R.string.edit_event)
        }
    }
}
