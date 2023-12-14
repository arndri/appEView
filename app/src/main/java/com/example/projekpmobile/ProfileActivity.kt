// ProfileActivity.kt

package com.example.projekpmobile

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance().reference

        // Retrieve and display the username
        val userId = auth.currentUser?.uid
        if (userId != null) {
            retrieveUsername(userId)
        }
    }

    private fun retrieveUsername(userId: String) {
        val usernameTextView = findViewById<TextView>(R.id.textViewUsername)

        // Reference to the "users" node in the database
        val userRef = database.child(userId)

        // Read the username from the database
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").value?.toString()
                usernameTextView.text = username
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error
            }
        })
    }
}
