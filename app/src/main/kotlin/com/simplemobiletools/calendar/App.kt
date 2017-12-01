package com.simplemobiletools.calendar

import android.support.multidex.MultiDexApplication
import com.facebook.stetho.Stetho
import com.simplemobiletools.commons.extensions.checkUseEnglish
import com.squareup.leakcanary.LeakCanary

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            if (LeakCanary.isInAnalyzerProcess(this)) {
                return
            }
            LeakCanary.install(this)
            Stetho.initializeWithDefaults(this)
        }

        checkUseEnglish()
    }
}
