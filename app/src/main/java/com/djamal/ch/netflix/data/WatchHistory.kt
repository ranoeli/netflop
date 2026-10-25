package com.djamal.ch.netflix.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Positions de lecture des films/épisodes, pour « Reprendre la lecture ». */
class WatchHistory(context: Context) {

    data class Entry(val request: PlayRequest, val position: Long, val duration: Long, val updatedAt: Long)

    private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)

    fun position(key: String): Long = load()[key]?.position ?: 0L

    fun recent(): List<Entry> = load().values.sortedByDescending { it.updatedAt }

    fun save(request: PlayRequest, position: Long, duration: Long) {
        if (duration <= 0) return
        val entries = load()
        when {
            position > duration * 0.95 -> entries.remove(request.key) // terminé
            position > 30_000 -> entries[request.key] = Entry(request, position, duration, System.currentTimeMillis())
            else -> return
        }
        val json = JSONArray()
        entries.values.sortedByDescending { it.updatedAt }.take(MAX_ENTRIES).forEach { e ->
            json.put(
                JSONObject()
                    .put("key", e.request.key)
                    .put("title", e.request.title)
                    .put("image", e.request.image.orEmpty())
                    .put("url", e.request.url)
                    .put("position", e.position)
                    .put("duration", e.duration)
                    .put("updatedAt", e.updatedAt)
            )
        }
        prefs.edit().putString(KEY, json.toString()).apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private fun load(): MutableMap<String, Entry> {
        val json = JSONArray(prefs.getString(KEY, "[]"))
        return (0 until json.length()).map { json.getJSONObject(it) }.associateTo(LinkedHashMap()) { o ->
            val request = PlayRequest(o.getString("key"), o.getString("title"), o.optString("image").ifEmpty { null }, o.getString("url"), live = false)
            request.key to Entry(request, o.getLong("position"), o.getLong("duration"), o.getLong("updatedAt"))
        }
    }

    private companion object {
        const val KEY = "entries"
        const val MAX_ENTRIES = 40
    }
}
