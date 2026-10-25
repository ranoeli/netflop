package com.djamal.ch.netflix.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.djamal.ch.netflix.data.Catalog
import com.djamal.ch.netflix.data.Media
import com.djamal.ch.netflix.ui.theme.TextGrey
import java.text.Normalizer

@Composable
fun SearchScreen(catalog: Catalog, onOpen: (Media) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val index = remember(catalog) { (catalog.movies + catalog.series + catalog.live).map { it to it.name.normalized() } }
    val q = query.trim().normalized()
    val results = remember(q, index) {
        if (q.length < 2) emptyList() else index.asSequence().filter { it.second.contains(q) }.map { it.first }.take(150).toList()
    }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().padding(start = ContentStart, top = 32.dp, end = 48.dp)) {
        TvTextField(
            query, { query = it }, "Films, séries, chaînes…", Modifier.fillMaxWidth(0.6f),
            imeAction = ImeAction.Search,
            onImeAction = {
                keyboard?.hide()
                focus.moveFocus(FocusDirection.Down)
            },
        )
        when {
            q.length < 2 -> Text("Tapez au moins 2 lettres", Modifier.padding(top = 16.dp), color = TextGrey)
            results.isEmpty() -> Text("Aucun résultat pour « $query »", Modifier.padding(top = 16.dp), color = TextGrey)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(124.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(results) { media -> PosterCard(media, onClick = { onOpen(media) }) }
        }
    }
}

private val DIACRITICS = Regex("\\p{Mn}+")

/** "Épisode" → "episode", pour chercher sans se soucier des accents. */
private fun String.normalized() = Normalizer.normalize(this, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase()
