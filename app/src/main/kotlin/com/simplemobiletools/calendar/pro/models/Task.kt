package com.simplemobiletools.calendar.pro.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.commons.extensions.addBitIf
import java.io.Serializable

@Entity(tableName = "tasks", indices = [(Index(value = ["id"], unique = true))])
data class Task(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "start_ts") var startTS: Long = 0L,
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "description") var description: String = "",
    @ColumnInfo(name = "reminder_1_minutes") var reminder1Minutes: Int = REMINDER_OFF,
    @ColumnInfo(name = "reminder_2_minutes") var reminder2Minutes: Int = REMINDER_OFF,
    @ColumnInfo(name = "reminder_3_minutes") var reminder3Minutes: Int = REMINDER_OFF,
    @ColumnInfo(name = "repeat_interval") var repeatInterval: Int = 0,
    @ColumnInfo(name = "repeat_rule") var repeatRule: Int = 0,
    @ColumnInfo(name = "repeat_limit") var repeatLimit: Long = 0L,
    @ColumnInfo(name = "repetition_exceptions") var repetitionExceptions: ArrayList<String> = ArrayList(),
    @ColumnInfo(name = "import_id") var importId: String = "",
    @ColumnInfo(name = "time_zone") var timeZone: String = "",
    @ColumnInfo(name = "flags") var flags: Int = 0,
    @ColumnInfo(name = "event_type") var eventType: Long = REGULAR_EVENT_TYPE_ID,
    @ColumnInfo(name = "last_updated") var lastUpdated: Long = 0L,
    @ColumnInfo(name = "source") var source: String = SOURCE_SIMPLE_CALENDAR,
    @ColumnInfo(name = "color") var color: Int = 0
) : Serializable {

    companion object {
        private const val serialVersionUID = -32456795132345616L
    }

    fun getIsAllDay() = flags and FLAG_ALL_DAY != 0

    // properly return the start time of all-day tasks as midnight
    fun getTaskStartTS(): Long {
        return if (getIsAllDay()) {
            Formatter.getDateTimeFromTS(startTS).withTime(0, 0, 0, 0).seconds()
        } else {
            startTS
        }
    }

    var isPastTask: Boolean
        get() = flags and FLAG_IS_IN_PAST != 0
        set(isPastTask) {
            flags = flags.addBitIf(isPastTask, FLAG_IS_IN_PAST)
        }
}
