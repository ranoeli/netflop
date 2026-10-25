package com.djamal.ch.netflix.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.djamal.ch.netflix.AppViewModel
import com.djamal.ch.netflix.R
import com.djamal.ch.netflix.data.Details
import com.djamal.ch.netflix.data.Episode
import com.djamal.ch.netflix.data.Kind
import com.djamal.ch.netflix.data.Media
import com.djamal.ch.netflix.data.PlayRequest
import com.djamal.ch.netflix.ui.theme.SurfaceGrey
import com.djamal.ch.netflix.ui.theme.TextGrey

@Composable
fun DetailScreen(vm: AppViewModel, media: Media, onPlay: (PlayRequest) -> Unit) {
    val details by produceState<Details?>(null, media) { value = vm.details(media) }

    Box(Modifier.fillMaxSize()) {
        Backdrop(details?.backdrop ?: media.image, Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().padding(start = 56.dp, top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(media.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.6f))
            MetaLine(media, details)
            (details?.plot ?: media.plot)?.let {
                Text(it, color = TextGrey, maxLines = if (media.kind == Kind.SERIES) 3 else 5, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.55f))
            }
            details?.cast?.let {
                Text("Avec : $it", color = TextGrey, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.55f))
            }
            Spacer(Modifier.size(4.dp))
            when {
                media.kind != Kind.SERIES -> {
                    val request = vm.playRequest(media, details)
                    val resume = remember(vm.historyVersion) { vm.history.position(request.key) > 0 }
                    PlayButton(if (resume) "Reprendre" else "Lecture") { onPlay(request) }
                }
                details == null -> Text("Chargement des épisodes…", color = TextGrey)
                else -> Episodes(vm, media, details!!.episodes, onPlay)
            }
        }
    }
}

@Composable
private fun PlayButton(label: String, onClick: () -> Unit) {
    val requester = remember { FocusRequester() }
    Button(
        onClick = onClick,
        modifier = Modifier.focusRequester(requester),
        colors = ButtonDefaults.colors(
            containerColor = Color.White,
            contentColor = Color.Black,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White.copy(alpha = 0.5f)))),
    ) {
        Icon(painterResource(R.drawable.ic_play), contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
    LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }
}

@Composable
private fun Episodes(vm: AppViewModel, series: Media, episodes: List<Episode>, onPlay: (PlayRequest) -> Unit) {
    if (episodes.isEmpty()) {
        Text("Aucun épisode disponible", color = TextGrey)
        return
    }
    val seasons = remember(episodes) { episodes.map { it.season }.distinct() }
    val history = remember(vm.historyVersion) { vm.history.recent() }
    val progress = remember(history) { history.associate { it.request.key to it.position.toFloat() / it.duration } }
    // Dernier épisode commencé de cette série, sinon le tout premier
    val resumeEpisode = remember(history, episodes) {
        val keys = episodes.associateBy { vm.episodeKey(it) }
        history.firstNotNullOfOrNull { keys[it.request.key] }
    }
    val target = resumeEpisode ?: episodes.first()
    var season by rememberSaveable { mutableIntStateOf(target.season) }

    PlayButton("${if (resumeEpisode != null) "Reprendre" else "Lecture"} S${target.season}:E${target.number}") {
        onPlay(vm.playEpisode(series, target))
    }

    if (seasons.size > 1) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(seasons) { s ->
                Surface(
                    onClick = { season = s },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (s == season) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                        contentColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black,
                    ),
                ) {
                    Text("Saison $s", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
            }
        }
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(end = 48.dp, top = 6.dp, bottom = 24.dp),
    ) {
        items(episodes.filter { it.season == season }) { episode ->
            EpisodeCard(episode, series.image, progress[vm.episodeKey(episode)]) { onPlay(vm.playEpisode(series, episode)) }
        }
    }
}

@Composable
private fun EpisodeCard(episode: Episode, fallbackImage: String?, progress: Float?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Card(
        onClick = onClick,
        modifier = Modifier.width(210.dp),
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(containerColor = SurfaceGrey),
        scale = CardDefaults.scale(focusedScale = 1.06f),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = shape)),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            AsyncImage(episode.image ?: fallbackImage, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            progress?.let { ProgressBar(it, Modifier.align(Alignment.BottomStart)) }
        }
        Text(
            "${episode.number}. ${episode.title}",
            Modifier.padding(8.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
