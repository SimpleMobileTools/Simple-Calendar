package com.simplemobiletools.calendar.pro.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.simplemobiletools.calendar.pro.R
import com.google.firebase.firestore.FirebaseFirestore
import com.simplemobiletools.calendar.pro.databases.User

class CreateAccountActivity : SimpleActivity() {

    private var emailEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var confirmPasswordEditText: EditText? = null
    private var createBtn: Button? = null
    private var loginBtn: TextView? = null
    private var dialogBoxError: TextView? = null
    private lateinit var firebaseAuth:FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

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
        firestore =  FirebaseFirestore.getInstance()

        createBtn?.setOnClickListener(View.OnClickListener {
            val email = emailEditText?.text.toString()
            val confirmPassword = confirmPasswordEditText?.text.toString()
            val password = passwordEditText?.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if(password == confirmPassword){
                    firebaseAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener{
                        if(it.isSuccessful){
                            val userId = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener
                            val user = User(userId, email)
                            firestore.collection("users").document(userId).set(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Compte créé avec succès", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@CreateAccountActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
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

