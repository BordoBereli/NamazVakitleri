package com.kutluoglu.app_update.data

interface UpdateConfigSource {
    suspend fun fetchAndActivate(): Boolean
    fun getLong(key: String): Long
    fun getString(key: String): String
}
