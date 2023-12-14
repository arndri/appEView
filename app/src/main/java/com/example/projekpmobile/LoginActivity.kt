package com.example.projekpmobile

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var createAccountTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.text2)
        passwordEditText = findViewById(R.id.text3)
        loginButton = findViewById(R.id.btn_log)
        firebaseAuth = FirebaseAuth.getInstance()
        createAccountTextView = findViewById(R.id.txt_create)

        // Set OnClickListener for the Login button
        loginButton.setOnClickListener { // Handle the click event
            onLoginButtonClick()
        }
        createAccountTextView.setOnClickListener {
            navigateToRegisterActivity()
        }
    }

    private fun onLoginButtonClick() {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            // Show a Toast message if username or password is empty
            Toast.makeText(this, "Both username and password are required.", Toast.LENGTH_SHORT).show()
            return // Exit the function early if either field is empty
        }

        // Use Firebase Authentication to sign in the user
        firebaseAuth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // If login is successful, navigate to MainActivity
                    val intent = Intent(this, MapsActivity::class.java)
                    startActivity(intent)
                    finish() // Close the LoginActivity
                } else {
                    // If login fails, show an error message
                    Toast.makeText(this, "Login failed. ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun navigateToRegisterActivity() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

}
