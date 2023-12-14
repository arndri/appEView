package com.example.projekpmobile

import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var listViewPins: ListView
    private val pinList: MutableList<Pair<String, String>> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance().reference
        listViewPins = findViewById(R.id.listViewPins)

        // Retrieve and display the username
        val userId = auth.currentUser?.uid
        if (userId != null) {
            retrieveUsername(userId)
            retrieveUserPins(userId)
        } else {
            showToast("User ID is null")
        }
    }

    private fun retrieveUsername(userId: String) {
        val usernameTextView = findViewById<TextView>(R.id.textViewUsername)

        // Reference to the "users" node in the database under the specific user ID
        val userRef = database.child("users").child(userId)

        // Read the username from the database
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").value?.toString()
                usernameTextView.text = username
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Data retrieval canceled or failed")
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun retrieveUserPins(userId: String) {
        val pinsRef = database.child("pins")
        val query = pinsRef.orderByChild("addedByUserId").equalTo(userId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pinList = ArrayList<Pair<String, String>>()

                for (pinSnapshot in snapshot.children) {
                    val pinId = pinSnapshot.child("id").value?.toString()
                    val pinName = pinSnapshot.child("name").value?.toString()
                    val pinDate = pinSnapshot.child("date").value?.toString()
                    val pinTime = pinSnapshot.child("time").value?.toString()
                    val pinDescription = pinSnapshot.child("description").value?.toString()

                    val pinInfo = "Name: $pinName\nDate: $pinDate\nTime: $pinTime\nDescription: $pinDescription"
                    pinList.add(Pair(pinId!!, pinInfo))
                }

                val adapter = PinAdapter(this@ProfileActivity, pinList)
                listViewPins.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Data retrieval canceled or failed")
            }
        })
    }
    fun handleUpdate(pinId: String) {
        // Reference to the "pins" node in the database under the specific pinId
        val pinRef = database.child("pins").child(pinId)

        // Read the data from the database
        pinRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pinName = snapshot.child("name").value?.toString()
                val pinDate = snapshot.child("date").value?.toString()
                val pinTime = snapshot.child("time").value?.toString()
                val pinDescription = snapshot.child("description").value?.toString()

                // Now, you have the data, and you can use it as needed
                showToast("Data fetched - Name: $pinName, Date: $pinDate, Time: $pinTime, Description: $pinDescription")

                // Optionally, you can open a dialog or another activity to allow the user to edit the data
                openUpdateDialog(pinId, pinName, pinDate, pinTime, pinDescription)
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Data retrieval canceled or failed")
            }
        })
    }
    private fun openUpdateDialog(pinId: String, pinName: String?, pinDate: String?, pinTime: String?, pinDescription: String?) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_pin, null)

        val editTextName = view.findViewById<EditText>(R.id.editTextName)
        val datePicker = view.findViewById<DatePicker>(R.id.datePicker)
        val timePicker = view.findViewById<TimePicker>(R.id.timePicker)
        val editTextDescription = view.findViewById<EditText>(R.id.editTextDescription)
        val buttonAdd = view.findViewById<Button>(R.id.buttonAdd)
        val buttonCancel = view.findViewById<Button>(R.id.buttonCancel)

        // Set the retrieved data to UI components
        editTextName.setText(pinName)

        // Set date to datePicker
        val dateParts = pinDate?.split("-")
        if (dateParts?.size == 3) {
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1 // Month is 0-based
            val day = dateParts[2].toInt()
            datePicker.updateDate(year, month, day)
        }

        // Set time to timePicker
        val timeParts = pinTime?.split(":")
        if (timeParts?.size == 2) {
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            timePicker.hour = hour
            timePicker.minute = minute
        }

        editTextDescription.setText(pinDescription)

        builder.setView(view)
            .setTitle("Update Pin")

        val dialog = builder.create()

        buttonAdd.setOnClickListener {
            // Get the updated data from UI components
            val updatedName = editTextName.text.toString()
            val updatedDate = getDateFromDatePicker(datePicker)
            val updatedTime = getTimeFromTimePicker(timePicker)
            val updatedDescription = editTextDescription.text.toString()

            // Update the data in the database using pinId and the updated data
            updatePinData(pinId, updatedName, updatedDate, updatedTime, updatedDescription)

            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            // User canceled the dialog
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updatePinData(pinId: String, updatedName: String, updatedDate: Date, updatedTime: Date, updatedDescription: String) {
        // Reference to the "pins" node in the database under the specific pinId
        val pinRef = database.child("pins").child(pinId)

        // Create a Map to update the data
        val updatedPinData = mapOf(
            "name" to updatedName,
            "date" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(updatedDate),
            "time" to SimpleDateFormat("HH:mm", Locale.getDefault()).format(updatedTime),
            "description" to updatedDescription
        )

        // Update the data in the database
        pinRef.updateChildren(updatedPinData).addOnSuccessListener {
            showToast("Pin updated successfully")
        }.addOnFailureListener {
            showToast("Failed to update pin")
        }
    }
    private fun getDateFromDatePicker(datePicker: DatePicker): Date {
        val year = datePicker.year
        val month = datePicker.month
        val day = datePicker.dayOfMonth

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)

        return calendar.time
    }

    private fun getTimeFromTimePicker(timePicker: TimePicker): Date {
        val calendar = Calendar.getInstance()
        calendar.set(0, 0, 0, timePicker.hour, timePicker.minute)

        return calendar.time
    }
}
