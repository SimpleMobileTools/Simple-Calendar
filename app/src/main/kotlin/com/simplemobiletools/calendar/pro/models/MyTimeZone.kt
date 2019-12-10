package com.simplemobiletools.calendar.pro.models

import java.io.Serializable

// sample MyTimeZone(title="GMT+1", zoneName="Europe/Bratislava")
data class MyTimeZone(var title: String, val zoneName: String) : Serializable {
    companion object {
        private const val serialVersionUID = -32456354132688616L
    }
}
