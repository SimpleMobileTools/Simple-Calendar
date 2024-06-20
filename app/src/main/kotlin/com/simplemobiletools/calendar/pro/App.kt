package com.simplemobiletools.calendar.pro

import androidx.multidex.MultiDexApplication
import com.simplemobiletools.commons.extensions.checkUseEnglish
import com.google.firebase.FirebaseApp

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
        FirebaseApp.initializeApp(this)
    }
}
