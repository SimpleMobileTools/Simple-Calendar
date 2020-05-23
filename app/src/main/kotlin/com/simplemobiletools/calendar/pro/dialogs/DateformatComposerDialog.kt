package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.helpers.Config
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.views.MyEditText
import com.simplemobiletools.commons.views.MyTextView
import kotlinx.android.synthetic.main.dialog_dateformat_composer.view.*
import org.joda.time.DateTime

class DateformatComposerDialog(val activity: Activity, val customDateformat: String, val defaultFormat: String, val callback: (newValue: Any) -> Unit) {
    private var dialog: AlertDialog
    private val input: MyEditText
    private val example: MyTextView
    private val today: DateTime
    private val formatErrorMessage: String
    private var formatString: String
    lateinit var mConfig: Config

//    https://developer.android.com/reference/java/time/format/DateTimeFormatter
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_dateformat_composer, null).apply {
            input = datecomposer_input
            example = datecomposer_example
            today = Formatter.getLocalDateTimeFromCode(Formatter.getTodayCode())
            formatErrorMessage = context!!.getString(R.string.format_string_error)
            formatString = customDateformat

            datecomposer_example.text = today.toString(customDateformat)
            datecomposer_input.setText(customDateformat)

            datecomposer_day_in_month.text = today.toString("dd")
            datecomposer_day_name_short.text= today.toString("EE")
            datecomposer_day_name_long.text = today.toString("EEEE")

            datecomposer_week_number_in_year.text = today.toString("w")
            val week_name = context!!.getString(R.string.week) + " " + today.toString("w")
            datecomposer_week_label.text = week_name

            datecomposer_month_of_year.text = today.toString("M")
            datecomposer_month_name_short.text = today.toString("MMM")
            datecomposer_month_name_long.text = today.toString("MMMM")

            datecomposer_year_short.text = today.toString("YY")
            datecomposer_year_long.text = today.toString("YYYY")

            datecomposer_day_in_month.setOnClickListener {
                appendToInput("dd")
            }
            datecomposer_day_name_short.setOnClickListener {
                appendToInput("EE")
            }
            datecomposer_day_name_long.setOnClickListener {
                appendToInput("EEEE")
            }
            datecomposer_week_number_in_year.setOnClickListener {
                appendToInput("w")
            }
            datecomposer_week_label.setOnClickListener {
                val week_label = "'" + context!!.getString(R.string.week) + "' " + today.toString("w")
                appendToInput(week_label)
            }
            datecomposer_month_of_year.setOnClickListener {
                appendToInput("M")
            }
            datecomposer_month_name_short.setOnClickListener {
                appendToInput("MM")
            }
            datecomposer_month_name_long.setOnClickListener {
                appendToInput("MMMM")
            }
            datecomposer_year_short.setOnClickListener {
                appendToInput("YY")
            }
            datecomposer_year_long.setOnClickListener {
                appendToInput("YYYY")
            }
            datecomposer_dot.setOnClickListener {
                appendToInput(".")
            }
            datecomposer_comma.setOnClickListener {
                appendToInput(",")
            }
            datecomposer_slash.setOnClickListener {
                appendToInput("/")
            }
            datecomposer_backslah.setOnClickListener {
                appendToInput("\\")
            }
            datecomposer_par_open.setOnClickListener {
                appendToInput("(")
            }
            datecomposer_par_close.setOnClickListener {
                appendToInput(")")
            }
            datecomposer_bracket_open.setOnClickListener {
                appendToInput("[")
            }
            datecomposer_bracket_close.setOnClickListener {
                appendToInput("]")
            }
            datecomposer_newline.setOnClickListener {
                appendToInput("\n")
            }
            datecomposer_space.setOnClickListener {
                appendToInput(" ")
            }
            datecomposer_default.setOnClickListener {
                datecomposer_input.setText("")
                appendToInput(defaultFormat)
            }
            datecomposer_clear.setOnClickListener {
                datecomposer_input.setText("")
                datecomposer_example.text = ""
            }

            datecomposer_input.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val newFormat = input.text.toString()
                    try {
                        example.text = today.toString(newFormat)
                        formatString = newFormat
                    }
                    catch (e: Exception) {
                        example.text = formatErrorMessage
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }
            })
        }

        dialog = AlertDialog.Builder(activity)
                    .setPositiveButton(R.string.ok) { dialog, which -> getFormat(formatString) }
                    .setNegativeButton(R.string.cancel, null)
                    .create().apply {
                        activity.setupDialogStuff(view, this)
                    }
    }

    private fun appendToInput(append: String)
    {
        val newFormat = input.text.toString() + append
        input.setText(newFormat)
        try {
            example.text = today.toString(newFormat)
            formatString = newFormat
        }
        catch (e: Exception) {
            example.text = formatErrorMessage
        }
    }

    private fun getFormat(fmt: String) {
        callback(fmt)
        dialog.dismiss()
    }
}
