package com.simplemobiletools.calendar

import android.support.multidex.MultiDexApplication
import com.facebook.stetho.Stetho
import com.simplemobiletools.calendar.BuildConfig.USE_LEAK_CANARY
import com.simplemobiletools.calendar.extensions.config
import com.squareup.leakcanary.LeakCanary
import java.util.*

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (USE_LEAK_CANARY) {
            if (LeakCanary.isInAnalyzerProcess(this)) {
                return
            }
            LeakCanary.install(this)
        }

        if (config.useEnglish) {
            val conf = resources.configuration
            conf.locale = Locale.ENGLISH
            resources.updateConfiguration(conf, resources.displayMetrics)
        }

        Stetho.initializeWithDefaults(this)
    }
}
