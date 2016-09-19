package com.simplemobiletools.calendar.extensions

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.MyWidgetProvider
import com.simplemobiletools.calendar.R

fun Context.updateWidget() {
    val intent = Intent(this, MyWidgetProvider::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    val ids = intArrayOf(R.xml.widget_info)
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    sendBroadcast(intent)
}
