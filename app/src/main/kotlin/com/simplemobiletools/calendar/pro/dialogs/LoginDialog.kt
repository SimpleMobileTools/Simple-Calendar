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
import android.widget.EditText
import com.simplemobiletools.calendar.pro.databinding.ActivityLoginBinding

class LoginDialog(val activity : SimpleActivity) {
    private val binding by activity.viewBinding(ActivityLoginBinding::inflate)
    //private var userNameEditText : EditText
    //private var passwordEditText : EditText

    //private var connection




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



            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.login) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(null)
                        ensureBackgroundThread {
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}
