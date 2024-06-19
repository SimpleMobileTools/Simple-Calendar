package com.simplemobiletools.calendar.pro.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.simplemobiletools.calendar.pro.R
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : SimpleActivity() {
    private var errorConnectAccountTextView: TextView? = null
    private var emailEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var connectBtn: Button? = null
    private var createAccountBtn: TextView? = null
    private lateinit var firebaseAuth:FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        errorConnectAccountTextView = findViewById(R.id.errorConnexionTextView)
        emailEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        connectBtn = findViewById(R.id.login_button)
        createAccountBtn = findViewById(R.id.create_account)

        firebaseAuth = FirebaseAuth.getInstance()

        connectBtn?.setOnClickListener(View.OnClickListener {
                val email =  emailEditText?.text.toString()
                val password = passwordEditText?.text.toString()

                if (email.isNotEmpty() && password.isNotEmpty()){
                    firebaseAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener{
                        if(it.isSuccessful){
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                        }else{
                            Toast.makeText(this,it.exception.toString(),Toast.LENGTH_SHORT).show()
                        }
                    }

                }
                else{
                    Toast.makeText(this,"Veuillez remplir tous les champs",Toast.LENGTH_SHORT).show()
                }
        })

        createAccountBtn?.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@LoginActivity, CreateAccountActivity::class.java)
            startActivity(intent)
        })
    }
}



