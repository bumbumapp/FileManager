package com.securefilemanager.app.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.securefilemanager.app.R
import kotlinx.android.synthetic.main.activity_about.about_layout_source
import kotlinx.android.synthetic.main.activity_about.about_layout_license

// This activity is inspired by the andOTP - https://github.com/andOTP/andOTP
class About : BaseAbstractActivity() {
    companion object {
        private const val GITHUB_URI: String =
            "https://github.com/bumbumapp/FileManager"
        private const val LICENSE_URI: String = "https://github.com/Secure-File-Manager/Secure-File-Manager/blob/master/LICENSE"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(R.layout.activity_about)
        about_layout_license.setOnClickListener {
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(LICENSE_URI)
            )
            startActivity(urlIntent)
        }

        about_layout_source.setOnClickListener {
            val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URI))
            startActivity(urlIntent)
        }
    }

}
