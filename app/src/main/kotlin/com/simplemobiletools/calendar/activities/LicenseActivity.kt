package com.simplemobiletools.calendar.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.simplemobiletools.calendar.R
import kotlinx.android.synthetic.main.activity_license.*

class LicenseActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)

        license_kotlin_title.setOnClickListener { openUrl(R.string.kotlin_url) }
        license_ambilwarna_title.setOnClickListener { openUrl(R.string.ambilwarna_url) }
        license_joda_title.setOnClickListener { openUrl(R.string.joda_url) }
        license_stetho_title.setOnClickListener { openUrl(R.string.stetho_url) }
    }

    private fun openUrl(id: Int) {
        val url = resources.getString(id)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }
}
