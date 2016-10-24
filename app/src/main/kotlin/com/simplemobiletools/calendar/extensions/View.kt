package com.simplemobiletools.calendar.extensions

import android.view.View

fun View.beVisibleIf(beVisible: Boolean) = if (beVisible) visibility = View.VISIBLE else visibility = View.GONE

fun View.beGoneIf(beGone: Boolean) = if (beGone) visibility = View.GONE else visibility = View.VISIBLE
