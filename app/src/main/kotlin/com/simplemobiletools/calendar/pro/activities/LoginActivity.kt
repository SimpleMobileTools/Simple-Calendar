package com.simplemobiletools.calendar.pro.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.simplemobiletools.calendar.pro.R

class LoginActivity : AppCompatActivity() {
    private var errorConnectAccountTextView: TextView? = null
    private var usernameEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var connectBtn: Button? = null
    private var createAccountBtn: TextView? = null
    private var username: String? = null
    private var password: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        errorConnectAccountTextView = findViewById(R.id.errorConnexionTextView)
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        connectBtn = findViewById(R.id.login_button)
        createAccountBtn = findViewById(R.id.create_account)

        connectBtn?.setOnClickListener(View.OnClickListener {
                username =  usernameEditText?.text.toString()
                password = passwordEditText?.text.toString()
        })
    }
}
