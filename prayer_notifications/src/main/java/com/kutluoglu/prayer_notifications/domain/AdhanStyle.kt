package com.kutluoglu.prayer_notifications.domain

enum class AdhanStyle(val id: String) {
    DEFAULT("default");

    companion object {
        fun fromId(id: String?): AdhanStyle = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
