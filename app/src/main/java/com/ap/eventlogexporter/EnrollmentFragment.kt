package com.ap.eventlogexporter

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class EnrollmentFragment : Fragment() {
    companion object {
        val TAG: String = EnrollmentFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.enrollment_layout, container, false)

        val enrollButton = view.findViewById<Button>(R.id.enrollButton)
        val enrollEntry =
            view.findViewById<EditText>(R.id.enrollEntry) // Initialize editText variable

        enrollEntry.hint = "Enter enrollment ID (P1-1XXX or P3-3XXX)"

        enrollButton.text = getString(R.string.enroll_button)
        enrollButton.setOnClickListener {
            handleEnrollment()
        }
        return view
    }

    private fun handleEnrollment() {
        val enrollEntry = requireView().findViewById<EditText>(R.id.enrollEntry)
        val enteredId = enrollEntry.text.toString()

        // Define the desired format using a regular expression
        val validFormat = Regex("(debug|(P(1-1|3-3)\\d{3}))") // P1-1ddd or P3-3ddd where d is a number

        if (enteredId.matches(validFormat)) {
            val sharedPreferences =
                requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().apply {
                putString("enrolledId", enteredId)
                putBoolean("enrollmentCompleted", true)
                apply()
            }

            activity?.recreate()
            Log.d(TAG, "Activity Recreated")

        } else {
            // Display a toast to inform the user about the invalid format
            Toast.makeText(
                requireContext(),
                "Invalid ID format. Please enter a valid ID.",
                Toast.LENGTH_SHORT
            ).show()

            // Clear the text in the EditText
            enrollEntry.text.clear()
        }
    }
}
