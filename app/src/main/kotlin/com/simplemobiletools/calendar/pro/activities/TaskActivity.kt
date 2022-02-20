package com.simplemobiletools.calendar.pro.activities

import android.os.Bundle
import android.view.Menu
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.commons.extensions.checkAppSideloading

class TaskActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        if (checkAppSideloading()) {
            return
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu, true)
        return true
    }
}
