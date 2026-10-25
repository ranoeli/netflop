package com.djamal.ch.netflix.data

import android.util.JsonReader
import android.util.Log
import android.util.JsonToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.Reader
import java.util.concurrent.TimeUnit

/** Client de l'API Xtream Codes (player_api.php), celle qu'utilisent TiviMate & co. */
class XtreamSource(server: String, private val user: String, private val pass: String) : IptvSource {

    private val base = server.trim().trimEnd('/').let { if (it.startsWith("http")) it else "http://$it" }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun login(): Boolean = get(apiUrl(null)) { reader ->
        JSONObject(reader.readText()).optJSONObject("user_info")?.optInt("auth") == 1
    }

    override suspend fun catalog(): Catalog = coroutineScope {
        val liveCats = async { categories("get_live_categories") }
        val movieCats = async { categories("get_vod_categories") }
        val seriesCats = async { categories("get_series_categories") }
        val live = async {
            list("get_live_streams", setOf("stream_id", "name", "stream_icon", "category_id")) {
                Media(Kind.LIVE, it["stream_id"] ?: return@list null, it["name"].orEmpty(), it["stream_icon"].orBlank(), it["category_id"])
            }
        }
        val movies = async {
            list("get_vod_streams", setOf("stream_id", "name", "stream_icon", "category_id", "rating", "container_extension", "added")) {
                Media(
                    Kind.MOVIE, it["stream_id"] ?: return@list null, it["name"].orEmpty(), it["stream_icon"].orBlank(), it["category_id"],
                    rating = it["rating"], ext = it["container_extension"], added = it["added"]?.toLongOrNull() ?: 0,
                )
            }
        }
        val series = async {
            list("get_series", setOf("series_id", "name", "cover", "category_id", "rating", "plot", "last_modified")) {
                Media(
                    Kind.SERIES, it["series_id"] ?: return@list null, it["name"].orEmpty(), it["cover"].orBlank(), it["category_id"],
                    rating = it["rating"], plot = it["plot"].orBlank(), added = it["last_modified"]?.toLongOrNull() ?: 0,
                )
            }
        }
        Catalog(liveCats.await(), live.await(), movieCats.await(), movies.await(), seriesCats.await(), series.await())
    }

    override suspend fun details(media: Media): Details = when (media.kind) {
        Kind.MOVIE -> get(apiUrl("get_vod_info", "vod_id" to media.id)) { reader ->
            val root = JSONObject(reader.readText())
            val ext = root.optJSONObject("movie_data")?.str("container_extension") ?: media.ext
            (root.optJSONObject("info") ?: JSONObject()).toDetails(ext)
        }
        Kind.SERIES -> get(apiUrl("get_series_info", "series_id" to media.id)) { reader ->
            val root = JSONObject(reader.readText())
            (root.optJSONObject("info") ?: JSONObject()).toDetails(null, parseEpisodes(root.opt("episodes")))
        }
        Kind.LIVE -> Details()
    }

    override fun streamUrl(kind: Kind, id: String, ext: String?): String = when (kind) {
        Kind.LIVE -> "$base/live/$user/$pass/$id.ts"
        Kind.MOVIE -> "$base/movie/$user/$pass/$id.${ext ?: "mp4"}"
        Kind.SERIES -> "$base/series/$user/$pass/$id.${ext ?: "mp4"}"
    }

    private suspend fun categories(action: String) = list(action, setOf("category_id", "category_name")) {
        Category(it["category_id"] ?: return@list null, it["category_name"].orEmpty())
    }

    private suspend fun <T> list(action: String, keep: Set<String>, map: (Map<String, String>) -> T?): List<T> =
        get(apiUrl(action)) { reader -> readObjects(reader, keep).mapNotNull(map) }

    private fun apiUrl(action: String?, vararg params: Pair<String, String>): HttpUrl =
        "$base/player_api.php".toHttpUrl().newBuilder()
            .addQueryParameter("username", user)
            .addQueryParameter("password", pass)
            .apply {
                action?.let { addQueryParameter("action", it) }
                params.forEach { (k, v) -> addQueryParameter(k, v) }
            }
            .build()

    private suspend fun <T> get(url: HttpUrl, parse: (Reader) -> T): T = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // Le corps de la réponse explique souvent le refus (compte expiré, IP bloquée…)
                val body = response.body?.string().orEmpty()
                Log.w("Xtream", "HTTP ${response.code} ${response.protocol}\nrequête: ${request.headers}\nréponse: ${response.headers}\n${body.take(500)}")
                val reason = body.replace(Regex("<[^>]*>"), " ").trim().take(200)
                throw IOException("HTTP ${response.code} ${response.message} $reason".trim())
            }
            parse(response.body!!.charStream())
        }
    }
}

/**
 * Lecture en streaming d'un tableau d'objets JSON : les listes VOD peuvent faire des dizaines de Mo,
 * on ne garde que les champs utiles. Les serveurs mélangent nombres et chaînes, tout est lu en String.
 */
private fun readObjects(source: Reader, keep: Set<String>): List<Map<String, String>> {
    val out = ArrayList<Map<String, String>>()
    JsonReader(source).use { reader ->
        reader.isLenient = true
        // Certains serveurs renvoient {} ou null au lieu d'un tableau vide
        if (reader.peek() != JsonToken.BEGIN_ARRAY) return out
        reader.beginArray()
        while (reader.hasNext()) {
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                reader.skipValue()
                continue
            }
            val fields = HashMap<String, String>(keep.size)
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                when {
                    name !in keep -> reader.skipValue()
                    reader.peek() == JsonToken.STRING || reader.peek() == JsonToken.NUMBER -> fields[name] = reader.nextString()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            out += fields
        }
        reader.endArray()
    }
    return out
}

private fun parseEpisodes(node: Any?): List<Episode> {
    // "episodes" est soit {"1": [...], "2": [...]}, soit [[...], [...]] selon les serveurs
    val seasons = when (node) {
        is JSONObject -> node.keys().asSequence().mapNotNull { node.optJSONArray(it) }.toList()
        is JSONArray -> (0 until node.length()).mapNotNull { node.optJSONArray(it) }
        else -> emptyList()
    }
    return seasons
        .flatMap { season -> (0 until season.length()).mapNotNull { season.optJSONObject(it) } }
        .map { e ->
            val info = e.optJSONObject("info")
            Episode(
                id = e.optString("id"),
                season = e.optInt("season", 1),
                number = e.optInt("episode_num"),
                title = e.str("title") ?: "Épisode ${e.optInt("episode_num")}",
                image = info?.str("movie_image"),
                plot = info?.str("plot"),
                ext = e.str("container_extension"),
            )
        }
        .sortedWith(compareBy({ it.season }, { it.number }))
}

private fun JSONObject.toDetails(ext: String?, episodes: List<Episode> = emptyList()) = Details(
    plot = str("plot") ?: str("description"),
    backdrop = when (val b = opt("backdrop_path")) {
        is JSONArray -> b.optString(0).ifBlank { null }
        is String -> b.ifBlank { null }
        else -> null
    },
    genre = str("genre"),
    year = (str("releasedate") ?: str("releaseDate") ?: str("release_date"))?.take(4),
    duration = str("duration"),
    cast = str("cast") ?: str("actors"),
    rating = str("rating"),
    ext = ext,
    episodes = episodes,
)

private fun JSONObject.str(key: String) = optString(key).takeIf { it.isNotBlank() && it != "null" }

private fun String?.orBlank() = this?.takeIf { it.isNotBlank() }
