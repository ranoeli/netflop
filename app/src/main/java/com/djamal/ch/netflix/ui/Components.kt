package com.djamal.ch.netflix.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.djamal.ch.netflix.data.Details
import com.djamal.ch.netflix.data.Kind
import com.djamal.ch.netflix.data.Media
import com.djamal.ch.netflix.ui.theme.MatchGreen
import com.djamal.ch.netflix.ui.theme.NetflixBlack
import com.djamal.ch.netflix.ui.theme.NetflixRed
import com.djamal.ch.netflix.ui.theme.SurfaceGrey
import com.djamal.ch.netflix.ui.theme.TextGrey

/** Marge gauche du contenu, pour laisser la place au menu latéral replié. */
val ContentStart = 88.dp

private val CardShape = RoundedCornerShape(6.dp)

@Composable
private fun cardBorder() = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = CardShape))

@Composable
fun PosterCard(media: Media, onClick: () -> Unit, modifier: Modifier = Modifier, onFocus: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(110.dp)
            .aspectRatio(2f / 3f)
            .onFocusChanged { if (it.isFocused) onFocus() },
        shape = CardDefaults.shape(CardShape),
        colors = CardDefaults.colors(containerColor = SurfaceGrey),
        scale = CardDefaults.scale(focusedScale = 1.1f),
        border = cardBorder(),
    ) {
        Box(Modifier.fillMaxSize()) {
            // Visible tant que l'affiche n'est pas chargée (ou si elle n'existe pas)
            Text(
                media.name,
                Modifier.align(Alignment.Center).padding(8.dp),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 4,
            )
            AsyncImage(
                model = media.image,
                contentDescription = media.name,
                contentScale = if (media.kind == Kind.LIVE) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            media.progress?.let { ProgressBar(it, Modifier.align(Alignment.BottomStart)) }
        }
    }
}

@Composable
fun ChannelCard(media: Media, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 10f),
        shape = CardDefaults.shape(CardShape),
        colors = CardDefaults.colors(containerColor = SurfaceGrey),
        scale = CardDefaults.scale(focusedScale = 1.08f),
        border = cardBorder(),
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = media.image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Text(
                media.name,
                Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(4.dp).background(Color.Gray.copy(alpha = 0.6f))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(NetflixRed))
    }
}

/** Image de fond fondue dans le noir à gauche et en bas, comme sur Netflix. */
@Composable
fun Backdrop(image: String?, modifier: Modifier = Modifier) {
    Box(modifier) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(image).crossfade(400).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.horizontalGradient(0f to NetflixBlack, 0.45f to NetflixBlack.copy(alpha = 0.4f), 1f to Color.Transparent)
            )
        )
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to NetflixBlack)))
    }
}

/** Note, année, durée, genre. */
@Composable
fun MetaLine(media: Media, details: Details?) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        (details?.rating ?: media.rating)?.toFloatOrNull()?.takeIf { it > 0 }?.let {
            Text("★ %.1f".format(it), color = MatchGreen, fontWeight = FontWeight.Bold)
        }
        listOfNotNull(details?.year, details?.duration?.let(::formatDuration), details?.genre).forEach {
            Text(it, color = TextGrey, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** "01:52:31" → "1 h 52 min" */
fun formatDuration(raw: String): String {
    val parts = raw.split(":").map { it.toIntOrNull() ?: return raw }
    if (parts.size < 2) return raw
    val (h, m) = parts
    return if (h > 0) "$h h ${"%02d".format(m)} min" else "$m min"
}

/**
 * Champ texte pour télécommande : la case se sélectionne comme un bouton, et le clavier
 * ne s'ouvre qu'en appuyant sur OK (sinon il surgit à chaque passage et vole les flèches).
 */
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(4.dp)
    LaunchedEffect(editing) { if (editing) runCatching { field.requestFocus() } }

    Box(
        modifier
            .onFocusChanged { focused = it.hasFocus }
            .border(2.dp, if (focused) Color.White else Color.Gray.copy(alpha = 0.4f), shape)
            .background(SurfaceGrey, shape)
            .clickable { editing = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(field)
                .focusProperties { canFocus = editing }
                .onFocusChanged { if (!it.isFocused) editing = false }
                // Le champ garde sinon les flèches haut/bas et on ne peut plus en sortir
                .onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (it.key) {
                        Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                        Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                        else -> false
                    }
                },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(Color.White),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
                imeAction = imeAction,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = TextGrey, fontSize = 16.sp)
                    inner()
                }
            },
        )
    }
}

@Composable
fun CenteredMessage(title: String, subtitle: String? = null, content: @Composable () -> Unit = {}) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            subtitle?.let { Text(it, color = TextGrey, textAlign = TextAlign.Center, modifier = Modifier.width(520.dp)) }
            content()
        }
    }
}
