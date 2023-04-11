package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.view.ViewGroup
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.adapters.CheckableColorAdapter
import com.simplemobiletools.calendar.pro.views.AutoGridLayoutManager
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_select_event_color.view.*

class SelectEventColorDialog(val activity: Activity, val colors: IntArray, var currentColor: Int, val callback: (color: Int) -> Unit) {

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_select_event_color, null) as ViewGroup
        val colorAdapter = CheckableColorAdapter(activity, colors, currentColor)
        view.color_grid.apply {
            val width = activity.resources.getDimensionPixelSize(R.dimen.smaller_icon_size)
            val spacing = activity.resources.getDimensionPixelSize(R.dimen.small_margin) * 2
            layoutManager = AutoGridLayoutManager(context = activity, itemWidth = width + spacing)
            adapter = colorAdapter
        }

        activity.getAlertDialogBuilder()
            .apply {
                setPositiveButton(R.string.ok) { dialog, _ ->
                    callback(colorAdapter.currentColor)
                    dialog?.dismiss()
                }
                setNeutralButton(R.string.default_calendar_color) { dialog, _ ->
                    callback(0)
                    dialog?.dismiss()
                }

                activity.setupDialogStuff(view, this, R.string.event_color)
            }
    }
}
