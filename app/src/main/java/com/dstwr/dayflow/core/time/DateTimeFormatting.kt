package com.dstwr.dayflow.core.time

import java.text.DateFormat
import java.util.Date

object DateTimeFormatting {
    fun dateTime(timestamp: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
    fun date(timestamp: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
}
