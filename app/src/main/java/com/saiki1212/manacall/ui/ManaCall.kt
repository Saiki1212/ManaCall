package com.saiki1212.manacall.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.saiki1212.manacall.R
import com.saiki1212.manacall.calling.CallHandoff
import com.saiki1212.manacall.contacts.ContactsRepository
import com.saiki1212.manacall.contacts.ContactsSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

internal val Ink = Color(0xFF18203A)
internal val MutedInk = Color(0xFF4E5770)
internal val Indigo = Color(0xFF333889)
internal val Lavender = Color(0xFFE9E9FA)
internal val Line = Color(0xFFD8D9E8)
internal val Paper = Color(0xFFFAF9FF)
internal val Green = Color(0xFF123B2D)
internal val PaleGreen = Color(0xFFE3F1EB)

private val colors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondaryContainer = Lavender,
    onSecondaryContainer = Indigo,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    outlineVariant = Line,
)

@Composable
internal fun ManaCall(
    lifecycle: Lifecycle,
    repository: ContactsRepository,
    callHandoff: CallHandoff,
    contactsGranted: Boolean,
    permissionRequiresSettings: Boolean,
    requestContacts: () -> Unit,
    whatsappAvailable: Boolean,
    setLanguage: (String) -> Unit,
    settingsError: Boolean,
    clearSettingsError: () -> Unit,
) {
    var snapshot by remember { mutableStateOf<ContactsSnapshot?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableStateOf(ContactTab.Photos) }
    val gridState = key(tab) { rememberLazyGridState() }
    val currentSnapshot = snapshot
    val selected = if (contactsGranted) currentSnapshot?.contacts?.find { it.key == selectedKey } else null
    val language = LocalConfiguration.current.locales[0].language

    LaunchedEffect(contactsGranted, retry, lifecycle, repository) {
        if (!contactsGranted) {
            snapshot = null
            selectedKey = null
            failed = false
            return@LaunchedEffect
        }

        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            repository.changes().catch { problem ->
                if (problem !is RuntimeException || problem is CancellationException) throw problem
                failed = true
            }.collect {
                try {
                    val updated = repository.load()
                    snapshot = updated
                    failed = false
                    if (updated.contacts.none { it.key == selectedKey }) selectedKey = null
                } catch (problem: RuntimeException) {
                    if (problem is CancellationException) throw problem
                    failed = true
                }
            }
        }
    }

    BackHandler(selected != null) { selectedKey = null }
    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = {
                if (selected == null) {
                    ContactsHeader(language, setLanguage)
                } else {
                    DetailHeader { selectedKey = null }
                }
            },
            bottomBar = {
                if (contactsGranted && selected == null) ContactsNavigation(tab) { tab = it }
            },
        ) { insets ->
            Box(Modifier.fillMaxSize().padding(insets)) {
                when {
                    !contactsGranted -> Status(
                        title = R.string.allow_contacts_title,
                        message = if (permissionRequiresSettings) {
                            R.string.allow_contacts_settings_message
                        } else {
                            R.string.allow_contacts_message
                        },
                        button = if (permissionRequiresSettings) R.string.open_settings else R.string.allow_contacts,
                        onClick = requestContacts,
                    )
                    failed -> Status(
                        title = R.string.contacts_error,
                        message = R.string.contacts_error_message,
                        button = R.string.try_again,
                    ) {
                        failed = false
                        retry++
                    }
                    currentSnapshot == null -> Column(
                        modifier = Modifier.align(Alignment.Center).semantics {
                            liveRegion = LiveRegionMode.Polite
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.loading_contacts))
                    }
                    selected != null -> ContactActions(
                        contact = selected,
                        photoRevision = currentSnapshot.photoRevision,
                        callHandoff = callHandoff,
                        whatsappAvailable = whatsappAvailable,
                    )
                    else -> PhotoGrid(
                        snapshot = currentSnapshot,
                        tab = tab,
                        gridState = gridState,
                        openContact = { selectedKey = it },
                    )
                }
            }
        }

        if (settingsError) MessageDialog(R.string.settings_unavailable, clearSettingsError)
    }
}

@Composable
internal fun Status(
    @StringRes title: Int,
    @StringRes message: Int,
    @StringRes button: Int? = null,
    onClick: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(stringResource(message), color = MutedInk, style = MaterialTheme.typography.bodyLarge)
        if (button != null) {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                contentPadding = PaddingValues(12.dp),
            ) {
                Text(stringResource(button), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
internal fun MessageDialog(@StringRes message: Int, close: () -> Unit) {
    AlertDialog(
        onDismissRequest = close,
        text = { Text(stringResource(message), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.close))
            }
        },
    )
}
