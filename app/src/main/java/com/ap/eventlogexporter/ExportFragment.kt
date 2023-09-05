package com.ap.eventlogexporter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

class ExportFragment : Fragment() {

    companion object {
        private const val TAG = "ExportFragment"
        private const val REQUEST_CODE = 555
    }

    private lateinit var lineCountTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.export_layout, container, false)

        lineCountTextView = view.findViewById(R.id.lineCountTextView)

        // Retrieve the number of lines from SharedPreferences and set it to the TextView
        val sharedPreferences = requireContext().applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val numberOfLines = sharedPreferences.getInt("event_logNumberOfLines", 0)
        lineCountTextView.text = getString(R.string.number_of_events, numberOfLines)

        val permission = Manifest.permission.ACCESS_NETWORK_STATE
        if (ContextCompat.checkSelfPermission(
                requireContext().applicationContext,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), REQUEST_CODE)
        }

        val emailButton = view.findViewById<Button>(R.id.emailButton)
        emailButton.setOnClickListener {
            onEmailButtonClick()
        }
        emailButton.text = getString(R.string.email_event_log)

        val uploadButton = view.findViewById<Button>(R.id.uploadButton)
        uploadButton.setOnClickListener {
            onUploadButtonClick()
        }
        uploadButton.text = getString(R.string.upload_event_log)

        return view
    }

    private fun onUploadButtonClick() {
        val permission = Manifest.permission.INTERNET
        if (ContextCompat.checkSelfPermission(
                requireContext().applicationContext,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), REQUEST_CODE)
        }
        val sharedPreferences = requireContext().applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val fileUri = sharedPreferences.getString("event_logFilePath", null)?.let { File(it) }

        lifecycleScope.launch {
            val uploadTask = UploadFileTask()
            lateinit var uploadResult: String // Declare uploadResult outside the try-catch block

            try {
                uploadResult = uploadTask.uploadFile(fileUri)
                Log.d(TAG, "File upload result received: $uploadResult")
            } catch (e: Exception) {
                // Handle any exceptions that may occur during the upload
                uploadResult = "File upload failed with exception: ${e.message}"
                Log.e(TAG, uploadResult)
            }
            // Show a toast or handle the result here
            Toast.makeText(requireContext().applicationContext, uploadResult, Toast.LENGTH_LONG).show()
        }
    }

    private fun onEmailButtonClick() {
        val sharedPreferences = requireContext().applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val enrolledId = sharedPreferences.getString("enrolledId", null)

        val filePath = sharedPreferences.getString("eventLogFilePath", null)
        val fileUri = if (!filePath.isNullOrEmpty() && File(filePath).exists()) {
            Uri.fromFile(File(filePath))
        } else {
            null
        }

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${enrolledId} Event Log")
            putExtra(Intent.EXTRA_TEXT, "Here's the event log file for ${enrolledId}.")
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Create a chooser to allow the user to select an email app
        val chooserIntent = Intent.createChooser(emailIntent, getString(R.string.send_via_email))

        // Check if there are email apps available on the device
        if (requireContext().applicationContext.let { emailIntent.resolveActivity(it.packageManager) } != null) {
            startActivity(chooserIntent)
        } else {
            // Handle the case where no email app is available
            // You can show a Toast or alert the user
        }
    }

    override fun onResume() {
        super.onResume()
        // Retrieve the number of lines from SharedPreferences and update the TextView
        val sharedPreferences = requireContext().applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val numberOfLines = sharedPreferences.getInt("event_logNumberOfLines", 0)
        lineCountTextView.text = getString(R.string.number_of_events, numberOfLines)
    }
}
