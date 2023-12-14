package com.example.projekpmobile

import android.app.AlertDialog
import android.content.Intent
import android.icu.util.Calendar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.projekpmobile.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.Marker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val profileButton = findViewById<ImageButton>(R.id.profileButton)
        profileButton.setOnClickListener {
            // Open the ProfileActivity when the profile button is clicked
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Set a listener for when the user clicks on the map
        mMap.setOnMapClickListener(this)
        mMap.setOnMarkerClickListener(this)
        // Move the camera to a default location (e.g., Sydney)
        val sydney = LatLng(-34.0, 151.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        mMap.uiSettings.isZoomControlsEnabled = true

        fetchPinsAndAddMarkers()
    }

    override fun onMapClick(latLng: LatLng) {
        // Add a marker at the clicked location
        showAddPinDialog(latLng)
        // Display the coordinates in a Toast
        showToast("Clicked\nLat: ${latLng.latitude}, Lng: ${latLng.longitude}")
    }

    private fun showAddPinDialog(latLng: LatLng) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_pin, null)

        val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)

        val buttonAdd = dialogView.findViewById<Button>(R.id.buttonAdd)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Add Pin Information")
            .setView(dialogView)
            .create()

        buttonAdd.setOnClickListener {
            // Handle the "Add" button click
            val name = dialogView.findViewById<EditText>(R.id.editTextName).text.toString()
            val description =
                dialogView.findViewById<EditText>(R.id.editTextDescription).text.toString()

            // Get the selected date and time
            val year = datePicker.year
            val month = datePicker.month
            val day = datePicker.dayOfMonth
            val hour = timePicker.currentHour
            val minute = timePicker.currentMinute

            // Create a Calendar instance and set the selected date and time
            val calendar = Calendar.getInstance()
            calendar.set(year, month, day, hour, minute)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val date = dateFormat.format(calendar.time)
            val time = timeFormat.format(calendar.time)
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                // Add a marker at the clicked location
                val marker = mMap.addMarker(MarkerOptions().position(latLng).title(name))

                // Display the coordinates in a Toast
                showToast("Clicked\nLat: ${latLng.latitude}, Lng: ${latLng.longitude}")

                // Store pin information in the Realtime Database under the user's ID
                val pinsRef = FirebaseDatabase.getInstance().reference.child("pins")
                val pinId = pinsRef.push().key
                val pin = Pin(
                    pinId,
                    name,
                    date,
                    time,
                    description,
                    latLng.latitude,
                    latLng.longitude,
                    userId
                )
                pinsRef.child(pinId!!).setValue(pin)

                // Dismiss the dialog
                alertDialog.dismiss()
            } else {
                // The user is not authenticated, handle accordingly
                showToast("User not authenticated.")
            }
        }

        buttonCancel.setOnClickListener {
            // Handle the "Cancel" button click
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun fetchPinsAndAddMarkers() {
        val pinsRef = FirebaseDatabase.getInstance().reference.child("pins")

        pinsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Clear existing markers on the map
                mMap.clear()

                // Iterate through all pins in the dataSnapshot
                for (pinSnapshot in dataSnapshot.children) {
                    val pin = pinSnapshot.getValue(Pin::class.java)

                    // Add a marker for each pin
                    if (pin != null && pin.latitude != null && pin.longitude != null) {
                        val pinLatLng = LatLng(pin.latitude, pin.longitude)
                        val marker =  mMap.addMarker(MarkerOptions().position(pinLatLng).title(pin.name).snippet("Date: ${pin.date}"))

                        marker!!.tag = pinSnapshot.key

                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
                Toast.makeText(
                    this@MapsActivity,
                    "Error fetching pins: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
    override fun onMarkerClick(marker: Marker): Boolean {
        // Retrieve pin ID from the marker's tag
        val pinId = marker.tag as? String

        if (pinId != null) {
            // Fetch detailed pin information using pin ID
            val pinsRef = FirebaseDatabase.getInstance().reference.child("pins").child(pinId)
            pinsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val pin = dataSnapshot.getValue(Pin::class.java)
                    if (pin != null) {
                        // Show a dialog with detailed pin information
                        showPinInfoDialog(pin)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle error
                    Toast.makeText(
                        this@MapsActivity,
                        "Error fetching pin details: ${databaseError.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        // Return true to consume the event (showing the default info window)
        return true
    }

    private fun showPinInfoDialog(pin: Pin) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_info_window, null)

        // Set the dialog view with pin information
        dialogView.findViewById<TextView>(R.id.textViewPinName).text = "Name: ${pin.name}"
        dialogView.findViewById<TextView>(R.id.textViewPinDate).text = "Date: ${pin.date}"
        dialogView.findViewById<TextView>(R.id.textViewPinTime).text = "Time: ${pin.time}"
        dialogView.findViewById<TextView>(R.id.textViewPinDescription).text = "Description: ${pin.description}"

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Pin Information")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                // Handle OK button click if needed
            }
            .create()

        alertDialog.show()
    }
}

