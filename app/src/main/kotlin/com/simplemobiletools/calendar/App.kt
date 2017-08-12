package com.simplemobiletools.calendar

import com.facebook.stetho.Stetho

class App : BuildVariantApplication() {
    override fun onCreate() {
        super.onCreate()
        if (!shouldInit()) {
            return
        }
        Stetho.initializeWithDefaults(this)
    }
}
