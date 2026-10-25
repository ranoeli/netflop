package com.djamal.ch.netflix.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.viewmodel.compose.viewModel
import com.djamal.ch.netflix.AppViewModel
import com.djamal.ch.netflix.data.Kind
import com.djamal.ch.netflix.data.Media
import com.djamal.ch.netflix.data.PlayRequest

private sealed interface Screen {
    val key: String

    data class Detail(val media: Media) : Screen {
        override val key get() = "detail:${media.kind}:${media.id}"
    }

    data class Player(val request: PlayRequest) : Screen {
        override val key get() = "player:${request.key}"
    }
}

@Composable
fun App(vm: AppViewModel = viewModel()) {
    if (vm.source == null) {
        LoginScreen(vm)
        return
    }

    // Pile de navigation minimale : l'écran principal + fiches et lecteur empilés au-dessus
    val stack = remember { mutableStateListOf<Screen>() }
    val saveable = rememberSaveableStateHolder()
    BackHandler(enabled = stack.isNotEmpty()) { stack.removeAt(stack.lastIndex) }

    val play: (PlayRequest) -> Unit = { stack += Screen.Player(it) }
    val open: (Media) -> Unit = { media ->
        when {
            media.resume != null -> play(media.resume)
            media.kind == Kind.LIVE -> play(vm.playRequest(media, null))
            else -> stack += Screen.Detail(media)
        }
    }

    when (val top = stack.lastOrNull()) {
        null -> saveable.SaveableStateProvider("main") { MainScreen(vm, onOpen = open) }
        is Screen.Detail -> saveable.SaveableStateProvider(top.key) { DetailScreen(vm, top.media, onPlay = play) }
        is Screen.Player -> PlayerScreen(vm, top.request)
    }
}
