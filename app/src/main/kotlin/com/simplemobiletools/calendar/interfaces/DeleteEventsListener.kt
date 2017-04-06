package com.simplemobiletools.calendar.interfaces

import java.util.*

interface DeleteEventsListener {
    fun deleteItems(ids: ArrayList<Int>)

    fun addEventRepeatException(parentIds: ArrayList<Int>, timestamps: ArrayList<Int>)
}
