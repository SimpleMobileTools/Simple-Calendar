package com.simplemobiletools.calendar.pro.activities

import android.os.Bundle
import android.view.Menu
import android.view.WindowManager
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.TASK_ID
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.mydebug
import kotlinx.android.synthetic.main.activity_task.*
import org.joda.time.DateTime

class TaskActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        if (checkAppSideloading()) {
            return
        }

        val intent = intent ?: return
        val taskId = intent.getLongExtra(TASK_ID, 0L)
        updateColors()
        gotTask()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu, true)
        return true
    }

    private fun gotTask() {
        task_all_day.setOnCheckedChangeListener { compoundButton, isChecked -> toggleAllDay(isChecked) }
        task_all_day_holder.setOnClickListener {
            task_all_day.toggle()
        }

        task_start_date.text = Formatter.getDate(this, DateTime.now())
        task_start_time.text = Formatter.getTime(this, DateTime.now())
        setupNewTask()
    }

    private fun setupNewTask() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        task_title.requestFocus()
        updateActionBarTitle(getString(R.string.new_task))
    }

    private fun toggleAllDay(isChecked: Boolean) {
        hideKeyboard()
        task_start_time.beGoneIf(isChecked)
    }

    private fun updateColors() {
        updateTextColors(task_scrollview)
        task_time_image.applyColorFilter(config.textColor)
    }
}
