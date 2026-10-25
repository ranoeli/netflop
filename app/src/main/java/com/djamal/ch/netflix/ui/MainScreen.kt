package com.djamal.ch.netflix.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.djamal.ch.netflix.AppViewModel
import com.djamal.ch.netflix.LoadState
import com.djamal.ch.netflix.R
import com.djamal.ch.netflix.data.Catalog
import com.djamal.ch.netflix.data.Category
import com.djamal.ch.netflix.data.Kind
import com.djamal.ch.netflix.data.Media
import com.djamal.ch.netflix.data.WatchHistory
import com.djamal.ch.netflix.ui.theme.NetflixBlack

enum class Tab(val label: String, @DrawableRes val icon: Int) {
    SEARCH("Recherche", R.drawable.ic_search),
    HOME("Accueil", R.drawable.ic_home),
    MOVIES("Films", R.drawable.ic_movie),
    SERIES("Séries", R.drawable.ic_series),
    LIVE("TV en direct", R.drawable.ic_live),
}

@Composable
fun MainScreen(vm: AppViewModel, onOpen: (Media) -> Unit) {
    when (val state = vm.state) {
        is LoadState.Loading -> CenteredMessage("Chargement du catalogue…", "La première fois, ça peut prendre quelques secondes.")
        is LoadState.Error -> CenteredMessage("Impossible de charger le catalogue", state.message) {
            val retry = remember { FocusRequester() }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = vm::load, modifier = Modifier.focusRequester(retry)) { Text("Réessayer") }
                OutlinedButton(onClick = vm::logout) { Text("Changer de compte") }
            }
            LaunchedEffect(Unit) { runCatching { retry.requestFocus() } }
        }
        is LoadState.Ready -> Shell(vm, state.catalog, onOpen)
    }
}

@Composable
private fun Shell(vm: AppViewModel, catalog: Catalog, onOpen: (Media) -> Unit) {
    ModalNavigationDrawer(
        drawerContent = { DrawerItems(vm) },
        scrimBrush = Brush.horizontalGradient(listOf(Color.Black, Color.Black.copy(alpha = 0.6f), Color.Transparent)),
    ) {
        Box(Modifier.fillMaxSize()) {
            // key() : chaque onglet garde son propre état (scroll, focus) au lieu de le partager
            key(vm.tab) {
                when (vm.tab) {
                    Tab.HOME -> {
                        val shelves = remember(catalog, vm.historyVersion) { homeShelves(catalog, vm.history.recent()) }
                        BrowseScreen(vm, shelves, onOpen)
                    }
                    Tab.MOVIES -> BrowseScreen(vm, remember(catalog) { categoryShelves(catalog.movieCategories, catalog.movies) }, onOpen)
                    Tab.SERIES -> BrowseScreen(vm, remember(catalog) { categoryShelves(catalog.seriesCategories, catalog.series) }, onOpen)
                    Tab.LIVE -> LiveScreen(catalog, onOpen)
                    Tab.SEARCH -> SearchScreen(catalog, onOpen)
                }
            }
            // Fondu sous le menu replié, pour que les affiches qui défilent ne gênent pas les icônes
            Box(
                Modifier.fillMaxHeight().width(ContentStart).background(
                    Brush.horizontalGradient(0f to NetflixBlack, 0.7f to NetflixBlack.copy(alpha = 0.85f), 1f to Color.Transparent)
                )
            )
        }
    }
}

@Composable
private fun NavigationDrawerScope.DrawerItems(vm: AppViewModel) {
    val colors = NavigationDrawerItemDefaults.colors(
        focusedContainerColor = Color.White,
        focusedContentColor = Color.Black,
        selectedContainerColor = Color.White.copy(alpha = 0.15f),
        selectedContentColor = Color.White,
        focusedSelectedContainerColor = Color.White,
        focusedSelectedContentColor = Color.Black,
    )
    Column(
        Modifier.fillMaxHeight().padding(12.dp).selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Tab.entries.forEach { tab ->
            NavigationDrawerItem(
                selected = vm.tab == tab,
                onClick = { vm.selectTab(tab) },
                leadingContent = { Icon(painterResource(tab.icon), contentDescription = null) },
                colors = colors,
            ) { Text(tab.label) }
        }
        NavigationDrawerItem(
            selected = false,
            onClick = vm::logout,
            leadingContent = { Icon(painterResource(R.drawable.ic_logout), contentDescription = null) },
            colors = colors,
        ) { Text("Déconnexion") }
    }
}

private fun homeShelves(catalog: Catalog, history: List<WatchHistory.Entry>): List<Shelf> = buildList {
    if (history.isNotEmpty()) {
        add(Shelf("Reprendre la lecture", history.map { e ->
            Media(Kind.MOVIE, e.request.key, e.request.title, e.request.image, null, progress = e.position.toFloat() / e.duration, resume = e.request)
        }))
    }
    catalog.movies.sortedByDescending { it.added }.take(40).takeIf { it.isNotEmpty() }?.let { add(Shelf("Films récemment ajoutés", it)) }
    catalog.series.sortedByDescending { it.added }.take(40).takeIf { it.isNotEmpty() }?.let { add(Shelf("Séries mises à jour", it)) }
    // Puis quelques catégories, en alternant films et séries
    val movies = categoryShelves(catalog.movieCategories, catalog.movies).take(6)
    val series = categoryShelves(catalog.seriesCategories, catalog.series).take(6)
    for (i in 0 until maxOf(movies.size, series.size)) {
        movies.getOrNull(i)?.let(::add)
        series.getOrNull(i)?.let(::add)
    }
}

private fun categoryShelves(categories: List<Category>, items: List<Media>): List<Shelf> {
    val byCategory = items.groupBy { it.categoryId }
    return categories.mapNotNull { c -> byCategory[c.id]?.let { Shelf(c.name, it) } }
}
