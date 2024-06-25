package com.simplemobiletools.calendar.pro.activities

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simplemobiletools.calendar.pro.App
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.ouragenda.database.AppDatabase
import com.simplemobiletools.calendar.pro.ouragenda.database.FriendDao
import com.simplemobiletools.calendar.pro.ouragenda.model.Friend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityAmis : SimpleActivity() {
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: AppDatabase
    private lateinit var friendDao: FriendDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_ajout_amis)

        val listView: ListView = findViewById(R.id.friends_list)
        val addFriendButton: Button = findViewById(R.id.add_friend_button)

        db = AppDatabase.getDatabase(this)
        friendDao = db.friendDao()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter
        CoroutineScope(Dispatchers.IO).launch {
            adapter.addAll(friendDao.getAllFriends().map { it.name })
        }
        addFriendButton.setOnClickListener {
            showInputDialog()
        }

        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
    }

    private fun showInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Email's friend")

        // Configurez l'entrée de texte
        val input = EditText(this)
        builder.setView(input)

        // Configurez les boutons
        builder.setPositiveButton("OK") { dialog, which ->
            val inputText = input.text.toString()
            firestore.collection("users")
                .whereEqualTo("mail", inputText)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.size() == 1) {
                        if (adapter.getPosition(inputText) != -1) {
                            Toast.makeText(this, "$inputText is already a friend.", Toast.LENGTH_SHORT).show()
                        } else {
                            adapter.addAll(inputText)
                            CoroutineScope(Dispatchers.IO).launch {
                                friendDao.insert(Friend(name = inputText))
                            }
                        }
                    } else if (documents.size() > 1) throw RuntimeException("more than one user with mail ${inputText}???")
                    else {
                        Toast.makeText(this, "No user $inputText found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to search users.", Toast.LENGTH_SHORT).show()
                }
        }
        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        builder.show()
    }
}

