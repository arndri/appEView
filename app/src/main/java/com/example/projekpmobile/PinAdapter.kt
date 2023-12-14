package com.example.projekpmobile

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase


class PinAdapter(context: Context, private val pinList: List<Pair<String, String>>) : ArrayAdapter<Pair<String, String>>(context, 0, pinList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var listItemView = convertView

        if (listItemView == null) {
            listItemView =
                LayoutInflater.from(context).inflate(R.layout.pin_list_item, parent, false)
        }

        val pinInfoTextView = listItemView?.findViewById<TextView>(R.id.pinInfoTextView)
        val buttonUpdate = listItemView?.findViewById<Button>(R.id.buttonUpdate)
        val buttonDelete = listItemView?.findViewById<Button>(R.id.buttonDelete)

        val pinInfoPair = getItem(position)

        // Check if pinInfoPair is not null before accessing its components
        if (pinInfoPair != null) {
            val (pinId, pinInfo) = pinInfoPair

            pinInfoTextView?.text = pinInfo

            buttonUpdate?.setOnClickListener {
                if (context is ProfileActivity) {
                    val profileActivity = context as ProfileActivity
                    profileActivity.handleUpdate(pinId)
                }
            }

            buttonDelete?.setOnClickListener {
                // Handle delete action here
                deletePin(pinId)
            }
        }

        return listItemView!!
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun deletePin(pinId: String) {
        val pinsRef = FirebaseDatabase.getInstance().getReference("pins").child(pinId)
        pinsRef.removeValue()
        showToast("Pin deleted")
    }
}