package com.simplemobiletools.calendar.pro.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.view.MenuItem
import android.widget.Toast
import com.simplemobiletools.calendar.pro.BuildConfig
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.adapters.EventListAdapter
import com.simplemobiletools.calendar.pro.adapters.QuickFilterEventTypeAdapter
import com.simplemobiletools.calendar.pro.databases.EventsDatabase
import com.simplemobiletools.calendar.pro.databinding.ActivityMainBinding
import com.simplemobiletools.calendar.pro.dialogs.SelectEventTypesDialog
import com.simplemobiletools.calendar.pro.dialogs.SetRemindersDialog
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.fragments.*
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.IcsImporter.ImportResult
import com.simplemobiletools.calendar.pro.jobs.CalDAVUpdateListener
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.ListEvent
import com.simplemobiletools.calendar.pro.models.ListItem
import com.simplemobiletools.calendar.pro.models.ListSectionDay
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.commons.models.SimpleContact
import com.simplemobiletools.commons.views.MyLinearLayoutManager
import com.simplemobiletools.commons.views.MyRecyclerView
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : SimpleActivity(), RefreshRecyclerViewListener {

    private var showCalDAVRefreshToast = false
    private var mShouldFilterBeVisible = false
    private var mLatestSearchQuery = ""
    private var shouldGoToTodayBeVisible = false
    private var goToTodayButton: MenuItem? = null
    private var currentFragments = ArrayList<MyFragmentHolder>()

    private var mStoredTextColor = 0
    private var mStoredBackgroundColor = 0
    private var mStoredPrimaryColor = 0
    private var mStoredDayCode = ""
    private var mStoredFirstDayOfWeek = 0
    private var mStoredMidnightSpan = true
    private var mStoredUse24HourFormat = false
    private var mStoredDimPastEvents = true
    private var mStoredDimCompletedTasks = true
    private var mStoredHighlightWeekends = false
    private var mStoredStartWeekWithCurrentDay = false
    private var mStoredHighlightWeekendsColor = 0

    // search results have endless scrolling, so reaching the top/bottom fetches further results
    private var minFetchedSearchTS = 0L
    private var maxFetchedSearchTS = 0L
    private var searchResultEvents = ArrayList<Event>()
    private var bottomItemAtRefresh: ListItem? = null

    private val binding by viewBinding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()
        updateMaterialActivityViews(binding.mainCoordinator, binding.mainHolder, useTransparentNavigation = false, useTopSearchMenu = true)

        checkWhatsNewDialog()
        binding.calendarFab.beVisibleIf(config.storedView != YEARLY_VIEW && config.storedView != WEEKLY_VIEW)
        binding.calendarFab.setOnClickListener {
            if (config.allowCreatingTasks) {
                if (binding.fabExtendedOverlay.isVisible()) {
                    openNewEvent()

                    Handler().postDelayed({
                        hideExtendedFab()
                    }, 300)
                } else {
                    showExtendedFab()
                }
            } else {
                openNewEvent()
            }
        }
        binding.fabEventLabel.setOnClickListener { openNewEvent() }
        binding.fabTaskLabel.setOnClickListener { openNewTask() }

        binding.fabExtendedOverlay.setOnClickListener {
            hideExtendedFab()
        }

        binding.fabTaskIcon.setOnClickListener {
            openNewTask()

            Handler().postDelayed({
                hideExtendedFab()
            }, 300)
        }

        storeStateVariables()

        if (!hasPermission(PERMISSION_WRITE_CALENDAR) || !hasPermission(PERMISSION_READ_CALENDAR)) {
            config.caldavSync = false
        }

        if (config.caldavSync) {
            refreshCalDAVCalendars(false)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshCalDAVCalendars(true)
        }

        checkIsViewIntent()

        if (!checkIsOpenIntent()) {
            updateViewPager()
        }

        checkAppOnSDCard()

        if (savedInstanceState == null) {
            checkCalDAVUpdateListener()
        }

        addBirthdaysAnniversariesAtStart()

        if (isPackageInstalled("com.simplemobiletools.calendar")) {
            ConfirmationDialog(
                activity = this,
                message = "",
                messageId = com.simplemobiletools.commons.R.string.upgraded_from_free_calendar,
                positive = com.simplemobiletools.commons.R.string.ok,
                negative = 0,
                cancelOnTouchOutside = false
            ) {}
        }

        addImportIdsToTasks {
            refreshViewPager()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mStoredTextColor != getProperTextColor() || mStoredBackgroundColor != getProperBackgroundColor() || mStoredPrimaryColor != getProperPrimaryColor()
            || mStoredDayCode != Formatter.getTodayCode() || mStoredDimPastEvents != config.dimPastEvents || mStoredDimCompletedTasks != config.dimCompletedTasks
            || mStoredHighlightWeekends != config.highlightWeekends || mStoredHighlightWeekendsColor != config.highlightWeekendsColor
        ) {
            updateViewPager()
        }

        eventsHelper.getEventTypes(this, false) {
            val newShouldFilterBeVisible = it.size > 1 || config.displayEventTypes.isEmpty()
            if (newShouldFilterBeVisible != mShouldFilterBeVisible) {
                mShouldFilterBeVisible = newShouldFilterBeVisible
                refreshMenuItems()
            }
        }

        if (config.storedView == WEEKLY_VIEW) {
            if (mStoredFirstDayOfWeek != config.firstDayOfWeek || mStoredUse24HourFormat != config.use24HourFormat
                || mStoredMidnightSpan != config.showMidnightSpanningEventsAtTop || mStoredStartWeekWithCurrentDay != config.startWeekWithCurrentDay
            ) {
                updateViewPager()
            }
        }

        updateStatusbarColor(getProperBackgroundColor())
        binding.apply {
            mainMenu.updateColors()
            storeStateVariables()
            updateWidgets()
            updateTextColors(calendarCoordinator)
            fabExtendedOverlay.background = ColorDrawable(getProperBackgroundColor().adjustAlpha(0.8f))
            fabEventLabel.setTextColor(getProperTextColor())
            fabTaskLabel.setTextColor(getProperTextColor())

            fabTaskIcon.drawable.applyColorFilter(mStoredPrimaryColor.getContrastColor())
            fabTaskIcon.background.applyColorFilter(mStoredPrimaryColor)

            searchHolder.background = ColorDrawable(getProperBackgroundColor())
            checkSwipeRefreshAvailability()
            checkShortcuts()

            if (!mainMenu.isSearchOpen) {
                refreshMenuItems()
            }
        }

        setupQuickFilter()

        if (config.caldavSync) {
            updateCalDAVEvents()
        }
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            EventsDatabase.destroyInstance()
            stopCalDAVUpdateListener()
        }
    }

    fun refreshMenuItems() {
        if (binding.fabExtendedOverlay.isVisible()) {
            hideExtendedFab()
        }

        shouldGoToTodayBeVisible = currentFragments.lastOrNull()?.shouldGoToTodayBeVisible() ?: false
        binding.mainMenu.getToolbar().menu.apply {
            goToTodayButton = findItem(R.id.go_to_today)
            findItem(R.id.print).isVisible = config.storedView != MONTHLY_DAILY_VIEW
            findItem(R.id.filter).isVisible = mShouldFilterBeVisible
            findItem(R.id.go_to_today).isVisible = shouldGoToTodayBeVisible && !binding.mainMenu.isSearchOpen
            findItem(R.id.go_to_date).isVisible = config.storedView != EVENTS_LIST_VIEW
            findItem(R.id.refresh_caldav_calendars).isVisible = config.caldavSync
            findItem(R.id.more_apps_from_us).isVisible = !resources.getBoolean(com.simplemobiletools.commons.R.bool.hide_google_relations)
        }
    }

    private fun setupOptionsMenu() = binding.apply {
        mainMenu.getToolbar().inflateMenu(R.menu.menu_main)
        mainMenu.toggleHideOnScroll(false)
        mainMenu.setupMenu()

        mainMenu.onSearchTextChangedListener = { text ->
            searchQueryChanged(text)
        }

        mainMenu.getToolbar().setOnMenuItemClickListener { menuItem ->
            if (fabExtendedOverlay.isVisible()) {
                hideExtendedFab()
            }

            when (menuItem.itemId) {
                R.id.change_view -> showViewDialog()
                R.id.go_to_today -> goToToday()
                R.id.go_to_date -> showGoToDateDialog()
                R.id.print -> printView()
                R.id.filter -> showFilterDialog()
                R.id.refresh_caldav_calendars -> refreshCalDAVCalendars(true)
                R.id.add_holidays -> addHolidays()
                R.id.add_birthdays -> tryAddBirthdays()
                R.id.add_anniversaries -> tryAddAnniversaries()
                R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
                R.id.settings -> launchSettings()
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun onBackPressed() {
        if (binding.mainMenu.isSearchOpen) {
            closeSearch()
        } else {
            binding.swipeRefreshLayout.isRefreshing = false
            checkSwipeRefreshAvailability()
            when {
                binding.fabExtendedOverlay.isVisible() -> hideExtendedFab()
                currentFragments.size > 1 -> removeTopFragment()
                else -> super.onBackPressed()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIsOpenIntent()
        checkIsViewIntent()
    }

    private fun storeStateVariables() {
        mStoredTextColor = getProperTextColor()
        mStoredPrimaryColor = getProperPrimaryColor()
        mStoredBackgroundColor = getProperBackgroundColor()
        config.apply {
            mStoredFirstDayOfWeek = firstDayOfWeek
            mStoredUse24HourFormat = use24HourFormat
            mStoredDimPastEvents = dimPastEvents
            mStoredDimCompletedTasks = dimCompletedTasks
            mStoredHighlightWeekends = highlightWeekends
            mStoredHighlightWeekendsColor = highlightWeekendsColor
            mStoredMidnightSpan = showMidnightSpanningEventsAtTop
            mStoredStartWeekWithCurrentDay = startWeekWithCurrentDay
        }
        mStoredDayCode = Formatter.getTodayCode()
    }

    private fun setupQuickFilter() {
        eventsHelper.getEventTypes(this, false) {
            val quickFilterEventTypes = config.quickFilterEventTypes
            binding.quickEventTypeFilter.adapter = QuickFilterEventTypeAdapter(this, it, quickFilterEventTypes) {
                if (config.displayEventTypes.isEmpty() && !config.wasFilteredOutWarningShown) {
                    toast(R.string.everything_filtered_out, Toast.LENGTH_LONG)
                    config.wasFilteredOutWarningShown = true
                }

                refreshViewPager()
                updateWidgets()
            }
        }
    }

    private fun closeSearch() {
        binding.mainMenu.closeSearch()
        minFetchedSearchTS = 0L
        maxFetchedSearchTS = 0L
        searchResultEvents.clear()
        bottomItemAtRefresh = null
    }

    private fun checkCalDAVUpdateListener() {
        if (isNougatPlus()) {
            val updateListener = CalDAVUpdateListener()
            if (config.caldavSync) {
                if (!updateListener.isScheduled(applicationContext)) {
                    updateListener.scheduleJob(applicationContext)
                }
            } else {
                updateListener.cancelJob(applicationContext)
            }
        }
    }

    private fun stopCalDAVUpdateListener() {
        if (isNougatPlus()) {
            if (!config.caldavSync) {
                val updateListener = CalDAVUpdateListener()
                updateListener.cancelJob(applicationContext)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun checkShortcuts() {
        val appIconColor = config.appIconColor
        if (isNougatMR1Plus() && config.lastHandledShortcutColor != appIconColor) {
            val newEvent = getNewEventShortcut(appIconColor)
            val shortcuts = arrayListOf(newEvent)

            if (config.allowCreatingTasks) {
                shortcuts.add(getNewTaskShortcut(appIconColor))
            }

            try {
                shortcutManager.dynamicShortcuts = shortcuts
                config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getNewEventShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.new_event)
        val newEventDrawable = resources.getDrawable(R.drawable.shortcut_event, theme)
        (newEventDrawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_event_background).applyColorFilter(appIconColor)
        val newEventBitmap = newEventDrawable.convertToBitmap()

        val newEventIntent = Intent(this, SplashActivity::class.java)
        newEventIntent.action = SHORTCUT_NEW_EVENT
        return ShortcutInfo.Builder(this, "new_event")
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(newEventBitmap))
            .setIntent(newEventIntent)
            .build()
    }

    @SuppressLint("NewApi")
    private fun getNewTaskShortcut(appIconColor: Int): ShortcutInfo {
        val newTask = getString(R.string.new_task)
        val newTaskDrawable = resources.getDrawable(R.drawable.shortcut_task, theme)
        (newTaskDrawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_task_background).applyColorFilter(appIconColor)
        val newTaskBitmap = newTaskDrawable.convertToBitmap()
        val newTaskIntent = Intent(this, SplashActivity::class.java)
        newTaskIntent.action = SHORTCUT_NEW_TASK
        return ShortcutInfo.Builder(this, "new_task")
            .setShortLabel(newTask)
            .setLongLabel(newTask)
            .setIcon(Icon.createWithBitmap(newTaskBitmap))
            .setIntent(newTaskIntent)
            .build()
    }

    private fun checkIsOpenIntent(): Boolean {
        val dayCodeToOpen = intent.getStringExtra(DAY_CODE) ?: ""
        val viewToOpen = intent.getIntExtra(VIEW_TO_OPEN, DAILY_VIEW)
        intent.removeExtra(VIEW_TO_OPEN)
        intent.removeExtra(DAY_CODE)
        if (dayCodeToOpen.isNotEmpty()) {
            binding.calendarFab.beVisible()
            if (viewToOpen != LAST_VIEW) {
                config.storedView = viewToOpen
            }
            updateViewPager(dayCodeToOpen)
            return true
        }

        val eventIdToOpen = intent.getLongExtra(EVENT_ID, 0L)
        val eventOccurrenceToOpen = intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L)
        intent.removeExtra(EVENT_ID)
        intent.removeExtra(EVENT_OCCURRENCE_TS)
        if (eventIdToOpen != 0L && eventOccurrenceToOpen != 0L) {
            hideKeyboard()
            Intent(this, EventActivity::class.java).apply {
                putExtra(EVENT_ID, eventIdToOpen)
                putExtra(EVENT_OCCURRENCE_TS, eventOccurrenceToOpen)
                startActivity(this)
            }
        }

        return false
    }

    private fun checkIsViewIntent() {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            if (uri?.authority?.equals("com.android.calendar") == true || uri?.authority?.substringAfter("@") == "com.android.calendar") {
                if (uri.path!!.startsWith("/events")) {
                    ensureBackgroundThread {
                        // intents like content://com.android.calendar/events/1756
                        val eventId = uri.lastPathSegment
                        val id = eventsDB.getEventIdWithLastImportId("%-$eventId")
                        if (id != null) {
                            hideKeyboard()
                            Intent(this, EventActivity::class.java).apply {
                                putExtra(EVENT_ID, id)
                                startActivity(this)
                            }
                        } else {
                            toast(R.string.caldav_event_not_found, Toast.LENGTH_LONG)
                        }
                    }
                } else if (uri.path!!.startsWith("/time") || intent?.extras?.getBoolean("DETAIL_VIEW", false) == true) {
                    // clicking date on a third party widget: content://com.android.calendar/time/1507309245683
                    // or content://0@com.android.calendar/time/1584958526435
                    val timestamp = uri.pathSegments.last()
                    if (timestamp.areDigitsOnly()) {
                        openDayAt(timestamp.toLong())
                        return
                    }
                }
            } else {
                tryImportEventsFromFile(uri!!) {
                    if (it) {
                        runOnUiThread {
                            updateViewPager()
                            setupQuickFilter()
                        }
                    }
                }
            }
        }
    }

    private fun showViewDialog() {
        val items = arrayListOf(
            RadioItem(DAILY_VIEW, getString(R.string.daily_view)),
            RadioItem(WEEKLY_VIEW, getString(R.string.weekly_view)),
            RadioItem(MONTHLY_VIEW, getString(R.string.monthly_view)),
            RadioItem(MONTHLY_DAILY_VIEW, getString(R.string.monthly_daily_view)),
            RadioItem(YEARLY_VIEW, getString(R.string.yearly_view)),
            RadioItem(EVENTS_LIST_VIEW, getString(R.string.simple_event_list))
        )

        RadioGroupDialog(this, items, config.storedView) {
            resetActionBarTitle()
            closeSearch()
            updateView(it as Int)
            shouldGoToTodayBeVisible = false
            refreshMenuItems()
        }
    }

    private fun goToToday() {
        currentFragments.last().goToToday()
    }

    fun showGoToDateDialog() {
        currentFragments.last().showGoToDateDialog()
    }

    private fun printView() {
        currentFragments.last().printView()
    }

    private fun resetActionBarTitle() {
        binding.mainMenu.updateHintText(getString(com.simplemobiletools.commons.R.string.search))
    }

    private fun showFilterDialog() {
        SelectEventTypesDialog(this, config.displayEventTypes) {
            if (config.displayEventTypes != it) {
                config.displayEventTypes = it

                refreshViewPager()
                setupQuickFilter()
                updateWidgets()
            }
        }
    }

    fun toggleGoToTodayVisibility(beVisible: Boolean) {
        shouldGoToTodayBeVisible = beVisible
        if (goToTodayButton?.isVisible != beVisible) {
            refreshMenuItems()
        }
    }

    private fun updateCalDAVEvents() {
        ensureBackgroundThread {
            calDAVHelper.refreshCalendars(showToasts = false, scheduleNextSync = true) {
                refreshViewPager()
            }
        }
    }

    private fun refreshCalDAVCalendars(showRefreshToast: Boolean) {
        showCalDAVRefreshToast = showRefreshToast
        if (showRefreshToast) {
            toast(R.string.refreshing)
        }
        updateCalDAVEvents()
        syncCalDAVCalendars {
            calDAVHelper.refreshCalendars(showToasts = true, scheduleNextSync = true) {
                calDAVChanged()
            }
        }
    }

    private fun calDAVChanged() {
        refreshViewPager()
        if (showCalDAVRefreshToast) {
            toast(R.string.refreshing_complete)
        }
        runOnUiThread {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun addHolidays() {
        val items = getHolidayRadioItems()
        RadioGroupDialog(this, items) { selectedHoliday ->
            SetRemindersDialog(this, OTHER_EVENT) {
                val reminders = it
                toast(com.simplemobiletools.commons.R.string.importing)
                ensureBackgroundThread {
                    val holidays = getString(R.string.holidays)
                    var eventTypeId = eventsHelper.getEventTypeIdWithClass(HOLIDAY_EVENT)
                    if (eventTypeId == -1L) {
                        eventTypeId = eventsHelper.createPredefinedEventType(holidays, R.color.default_holidays_color, HOLIDAY_EVENT, true)
                    }
                    val result = IcsImporter(this).importEvents(selectedHoliday as String, eventTypeId, 0, false, reminders)
                    handleParseResult(result)
                    if (result != ImportResult.IMPORT_FAIL) {
                        runOnUiThread {
                            updateViewPager()
                            setupQuickFilter()
                        }
                    }
                }
            }
        }
    }

    private fun tryAddBirthdays() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                SetRemindersDialog(this, BIRTHDAY_EVENT) {
                    val reminders = it
                    val privateCursor = getMyContactsCursor(false, false)

                    ensureBackgroundThread {
                        val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)
                        addPrivateEvents(true, privateContacts, reminders) { eventsFound, eventsAdded ->
                            addContactEvents(true, reminders, eventsFound, eventsAdded) {
                                when {
                                    it > 0 -> {
                                        toast(R.string.birthdays_added)
                                        updateViewPager()
                                        setupQuickFilter()
                                    }

                                    it == -1 -> toast(R.string.no_new_birthdays)
                                    else -> toast(R.string.no_birthdays)
                                }
                            }
                        }
                    }
                }
            } else {
                toast(com.simplemobiletools.commons.R.string.no_contacts_permission)
            }
        }
    }

    private fun tryAddAnniversaries() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                SetRemindersDialog(this, ANNIVERSARY_EVENT) {
                    val reminders = it
                    val privateCursor = getMyContactsCursor(false, false)

                    ensureBackgroundThread {
                        val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)
                        addPrivateEvents(false, privateContacts, reminders) { eventsFound, eventsAdded ->
                            addContactEvents(false, reminders, eventsFound, eventsAdded) {
                                when {
                                    it > 0 -> {
                                        toast(R.string.anniversaries_added)
                                        updateViewPager()
                                        setupQuickFilter()
                                    }

                                    it == -1 -> toast(R.string.no_new_anniversaries)
                                    else -> toast(R.string.no_anniversaries)
                                }
                            }
                        }
                    }
                }
            } else {
                toast(com.simplemobiletools.commons.R.string.no_contacts_permission)
            }
        }
    }

    private fun addBirthdaysAnniversariesAtStart() {
        if ((!config.addBirthdaysAutomatically && !config.addAnniversariesAutomatically) || !hasPermission(PERMISSION_READ_CONTACTS)) {
            return
        }

        val privateCursor = getMyContactsCursor(false, false)

        ensureBackgroundThread {
            val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)
            if (config.addBirthdaysAutomatically) {
                addPrivateEvents(true, privateContacts, config.birthdayReminders) { eventsFound, eventsAdded ->
                    addContactEvents(true, config.birthdayReminders, eventsFound, eventsAdded) {
                        if (it > 0) {
                            toast(R.string.birthdays_added)
                            updateViewPager()
                            setupQuickFilter()
                        }
                    }
                }
            }

            if (config.addAnniversariesAutomatically) {
                addPrivateEvents(false, privateContacts, config.anniversaryReminders) { eventsFound, eventsAdded ->
                    addContactEvents(false, config.anniversaryReminders, eventsFound, eventsAdded) {
                        if (it > 0) {
                            toast(R.string.anniversaries_added)
                            updateViewPager()
                            setupQuickFilter()
                        }
                    }
                }
            }
        }
    }

    private fun handleParseResult(result: ImportResult) {
        toast(
            when (result) {
                ImportResult.IMPORT_NOTHING_NEW -> com.simplemobiletools.commons.R.string.no_new_items
                ImportResult.IMPORT_OK -> R.string.holidays_imported_successfully
                ImportResult.IMPORT_PARTIAL -> R.string.importing_some_holidays_failed
                else -> R.string.importing_holidays_failed
            }, Toast.LENGTH_LONG
        )
    }

    private fun addContactEvents(birthdays: Boolean, reminders: ArrayList<Int>, initEventsFound: Int, initEventsAdded: Int, callback: (Int) -> Unit) {
        var eventsFound = initEventsFound
        var eventsAdded = initEventsAdded
        val uri = Data.CONTENT_URI
        val projection = arrayOf(
            Contacts.DISPLAY_NAME,
            CommonDataKinds.Event.CONTACT_ID,
            CommonDataKinds.Event.CONTACT_LAST_UPDATED_TIMESTAMP,
            CommonDataKinds.Event.START_DATE
        )

        val selection = "${Data.MIMETYPE} = ? AND ${CommonDataKinds.Event.TYPE} = ?"
        val type = if (birthdays) CommonDataKinds.Event.TYPE_BIRTHDAY else CommonDataKinds.Event.TYPE_ANNIVERSARY
        val selectionArgs = arrayOf(CommonDataKinds.Event.CONTENT_ITEM_TYPE, type.toString())

        val dateFormats = getDateFormats()
        val yearDateFormats = getDateFormatsWithYear()
        val existingEvents = if (birthdays) eventsDB.getBirthdays() else eventsDB.getAnniversaries()
        val importIDs = HashMap<String, Long>()
        existingEvents.forEach {
            importIDs[it.importId] = it.startTS
        }

        val eventTypeId = if (birthdays) eventsHelper.getLocalBirthdaysEventTypeId() else eventsHelper.getAnniversariesEventTypeId()
        val source = if (birthdays) SOURCE_CONTACT_BIRTHDAY else SOURCE_CONTACT_ANNIVERSARY

        queryCursor(uri, projection, selection, selectionArgs, showErrors = true) { cursor ->
            val contactId = cursor.getIntValue(CommonDataKinds.Event.CONTACT_ID).toString()
            val name = cursor.getStringValue(Contacts.DISPLAY_NAME)
            val startDate = cursor.getStringValue(CommonDataKinds.Event.START_DATE)

            for (format in dateFormats) {
                try {
                    val formatter = SimpleDateFormat(format, Locale.getDefault())
                    val date = formatter.parse(startDate)
                    val flags = if (format in yearDateFormats) {
                        FLAG_ALL_DAY
                    } else {
                        FLAG_ALL_DAY or FLAG_MISSING_YEAR
                    }

                    val timestamp = date.time / 1000L
                    val lastUpdated = cursor.getLongValue(CommonDataKinds.Event.CONTACT_LAST_UPDATED_TIMESTAMP)
                    val event = Event(
                        null, timestamp, timestamp, name, reminder1Minutes = reminders[0], reminder2Minutes = reminders[1],
                        reminder3Minutes = reminders[2], importId = contactId, timeZone = DateTimeZone.getDefault().id, flags = flags,
                        repeatInterval = YEAR, repeatRule = REPEAT_SAME_DAY, eventType = eventTypeId, source = source, lastUpdated = lastUpdated
                    )

                    val importIDsToDelete = ArrayList<String>()
                    for ((key, value) in importIDs) {
                        if (key == contactId && value != timestamp) {
                            val deleted = eventsDB.deleteBirthdayAnniversary(source, key)
                            if (deleted == 1) {
                                importIDsToDelete.add(key)
                            }
                        }
                    }

                    importIDsToDelete.forEach {
                        importIDs.remove(it)
                    }

                    eventsFound++
                    if (!importIDs.containsKey(contactId)) {
                        // avoid adding duplicate birthdays/anniversaries
                        if (existingEvents.none { it.title == event.title && it.startTS == event.startTS }) {
                            eventsHelper.insertEvent(event, false, false) {
                                eventsAdded++
                            }
                        }
                    }
                    break
                } catch (e: Exception) {
                }
            }
        }

        runOnUiThread {
            callback(if (eventsAdded == 0 && eventsFound > 0) -1 else eventsAdded)
        }
    }

    private fun addPrivateEvents(
        birthdays: Boolean,
        contacts: ArrayList<SimpleContact>,
        reminders: ArrayList<Int>,
        callback: (eventsFound: Int, eventsAdded: Int) -> Unit
    ) {
        var eventsAdded = 0
        var eventsFound = 0
        if (contacts.isEmpty()) {
            callback(0, 0)
            return
        }

        try {
            val eventTypeId = if (birthdays) eventsHelper.getLocalBirthdaysEventTypeId() else eventsHelper.getAnniversariesEventTypeId()
            val source = if (birthdays) SOURCE_CONTACT_BIRTHDAY else SOURCE_CONTACT_ANNIVERSARY

            val existingEvents = if (birthdays) eventsDB.getBirthdays() else eventsDB.getAnniversaries()
            val importIDs = HashMap<String, Long>()
            existingEvents.forEach {
                importIDs[it.importId] = it.startTS
            }

            contacts.forEach { contact ->
                val events = if (birthdays) contact.birthdays else contact.anniversaries
                events.forEach { birthdayAnniversary ->
                    // private contacts are created in Simple Contacts Pro, so we can guarantee that they exist only in these 2 formats
                    val format = if (birthdayAnniversary.startsWith("--")) {
                        "--MM-dd"
                    } else {
                        "yyyy-MM-dd"
                    }

                    val formatter = SimpleDateFormat(format, Locale.getDefault())
                    val date = formatter.parse(birthdayAnniversary)
                    if (date.year < 70) {
                        date.year = 70
                    }

                    val timestamp = date.time / 1000L
                    val lastUpdated = System.currentTimeMillis()
                    val event = Event(
                        null, timestamp, timestamp, contact.name, reminder1Minutes = reminders[0], reminder2Minutes = reminders[1],
                        reminder3Minutes = reminders[2], importId = contact.contactId.toString(), timeZone = DateTimeZone.getDefault().id, flags = FLAG_ALL_DAY,
                        repeatInterval = YEAR, repeatRule = REPEAT_SAME_DAY, eventType = eventTypeId, source = source, lastUpdated = lastUpdated
                    )

                    val importIDsToDelete = ArrayList<String>()
                    for ((key, value) in importIDs) {
                        if (key == contact.contactId.toString() && value != timestamp) {
                            val deleted = eventsDB.deleteBirthdayAnniversary(source, key)
                            if (deleted == 1) {
                                importIDsToDelete.add(key)
                            }
                        }
                    }

                    importIDsToDelete.forEach {
                        importIDs.remove(it)
                    }

                    eventsFound++
                    if (!importIDs.containsKey(contact.contactId.toString())) {
                        eventsHelper.insertEvent(event, false, false) {
                            eventsAdded++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }

        callback(eventsFound, eventsAdded)
    }

    private fun updateView(view: Int) {
        binding.calendarFab.beVisibleIf(view != YEARLY_VIEW && view != WEEKLY_VIEW)
        val dateCode = getDateCodeToDisplay(view)
        config.storedView = view
        checkSwipeRefreshAvailability()
        updateViewPager(dateCode)
        if (goToTodayButton?.isVisible == true) {
            shouldGoToTodayBeVisible = false
            refreshMenuItems()
        }
    }

    private fun getDateCodeToDisplay(newView: Int): String? {
        val fragment = currentFragments.last()
        val currentView = fragment.viewType
        if (newView == EVENTS_LIST_VIEW || currentView == EVENTS_LIST_VIEW) {
            return null
        }

        val fragmentDate = fragment.getCurrentDate()
        val viewOrder = arrayListOf(DAILY_VIEW, WEEKLY_VIEW, MONTHLY_VIEW, YEARLY_VIEW)
        val currentViewIndex = viewOrder.indexOf(if (currentView == MONTHLY_DAILY_VIEW) MONTHLY_VIEW else currentView)
        val newViewIndex = viewOrder.indexOf(if (newView == MONTHLY_DAILY_VIEW) MONTHLY_VIEW else newView)

        return if (fragmentDate != null && currentViewIndex <= newViewIndex) {
            getDateCodeFormatForView(newView, fragmentDate)
        } else {
            getDateCodeFormatForView(newView, DateTime())
        }
    }

    private fun getDateCodeFormatForView(view: Int, date: DateTime): String {
        return when (view) {
            WEEKLY_VIEW -> getFirstDayOfWeek(date)
            YEARLY_VIEW -> date.toString()
            else -> Formatter.getDayCodeFromDateTime(date)
        }
    }

    private fun updateViewPager(dayCode: String? = null) {
        val fragment = getFragmentsHolder()
        currentFragments.forEach {
            try {
                supportFragmentManager.beginTransaction().remove(it).commitNow()
            } catch (ignored: Exception) {
                return
            }
        }

        currentFragments.clear()
        currentFragments.add(fragment)
        val bundle = Bundle()
        val fixedDayCode = fixDayCode(dayCode)

        when (config.storedView) {
            DAILY_VIEW -> bundle.putString(DAY_CODE, fixedDayCode ?: Formatter.getTodayCode())
            WEEKLY_VIEW -> bundle.putString(WEEK_START_DATE_TIME, fixedDayCode ?: getFirstDayOfWeek(DateTime()))
            MONTHLY_VIEW, MONTHLY_DAILY_VIEW -> bundle.putString(DAY_CODE, fixedDayCode ?: Formatter.getTodayCode())
            YEARLY_VIEW -> bundle.putString(YEAR_TO_OPEN, fixedDayCode)
        }

        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
        binding.mainMenu.toggleForceArrowBackIcon(false)
    }

    private fun fixDayCode(dayCode: String? = null): String? = when {
        config.storedView == WEEKLY_VIEW && (dayCode?.length == Formatter.DAYCODE_PATTERN.length) -> getFirstDayOfWeek(Formatter.getDateTimeFromCode(dayCode))
        config.storedView == YEARLY_VIEW && (dayCode?.length == Formatter.DAYCODE_PATTERN.length) -> Formatter.getYearFromDayCode(dayCode)
        else -> dayCode
    }

    private fun showExtendedFab() {
        animateFabIcon(false)
        binding.apply {
            arrayOf(fabEventLabel, fabExtendedOverlay, fabTaskIcon, fabTaskLabel).forEach {
                it.fadeIn()
            }
        }
    }

    private fun hideExtendedFab() {
        animateFabIcon(true)
        binding.apply {
            arrayOf(fabEventLabel, fabExtendedOverlay, fabTaskIcon, fabTaskLabel).forEach {
                it.fadeOut()
            }
        }
    }

    private fun animateFabIcon(showPlus: Boolean) {
        val newDrawableId = if (showPlus) {
            com.simplemobiletools.commons.R.drawable.ic_plus_vector
        } else {
            R.drawable.ic_today_vector
        }
        val newDrawable = resources.getColoredDrawableWithColor(newDrawableId, getProperPrimaryColor())
        binding.calendarFab.setImageDrawable(newDrawable)
    }

    private fun openNewEvent() {
        hideKeyboard()
        val lastFragment = currentFragments.last()
        val allowChangingDay = lastFragment !is DayFragmentsHolder && lastFragment !is MonthDayFragmentsHolder
        launchNewEventIntent(lastFragment.getNewEventDayCode(), allowChangingDay)
    }

    private fun openNewTask() {
        hideKeyboard()
        val lastFragment = currentFragments.last()
        val allowChangingDay = lastFragment !is DayFragmentsHolder && lastFragment !is MonthDayFragmentsHolder
        launchNewTaskIntent(lastFragment.getNewEventDayCode(), allowChangingDay)
    }

    fun openMonthFromYearly(dateTime: DateTime) {
        if (currentFragments.last() is MonthFragmentsHolder) {
            return
        }

        val fragment = MonthFragmentsHolder()
        currentFragments.add(fragment)
        val bundle = Bundle()
        bundle.putString(DAY_CODE, Formatter.getDayCodeFromDateTime(dateTime))
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
        resetActionBarTitle()
        binding.calendarFab.beVisible()
        showBackNavigationArrow()
    }

    fun openDayFromMonthly(dateTime: DateTime) {
        if (currentFragments.last() is DayFragmentsHolder) {
            return
        }

        val fragment = DayFragmentsHolder()
        currentFragments.add(fragment)
        val bundle = Bundle()
        bundle.putString(DAY_CODE, Formatter.getDayCodeFromDateTime(dateTime))
        fragment.arguments = bundle
        try {
            supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
            showBackNavigationArrow()
        } catch (e: Exception) {
        }
    }

    private fun getFragmentsHolder() = when (config.storedView) {
        DAILY_VIEW -> DayFragmentsHolder()
        MONTHLY_VIEW -> MonthFragmentsHolder()
        MONTHLY_DAILY_VIEW -> MonthDayFragmentsHolder()
        YEARLY_VIEW -> YearFragmentsHolder()
        EVENTS_LIST_VIEW -> EventListFragment()
        else -> WeekFragmentsHolder()
    }

    private fun removeTopFragment() {
        supportFragmentManager.beginTransaction().remove(currentFragments.last()).commit()
        currentFragments.removeAt(currentFragments.size - 1)
        toggleGoToTodayVisibility(currentFragments.last().shouldGoToTodayBeVisible())
        currentFragments.last().apply {
            refreshEvents()
        }

        binding.calendarFab.beGoneIf(currentFragments.size == 1 && config.storedView == YEARLY_VIEW)
        if (currentFragments.size > 1) {
            showBackNavigationArrow()
        } else {
            binding.mainMenu.toggleForceArrowBackIcon(false)
        }
    }

    private fun showBackNavigationArrow() {
        binding.mainMenu.toggleForceArrowBackIcon(true)
        binding.mainMenu.onNavigateBackClickListener = {
            onBackPressed()
        }
    }

    private fun refreshViewPager() {
        runOnUiThread {
            if (!isDestroyed) {
                currentFragments.last().refreshEvents()
            }
        }
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_JODA

        val faqItems = arrayListOf(
            FAQItem("${getString(R.string.faq_2_title)} ${getString(R.string.faq_2_title_extra)}", R.string.faq_2_text),
            FAQItem(R.string.faq_5_title, R.string.faq_5_text),
            FAQItem(R.string.faq_3_title, R.string.faq_3_text),
            FAQItem(R.string.faq_6_title, R.string.faq_6_text),
            FAQItem(R.string.faq_1_title, R.string.faq_1_text),
            FAQItem(com.simplemobiletools.commons.R.string.faq_1_title_commons, com.simplemobiletools.commons.R.string.faq_1_text_commons),
            FAQItem(com.simplemobiletools.commons.R.string.faq_4_title_commons, com.simplemobiletools.commons.R.string.faq_4_text_commons),
            FAQItem(R.string.faq_4_title, R.string.faq_4_text)
        )

        if (!resources.getBoolean(com.simplemobiletools.commons.R.bool.hide_google_relations)) {
            faqItems.add(FAQItem(com.simplemobiletools.commons.R.string.faq_2_title_commons, com.simplemobiletools.commons.R.string.faq_2_text_commons))
            faqItems.add(FAQItem(com.simplemobiletools.commons.R.string.faq_6_title_commons, com.simplemobiletools.commons.R.string.faq_6_text_commons))
            faqItems.add(FAQItem(com.simplemobiletools.commons.R.string.faq_7_title_commons, com.simplemobiletools.commons.R.string.faq_7_text_commons))
        }

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }

    private fun searchQueryChanged(text: String) {
        mLatestSearchQuery = text

        if (text.isNotEmpty() && binding.searchHolder.isGone()) {
            binding.searchHolder.fadeIn()
        } else if (text.isEmpty()) {
            binding.searchHolder.fadeOut()
            binding.searchResultsList.adapter = null
        }

        val placeholderTextId = if (config.displayEventTypes.isEmpty()) {
            R.string.everything_filtered_out
        } else {
            com.simplemobiletools.commons.R.string.no_items_found
        }

        binding.searchPlaceholder.setText(placeholderTextId)
        binding.searchPlaceholder2.beVisibleIf(text.length == 1)
        if (text.length >= 2) {
            if (binding.searchResultsList.adapter == null) {
                minFetchedSearchTS = DateTime().minusYears(2).seconds()
                maxFetchedSearchTS = DateTime().plusYears(2).seconds()
            }

            eventsHelper.getEvents(minFetchedSearchTS, maxFetchedSearchTS, searchQuery = text) { events ->
                if (text == mLatestSearchQuery) {
                    // if we have less than MIN_EVENTS_THRESHOLD events, search again by extending the time span
                    showSearchResultEvents(events, INITIAL_EVENTS)

                    if (events.size < MIN_EVENTS_TRESHOLD) {
                        minFetchedSearchTS = 0L
                        maxFetchedSearchTS = MAX_SEARCH_YEAR

                        eventsHelper.getEvents(minFetchedSearchTS, maxFetchedSearchTS, searchQuery = text) { events ->
                            events.forEach { event ->
                                try {
                                    if (searchResultEvents.firstOrNull { it.id == event.id && it.startTS == event.startTS } == null) {
                                        searchResultEvents.add(0, event)
                                    }
                                } catch (ignored: ConcurrentModificationException) {
                                }
                            }

                            showSearchResultEvents(searchResultEvents, INITIAL_EVENTS)
                        }
                    }
                }
            }
        } else if (text.length == 1) {
            binding.searchPlaceholder.beVisible()
            binding.searchResultsList.beGone()
        }
    }

    private fun showSearchResultEvents(events: ArrayList<Event>, updateStatus: Int) {
        val currentSearchQuery = binding.mainMenu.getCurrentQuery()
        val filtered = try {
            events.filter {
                it.title.contains(currentSearchQuery, true) || it.location.contains(currentSearchQuery, true) || it.description.contains(
                    currentSearchQuery,
                    true
                )
            }
        } catch (e: ConcurrentModificationException) {
            return
        }

        searchResultEvents = filtered.toMutableList() as ArrayList<Event>
        runOnUiThread {
            binding.searchResultsList.beVisibleIf(filtered.isNotEmpty())
            binding.searchPlaceholder.beVisibleIf(filtered.isEmpty())
            val listItems = getEventListItems(filtered)
            val currAdapter = binding.searchResultsList.adapter
            if (currAdapter == null) {
                val eventsAdapter = EventListAdapter(this, listItems, true, this, binding.searchResultsList) {
                    hideKeyboard()
                    if (it is ListEvent) {
                        Intent(applicationContext, getActivityToOpen(it.isTask)).apply {
                            putExtra(EVENT_ID, it.id)
                            putExtra(EVENT_OCCURRENCE_TS, it.startTS)
                            startActivity(this)
                        }
                    }
                }

                binding.searchResultsList.adapter = eventsAdapter

                binding.searchResultsList.endlessScrollListener = object : MyRecyclerView.EndlessScrollListener {
                    override fun updateTop() {
                        fetchPreviousPeriod()
                    }

                    override fun updateBottom() {
                        fetchNextPeriod()
                    }
                }
            } else {
                (currAdapter as EventListAdapter).updateListItems(listItems)
                if (updateStatus == UPDATE_TOP) {
                    val item = listItems.indexOfFirst { it == bottomItemAtRefresh }
                    if (item != -1) {
                        binding.searchResultsList.scrollToPosition(item)
                    }
                } else if (updateStatus == UPDATE_BOTTOM) {
                    binding.searchResultsList.smoothScrollBy(0, resources.getDimension(R.dimen.endless_scroll_move_height).toInt())
                } else {
                    val firstNonPastSectionIndex = listItems.indexOfFirst { it is ListSectionDay && !it.isPastSection }
                    if (firstNonPastSectionIndex != -1) {
                        binding.searchResultsList.scrollToPosition(firstNonPastSectionIndex)
                    }
                }
            }
        }
    }

    private fun fetchPreviousPeriod() {
        if (minFetchedSearchTS == 0L) {
            return
        }

        val lastPosition = (binding.searchResultsList.layoutManager as MyLinearLayoutManager).findLastVisibleItemPosition()
        bottomItemAtRefresh = (binding.searchResultsList.adapter as EventListAdapter).listItems[lastPosition]

        val oldMinFetchedTS = minFetchedSearchTS - 1
        minFetchedSearchTS -= FETCH_INTERVAL
        eventsHelper.getEvents(minFetchedSearchTS, oldMinFetchedTS, searchQuery = mLatestSearchQuery) { events ->
            events.forEach { event ->
                try {
                    if (searchResultEvents.firstOrNull { it.id == event.id && it.startTS == event.startTS } == null) {
                        searchResultEvents.add(0, event)
                    }
                } catch (ignored: ConcurrentModificationException) {
                }
            }

            showSearchResultEvents(searchResultEvents, UPDATE_TOP)
        }
    }

    private fun fetchNextPeriod() {
        if (maxFetchedSearchTS == MAX_SEARCH_YEAR) {
            return
        }

        val oldMaxFetchedTS = maxFetchedSearchTS + 1
        maxFetchedSearchTS += FETCH_INTERVAL
        eventsHelper.getEvents(oldMaxFetchedTS, maxFetchedSearchTS, searchQuery = mLatestSearchQuery) { events ->
            events.forEach { event ->
                try {
                    if (searchResultEvents.firstOrNull { it.id == event.id && it.startTS == event.startTS } == null) {
                        searchResultEvents.add(0, event)
                    }
                } catch (ignored: ConcurrentModificationException) {
                }
            }

            showSearchResultEvents(searchResultEvents, UPDATE_BOTTOM)
        }
    }

    private fun checkSwipeRefreshAvailability() {
        binding.swipeRefreshLayout.isEnabled = config.caldavSync && config.pullToRefresh && config.storedView != WEEKLY_VIEW
        if (!binding.swipeRefreshLayout.isEnabled) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun refreshItems() {
        refreshViewPager()
    }

    private fun openDayAt(timestamp: Long) {
        val dayCode = Formatter.getDayCodeFromTS(timestamp / 1000L)
        binding.calendarFab.beVisible()
        config.storedView = DAILY_VIEW
        updateViewPager(dayCode)
    }

    // events fetched from Thunderbird, https://www.thunderbird.net/en-US/calendar/holidays and
    // https://holidays.kayaposoft.com/public_holidays.php?year=2021
    private fun getHolidayRadioItems(): ArrayList<RadioItem> {
        val items = ArrayList<RadioItem>()

        LinkedHashMap<String, String>().apply {
            put("Algeria", "algeria.ics")
            put("Argentina", "argentina.ics")
            put("Australia", "australia.ics")
            put("België", "belgium.ics")
            put("Bolivia", "bolivia.ics")
            put("Brasil", "brazil.ics")
            put("България", "bulgaria.ics")
            put("Canada", "canada.ics")
            put("China", "china.ics")
            put("Colombia", "colombia.ics")
            put("Česká republika", "czech.ics")
            put("Danmark", "denmark.ics")
            put("Deutschland", "germany.ics")
            put("Eesti", "estonia.ics")
            put("España", "spain.ics")
            put("Éire", "ireland.ics")
            put("France", "france.ics")
            put("Fürstentum Liechtenstein", "liechtenstein.ics")
            put("Hellas", "greece.ics")
            put("Hrvatska", "croatia.ics")
            put("India", "india.ics")
            put("Indonesia", "indonesia.ics")
            put("Ísland", "iceland.ics")
            put("Israel", "israel.ics")
            put("Italia", "italy.ics")
            put("Қазақстан Республикасы", "kazakhstan.ics")
            put("المملكة المغربية", "morocco.ics")
            put("Latvija", "latvia.ics")
            put("Lietuva", "lithuania.ics")
            put("Luxemburg", "luxembourg.ics")
            put("Makedonija", "macedonia.ics")
            put("Malaysia", "malaysia.ics")
            put("Magyarország", "hungary.ics")
            put("México", "mexico.ics")
            put("Nederland", "netherlands.ics")
            put("República de Nicaragua", "nicaragua.ics")
            put("日本", "japan.ics")
            put("Nigeria", "nigeria.ics")
            put("Norge", "norway.ics")
            put("Österreich", "austria.ics")
            put("Pākistān", "pakistan.ics")
            put("Polska", "poland.ics")
            put("Portugal", "portugal.ics")
            put("Россия", "russia.ics")
            put("República de Costa Rica", "costarica.ics")
            put("República Oriental del Uruguay", "uruguay.ics")
            put("République d'Haïti", "haiti.ics")
            put("România", "romania.ics")
            put("Schweiz", "switzerland.ics")
            put("Singapore", "singapore.ics")
            put("한국", "southkorea.ics")
            put("Srbija", "serbia.ics")
            put("Slovenija", "slovenia.ics")
            put("Slovensko", "slovakia.ics")
            put("South Africa", "southafrica.ics")
            put("Sri Lanka", "srilanka.ics")
            put("Suomi", "finland.ics")
            put("Sverige", "sweden.ics")
            put("Taiwan", "taiwan.ics")
            put("ราชอาณาจักรไทย", "thailand.ics")
            put("Türkiye Cumhuriyeti", "turkey.ics")
            put("Ukraine", "ukraine.ics")
            put("United Kingdom", "unitedkingdom.ics")
            put("United States", "unitedstates.ics")

            var i = 0
            for ((country, file) in this) {
                items.add(RadioItem(i++, country, file))
            }
        }

        return items
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(39, R.string.release_39))
            add(Release(40, R.string.release_40))
            add(Release(42, R.string.release_42))
            add(Release(44, R.string.release_44))
            add(Release(46, R.string.release_46))
            add(Release(48, R.string.release_48))
            add(Release(49, R.string.release_49))
            add(Release(51, R.string.release_51))
            add(Release(52, R.string.release_52))
            add(Release(54, R.string.release_54))
            add(Release(57, R.string.release_57))
            add(Release(59, R.string.release_59))
            add(Release(60, R.string.release_60))
            add(Release(62, R.string.release_62))
            add(Release(67, R.string.release_67))
            add(Release(69, R.string.release_69))
            add(Release(71, R.string.release_71))
            add(Release(73, R.string.release_73))
            add(Release(76, R.string.release_76))
            add(Release(77, R.string.release_77))
            add(Release(80, R.string.release_80))
            add(Release(84, R.string.release_84))
            add(Release(86, R.string.release_86))
            add(Release(88, R.string.release_88))
            add(Release(98, R.string.release_98))
            add(Release(117, R.string.release_117))
            add(Release(119, R.string.release_119))
            add(Release(129, R.string.release_129))
            add(Release(143, R.string.release_143))
            add(Release(155, R.string.release_155))
            add(Release(167, R.string.release_167))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
