package com.simplemobiletools.calendar.pro.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import com.simplemobiletools.calendar.pro.BuildConfig
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.adapters.EventListAdapter
import com.simplemobiletools.calendar.pro.adapters.QuickFilterEventTypeAdapter
import com.simplemobiletools.calendar.pro.databases.EventsDatabase
import com.simplemobiletools.calendar.pro.dialogs.ExportEventsDialog
import com.simplemobiletools.calendar.pro.dialogs.FilterEventTypesDialog
import com.simplemobiletools.calendar.pro.dialogs.ImportEventsDialog
import com.simplemobiletools.calendar.pro.dialogs.SetRemindersDialog
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.fragments.*
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.IcsExporter.ExportResult
import com.simplemobiletools.calendar.pro.helpers.IcsImporter.ImportResult
import com.simplemobiletools.calendar.pro.jobs.CalDAVUpdateListener
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.calendar.pro.models.ListEvent
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.commons.models.SimpleContact
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

class MainActivity : SimpleActivity(), RefreshRecyclerViewListener {
    private val PICK_IMPORT_SOURCE_INTENT = 1
    private val PICK_EXPORT_FILE_INTENT = 2

    private var showCalDAVRefreshToast = false
    private var mShouldFilterBeVisible = false
    private var mIsSearchOpen = false
    private var mLatestSearchQuery = ""
    private var mSearchMenuItem: MenuItem? = null
    private var shouldGoToTodayBeVisible = false
    private var goToTodayButton: MenuItem? = null
    private var currentFragments = ArrayList<MyFragmentHolder>()
    private var eventTypesToExport = ArrayList<Long>()

