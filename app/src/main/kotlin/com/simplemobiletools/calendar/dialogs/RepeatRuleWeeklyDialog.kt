package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.View
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.views.MyAppCompatCheckbox
import kotlinx.android.synthetic.main.dialog_vertical_linear_layout.view.*

class RepeatRuleWeeklyDialog(val activity: Activity, val curRepeatRule: Int, val callback: (repeatRule: Int) -> Unit) :
        AlertDialog.Builder(activity) {
    val dialog: AlertDialog
    val view: View = activity.layoutInflater.inflate(R.layout.dialog_vertical_linear_layout, null)

    init {
        val days = arrayOf(R.string.monday, R.string.tuesday, R.string.wednesday, R.string.thursday, R.string.friday, R.string.saturday, R.string.sunday)
        val res = activity.resources
        val checkboxes = ArrayList<MyAppCompatCheckbox>(7)
        for (i in 0..6) {
            val pow = Math.pow(2.0, i.toDouble()).toInt()
            (activity.layoutInflater.inflate(R.layout.my_checkbox, null) as MyAppCompatCheckbox).apply {
                isChecked = curRepeatRule and pow != 0
                text = res.getString(days[i])
                id = pow
                checkboxes.add(this)
            }
        }

        if (activity.config.isSundayFirst) {
            checkboxes.add(0, checkboxes.removeAt(6))
        }

        checkboxes.forEach {
            view.dialog_vertical_linear_layout.addView(it)
        }

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialog, which -> callback(getRepeatRuleSum()) })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    private fun getRepeatRuleSum(): Int {
        var sum = 0
        val cnt = view.dialog_vertical_linear_layout.childCount
        for (i in 0..cnt - 1) {
            val child = view.dialog_vertical_linear_layout.getChildAt(i)
            if (child is MyAppCompatCheckbox) {
                if (child.isChecked)
                    sum += child.id
            }
        }
        return sum
    }
}
