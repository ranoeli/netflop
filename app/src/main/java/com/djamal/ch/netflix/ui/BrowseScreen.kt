package com.djamal.ch.netflix.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.djamal.ch.netflix.AppViewModel
import com.djamal.ch.netflix.data.Details
import com.djamal.ch.netflix.data.Media
import com.djamal.ch.netflix.ui.theme.TextGrey
import kotlinx.coroutines.delay

data class Shelf(val title: String, val items: List<Media>)

/** Écran façon Netflix : grande bannière du titre sélectionné + rangées horizontales. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowseScreen(vm: AppViewModel, shelves: List<Shelf>, onOpen: (Media) -> Unit) {
    if (shelves.isEmpty()) {
        CenteredMessage("Rien à afficher ici", "Votre fournisseur ne propose pas de contenu dans cette section.")
        return
    }
    val first = shelves[0].items[0]
    // Au retour d'une fiche, on redonne le focus à la dernière affiche sélectionnée
    var pendingFocus by remember { mutableStateOf<String?>(vm.lastFocus?.first ?: focusKey(0, first)) }
    var hero by remember { mutableStateOf(vm.lastFocus?.second ?: first) }
    val details by produceState<Details?>(null, hero) {
        value = null
        delay(400) // on attend que l'utilisateur s'arrête sur une affiche avant d'appeler l'API
        value = vm.details(hero)
    }

    // La rangée focalisée remonte en haut (sous la bannière) au lieu de défiler au minimum
    val defaultSpec = LocalBringIntoViewSpec.current
    val titleSpace = with(LocalDensity.current) { 44.dp.toPx() }
    val rowOnTop = remember(titleSpace) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) = offset - titleSpace
        }
    }

    Box(Modifier.fillMaxSize()) {
        Backdrop(
            details?.backdrop ?: hero.image,
            Modifier.align(Alignment.TopEnd).fillMaxWidth(0.68f).fillMaxHeight(0.62f),
        )
        Column(Modifier.fillMaxSize()) {
            HeroInfo(hero, details, Modifier.height(200.dp).fillMaxWidth(0.55f).padding(start = ContentStart, top = 32.dp))
            CompositionLocalProvider(LocalBringIntoViewSpec provides rowOnTop) {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 300.dp)) {
                    itemsIndexed(shelves) { row, shelf ->
                        CompositionLocalProvider(LocalBringIntoViewSpec provides defaultSpec) {
                            ShelfRow(
                                row = row,
                                shelf = shelf,
                                pendingFocus = pendingFocus,
                                onFocusRestored = { pendingFocus = null },
                                onFocus = { key, media ->
                                    hero = media
                                    vm.lastFocus = key to media
                                },
                                onOpen = onOpen,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ShelfRow(
    row: Int,
    shelf: Shelf,
    pendingFocus: String?,
    onFocusRestored: () -> Unit,
    onFocus: (String, Media) -> Unit,
    onOpen: (Media) -> Unit,
) {
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            shelf.title,
            Modifier.padding(start = ContentStart),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(
            // En revenant sur une rangée, on retrouve l'affiche où on était
            Modifier.focusRestorer(),
            contentPadding = PaddingValues(start = ContentStart, end = 48.dp, top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(shelf.items) { media ->
                val key = focusKey(row, media)
                val requester = remember { FocusRequester() }
                if (key == pendingFocus) {
                    LaunchedEffect(Unit) {
                        runCatching { requester.requestFocus() }
                        onFocusRestored()
                    }
                }
                PosterCard(
                    media,
                    onClick = { onOpen(media) },
                    onFocus = { onFocus(key, media) },
                    modifier = Modifier.focusRequester(requester),
                )
            }
        }
    }
}

@Composable
private fun HeroInfo(media: Media, details: Details?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            media.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        MetaLine(media, details)
        (details?.plot ?: media.plot)?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = TextGrey, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun focusKey(row: Int, media: Media) = "$row/${media.kind}/${media.id}"
