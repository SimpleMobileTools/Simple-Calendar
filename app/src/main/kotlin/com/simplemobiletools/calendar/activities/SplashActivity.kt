package com.simplemobiletools.calendar.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.simplemobiletools.calendar.helpers.DAY_CODE
import com.simplemobiletools.calendar.helpers.EVENT_ID
import com.simplemobiletools.calendar.helpers.EVENT_OCCURRENCE_TS

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when {
            intent.extras?.containsKey(DAY_CODE) == true -> Intent(this, MainActivity::class.java).apply {
                putExtra(DAY_CODE, intent.getStringExtra(DAY_CODE))
                startActivity(this)
            }
            intent.extras?.containsKey(EVENT_ID) == true -> Intent(this, MainActivity::class.java).apply {
                putExtra(EVENT_ID, intent.getIntExtra(EVENT_ID, 0))
                putExtra(EVENT_OCCURRENCE_TS, intent.getIntExtra(EVENT_OCCURRENCE_TS, 0))
                startActivity(this)
            }
            else -> startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
