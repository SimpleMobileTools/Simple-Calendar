package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.widget.RadioGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.helpers.Config
import com.simplemobiletools.calendar.helpers.EVENTS_LIST_VIEW
import com.simplemobiletools.calendar.helpers.MONTHLY_VIEW
import com.simplemobiletools.calendar.helpers.YEARLY_VIEW
import kotlinx.android.synthetic.main.dialog_change_views.view.*

class ChangeViewDialog(val activity: Activity, val callback: (newView: Int) -> Unit) : AlertDialog.Builder(activity), RadioGroup.OnCheckedChangeListener {
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
        callback.invoke(getNewView(checkedId))
        dialog?.dismiss()
    }

    fun getNewView(id: Int) = when (id) {
        R.id.dialog_radio_yearly -> YEARLY_VIEW
        R.id.dialog_radio_events_list -> EVENTS_LIST_VIEW
        else -> MONTHLY_VIEW
    }

    fun getSavedItem() = when (Config.newInstance(activity).storedView) {
        YEARLY_VIEW -> R.id.dialog_radio_yearly
        EVENTS_LIST_VIEW -> R.id.dialog_radio_events_list
        else -> R.id.dialog_radio_monthly
    }
}
