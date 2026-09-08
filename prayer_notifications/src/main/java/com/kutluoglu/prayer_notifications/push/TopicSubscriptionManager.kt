package com.kutluoglu.prayer_notifications.push

import com.kutluoglu.prayer.model.location.LocationData
import org.koin.core.annotation.Single
import java.util.Locale

/**
 * Keeps the device subscribed to the FCM topics relevant to the current
 * location: the global "announcements" topic plus a country and city topic.
 */
@Single
class TopicSubscriptionManager(
    private val client: FcmClient
) {

    private var locationTopics: Set<String> = emptySet()

    suspend fun registerGlobal() {
        client.subscribe(TOPIC_GLOBAL)
    }

    suspend fun syncForLocation(location: LocationData?) {
        val desired = setOfNotNull(countryTopic(location), cityTopic(location))
        (desired - locationTopics).forEach { client.subscribe(it) }
        (locationTopics - desired).forEach { client.unsubscribe(it) }
        locationTopics = desired
    }

    internal fun countryTopic(location: LocationData?): String? {
        val raw = location?.countryCode?.takeIf { it.isNotBlank() }
            ?: location?.country?.takeIf { it.isNotBlank() }
            ?: return null
        return TOPIC_NAMESPACE_COUNTRY + raw.normalizeTopicSegment()
    }

    internal fun cityTopic(location: LocationData?): String? {
        val raw = location?.city?.takeIf { it.isNotBlank() } ?: return null
        return TOPIC_NAMESPACE_CITY + raw.normalizeTopicSegment()
    }

    companion object {
        const val TOPIC_GLOBAL = "announcements"
        const val TOPIC_NAMESPACE_COUNTRY = "country_"
        const val TOPIC_NAMESPACE_CITY = "city_"

        private fun String.normalizeTopicSegment(): String {
            val normalized = lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9_-]"), "-")
                .trim('-')
            return normalized.ifEmpty { "unknown" }
        }
    }
}