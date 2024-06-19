package com.simplemobiletools.calendar.pro.activities
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.simplemobiletools.calendar.pro.R

class AddFriendActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajouter_amis)

        val friendNameEditText: EditText = findViewById(R.id.friend_name)
        val saveFriendButton: Button = findViewById(R.id.save_friend_button)

        saveFriendButton.setOnClickListener {
            val friendName = friendNameEditText.text.toString()
            val resultIntent = Intent()
            resultIntent.putExtra(MainActivityAmis.EXTRA_FRIEND_NAME, friendName)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}

