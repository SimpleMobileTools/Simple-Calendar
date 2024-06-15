package com.simplemobiletools.calendar.pro.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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

    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_create_account)

            usernameEditText = findViewById(R.id.createUsername)
            passwordEditText = findViewById(R.id.createPassword)
            confirmPasswordEditText = findViewById(R.id.confirmPassword)
            createBtn = findViewById(R.id.create_button)
            loginBtn = findViewById(R.id.login_button)

            createBtn?.setOnClickListener(View.OnClickListener {
                username =  usernameEditText?.text.toString()
                confirmPassword = confirmPasswordEditText?.text.toString()
                createPassword = passwordEditText?.text.toString()
                if (confirmPassword==createPassword) password = createPassword

            })

            loginBtn?.setOnClickListener(View.OnClickListener {
                val intent = Intent(this@CreateAccountActivity, LoginActivity::class.java)
                startActivity(intent)
            })
    }

}

