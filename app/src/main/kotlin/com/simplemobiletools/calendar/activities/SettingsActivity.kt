package com.simplemobiletools.calendar.activities

import android.Manifest
import android.accounts.AccountManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.CalendarScopes
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.asynctasks.FetchGoogleEventsTask
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.getDefaultReminderTypeIndex
import com.simplemobiletools.calendar.extensions.getDefaultReminderValue
import com.simplemobiletools.calendar.extensions.setupReminderPeriod
import com.simplemobiletools.calendar.helpers.DAY_MINS
import com.simplemobiletools.calendar.helpers.HOUR_MINS
import com.simplemobiletools.calendar.helpers.REMINDER_CUSTOM
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : SimpleActivity() {
    private val GET_RINGTONE_URI = 1
    private val ACCOUNTS_PERMISSION = 2
    private val REQUEST_ACCOUNT_NAME = 3
    private val REQUEST_GOOGLE_PLAY_SERVICES = 4

    companion object {
        val REQUEST_AUTHORIZATION = 5
    }

    lateinit var credential: GoogleAccountCredential

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        credential = GoogleAccountCredential.usingOAuth2(this, arrayListOf(CalendarScopes.CALENDAR_READONLY)).setBackOff(ExponentialBackOff())
    }

    override fun onResume() {
        super.onResume()

        setupCustomizeColors()
        setupSundayFirst()
        setupGoogleSync()
        setupWeeklyStart()
        setupWeeklyEnd()
        setupWeekNumbers()
        setupVibrate()
        setupReminderSound()
        setupEventReminder()
        updateTextColors(settings_holder)
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupGoogleSync() {
        settings_google_sync.isChecked = config.googleSync
        settings_google_sync_holder.setOnClickListener {
            settings_google_sync.toggle()
            config.googleSync = settings_google_sync.isChecked

            if (settings_google_sync.isChecked) {
                tryEnablingSync()
            }
        }
    }

    private fun setupSundayFirst() {
        settings_sunday_first.isChecked = config.isSundayFirst
        settings_sunday_first_holder.setOnClickListener {
            settings_sunday_first.toggle()
            config.isSundayFirst = settings_sunday_first.isChecked
        }
    }

    private fun setupWeeklyStart() {
        settings_start_weekly_at.apply {
            adapter = getWeeklyAdapter()
            setSelection(config.startWeeklyAt)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (selectedItemPosition >= config.endWeeklyAt) {
                        toast(R.string.day_end_before_start)
                        setSelection(config.startWeeklyAt)
                    } else {
                        config.startWeeklyAt = selectedItemPosition
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
    }

    private fun setupWeeklyEnd() {
        settings_end_weekly_at.apply {
            adapter = getWeeklyAdapter()
            setSelection(config.endWeeklyAt)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (selectedItemPosition <= config.startWeeklyAt) {
                        toast(R.string.day_end_before_start)
                        setSelection(config.endWeeklyAt)
                    } else {
                        config.endWeeklyAt = selectedItemPosition
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
    }

    private fun setupWeekNumbers() {
        settings_week_numbers.isChecked = config.displayWeekNumbers
        settings_week_numbers_holder.setOnClickListener {
            settings_week_numbers.toggle()
            config.displayWeekNumbers = settings_week_numbers.isChecked
        }
    }

    private fun setupReminderSound() {
        val noRingtone = resources.getString(R.string.no_ringtone_selected)
        if (config.reminderSound.isEmpty()) {
            settings_reminder_sound.text = noRingtone
        } else {
            settings_reminder_sound.text = RingtoneManager.getRingtone(this, Uri.parse(config.reminderSound))?.getTitle(this) ?: noRingtone
        }
        settings_reminder_sound_holder.setOnClickListener {
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, resources.getString(R.string.notification_sound))
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(config.reminderSound))

                if (resolveActivity(packageManager) != null)
                    startActivityForResult(this, GET_RINGTONE_URI)
                else {
                    toast(R.string.no_ringtone_picker)
                }
            }
        }
    }

    private fun setupVibrate() {
        settings_vibrate.isChecked = config.vibrateOnReminder
        settings_vibrate_holder.setOnClickListener {
            settings_vibrate.toggle()
            config.vibrateOnReminder = settings_vibrate.isChecked
        }
    }

    private fun setupEventReminder() {
        val reminderType = config.defaultReminderType
        settings_default_reminder.setSelection(getDefaultReminderTypeIndex())
        custom_reminder_save.setTextColor(custom_reminder_other_val.currentTextColor)
        setupReminderPeriod(custom_reminder_other_period, custom_reminder_value)

        settings_custom_reminder_holder.beVisibleIf(reminderType == REMINDER_CUSTOM)
        custom_reminder_save.setOnClickListener { saveReminder() }

        settings_default_reminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, itemIndex: Int, p3: Long) {
                if (itemIndex == 2) {
                    settings_custom_reminder_holder.visibility = View.VISIBLE
                    showKeyboard(custom_reminder_value)
                } else {
                    hideKeyboard()
                    settings_custom_reminder_holder.visibility = View.GONE
                }

                config.defaultReminderType = getDefaultReminderValue(itemIndex)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }

    private fun saveReminder() {
        val multiplier = when (custom_reminder_other_period.selectedItemPosition) {
            1 -> HOUR_MINS
            2 -> DAY_MINS
            else -> 1
        }

        val value = custom_reminder_value.value
        config.defaultReminderMinutes = Integer.valueOf(if (value.isEmpty()) "0" else value) * multiplier
        config.defaultReminderType = REMINDER_CUSTOM
        toast(R.string.reminder_saved)
        hideKeyboard()
    }

    private fun getWeeklyAdapter(): ArrayAdapter<String> {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)
        for (i in 0..24) {
            adapter.add("$i:00")
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == GET_RINGTONE_URI) {
                val uri = resultData?.getParcelableExtra<Parcelable>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (uri == null) {
                    config.reminderSound = ""
                } else {
                    settings_reminder_sound.text = RingtoneManager.getRingtone(this, uri as Uri).getTitle(this)
                    config.reminderSound = uri.toString()
                }
            } else if (requestCode == REQUEST_ACCOUNT_NAME && resultData?.extras != null) {
                val accountName = resultData!!.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                config.syncAccountName = accountName
                tryEnablingSync()
            } else if (requestCode == REQUEST_AUTHORIZATION) {
                tryEnablingSync()
            }
        }
    }

    private fun tryEnablingSync() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices()
        } else if (!hasGetAccountsPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.GET_ACCOUNTS), ACCOUNTS_PERMISSION)
        } else if (config.syncAccountName.isEmpty()) {
            showAccountChooser()
        } else {
            credential.selectedAccountName = config.syncAccountName
            FetchGoogleEventsTask(this, credential).execute()
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    private fun acquireGooglePlayServices() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES).show()
        }
    }

    private fun hasGetAccountsPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == ACCOUNTS_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showAccountChooser()
            }
        }
    }

    private fun showAccountChooser() {
        if (config.syncAccountName.isEmpty()) {
            startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_NAME)
        }
    }
}
