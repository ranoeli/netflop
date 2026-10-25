package com.djamal.ch.netflix.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.djamal.ch.netflix.AppViewModel
import com.djamal.ch.netflix.R
import com.djamal.ch.netflix.ui.theme.NetflixRed
import com.djamal.ch.netflix.ui.theme.TextGrey

@Composable
fun LoginScreen(vm: AppViewModel) {
    var server by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val next: () -> Unit = { focus.moveFocus(FocusDirection.Down) }
    val link = m3uLink(server)
    val canSubmit = !busy && server.isNotBlank() && (link != null || (user.isNotBlank() && pass.isNotBlank()))

    // Le bouton peut garder une ancienne version de cette fonction : on relit donc l'état
    // au moment du clic, pas les valeurs capturées à la composition
    fun submit() {
        val current = m3uLink(server)
        if (busy || server.isBlank() || (current == null && (user.isBlank() || pass.isBlank()))) return
        keyboard?.hide()
        busy = true
        error = null
        val (s, u, p) = current ?: Triple(server.trim(), user.trim(), pass)
        vm.login(s, u, p) {
            busy = false
            error = it
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.width(440.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.app_name).uppercase(),
                color = NetflixRed,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
            )
            Text("Connexion à votre fournisseur IPTV (Xtream Codes)", color = TextGrey)
            TvTextField(server, { server = it }, "Serveur ou lien M3U complet", Modifier.fillMaxWidth(), onImeAction = next)
            if (link != null) {
                Text("Lien M3U reconnu : utilisateur « ${link.second} »", color = TextGrey)
            } else {
                TvTextField(user, { user = it }, "Nom d'utilisateur", Modifier.fillMaxWidth(), onImeAction = next)
                TvTextField(
                    pass, { pass = it }, "Mot de passe", Modifier.fillMaxWidth(),
                    password = true, imeAction = ImeAction.Done, onImeAction = ::submit,
                )
            }
            error?.let { Text(it, color = NetflixRed) }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = ::submit, enabled = canSubmit) {
                    Text(if (busy) "Connexion…" else "Se connecter")
                }
                OutlinedButton(onClick = vm::startDemo) {
                    Text("Mode démo")
                }
            }
        }
    }
}

/** "http://hote:port/get.php?username=U&password=P…" → (http://hote:port, U, P), sinon null. */
private fun m3uLink(text: String): Triple<String, String, String>? {
    val uri = Uri.parse(text.trim())
    if (!uri.isHierarchical || uri.scheme == null || uri.authority == null) return null
    val user = uri.getQueryParameter("username")?.takeIf { it.isNotBlank() } ?: return null
    val pass = uri.getQueryParameter("password")?.takeIf { it.isNotBlank() } ?: return null
    return Triple("${uri.scheme}://${uri.authority}", user, pass)
}
