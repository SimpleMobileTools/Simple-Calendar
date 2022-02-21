package com.simplemobiletools.calendar.pro.activities

import android.os.Bundle
import android.view.Menu
import android.view.WindowManager
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.helpers.TASK_ID
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.checkAppSideloading
import com.simplemobiletools.commons.extensions.updateActionBarTitle
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.activity_task.*

class TaskActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        if (checkAppSideloading()) {
            return
        }

        val intent = intent ?: return
        val taskId = intent.getLongExtra(TASK_ID, 0L)
        setupNewTask()
        updateColors()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu, true)
        return true
    }

    private fun setupNewTask() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        task_title.requestFocus()
        updateActionBarTitle(getString(R.string.new_task))
    }

    private fun updateColors() {
        updateTextColors(task_scrollview)
        task_time_image.applyColorFilter(config.textColor)
    }
}
