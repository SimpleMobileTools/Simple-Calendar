package com.simplemobiletools.calendar.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.simplemobiletools.calendar.BuildConfig
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.EventListAdapter
import com.simplemobiletools.calendar.dialogs.ExportEventsDialog
import com.simplemobiletools.calendar.dialogs.FilterEventTypesDialog
import com.simplemobiletools.calendar.dialogs.ImportEventsDialog
import com.simplemobiletools.calendar.extensions.*
import com.simplemobiletools.calendar.fragments.*
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.Release
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.DateTime
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : SimpleActivity(), RefreshRecyclerViewListener {
    private val CALDAV_SYNC_DELAY = 1000L

    private var showCalDAVRefreshToast = false
    private var mShouldFilterBeVisible = false
    private var mIsSearchOpen = false
    private var mLatestSearchQuery = ""
    private var mCalDAVSyncHandler = Handler()
    private var mSearchMenuItem: MenuItem? = null
    private var shouldGoToTodayBeVisible = false
    private var goToTodayButton: MenuItem? = null
    private var currentFragments = ArrayList<MyFragmentHolder>()

    private var mStoredTextColor = 0
    private var mStoredBackgroundColor = 0
    private var mStoredPrimaryColor = 0
    private var mStoredDayCode = ""
    private var mStoredIsSundayFirst = false
    private var mStoredUse24HourFormat = false
    private var mStoredDimPastEvents = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)

        // just get a reference to the database to make sure it is created properly
        dbHelper

        checkWhatsNewDialog()
        calendar_fab.beVisibleIf(config.storedView != YEARLY_VIEW)
        calendar_fab.setOnClickListener {
            launchNewEventIntent(currentFragments.last().getNewEventDayCode())
        }

        storeStateVariables()
        if (resources.getBoolean(R.bool.portrait_only)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        if (!checkViewIntents()) {
            return
        }

        if (!checkOpenIntents()) {
            updateViewPager()
        }

        if (!hasPermission(PERMISSION_WRITE_CALENDAR) || !hasPermission(PERMISSION_READ_CALENDAR)) {
            config.caldavSync = false
        }

        if (config.caldavSync) {
            refreshCalDAVCalendars(false)
        }
    }

    override fun onResume() {
        super.onResume()
        if (mStoredTextColor != config.textColor || mStoredBackgroundColor != config.backgroundColor || mStoredPrimaryColor != config.primaryColor
                || mStoredDayCode != Formatter.getTodayCode(applicationContext) || mStoredDimPastEvents != config.dimPastEvents) {
            updateViewPager()
        }

        dbHelper.getEventTypes {
            mShouldFilterBeVisible = it.size > 1 || config.displayEventTypes.isEmpty()
        }

        if (config.storedView == WEEKLY_VIEW) {
            if (mStoredIsSundayFirst != config.isSundayFirst || mStoredUse24HourFormat != config.use24HourFormat) {
                updateViewPager()
            }
        }

        storeStateVariables()
        updateWidgets()
        if (config.storedView != EVENTS_LIST_VIEW) {
            updateTextColors(calendar_coordinator)
        }
        search_placeholder.setTextColor(config.textColor)
        search_placeholder_2.setTextColor(config.textColor)
        calendar_fab.setColors(config.textColor, getAdjustedPrimaryColor(), config.backgroundColor)
        search_holder.background = ColorDrawable(config.backgroundColor)
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onStop() {
        super.onStop()
        mCalDAVSyncHandler.removeCallbacksAndMessages(null)
        contentResolver.unregisterContentObserver(calDAVSyncObserver)
        closeSearch()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.apply {
            goToTodayButton = findItem(R.id.go_to_today)
            findItem(R.id.filter).isVisible = mShouldFilterBeVisible
            findItem(R.id.go_to_today).isVisible = shouldGoToTodayBeVisible && config.storedView != EVENTS_LIST_VIEW
        }

        setupSearch(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu!!.apply {
            findItem(R.id.refresh_caldav_calendars).isVisible = config.caldavSync
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.change_view -> showViewDialog()
            R.id.go_to_today -> goToToday()
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
        if (currentFragments.size > 1) {
            removeTopFragment()
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkOpenIntents()
        checkViewIntents()
    }

    private fun storeStateVariables() {
        config.apply {
            mStoredIsSundayFirst = isSundayFirst
            mStoredTextColor = textColor
            mStoredPrimaryColor = primaryColor
            mStoredBackgroundColor = backgroundColor
            mStoredUse24HourFormat = use24HourFormat
            mStoredDimPastEvents = dimPastEvents
        }
        mStoredDayCode = Formatter.getTodayCode(applicationContext)
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
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                mIsSearchOpen = false
                search_holder.beGone()
                calendar_fab.beVisible()
                return true
            }
        })
    }

    private fun closeSearch() {
        mSearchMenuItem?.collapseActionView()
    }

    private fun checkOpenIntents(): Boolean {
        val dayCodeToOpen = intent.getStringExtra(DAY_CODE) ?: ""
        val openMonth = intent.getBooleanExtra(OPEN_MONTH, false)
        intent.removeExtra(OPEN_MONTH)
        intent.removeExtra(DAY_CODE)
        if (dayCodeToOpen.isNotEmpty()) {
            calendar_fab.beVisible()
            config.storedView = if (openMonth) MONTHLY_VIEW else DAILY_VIEW
            updateViewPager(dayCodeToOpen)
            return true
        }

        val eventIdToOpen = intent.getIntExtra(EVENT_ID, 0)
        val eventOccurrenceToOpen = intent.getIntExtra(EVENT_OCCURRENCE_TS, 0)
        intent.removeExtra(EVENT_ID)
        intent.removeExtra(EVENT_OCCURRENCE_TS)
        if (eventIdToOpen != 0 && eventOccurrenceToOpen != 0) {
            Intent(this, EventActivity::class.java).apply {
                putExtra(EVENT_ID, eventIdToOpen)
                putExtra(EVENT_OCCURRENCE_TS, eventOccurrenceToOpen)
                startActivity(this)
            }
        }

        return false
    }

    private fun checkViewIntents(): Boolean {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            if (uri.authority == "com.android.calendar") {
                if (uri.path.startsWith("/events")) {
                    // intents like content://com.android.calendar/events/1756
                    val eventId = uri.lastPathSegment
                    val id = dbHelper.getEventIdWithLastImportId(eventId)
                    if (id != 0) {
                        Intent(this, EventActivity::class.java).apply {
                            putExtra(EVENT_ID, id)
                            startActivity(this)
                        }
                    } else {
                        toast(R.string.unknown_error_occurred)
                    }
                } else if (intent?.extras?.getBoolean("DETAIL_VIEW", false) == true) {
                    // clicking date on a third party widget: content://com.android.calendar/time/1507309245683
                    val timestamp = uri.pathSegments.last()
                    if (timestamp.areDigitsOnly()) {
                        openDayAt(timestamp.toLong())
                        return false
                    }
                }
            } else {
                tryImportEventsFromFile(uri)
            }
        }
        return true
    }

    private fun showViewDialog() {
        val items = arrayListOf(
                RadioItem(DAILY_VIEW, getString(R.string.daily_view)),
                RadioItem(WEEKLY_VIEW, getString(R.string.weekly_view)),
                RadioItem(MONTHLY_VIEW, getString(R.string.monthly_view)),
                RadioItem(YEARLY_VIEW, getString(R.string.yearly_view)),
                RadioItem(EVENTS_LIST_VIEW, getString(R.string.simple_event_list)))

        RadioGroupDialog(this, items, config.storedView) {
            calendar_fab.beVisibleIf(it as Int != YEARLY_VIEW)
            resetActionBarTitle()
            closeSearch()
            updateView(it)
            shouldGoToTodayBeVisible = false
            invalidateOptionsMenu()
        }
    }

    private fun goToToday() {
        currentFragments.last().goToToday()
    }

    private fun resetActionBarTitle() {
        updateActionBarTitle(getString(R.string.app_launcher_name))
        updateActionBarSubtitle("")
    }

    private fun showFilterDialog() {
        FilterEventTypesDialog(this) {
            refreshViewPager()
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

        syncCalDAVCalendars(this, calDAVSyncObserver)
        scheduleCalDAVSync(true)
    }

    private val calDAVSyncObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            if (!selfChange) {
                mCalDAVSyncHandler.removeCallbacksAndMessages(null)
                mCalDAVSyncHandler.postDelayed({
                    recheckCalDAVCalendars {
                        refreshViewPager()
                        if (showCalDAVRefreshToast) {
                            toast(R.string.refreshing_complete)
                        }
                    }
                }, CALDAV_SYNC_DELAY)
            }
        }
    }

    private fun addHolidays() {
        val items = getHolidayRadioItems()
        RadioGroupDialog(this, items) {
            toast(R.string.importing)
            Thread {
                val holidays = getString(R.string.holidays)
                var eventTypeId = dbHelper.getEventTypeIdWithTitle(holidays)
                if (eventTypeId == -1) {
                    val eventType = EventType(0, holidays, resources.getColor(R.color.default_holidays_color))
                    eventTypeId = dbHelper.insertEventType(eventType)
                }

                val result = IcsImporter(this).importEvents(it as String, eventTypeId, 0, false)
                handleParseResult(result)
                if (result != IcsImporter.ImportResult.IMPORT_FAIL) {
                    runOnUiThread {
                        updateViewPager()
                    }
                }
            }.start()
        }
    }

    private fun tryAddBirthdays() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                Thread {
                    addContactEvents(true) {
                        if (it > 0) {
                            toast(R.string.birthdays_added)
                            updateViewPager()
                        } else {
                            toast(R.string.no_birthdays)
                        }
                    }
                }.start()
            } else {
                toast(R.string.no_contacts_permission)
            }
        }
    }

    private fun tryAddAnniversaries() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                Thread {
                    addContactEvents(false) {
                        if (it > 0) {
                            toast(R.string.anniversaries_added)
                            updateViewPager()
                        } else {
                            toast(R.string.no_anniversaries)
                        }
                    }
                }.start()
            } else {
                toast(R.string.no_contacts_permission)
            }
        }
    }

    private fun handleParseResult(result: IcsImporter.ImportResult) {
        toast(when (result) {
            IcsImporter.ImportResult.IMPORT_OK -> R.string.holidays_imported_successfully
            IcsImporter.ImportResult.IMPORT_PARTIAL -> R.string.importing_some_holidays_failed
            else -> R.string.importing_holidays_failed
        }, Toast.LENGTH_LONG)
    }

    private fun addContactEvents(birthdays: Boolean, callback: (Int) -> Unit) {
        var eventsAdded = 0
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Event.CONTACT_ID,
                ContactsContract.CommonDataKinds.Event.CONTACT_LAST_UPDATED_TIMESTAMP,
                ContactsContract.CommonDataKinds.Event.START_DATE)

        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
        val type = if (birthdays) ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY else ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY
        val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE, type.toString())
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                val dateFormats = getDateFormats()
                val existingEvents = if (birthdays) dbHelper.getBirthdays() else dbHelper.getAnniversaries()
                val importIDs = existingEvents.map { it.importId }
                val eventTypeId = if (birthdays) getBirthdaysEventTypeId() else getAnniversariesEventTypeId()

                do {
                    val contactId = cursor.getIntValue(ContactsContract.CommonDataKinds.Event.CONTACT_ID).toString()
                    val name = cursor.getStringValue(ContactsContract.Contacts.DISPLAY_NAME)
                    val startDate = cursor.getStringValue(ContactsContract.CommonDataKinds.Event.START_DATE)

                    for (format in dateFormats) {
                        try {
                            val formatter = SimpleDateFormat(format, Locale.getDefault())
                            val date = formatter.parse(startDate)
                            if (date.year < 70) {
                                date.year = 70
                            }

                            val timestamp = (date.time / 1000).toInt()
                            val source = if (birthdays) SOURCE_CONTACT_BIRTHDAY else SOURCE_CONTACT_ANNIVERSARY
                            val lastUpdated = cursor.getLongValue(ContactsContract.CommonDataKinds.Event.CONTACT_LAST_UPDATED_TIMESTAMP)
                            val event = Event(0, timestamp, timestamp, name, importId = contactId, flags = FLAG_ALL_DAY, repeatInterval = YEAR,
                                    eventType = eventTypeId, source = source, lastUpdated = lastUpdated)

                            if (!importIDs.contains(contactId)) {
                                dbHelper.insert(event, false) {
                                    eventsAdded++
                                }
                            }
                            break
                        } catch (e: Exception) {
                        }
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            showErrorToast(e)
        } finally {
            cursor?.close()
        }

        runOnUiThread {
            callback(eventsAdded)
        }
    }

    private fun getBirthdaysEventTypeId(): Int {
        val birthdays = getString(R.string.birthdays)
        var eventTypeId = dbHelper.getEventTypeIdWithTitle(birthdays)
        if (eventTypeId == -1) {
            val eventType = EventType(0, birthdays, resources.getColor(R.color.default_birthdays_color))
            eventTypeId = dbHelper.insertEventType(eventType)
        }
        return eventTypeId
    }

    private fun getAnniversariesEventTypeId(): Int {
        val anniversaries = getString(R.string.anniversaries)
        var eventTypeId = dbHelper.getEventTypeIdWithTitle(anniversaries)
        if (eventTypeId == -1) {
            val eventType = EventType(0, anniversaries, resources.getColor(R.color.default_anniversaries_color))
            eventTypeId = dbHelper.insertEventType(eventType)
        }
        return eventTypeId
    }

    private fun updateView(view: Int) {
        calendar_fab.beVisibleIf(view != YEARLY_VIEW)
        config.storedView = view
        updateViewPager()
        if (goToTodayButton?.isVisible == true) {
            shouldGoToTodayBeVisible = false
            invalidateOptionsMenu()
        }
    }

    private fun updateViewPager(dayCode: String? = Formatter.getTodayCode(applicationContext)) {
        val fragment = getFragmentsHolder()
        currentFragments.forEach {
            supportFragmentManager.beginTransaction().remove(it).commitNow()
        }
        currentFragments.clear()
        currentFragments.add(fragment)
        val bundle = Bundle()

        when (config.storedView) {
            DAILY_VIEW, MONTHLY_VIEW -> bundle.putString(DAY_CODE, dayCode)
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
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun getThisWeekDateTime(): String {
        var thisweek = DateTime().withDayOfWeek(1).withTimeAtStartOfDay().minusDays(if (config.isSundayFirst) 1 else 0)
        if (DateTime().minusDays(7).seconds() > thisweek.seconds()) {
            thisweek = thisweek.plusDays(7)
        }
        return thisweek.toString()
    }

    private fun getFragmentsHolder() = when (config.storedView) {
        DAILY_VIEW -> DayFragmentsHolder()
        MONTHLY_VIEW -> MonthFragmentsHolder()
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
            if (!isActivityDestroyed()) {
                currentFragments.last().refreshEvents()
            }
        }
    }

    private fun tryImportEvents() {
        handlePermission(PERMISSION_READ_STORAGE) {
            if (it) {
                importEvents()
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
            uri.scheme == "file" -> showImportEventsDialog(uri.path)
            uri.scheme == "content" -> {
                val tempFile = getTempFile()
                if (tempFile == null) {
                    toast(R.string.unknown_error_occurred)
                    return
                }

                val inputStream = contentResolver.openInputStream(uri)
                val out = FileOutputStream(tempFile)
                inputStream.copyTo(out)
                showImportEventsDialog(tempFile.absolutePath)
            }
            else -> toast(R.string.invalid_file_format)
        }
    }

    private fun showImportEventsDialog(path: String) {
        ImportEventsDialog(this, path) {
            if (it) {
                runOnUiThread {
                    updateViewPager()
                }
            }
        }
    }

    private fun tryExportEvents() {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                exportEvents()
            }
        }
    }

    private fun exportEvents() {
        FilePickerDialog(this, pickFile = false, showFAB = true) {
            ExportEventsDialog(this, it) { exportPastEvents, file, eventTypes ->
                Thread {
                    val events = dbHelper.getEventsToExport(exportPastEvents).filter { eventTypes.contains(it.eventType.toString()) }
                    if (events.isEmpty()) {
                        toast(R.string.no_entries_for_exporting)
                    } else {
                        IcsExporter().exportEvents(this, file, events as ArrayList<Event>, true) {
                            toast(when (it) {
                                IcsExporter.ExportResult.EXPORT_OK -> R.string.exporting_successful
                                IcsExporter.ExportResult.EXPORT_PARTIAL -> R.string.exporting_some_entries_failed
                                else -> R.string.exporting_failed
                            })
                        }
                    }
                }.start()
            }
        }
    }

    private fun launchSettings() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_JODA or LICENSE_STETHO or LICENSE_MULTISELECT or LICENSE_LEAK_CANARY

        val faqItems = arrayListOf(
                FAQItem(R.string.faq_1_title_commons, R.string.faq_1_text_commons),
                FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons),
                FAQItem(R.string.faq_4_title_commons, R.string.faq_4_text_commons),
                FAQItem(getString(R.string.faq_1_title), getString(R.string.faq_1_text)),
                FAQItem(getString(R.string.faq_2_title), getString(R.string.faq_2_text)),
                FAQItem(getString(R.string.faq_3_title), getString(R.string.faq_3_text)))

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }

    private fun searchQueryChanged(text: String) {
        mLatestSearchQuery = text
        search_placeholder_2.beGoneIf(text.length >= 2)
        if (text.length >= 2) {
            dbHelper.getEventsWithSearchQuery(text) { searchedText, events ->
                if (searchedText == mLatestSearchQuery) {
                    runOnUiThread {
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
            }
        } else {
            search_placeholder.beVisible()
            search_results_list.beGone()
        }
    }

    // only used at active search
    override fun refreshItems() {
        searchQueryChanged(mLatestSearchQuery)
        refreshViewPager()
    }

    private fun openDayAt(timestamp: Long) {
        val dayCode = Formatter.getDayCodeFromTS((timestamp / 1000).toInt())
        calendar_fab.beVisible()
        config.storedView = DAILY_VIEW
        updateViewPager(dayCode)
    }

    private fun getHolidayRadioItems(): ArrayList<RadioItem> {
        val items = ArrayList<RadioItem>()

        LinkedHashMap<String, String>().apply {
            put("Algeria", "algeria.ics")
            put("Argentina", "argentina.ics")
            put("Australia", "australia.ics")
            put("België", "belgium.ics")
            put("Bolivia", "bolivia.ics")
            put("Brasil", "brazil.ics")
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
            put("Hanguk", "southkorea.ics")
            put("Hellas", "greece.ics")
            put("Hrvatska", "croatia.ics")
            put("India", "india.ics")
            put("Indonesia", "indonesia.ics")
            put("Ísland", "iceland.ics")
            put("Italia", "italy.ics")
            put("Latvija", "latvia.ics")
            put("Lietuva", "lithuania.ics")
            put("Luxemburg", "luxembourg.ics")
            put("Makedonija", "macedonia.ics")
            put("Magyarország", "hungary.ics")
            put("México", "mexico.ics")
            put("Nederland", "netherlands.ics")
            put("日本", "japan.ics")
            put("Norge", "norway.ics")
            put("Österreich", "austria.ics")
            put("Pākistān", "pakistan.ics")
            put("Polska", "poland.ics")
            put("Portugal", "portugal.ics")
            put("Россия", "russia.ics")
            put("România", "romania.ics")
            put("Schweiz", "switzerland.ics")
            put("Singapore", "singapore.ics")
            put("Srbija", "serbia.ics")
            put("Slovenija", "slovenia.ics")
            put("Slovensko", "slovakia.ics")
            put("South Africa", "southafrica.ics")
            put("Suomi", "finland.ics")
            put("Sverige", "sweden.ics")
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
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
