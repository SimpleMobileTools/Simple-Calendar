package com.simplemobiletools.calendar.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.calendar.*
import com.simplemobiletools.calendar.Formatter
import com.simplemobiletools.calendar.adapters.MyMonthPagerAdapter
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

class MainActivity : SimpleActivity(), NavigationListener {
    private val PREFILLED_MONTHS = 73

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val today = DateTime().toString(Formatter.DAYCODE_PATTERN)
        fillViewPager(today)

        calendar_fab.setOnClickListener {
            val intent = Intent(applicationContext, EventActivity::class.java)
            val tomorrowCode = Formatter.getDayCodeFromDateTime(DateTime(DateTimeZone.getDefault()).plusDays(1))
            intent.putExtra(Constants.DAY_CODE, tomorrowCode)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Config.newInstance(applicationContext).isFirstRun = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
                return true
            }
            R.id.about -> {
                startActivity(Intent(applicationContext, AboutActivity::class.java))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun fillViewPager(targetMonth: String) {
        val codes = getMonths(targetMonth)
        val adapter = MyMonthPagerAdapter(supportFragmentManager, codes, this)
        view_pager.adapter = adapter
        view_pager.currentItem = codes.size / 2
    }

    private fun getMonths(code: String): List<String> {
        val months = ArrayList<String>(PREFILLED_MONTHS)
        val today = Formatter.getDateTimeFromCode(code)
        for (i in -PREFILLED_MONTHS / 2..PREFILLED_MONTHS / 2) {
            months.add(Formatter.getDayCodeFromDateTime(today.plusMonths(i)))
        }

        return months
    }

    override fun goLeft() {
        view_pager.currentItem = view_pager.currentItem - 1
    }

    override fun goRight() {
        view_pager.currentItem = view_pager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        fillViewPager(Formatter.getDayCodeFromDateTime(dateTime))
    }
}
