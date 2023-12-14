package com.example.projekpmobile

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize FirebaseAuth and DatabaseReference
        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("-")

        // Find references to the views
        emailEditText = findViewById(R.id.txt3)
        usernameEditText = findViewById(R.id.txt4)
        passwordEditText = findViewById(R.id.txt5)
        registerButton = findViewById(R.id.btn_reg)

        // Set OnClickListener for the Register button
        registerButton.setOnClickListener { onRegisterButtonClick() }
    }

    private fun onRegisterButtonClick() {
        // Retrieve data from EditText fields
        val email = emailEditText.text.toString()
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(email)) {
            // Show a Toast message if username or password is empty
            Toast.makeText(this, "Both username and password are required.", Toast.LENGTH_SHORT).show()
            return // Exit the function early if either field is empty
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration successful, save user data to the database
                    val userId = firebaseAuth.currentUser?.uid
                    if (userId != null) {
                        val userReference = databaseReference.child("users").child(userId)

                        val userData = HashMap<String, Any>()
                        userData["username"] = username
                        userData["email"] = email

                        userReference.setValue(userData)

                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                        // Redirect to LoginActivity after successful registration
                        redirectToLogin()
                    } else {
                        Toast.makeText(this, "User ID is null.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close the RegisterActivity
    }
}
