package com.simplemobiletools.calendar.views.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.widget.RadioGroup
import com.simplemobiletools.calendar.helpers.Config
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.R
import kotlinx.android.synthetic.main.dialog_change_views.view.*

class ChangeViewDialog(val activity: Activity) : AlertDialog.Builder(activity), RadioGroup.OnCheckedChangeListener {
    val dialog: AlertDialog?

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_change_views, null)
        view.dialog_radio_view.check(getSavedItem())
        view.dialog_radio_view.setOnCheckedChangeListener(this)

        dialog = AlertDialog.Builder(activity)
                .setTitle(activity.resources.getString(R.string.change_view))
                .setView(view)
                .create()

        dialog?.show()
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        (activity as ChangeViewListener).viewChanged(getNewView(checkedId))
        dialog?.dismiss()
    }

    fun getNewView(id: Int): Int {
        return when (id) {
            R.id.dialog_radio_yearly -> Constants.YEARLY_VIEW
            R.id.dialog_radio_events_list -> Constants.EVENTS_LIST_VIEW
            else -> Constants.MONTHLY_VIEW
        }
    }

    fun getSavedItem(): Int {
        return when (Config.newInstance(activity).storedView) {
            Constants.YEARLY_VIEW -> R.id.dialog_radio_yearly
            Constants.EVENTS_LIST_VIEW -> R.id.dialog_radio_events_list
            else -> R.id.dialog_radio_monthly
        }
    }

    interface ChangeViewListener {
        fun viewChanged(newView: Int)
    }
}