    private var mStoredTextColor = 0
    private var mStoredBackgroundColor = 0
    private var mStoredAdjustedPrimaryColor = 0
    private var mStoredDayCode = ""
    private var mStoredIsSundayFirst = false
    private var mStoredMidnightSpan = true
    private var mStoredUse24HourFormat = false
    private var mStoredDimPastEvents = true
    private var mStoredHighlightWeekends = false
    private var mStoredStartWeekWithCurrentDay = false
    private var mStoredHighlightWeekendsColor = 0
    private var mStoredHighlightSaturday = false
    private var mStoredHighlightSaturdayColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)

        checkWhatsNewDialog()
        calendar_fab.beVisibleIf(config.storedView != YEARLY_VIEW && config.storedView != WEEKLY_VIEW)
        calendar_fab.setOnClickListener {
            val lastFragment = currentFragments.last()
            val allowChangingDay = lastFragment !is DayFragmentsHolder && lastFragment !is MonthDayFragmentsHolder
            launchNewEventIntent(lastFragment.getNewEventDayCode(), allowChangingDay)
        }

        storeStateVariables()

        if (!hasPermission(PERMISSION_WRITE_CALENDAR) || !hasPermission(PERMISSION_READ_CALENDAR)) {
            config.caldavSync = false
        }

        if (config.caldavSync) {
            refreshCalDAVCalendars(false)
        }

        swipe_refresh_layout.setOnRefreshListener {
            refreshCalDAVCalendars(false)
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

        if (!config.wasUpgradedFromFreeShown && isPackageInstalled("com.simplemobiletools.calendar")) {
            ConfirmationDialog(this, "", R.string.upgraded_from_free, R.string.ok, 0) {}
            config.wasUpgradedFromFreeShown = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (mStoredTextColor != config.textColor || mStoredBackgroundColor != config.backgroundColor || mStoredAdjustedPrimaryColor != getAdjustedPrimaryColor()
            || mStoredDayCode != Formatter.getTodayCode() || mStoredDimPastEvents != config.dimPastEvents || mStoredHighlightWeekends != config.highlightWeekends
            || mStoredHighlightWeekendsColor != config.highlightWeekendsColor || mStoredHighlightSaturday != config.highlightSaturday
            || mStoredHighlightSaturdayColor != config.highlightSaturdayColor
        ) {
            updateViewPager()
        }

        eventsHelper.getEventTypes(this, false) {
            val newShouldFilterBeVisible = it.size > 1 || config.displayEventTypes.isEmpty()
            if (newShouldFilterBeVisible != mShouldFilterBeVisible) {
                mShouldFilterBeVisible = newShouldFilterBeVisible
            }
        }

        if (config.storedView == WEEKLY_VIEW) {
            if (mStoredIsSundayFirst != config.isSundayFirst || mStoredUse24HourFormat != config.use24HourFormat
                || mStoredMidnightSpan != config.showMidnightSpanningEventsAtTop || mStoredStartWeekWithCurrentDay != config.startWeekWithCurrentDay
            ) {
                updateViewPager()
            }
        }

        storeStateVariables()
        updateWidgets()
        updateTextColors(calendar_coordinator)

        search_holder.background = ColorDrawable(config.backgroundColor)
        checkSwipeRefreshAvailability()
        checkShortcuts()

        if (!mIsSearchOpen) {
            invalidateOptionsMenu()
        }

        setupQuickFilter()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        shouldGoToTodayBeVisible = currentFragments.last().shouldGoToTodayBeVisible()
        menu.apply {
            goToTodayButton = findItem(R.id.go_to_today)
            findItem(R.id.print).isVisible = config.storedView != MONTHLY_DAILY_VIEW
            findItem(R.id.filter).isVisible = mShouldFilterBeVisible
            findItem(R.id.go_to_today).isVisible = shouldGoToTodayBeVisible && !mIsSearchOpen
            findItem(R.id.go_to_date).isVisible = config.storedView != EVENTS_LIST_VIEW
        }

        setupSearch(menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.apply {
            findItem(R.id.refresh_caldav_calendars).isVisible = config.caldavSync
            findItem(R.id.filter).isVisible = mShouldFilterBeVisible
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.change_view -> showViewDialog()
            R.id.go_to_today -> goToToday()
            R.id.go_to_date -> showGoToDateDialog()
            R.id.print -> printView()
            R.id.filter -> showFilterDialog()
            R.id.refresh_caldav_calendars -> refreshCalDAVCalendars(true)
            R.id.add_holidays -> addHolidays()
            R.id.add_birthdays -> tryAddBirthdays()
            R.id.add_anniversaries -> tryAddAnniversaries()
            R.id.import_events -> tryImportEvents()
            R.id.export_events -> tryExportEvents()
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed() {
        swipe_refresh_layout.isRefreshing = false
        checkSwipeRefreshAvailability()
        if (currentFragments.size > 1) {
            removeTopFragment()
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIsOpenIntent()
        checkIsViewIntent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            tryImportEventsFromFile(resultData.data!!)
        } else if (requestCode == PICK_EXPORT_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportEventsTo(eventTypesToExport, outputStream)
        }
    }

    private fun storeStateVariables() {
        config.apply {
            mStoredIsSundayFirst = isSundayFirst
            mStoredTextColor = textColor
            mStoredBackgroundColor = backgroundColor
            mStoredUse24HourFormat = use24HourFormat
            mStoredDimPastEvents = dimPastEvents
            mStoredHighlightWeekends = highlightWeekends
            mStoredHighlightWeekendsColor = highlightWeekendsColor
            mStoredHighlightSaturday = highlightSaturday
            mStoredHighlightSaturdayColor = highlightSaturdayColor
            mStoredMidnightSpan = showMidnightSpanningEventsAtTop
            mStoredStartWeekWithCurrentDay = startWeekWithCurrentDay
        }
        mStoredAdjustedPrimaryColor = getAdjustedPrimaryColor()
        mStoredDayCode = Formatter.getTodayCode()
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        mSearchMenuItem = menu.findItem(R.id.search)
        (mSearchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (mIsSearchOpen) {
                        searchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                mIsSearchOpen = true
                search_holder.beVisible()
                calendar_fab.beGone()
                searchQueryChanged("")
                invalidateOptionsMenu()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                mIsSearchOpen = false
                search_holder.beGone()
                calendar_fab.beVisibleIf(currentFragments.last() !is YearFragmentsHolder && currentFragments.last() !is WeekFragmentsHolder)
                invalidateOptionsMenu()
                return true
            }
        })
    }

    private fun setupQuickFilter() {
        eventsHelper.getEventTypes(this, false) {
            val quickFilterEventTypes = config.quickFilterEventTypes
            quick_event_type_filter.adapter = QuickFilterEventTypeAdapter(this, it, quickFilterEventTypes) {
                refreshViewPager()
                updateWidgets()
            }
        }
    }

    private fun closeSearch() {
        mSearchMenuItem?.collapseActionView()
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
            val newEvent = getString(R.string.new_event)
            val manager = getSystemService(ShortcutManager::class.java)
            val drawable = resources.getDrawable(R.drawable.shortcut_plus)
            (drawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_plus_background).applyColorFilter(appIconColor)
            val bmp = drawable.convertToBitmap()

            val intent = Intent(this, SplashActivity::class.java)
            intent.action = SHORTCUT_NEW_EVENT
            val shortcut = ShortcutInfo.Builder(this, "new_event")
                .setShortLabel(newEvent)
                .setLongLabel(newEvent)
                .setIcon(Icon.createWithBitmap(bmp))
                .setIntent(intent)
                .build()

            try {
                manager.dynamicShortcuts = Arrays.asList(shortcut)
                config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    private fun checkIsOpenIntent(): Boolean {
        val dayCodeToOpen = intent.getStringExtra(DAY_CODE) ?: ""
        val viewToOpen = intent.getIntExtra(VIEW_TO_OPEN, DAILY_VIEW)
        intent.removeExtra(VIEW_TO_OPEN)
        intent.removeExtra(DAY_CODE)
        if (dayCodeToOpen.isNotEmpty()) {
            calendar_fab.beVisible()
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
                tryImportEventsFromFile(uri!!)
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
            invalidateOptionsMenu()
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
        updateActionBarTitle(getString(R.string.app_launcher_name))
        updateActionBarSubtitle("")
    }

    private fun showFilterDialog() {
        FilterEventTypesDialog(this) {
            refreshViewPager()
            setupQuickFilter()
            updateWidgets()
        }
    }

    fun toggleGoToTodayVisibility(beVisible: Boolean) {
        shouldGoToTodayBeVisible = beVisible
        if (goToTodayButton?.isVisible != beVisible) {
            invalidateOptionsMenu()
        }
    }

    private fun refreshCalDAVCalendars(showRefreshToast: Boolean) {
        showCalDAVRefreshToast = showRefreshToast
        if (showRefreshToast) {
            toast(R.string.refreshing)
        }

        syncCalDAVCalendars {
            calDAVHelper.refreshCalendars(true) {
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
            swipe_refresh_layout.isRefreshing = false
        }
    }

    private fun addHolidays() {
        val items = getHolidayRadioItems()
        RadioGroupDialog(this, items) { selectedHoliday ->
            SetRemindersDialog(this, OTHER_EVENT) {
                val reminders = it
                toast(R.string.importing)
                ensureBackgroundThread {
                    val holidays = getString(R.string.holidays)
                    var eventTypeId = eventsHelper.getEventTypeIdWithTitle(holidays)
                    if (eventTypeId == -1L) {
                        val eventType = EventType(null, holidays, resources.getColor(R.color.default_holidays_color))
                        eventTypeId = eventsHelper.insertOrUpdateEventTypeSync(eventType)
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
                    val privateCursor = getMyContactsCursor(false, false)?.loadInBackground()

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
                toast(R.string.no_contacts_permission)
            }
        }
    }

    private fun tryAddAnniversaries() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                SetRemindersDialog(this, ANNIVERSARY_EVENT) {
                    val reminders = it
                    val privateCursor = getMyContactsCursor(false, false)?.loadInBackground()

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
                toast(R.string.no_contacts_permission)
            }
        }
    }

    private fun addBirthdaysAnniversariesAtStart() {
        if ((!config.addBirthdaysAutomatically && !config.addAnniversariesAutomatically) || !hasPermission(PERMISSION_READ_CONTACTS)) {
            return
        }

        val privateCursor = getMyContactsCursor(false, false)?.loadInBackground()

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
                ImportResult.IMPORT_NOTHING_NEW -> R.string.no_new_items
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

        val eventTypeId = if (birthdays) eventsHelper.getBirthdaysEventTypeId() else eventsHelper.getAnniversariesEventTypeId()
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
                        eventsHelper.insertEvent(event, false, false) {
                            eventsAdded++
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
            val eventTypeId = if (birthdays) eventsHelper.getBirthdaysEventTypeId() else eventsHelper.getAnniversariesEventTypeId()
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
        calendar_fab.beVisibleIf(view != YEARLY_VIEW && view != WEEKLY_VIEW)
        config.storedView = view
        checkSwipeRefreshAvailability()
        updateViewPager()
        if (goToTodayButton?.isVisible == true) {
            shouldGoToTodayBeVisible = false
            invalidateOptionsMenu()
        }
    }

    private fun updateViewPager(dayCode: String? = Formatter.getTodayCode()) {
        val fragment = getFragmentsHolder()
        currentFragments.forEach {
            supportFragmentManager.beginTransaction().remove(it).commitNow()
        }
        currentFragments.clear()
        currentFragments.add(fragment)
        val bundle = Bundle()

        when (config.storedView) {
            DAILY_VIEW, MONTHLY_VIEW, MONTHLY_DAILY_VIEW -> bundle.putString(DAY_CODE, dayCode)
            WEEKLY_VIEW -> bundle.putString(WEEK_START_DATE_TIME, getThisWeekDateTime())
        }

        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
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
        calendar_fab.beVisible()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } catch (e: Exception) {
        }
    }

    private fun getThisWeekDateTime(): String {
        return if (!config.startWeekWithCurrentDay) {
            val currentOffsetHours = TimeZone.getDefault().rawOffset / 1000 / 60 / 60

            // not great, not terrible
            val useHours = if (currentOffsetHours >= 10) 8 else 12
            var thisweek = DateTime().withZone(DateTimeZone.UTC).withDayOfWeek(1).withHourOfDay(useHours).minusDays(if (config.isSundayFirst) 1 else 0)
            if (DateTime().minusDays(7).seconds() > thisweek.seconds()) {
                thisweek = thisweek.plusDays(7)
            }
            thisweek.toString()
        } else {
            DateTime().withZone(DateTimeZone.UTC).toString()
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
            updateActionBarTitle()
        }
        calendar_fab.beGoneIf(currentFragments.size == 1 && config.storedView == YEARLY_VIEW)
        supportActionBar?.setDisplayHomeAsUpEnabled(currentFragments.size > 1)
    }

    private fun refreshViewPager() {
        runOnUiThread {
            if (!isDestroyed) {
                currentFragments.last().refreshEvents()
            }
        }
    }

    private fun tryImportEvents() {
        if (isQPlus()) {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/calendar"
                startActivityForResult(this, PICK_IMPORT_SOURCE_INTENT)
            }
        } else {
            handlePermission(PERMISSION_READ_STORAGE) {
                if (it) {
                    importEvents()
                }
            }
        }
    }

    private fun importEvents() {
        FilePickerDialog(this) {
            showImportEventsDialog(it)
        }
    }

    private fun tryImportEventsFromFile(uri: Uri) {
        when {
            uri.scheme == "file" -> showImportEventsDialog(uri.path!!)
            uri.scheme == "content" -> {
                val tempFile = getTempFile()
                if (tempFile == null) {
                    toast(R.string.unknown_error_occurred)
                    return
                }

                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val out = FileOutputStream(tempFile)
                    inputStream!!.copyTo(out)
                    showImportEventsDialog(tempFile.absolutePath)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
            else -> toast(R.string.invalid_file_format)
        }
    }

    private fun showImportEventsDialog(path: String) {
        ImportEventsDialog(this, path) {
            if (it) {
                runOnUiThread {
                    updateViewPager()
                    setupQuickFilter()
                }
            }
        }
    }

    private fun tryExportEvents() {
        if (isQPlus()) {
            ExportEventsDialog(this, config.lastExportPath, true) { file, eventTypes ->
                eventTypesToExport = eventTypes

                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/calendar"
                    putExtra(Intent.EXTRA_TITLE, file.name)
                    addCategory(Intent.CATEGORY_OPENABLE)

                    startActivityForResult(this, PICK_EXPORT_FILE_INTENT)
                }
            }
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE) {
                if (it) {
                    ExportEventsDialog(this, config.lastExportPath, false) { file, eventTypes ->
                        getFileOutputStream(file.toFileDirItem(this), true) {
                            exportEventsTo(eventTypes, it)
                        }
                    }
                }
            }
        }
    }

    private fun exportEventsTo(eventTypes: ArrayList<Long>, outputStream: OutputStream?) {
        ensureBackgroundThread {
            val events = eventsHelper.getEventsToExport(eventTypes)
            if (events.isEmpty()) {
                toast(R.string.no_entries_for_exporting)
            } else {
                IcsExporter().exportEvents(this, outputStream, events, true) {
                    toast(
                        when (it) {
                            ExportResult.EXPORT_OK -> R.string.exporting_successful
                            ExportResult.EXPORT_PARTIAL -> R.string.exporting_some_entries_failed
                            else -> R.string.exporting_failed
                        }
                    )
                }
            }
        }
    }

    private fun launchSettings() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_JODA

        val faqItems = arrayListOf(
            FAQItem(R.string.faq_2_title, R.string.faq_2_text),
            FAQItem(R.string.faq_3_title, R.string.faq_3_text),
            FAQItem(R.string.faq_1_title, R.string.faq_1_text),
            FAQItem(R.string.faq_1_title_commons, R.string.faq_1_text_commons),
            FAQItem(R.string.faq_4_title_commons, R.string.faq_4_text_commons),
            FAQItem(R.string.faq_4_title, R.string.faq_4_text),
            FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons),
            FAQItem(R.string.faq_6_title_commons, R.string.faq_6_text_commons),
            FAQItem(R.string.faq_7_title_commons, R.string.faq_7_text_commons)
        )

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }

    private fun searchQueryChanged(text: String) {
        mLatestSearchQuery = text
        search_placeholder_2.beGoneIf(text.length >= 2)
        if (text.length >= 2) {
            eventsHelper.getEventsWithSearchQuery(text, this) { searchedText, events ->
                if (searchedText == mLatestSearchQuery) {
                    search_results_list.beVisibleIf(events.isNotEmpty())
                    search_placeholder.beVisibleIf(events.isEmpty())
                    val listItems = getEventListItems(events)
                    val eventsAdapter = EventListAdapter(this, listItems, true, this, search_results_list) {
                        if (it is ListEvent) {
                            Intent(applicationContext, EventActivity::class.java).apply {
                                putExtra(EVENT_ID, it.id)
                                startActivity(this)
                            }
                        }
                    }

                    search_results_list.adapter = eventsAdapter
                }
            }
        } else {
            search_placeholder.beVisible()
            search_results_list.beGone()
        }
    }

    private fun checkSwipeRefreshAvailability() {
        swipe_refresh_layout.isEnabled = config.caldavSync && config.pullToRefresh && config.storedView != WEEKLY_VIEW
        if (!swipe_refresh_layout.isEnabled) {
            swipe_refresh_layout.isRefreshing = false
        }
    }

    // only used at active search
    override fun refreshItems() {
        searchQueryChanged(mLatestSearchQuery)
        refreshViewPager()
    }

    private fun openDayAt(timestamp: Long) {
        val dayCode = Formatter.getDayCodeFromTS(timestamp / 1000L)
        calendar_fab.beVisible()
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
