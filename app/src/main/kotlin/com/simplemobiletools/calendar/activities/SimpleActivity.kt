package com.simplemobiletools.calendar.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.simplemobiletools.calendar.Config
import com.simplemobiletools.calendar.R

open class SimpleActivity : AppCompatActivity() {
    lateinit var mConfig: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        mConfig = Config.newInstance(applicationContext)
        setTheme(if (mConfig.isDarkTheme) R.style.AppTheme_Dark else R.style.AppTheme)
        super.onCreate(savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}
