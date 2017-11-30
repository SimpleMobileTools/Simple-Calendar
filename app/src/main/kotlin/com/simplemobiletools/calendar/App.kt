package com.simplemobiletools.calendar

import android.support.multidex.MultiDexApplication
import com.facebook.stetho.Stetho
import com.simplemobiletools.calendar.BuildConfig.USE_LEAK_CANARY
import com.simplemobiletools.commons.extensions.checkUseEnglish
import com.squareup.leakcanary.LeakCanary

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (USE_LEAK_CANARY) {
            if (LeakCanary.isInAnalyzerProcess(this)) {
                return
            }
            LeakCanary.install(this)
        }

        checkUseEnglish()
        Stetho.initializeWithDefaults(this)
    }
}
