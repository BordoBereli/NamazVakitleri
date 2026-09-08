package com.kutluoglu.prayer_notifications.push

data class PushMessage(
    val title: String,
    val body: String,
    val type: String = "announcement",
    val channel: String = "announcements",
    val screen: String? = null
)