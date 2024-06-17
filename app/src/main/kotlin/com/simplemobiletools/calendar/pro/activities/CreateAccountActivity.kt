package com.simplemobiletools.calendar.pro.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.helpers.getJavaDayOfWeekFromJoda

class CreateAccountActivity : SimpleActivity() {

    private var usernameEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var confirmPasswordEditText: EditText? = null
    private var createBtn: Button? = null
    private var loginBtn: TextView? = null
    private var username: String? = null
    private var createPassword: String? = null
    private var confirmPassword: String? = null
    private var password: String? = null
    private var dialogBoxError: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        usernameEditText = findViewById(R.id.createUsername)
        passwordEditText = findViewById(R.id.createPassword)
        confirmPasswordEditText = findViewById(R.id.confirmPassword)
        createBtn = findViewById(R.id.create_button)
        loginBtn = findViewById(R.id.login_button)
        dialogBoxError = findViewById(R.id.errorPassword)
        val myDialog: TextView = dialogBoxError!!
        myDialog.setTextColor(getResources().getColor(com.andrognito.patternlockview.R.color.white))

        createBtn?.setOnClickListener(View.OnClickListener {
            username = usernameEditText?.text.toString()
            confirmPassword = confirmPasswordEditText?.text.toString()
            createPassword = passwordEditText?.text.toString()
            if (confirmPassword == createPassword) {
                password = createPassword
            } else if (confirmPassword != createPassword) {
                myDialog.setTextColor(getResources().getColor(R.color.red_text))
            }


        })

        loginBtn?.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@CreateAccountActivity, LoginActivity::class.java)
            startActivity(intent)
        })
    }

}

