package dev.nora.pdfdrawr.helpers

import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// takes size in bytes and returns appropiate conversion depending on size
fun convertFileSize(sizeString: String?): String {
    if (sizeString == null) return ""

    var size: Long
    try {
        size = sizeString.toLong()

    }
    catch (e: Exception) {
        return ""
    }

    var s = ""
    val kilo = 1024L
    val mega = kilo * kilo
    val giga = mega * kilo
    val tera = giga * kilo
    val kb: Double = size.toDouble() / kilo
    val mb: Double = kb / kilo
    val gb: Double = mb / kilo
    val tb: Double = gb / kilo

    s = if (size < kilo) "$size Bytes"
    else if (size < mega) String.format(Locale.GERMANY,"%.2f", kb) + " KB"
    else if (size < giga) String.format(Locale.GERMANY,"%.2f", mb) + " MB"
    else if (size < tera) String.format(Locale.GERMANY,"%.2f", gb) + " GB"
    else String.format(Locale.GERMANY,"%.3f", tb) + " TB"

    return s
}

fun trimModifiedDate(dateString: String?): String {
    if(dateString == null) return ""
    val parsedDate: ZonedDateTime = ZonedDateTime.parse(
        dateString,
        DateTimeFormatter.RFC_1123_DATE_TIME
    )
    val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", Locale.GERMANY)
    return parsedDate.format(displayFormatter).toString()
}