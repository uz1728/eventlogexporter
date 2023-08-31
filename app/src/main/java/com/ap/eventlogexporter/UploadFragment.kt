package com.ap.eventlogexporter

import android.Manifest
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File

class UploadFragment : Fragment() {

    private val requestCode = 55555
    private lateinit var lineCountTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.upload_layout, container, false)

        lineCountTextView = view.findViewById(R.id.lineCountTextView)
        // Retrieve the number of lines from SharedPreferences and set it to the TextView
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val numberOfLines = sharedPreferences.getInt("numberOfLines", 0)
        lineCountTextView.text = "Number of Events: ${numberOfLines}"

        val permission = Manifest.permission.ACCESS_NETWORK_STATE
        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), requestCode)
        }

        val uploadButton = view.findViewById<Button>(R.id.uploadButton)
        uploadButton.setOnClickListener {
            onUploadButtonClick()
        }
        uploadButton.text = "Email Event Log"

        return view
    }

    private fun onUploadButtonClick() {
        val context = requireContext()

        val sharedPreferences =
            requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val enrolledId = sharedPreferences.getString("enrolledId", null)

        val file = File(context.filesDir, "${enrolledId}_event_log.txt")
        val uri = FileProvider.getUriForFile(context, "com.ap.eventlogexporter.fileprovider", file)

        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "text/plain"
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "${enrolledId} Event Log")
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Here's the event log file for ${enrolledId}.")
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Specify the email recipient (optional)
        // emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("recipient@example.com"))

        // Create a chooser to allow the user to select an email app
        val chooserIntent = Intent.createChooser(emailIntent, "Send via Email")

        // Check if there are email apps available on the device
        if (emailIntent.resolveActivity(context.packageManager) != null) {
            startActivity(chooserIntent)
        } else {
            // Handle the case where no email app is available
            // You can show a Toast or alert the user
        }
    }

    override fun onResume() {
        super.onResume()

        // Retrieve the number of lines from SharedPreferences and update the TextView
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val numberOfLines = sharedPreferences.getInt("numberOfLines", 0)
        lineCountTextView.text = "Number of Events: $numberOfLines"
    }
}
