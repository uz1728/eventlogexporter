package com.ap.eventlogexporter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.util.Log

class EnrollmentFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.enrollment_layout, container, false)

        val enrollButton = view.findViewById<Button>(R.id.enrollButton)
        val enrollEntry = view.findViewById<EditText>(R.id.enrollEntry) // Initialize editText variable

        enrollEntry.hint = "Enter enrollment ID (P1-1XXX or P3-3XXX)"

        enrollButton.text = "Enroll"
        enrollButton.setOnClickListener {
            val enteredId = enrollEntry.text.toString()

            // Define the desired format using a regular expression
            val validFormat = Regex("(P(1-1|3-3)\\d{3})") // P1-1ddd or P3-3ddd where d is a number

            if (enteredId.matches(validFormat)) {
                val sharedPreferences =
                    requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().apply {
                    putString("enrolledId", enteredId)
                    putBoolean("enrollmentCompleted", true)
                    apply()
                }

                activity?.recreate()
                Log.d("EnrollmentFragment", "Activity Recreated")

            } else {
                // Display a toast to inform the user about the invalid format
                Toast.makeText(requireContext(), "Invalid ID format. Please enter a valid ID.", Toast.LENGTH_SHORT).show()

                // Clear the text in the EditText
                enrollEntry.text.clear()
            }
        }
        return view
    }
}
