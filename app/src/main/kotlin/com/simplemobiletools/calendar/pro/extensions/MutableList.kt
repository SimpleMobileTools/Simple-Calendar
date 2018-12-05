package com.simplemobiletools.calendar.pro.extensions

import com.simplemobiletools.calendar.pro.helpers.CHOPPED_LIST_DEFAULT_SIZE

// inspired by https://stackoverflow.com/questions/2895342/java-how-can-i-split-an-arraylist-in-multiple-small-arraylists/2895365#2895365
fun MutableList<Long>.getChoppedList(chunkSize: Int = CHOPPED_LIST_DEFAULT_SIZE): ArrayList<ArrayList<Long>> {
    val parts = ArrayList<ArrayList<Long>>()
    val listSize = this.size
    var i = 0
    while (i < listSize) {
        val newList = subList(i, Math.min(listSize, i + chunkSize)).toMutableList() as ArrayList<Long>
        parts.add(newList)
        i += chunkSize
    }
    return parts
}
