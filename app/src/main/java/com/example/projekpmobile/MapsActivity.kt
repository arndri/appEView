package com.example.projekpmobile

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.location.Location
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.projekpmobile.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val profileButton = findViewById<ImageButton>(R.id.profileButton)
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Set a listener for when the user clicks on the map
        mMap.setOnMapClickListener(this)
        mMap.setOnMarkerClickListener(this)

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocationAndMoveCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        mMap.uiSettings.isZoomControlsEnabled = true

        fetchPinsAndAddMarkers()
    }

    override fun onMapClick(latLng: LatLng) {
        // Add a marker at the clicked location
        showAddPinDialog(latLng)
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
            .create()

        alertDialog.show()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, get the location and move the camera
                getCurrentLocationAndMoveCamera()
            } else {
                // Location permission denied, handle accordingly
                showToast("Location permission denied.")
            }
        }
    }
    private fun getCurrentLocationAndMoveCamera() {
        // Check for location permissions
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions are granted, proceed to get the location
            getAndMoveCamera()
        } else {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getAndMoveCamera() {
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        try {
            // Get the last known location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        // Get the current user location
                        val userLatLng = LatLng(location.latitude, location.longitude)

                        // Move the map camera to the user's location
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng))
                    } else {
                        showToast("Unable to get current location.")
                    }
                }
                .addOnFailureListener { e ->
                    showToast("Error getting current location: ${e.message}")
                }
        } catch (e: SecurityException) {
            showToast("Location permission not granted.")
        }
    }
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}

