package com.simplemobiletools.calendar.pro

import androidx.multidex.MultiDexApplication
import com.crowdin.platform.Crowdin
import com.crowdin.platform.CrowdinConfig
import com.simplemobiletools.commons.extensions.checkUseEnglish

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        checkUseEnglish()

        Crowdin.init(
            applicationContext,
            CrowdinConfig.Builder()
                .withDistributionHash("5a692655f8a6163d21119a7uo3a")
                .withUpdateInterval(10)
                .build()
        )
    }
}
