package com.djamal.ch.netflix

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.djamal.ch.netflix.data.Catalog
import com.djamal.ch.netflix.data.DemoSource
import com.djamal.ch.netflix.data.Details
import com.djamal.ch.netflix.data.Episode
import com.djamal.ch.netflix.data.IptvSource
import com.djamal.ch.netflix.data.Kind
import com.djamal.ch.netflix.data.Media
import com.djamal.ch.netflix.data.PlayRequest
import com.djamal.ch.netflix.data.WatchHistory
import com.djamal.ch.netflix.data.XtreamSource
import com.djamal.ch.netflix.ui.Tab
import kotlinx.coroutines.launch

sealed interface LoadState {
    data object Loading : LoadState
    data class Ready(val catalog: Catalog) : LoadState
    data class Error(val message: String) : LoadState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("account", Context.MODE_PRIVATE)
    private val detailsCache = HashMap<String, Details>()

    val history = WatchHistory(app)

    var source: IptvSource? by mutableStateOf(restoreSource())
        private set
    var state: LoadState by mutableStateOf(LoadState.Loading)
        private set
    var tab by mutableStateOf(Tab.HOME)
        private set

    /** Incrémenté à chaque sauvegarde de position, pour rafraîchir « Reprendre la lecture ». */
    var historyVersion by mutableIntStateOf(0)
        private set

    /** Dernier élément focalisé (clé + média), pour retrouver sa place en revenant d'une fiche. */
    var lastFocus: Pair<String, Media>? = null

    init {
        load()
    }

    fun login(server: String, user: String, pass: String, onResult: (error: String?) -> Unit) {
        viewModelScope.launch {
            val xtream = XtreamSource(server, user, pass)
            val result = runCatching { xtream.login() }
            when {
                result.isFailure -> onResult("Serveur injoignable : ${result.exceptionOrNull()?.message}")
                result.getOrDefault(false).not() -> onResult("Identifiants refusés par le serveur")
                else -> {
                    prefs.edit().clear().putString("server", server).putString("user", user).putString("pass", pass).apply()
                    source = xtream
                    load()
                    onResult(null)
                }
            }
        }
    }

    fun startDemo() {
        prefs.edit().clear().putBoolean("demo", true).apply()
        source = DemoSource
        load()
    }

    fun logout() {
        prefs.edit().clear().apply()
        history.clear()
        detailsCache.clear()
        lastFocus = null
        tab = Tab.HOME
        source = null
    }

    fun load() {
        val src = source ?: return
        state = LoadState.Loading
        viewModelScope.launch {
            state = runCatching { src.catalog() }.fold(
                onSuccess = { LoadState.Ready(it) },
                onFailure = { LoadState.Error(it.message ?: it.toString()) },
            )
        }
    }

    fun selectTab(newTab: Tab) {
        if (tab == newTab) return
        tab = newTab
        lastFocus = null
    }

    suspend fun details(media: Media): Details? {
        if (media.kind == Kind.LIVE || media.resume != null) return null
        val key = "${media.kind}:${media.id}"
        detailsCache[key]?.let { return it }
        return runCatching { source?.details(media) }.getOrNull()?.also { detailsCache[key] = it }
    }

    fun playRequest(media: Media, details: Details?) = PlayRequest(
        key = "${media.kind.name.lowercase()}:${media.id}",
        title = media.name,
        image = media.image,
        url = source!!.streamUrl(media.kind, media.id, details?.ext ?: media.ext),
        live = media.kind == Kind.LIVE,
    )

    fun playEpisode(series: Media, episode: Episode) = PlayRequest(
        key = episodeKey(episode),
        title = "${series.name} · S${episode.season}:E${episode.number}",
        image = episode.image ?: series.image,
        url = source!!.streamUrl(Kind.SERIES, episode.id, episode.ext),
        live = false,
    )

    fun episodeKey(episode: Episode) = "episode:${episode.id}"

    fun saveProgress(request: PlayRequest, position: Long, duration: Long) {
        history.save(request, position, duration)
        historyVersion++
    }

    private fun restoreSource(): IptvSource? {
        if (prefs.getBoolean("demo", false)) return DemoSource
        val server = prefs.getString("server", null) ?: return null
        return XtreamSource(server, prefs.getString("user", "")!!, prefs.getString("pass", "")!!)
    }
}
