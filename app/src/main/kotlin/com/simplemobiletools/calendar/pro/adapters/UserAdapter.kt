package com.simplemobiletools.calendar.pro.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.databases.User

class UserAdapter(private val userList: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.userEmail.text = user.mail

        // Set an OnClickListener for the Add Friend button
        holder.addFriendButton.setOnClickListener {
            // Add your desired functionality here
            // For example, you might want to call a function to add the user as a friend
            addFriend(user)
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userEmail: TextView = itemView.findViewById(R.id.user_mail_text_view)
        val addFriendButton: Button = itemView.findViewById(R.id.add_friend_button)
    }

    // Example function to handle adding a friend
    private fun addFriend(user: User) {
        // Implement the logic to add a friend here
    }
}
