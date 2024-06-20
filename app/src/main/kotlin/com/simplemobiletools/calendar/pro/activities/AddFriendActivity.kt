package com.simplemobiletools.calendar.pro.activities
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.databases.User
import com.simplemobiletools.calendar.pro.adapters.UserAdapter
import androidx.recyclerview.widget.ListAdapter

class AddFriendActivity : AppCompatActivity() {
    private lateinit var userAdapter: UserAdapter
    lateinit var firestore: FirebaseFirestore
    lateinit var firebaseAuth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private var userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajouter_amis)
        firestore = FirebaseFirestore.getInstance()
        val friendEmailEditText: EditText = findViewById(R.id.friend_email)
        val searchFriendButton: Button = findViewById(R.id.search_friend_button)
        userAdapter = UserAdapter(userList)

        searchFriendButton.setOnClickListener {
            val friendEmail = friendEmailEditText.text.toString()
            searchUsers(friendEmail)
        }
    }
    private fun searchUsers(query: String) {
        firestore.collection("users")
            .whereEqualTo("mail", query)
            .get()
            .addOnSuccessListener { documents ->
                val users = documents.map { it.toObject(User::class.java) }
                userAdapter=UserAdapter(users)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to search users.", Toast.LENGTH_SHORT).show()
            }
    }
    private fun addFriend(user: User) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return
        val friendId = user.id

        val currentUserRef = firestore.collection("users").document(currentUserId)
        val friendUserRef = firestore.collection("users").document(friendId.toString())

        currentUserRef.collection("friends").document(friendId.toString()).set(mapOf("friendId" to friendId))
        friendUserRef.collection("friends").document(currentUserId).set(mapOf("friendId" to currentUserId))

        Toast.makeText(this, "${user.mail} added as a friend.", Toast.LENGTH_SHORT).show()
    }
}

