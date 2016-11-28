package com.simplemobiletools.calendar.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.widget.RemoteViews
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.DayActivity
import com.simplemobiletools.calendar.adapters.EventListWidgetAdapter
import com.simplemobiletools.calendar.extensions.adjustAlpha
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import org.joda.time.DateTime
import java.util.*
import kotlin.comparisons.compareBy

class MyWidgetListProvider : AppWidgetProvider(), DBHelper.GetEventsListener {
    companion object {
        private var mTextColor = 0

        lateinit var mRemoteViews: RemoteViews
        lateinit var mRes: Resources
        lateinit var mContext: Context
        lateinit var mWidgetManager: AppWidgetManager
        lateinit var mIntent: Intent
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        initVariables(context)
        updateWidget()
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun initVariables(context: Context) {
        mContext = context
        mRes = mContext.resources

        val prefs = initPrefs(context)
        val storedTextColor = prefs.getInt(WIDGET_TEXT_COLOR, Color.WHITE)
        mTextColor = storedTextColor.adjustAlpha(HIGH_ALPHA)

        mWidgetManager = AppWidgetManager.getInstance(mContext)

        mRemoteViews = RemoteViews(mContext.packageName, R.layout.widget_event_list)
        mIntent = Intent(mContext, MyWidgetListProvider::class.java)

        val bgColor = prefs.getInt(WIDGET_BG_COLOR, Color.BLACK)
        mRemoteViews.setInt(R.id.widget_event_list, "setBackgroundColor", bgColor)
    }

    private fun updateWidget() {
        val thisWidget = ComponentName(mContext, MyWidgetListProvider::class.java)
        AppWidgetManager.getInstance(mContext).updateAppWidget(thisWidget, mRemoteViews)

        val fromTS = (DateTime().millis / 1000).toInt()
        val toTS = (DateTime().plusMonths(6).millis / 1000).toInt()
        DBHelper(mContext).getEvents(fromTS, toTS, this)
    }

    private fun setupDayOpenIntent(id: Int, dayCode: String) {
        Intent(mContext, DayActivity::class.java).apply {
            putExtra(DAY_CODE, dayCode)
            val pendingIntent = PendingIntent.getActivity(mContext, Integer.parseInt(dayCode), this, 0)
            mRemoteViews.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    private fun initPrefs(context: Context) = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

    override fun gotEvents(events: MutableList<Event>) {
        val listItems = ArrayList<ListItem>(events.size)
        val sorted = events.sortedWith(compareBy({ it.startTS }, { it.endTS }, { it.title }, { it.description }))
        var prevCode = ""
        sorted.forEach {
            val code = Formatter.getDayCodeFromTS(it.startTS)
            if (code != prevCode) {
                val day = Formatter.getDayTitle(mContext, code)
                listItems.add(ListSection(day))
                prevCode = code
            }
            listItems.add(ListEvent(it.id, it.startTS, it.endTS, it.title, it.description))
        }

        val eventsAdapter = EventListWidgetAdapter(mContext, listItems)
    }
}
