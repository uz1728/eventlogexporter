package com.ap.eventlogexporter

import android.Manifest
import android.content.Context
import android.content.Intent
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

    companion object {
        private const val TAG = "UploadFragment"
        private const val REQUEST_CODE = 555
    }

    private lateinit var lineCountTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.upload_layout, container, false)

        lineCountTextView = view.findViewById(R.id.lineCountTextView)

        // Retrieve the number of lines from SharedPreferences and set it to the TextView
        val sharedPreferences =
            requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val numberOfLines = sharedPreferences.getInt("numberOfLines", 0)
        lineCountTextView.text = getString(R.string.number_of_events, numberOfLines)

        val permission = Manifest.permission.ACCESS_NETWORK_STATE
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), REQUEST_CODE)
        }

        val uploadButton = view.findViewById<Button>(R.id.uploadButton)
        uploadButton.setOnClickListener {
            onUploadButtonClick()
        }
        uploadButton.text = getString(R.string.email_event_log)

        return view
    }

    private fun onUploadButtonClick() {
        val context = requireContext()

        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val enrolledId = sharedPreferences.getString("enrolledId", null)

        val file = File(context.filesDir, "${enrolledId}_event_log.txt")
        val uri = FileProvider.getUriForFile(context, "com.ap.eventlogexporter.fileprovider", file)

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${enrolledId} Event Log")
            putExtra(Intent.EXTRA_TEXT, "Here's the event log file for ${enrolledId}.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Create a chooser to allow the user to select an email app
        val chooserIntent = Intent.createChooser(emailIntent, getString(R.string.send_via_email))

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
        val sharedPreferences =
            requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val numberOfLines = sharedPreferences.getInt("numberOfLines", 0)
        lineCountTextView.text = getString(R.string.number_of_events, numberOfLines)
    }
}
