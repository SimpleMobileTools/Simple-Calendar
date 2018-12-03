package com.simplemobiletools.calendar.pro.activities

import android.content.Intent
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.commons.activities.BaseSplashActivity

class SplashActivity : BaseSplashActivity() {
    override fun initActivity() {
        when {
            intent.extras?.containsKey(DAY_CODE) == true -> Intent(this, MainActivity::class.java).apply {
                putExtra(DAY_CODE, intent.getStringExtra(DAY_CODE))
                putExtra(VIEW_TO_OPEN, intent.getIntExtra(VIEW_TO_OPEN, LAST_VIEW))
                startActivity(this)
            }
            intent.extras?.containsKey(EVENT_ID) == true -> Intent(this, MainActivity::class.java).apply {
                putExtra(EVENT_ID, intent.getLongExtra(EVENT_ID, 0L))
                putExtra(EVENT_OCCURRENCE_TS, intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L))
                startActivity(this)
            }
            else -> startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
