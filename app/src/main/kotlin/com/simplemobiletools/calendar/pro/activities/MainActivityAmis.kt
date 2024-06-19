package com.simplemobiletools.calendar.pro.activities
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.simplemobiletools.calendar.pro.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityAmis : AppCompatActivity() {
    private val friendsList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var db: AppDatabase
    private lateinit var friendDao: FriendDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_ajout_amis)

        db = AppDatabase.getDatabase(this)
        friendDao = db.friendDao()

        CoroutineScope(Dispatchers.IO).launch {
            val friendsFromDb = friendDao.getAllFriends()
            friendsList.addAll(friendsFromDb.map { it.name })
            runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        }

        val listView: ListView = findViewById(R.id.friends_list)
        val addFriendButton: Button = findViewById(R.id.add_friend_button)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, friendsList)
        listView.adapter = adapter

        addFriendButton.setOnClickListener {
            val intent = Intent(this, AddFriendActivity::class.java)
            startActivityForResult(intent, ADD_FRIEND_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_FRIEND_REQUEST_CODE && resultCode == RESULT_OK) {
            val newFriend = data?.getStringExtra(EXTRA_FRIEND_NAME)
            if (newFriend != null) {
                friendsList.add(newFriend)
                adapter.notifyDataSetChanged()
            }
            CoroutineScope(Dispatchers.IO).launch {
                if (newFriend != null) {
                    friendDao.insert(Friend(name = newFriend))
                }
            }
        }
    }

    companion object {
        const val ADD_FRIEND_REQUEST_CODE = 1
        const val EXTRA_FRIEND_NAME = "com.example.myapp.FRIEND_NAME"
    }
}

