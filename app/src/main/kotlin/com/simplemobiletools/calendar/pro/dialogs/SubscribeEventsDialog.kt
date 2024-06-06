package com.simplemobiletools.calendar.pro.dialogs
import android.app.Dialog
import androidx.appcompat.app.AlertDialog
import com.google.android.material.internal.ViewUtils.showKeyboard
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.MainActivity
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.databinding.DialogSubscribeBinding
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import android.view.View


class SubscribeEventsDialog(val activity: SimpleActivity) {
    private val binding by activity.viewBinding(DialogSubscribeBinding::inflate)
    private var url = binding.urlEditText.text
    //private val url

    init {
        ensureBackgroundThread {


            activity.runOnUiThread {
                initDialog()
            }
        }
    }

    private fun initDialog() {


        binding.apply {

        }

        activity.getAlertDialogBuilder()

            .setPositiveButton(com.simplemobiletools.commons.R.string.ok, null)
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)

            .apply {

                activity.setupDialogStuff(binding.root, this, R.string.import_events) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        url = binding.urlEditText.text
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(null)
                        ensureBackgroundThread {
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }

}
