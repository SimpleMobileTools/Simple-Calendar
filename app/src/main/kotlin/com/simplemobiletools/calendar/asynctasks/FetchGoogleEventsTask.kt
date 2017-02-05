package com.simplemobiletools.calendar.asynctasks

// more info about event fields at https://developers.google.com/google-apps/calendar/v3/reference/events/insert
/*class FetchGoogleEventsTask(val activity: Activity, credential: GoogleAccountCredential) : AsyncTask<Void, Void, List<Event>>() {
    private val CONFIRMED = "confirmed"
    private val PRIMARY = "primary"
    private val ITEMS = "items"
    private val OVERRIDES = "overrides"
    private val POPUP = "popup"
    private val RRULE = "RRULE:"
    private val NEXT_PAGE_TOKEN = "nextPageToken"

    private val DAILY = "DAILY"
    private val WEEKLY = "WEEKLY"
    private val MONTHLY = "MONTHLY"
    private val YEARLY = "YEARLY"

    private var service: com.google.api.services.calendar.Calendar
    private var lastError: Exception? = null

    init {
        val transport = AndroidHttp.newCompatibleTransport()
        service = com.google.api.services.calendar.Calendar.Builder(transport, GsonFactory(), credential)
                .setApplicationName(activity.resources.getString(R.string.app_name))
                .build()
    }

    override fun doInBackground(vararg params: Void): List<Event>? {
        try {
            return getDataFromApi()
        } catch (e: Exception) {
            lastError = e
            cancel(true)
            return ArrayList()
        }
    }

    private fun getDataFromApi(): List<Event> {
        val parsedEvents = ArrayList<Event>()
        var currToken = ""
        while (true) {
            val events = service.events().list(PRIMARY)
                    .setPageToken(currToken)
                    .execute()

            for (event in events) {
                if (event.key == ITEMS) {
                    val parsed = parseEvents(event.value.toString())
                }
            }

            if (events.containsKey(NEXT_PAGE_TOKEN)) {
                currToken = events[NEXT_PAGE_TOKEN] as String
            } else {
                break
            }
        }

        return parsedEvents
    }

    private fun parseEvents(json: String): List<Event> {
        val events = ArrayList<Event>()
        val token = object : TypeToken<List<GoogleEvent>>() {}.type
        val googleEvents = Gson().fromJson<ArrayList<GoogleEvent>>(json, token) ?: ArrayList<GoogleEvent>(8)
        for (googleEvent in googleEvents) {
            if (googleEvent.status != CONFIRMED)
                continue

            val reminder = getReminder(googleEvent.reminders)
            val recurrence = getRecurrence(googleEvent.recurrence)
            val start = googleEvent.start
            val end = googleEvent.end
            if (start.date != null) {
                val startTS = DateTime(start.date).withHourOfDay(1).seconds()
                val endTS = DateTime(end.date).withHourOfDay(1).seconds()
            } else {
                val startTS = DateTime(start.dateTime).seconds()
                val endTS = DateTime(end.dateTime).seconds()
            }
        }
        return events
    }

    private fun getReminder(json: JsonObject): Int {
        val array = json.getAsJsonArray(OVERRIDES)
        val token = object : TypeToken<List<GoogleEventReminder>>() {}.type
        val reminders = Gson().fromJson<ArrayList<GoogleEventReminder>>(array, token) ?: ArrayList<GoogleEventReminder>(2)
        for ((method, minutes) in reminders) {
            if (method == POPUP) {
                return minutes
            }
        }
        return -1
    }

    private fun getRecurrence(recurrence: JsonArray?): Int {
        if (recurrence == null) {
            return 0
        }

        var rule = recurrence[0].toString().trim('"')
        if (!rule.startsWith(RRULE))
            return 0

        rule = rule.substring(RRULE.length)

        val parts = rule.split(';')
        val frequency = parts[0].split("=")[1]
        return getFrequencyMinutes(frequency)
    }

    private fun getFrequencyMinutes(frequency: String): Int {
        return when (frequency) {
            DAILY -> DAY
            WEEKLY -> WEEK
            MONTHLY -> MONTH
            YEARLY -> YEAR
            else -> 0
        }
    }

    override fun onCancelled() {
        if (lastError != null) {
            if (lastError is UserRecoverableAuthIOException) {
                activity.startActivityForResult((lastError as UserRecoverableAuthIOException).intent, SettingsActivity.REQUEST_AUTHORIZATION)
            }
        }
    }
}*/
