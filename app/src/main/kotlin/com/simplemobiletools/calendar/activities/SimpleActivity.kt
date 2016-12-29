package com.simplemobiletools.calendar.activities

import android.os.Bundle
import com.simplemobiletools.calendar.helpers.Config
import com.simplemobiletools.commons.activities.BaseSimpleActivity

open class SimpleActivity : BaseSimpleActivity() {
    lateinit var config: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = Config.newInstance(applicationContext)
    }
}
