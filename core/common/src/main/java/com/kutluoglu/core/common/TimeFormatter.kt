package com.kutluoglu.core.common

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

/**
 * Created by F.K. on 22.10.2025.
 *
 */

val formatter = SimpleDateFormat("hh:mm").also {
    TimeZone.getTimeZone(ZoneId.systemDefault())
}
