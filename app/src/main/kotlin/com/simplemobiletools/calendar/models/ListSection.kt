package com.simplemobiletools.calendar.models

class ListSection(val title: String, val isToday: Boolean) : ListItem() {
    override fun toString(): String {
        return "ListSection {title=$title, isToday=$isToday}"
    }
}
