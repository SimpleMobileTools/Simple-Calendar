package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.RadioGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_change_views.view.*

class EventRepeatIntervalDialog(val activity: Activity, val repeatInterval: Int, val callback: (mins: Int) -> Unit) : AlertDialog.Builder(activity),
        RadioGroup.OnCheckedChangeListener {
    val dialog: AlertDialog?

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_event_repeat_interval, null).dialog_radio_view.apply {
            check(getCheckedItem())
            setOnCheckedChangeListener(this@EventRepeatIntervalDialog)
        }

        dialog = AlertDialog.Builder(activity)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_repeat_interval)
        }
    }

    private fun getCheckedItem() = when (repeatInterval) {
        DAY -> R.id.dialog_radio_daily
        WEEK -> R.id.dialog_radio_weekly
        BIWEEK -> R.id.dialog_radio_biweekly
        MONTH -> R.id.dialog_radio_monthly
        YEAR -> R.id.dialog_radio_yearly
        else -> R.id.dialog_radio_no_repetition
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        callback.invoke(getSelectionValue(checkedId))
        dialog?.dismiss()
    }

    private fun getSelectionValue(id: Int) = when (id) {
        R.id.dialog_radio_daily -> DAY
        R.id.dialog_radio_weekly -> WEEK
        R.id.dialog_radio_biweekly -> BIWEEK
        R.id.dialog_radio_monthly -> MONTH
        R.id.dialog_radio_yearly -> YEAR
        else -> 0
    }
}
