package com.simplemobiletools.calendar

import android.support.multidex.MultiDexApplication

abstract class BaseApp : MultiDexApplication() {
    open fun shouldInit() = true
}
