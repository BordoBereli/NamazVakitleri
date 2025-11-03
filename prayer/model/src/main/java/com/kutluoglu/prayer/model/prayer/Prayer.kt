package com.kutluoglu.prayer.model.prayer

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime


data class Prayer (
    val name: String,
    val arabicName: String,
    val time: LocalTime,
    val date: LocalDate,
    val isCurrent: Boolean = false,
    val notificationEnabled: Boolean = false
)