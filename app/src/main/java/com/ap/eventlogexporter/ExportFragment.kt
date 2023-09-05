package com.uza.eventlogexporter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.FileProvider
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
        val context = requireContext()

        val sharedPreferences =
            requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val participantId = sharedPreferences.getString("participantId", null)
        val file = sharedPreferences.getString("event_logFilePath", null)?.let { File(it) }

        val uri = file?.let {
            FileProvider.getUriForFile(context, "com.uza.eventlogexporter.fileprovider",
                it
            )
        }

        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "text/comma-separated-values"
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "${participantId} Event Log")
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Here's the event log file for ${participantId}.")
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Specify the email recipient (optional)
        // emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("recipient@example.com"))

        // Create a chooser to allow the user to select an email app
        val chooserIntent = Intent.createChooser(emailIntent, "Send via Email")
        // Suppress SecurityException by giving all possible packages read permission? (file transferred either way, exception is ineffective)
        val suppressSecurityException = true
        if (suppressSecurityException) {
            val resInfoList =
                context.packageManager.queryIntentActivities(
                    chooserIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
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
        val sharedPreferences = requireContext().applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val numberOfLines = sharedPreferences.getInt("event_logNumberOfLines", 0)
        lineCountTextView.text = getString(R.string.number_of_events, numberOfLines)
    }
}
