package com.simplemobiletools.calendar.pro.extensions

import com.simplemobiletools.calendar.pro.helpers.MONTH
import com.simplemobiletools.calendar.pro.helpers.WEEK
import com.simplemobiletools.calendar.pro.helpers.YEAR

fun Int.isXWeeklyRepetition() = this != 0 && this % WEEK == 0

fun Int.isXMonthlyRepetition() = this != 0 && this % MONTH == 0

fun Int.isXYearlyRepetition() = this != 0 && this % YEAR == 0
