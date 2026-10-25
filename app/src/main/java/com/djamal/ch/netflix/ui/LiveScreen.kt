package com.djamal.ch.netflix.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.djamal.ch.netflix.data.Catalog
import com.djamal.ch.netflix.data.Media
import com.djamal.ch.netflix.ui.theme.SurfaceGrey

/** TV en direct : catégories à gauche, chaînes en grille à droite. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LiveScreen(catalog: Catalog, onPlay: (Media) -> Unit) {
    val byCategory = remember(catalog) { catalog.live.groupBy { it.categoryId } }
    val categories = remember(catalog) { catalog.liveCategories.filter { byCategory[it.id] != null } }
    if (categories.isEmpty()) {
        CenteredMessage("Aucune chaîne", "Votre abonnement ne contient pas de TV en direct.")
        return
    }
    var selected by rememberSaveable { mutableStateOf(categories.first().id) }

    Column(Modifier.fillMaxSize().padding(start = ContentStart, top = 28.dp)) {
        Text("TV en direct", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxSize().padding(top = 12.dp)) {
            LazyColumn(
                Modifier.width(230.dp).focusRestorer(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(categories) { category ->
                    Surface(
                        onClick = { selected = category.id },
                        // Comme TiviMate : survoler une catégorie l'affiche tout de suite
                        modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) selected = category.id },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (selected == category.id) SurfaceGrey else Color.Transparent,
                            contentColor = Color.White,
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color.Black,
                        ),
                    ) {
                        Text(
                            category.name,
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(170.dp),
                modifier = Modifier.focusRestorer(),
                contentPadding = PaddingValues(start = 24.dp, end = 48.dp, top = 8.dp, bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(byCategory[selected].orEmpty()) { channel ->
                    ChannelCard(channel, onClick = { onPlay(channel) })
                }
            }
        }
    }
}
