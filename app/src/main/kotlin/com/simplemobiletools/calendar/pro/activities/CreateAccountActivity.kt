package com.simplemobiletools.calendar.pro.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.helpers.getJavaDayOfWeekFromJoda

class CreateAccountActivity : SimpleActivity() {

    private var emailEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var confirmPasswordEditText: EditText? = null
    private var createBtn: Button? = null
    private var loginBtn: TextView? = null
    private var dialogBoxError: TextView? = null
    private lateinit var firebaseAuth:FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        emailEditText = findViewById(R.id.createEmail)
        passwordEditText = findViewById(R.id.createPassword)
        confirmPasswordEditText = findViewById(R.id.confirmPassword)
        createBtn = findViewById(R.id.create_button)
        loginBtn = findViewById(R.id.login_button)
        dialogBoxError = findViewById(R.id.errorPassword)
        val myDialog: TextView = dialogBoxError!!
        myDialog.setTextColor(getResources().getColor(com.andrognito.patternlockview.R.color.white))

        firebaseAuth = FirebaseAuth.getInstance()

        createBtn?.setOnClickListener(View.OnClickListener {
            val email = emailEditText?.text.toString()
            val confirmPassword = confirmPasswordEditText?.text.toString()
            val password = passwordEditText?.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if(password == confirmPassword){
                    firebaseAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener{
                        if(it.isSuccessful){
                            Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@CreateAccountActivity, LoginActivity::class.java)
                            startActivity(intent)
                        }
                        else{
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else{
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }


        })

        loginBtn?.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@CreateAccountActivity, LoginActivity::class.java)
            startActivity(intent)
        })
    }

}

