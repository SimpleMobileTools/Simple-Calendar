package com.simplemobiletools.calendar.pro.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.makeText
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.simplemobiletools.calendar.pro.R
import org.json.JSONObject
import java.util.HashMap

class LoginActivity : SimpleActivity() {
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

        createAccountBtn?.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@LoginActivity, CreateAccountActivity::class.java)
            startActivity(intent)
        })
    }

    public fun connectUser(){
        var url:String = "http://10.0.2.2/apiOurgenda/actions/connexion.php"
        var params: HashMap<String, String> = HashMap()
        params.put("user", this.username.toString())
        params.put("azerty", this.password.toString())
        val json = JSONObject(params as Map<*, *>?)

        var jsonObjectRequest:JsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, json, Response.Listener {response ->
            textView.text = Toast.makeText(getApplicationContext(), "OPERATION SUCCESSFUL", Toast.LENGTH_LONG).show()

        }

        }
        fun onErrorResponse(error: VolleyError){

        }
    }



