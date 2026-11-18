package com.saiki1212.manacall.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.saiki1212.manacall.R
import com.saiki1212.manacall.contacts.Contact
import com.saiki1212.manacall.contacts.ContactsSnapshot

internal enum class ContactTab(
    @get:StringRes val title: Int,
    @get:DrawableRes val icon: Int,
    @get:StringRes val emptyTitle: Int,
    @get:StringRes val emptyMessage: Int,
) {
    Favorites(R.string.favorites, R.drawable.ic_star, R.string.empty_favorites, R.string.empty_favorites_message),
    Photos(R.string.photo_contacts, R.drawable.ic_photo, R.string.empty_photo_contacts, R.string.empty_photo_contacts_message),
    All(R.string.all_contacts, R.drawable.ic_people, R.string.empty_contacts, R.string.empty_contacts_message),
}

@Composable
internal fun ContactsHeader(language: String, setLanguage: (String) -> Unit) {
    Surface(color = Paper) {
        Row(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_manacall),
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = Color.Unspecified,
                )
                Text(
                    stringResource(R.string.app_name),
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Surface(color = Lavender, shape = RoundedCornerShape(16.dp)) {
                val label = stringResource(R.string.language)
                Row(
                    Modifier.padding(2.dp).semantics { contentDescription = label },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LanguageChip("en", language != "te", R.string.english, R.string.english_accessibility, setLanguage)
                    LanguageChip("te", language == "te", R.string.telugu, R.string.telugu, setLanguage)
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(
    tag: String,
    active: Boolean,
    @StringRes label: Int,
    @StringRes spokenLabel: Int,
    setLanguage: (String) -> Unit,
) {
    val spoken = stringResource(spokenLabel)
    Surface(
        onClick = { if (!active) setLanguage(tag) },
        modifier = Modifier.minimumInteractiveComponentSize().semantics {
            contentDescription = spoken
            selected = active
            role = Role.Button
        },
        shape = RoundedCornerShape(13.dp),
        color = if (active) Indigo else Lavender,
        contentColor = if (active) Color.White else Indigo,
    ) {
        Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun ContactsNavigation(tab: ContactTab, changeTab: (ContactTab) -> Unit) {
    Surface(color = Paper) {
        Column {
            HorizontalDivider(color = Line)
            Row(
                Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ContactTab.entries.forEach { destination ->
                    val active = tab == destination
                    Box(
                        Modifier.weight(1f).heightIn(min = 58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) Indigo else Paper)
                            .selectable(active, role = Role.Tab, onClick = { changeTab(destination) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(destination.icon),
                            contentDescription = stringResource(destination.title),
                            modifier = Modifier.size(36.dp),
                            tint = if (active) Color.White else Indigo,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PhotoGrid(
    snapshot: ContactsSnapshot,
    tab: ContactTab,
    gridState: LazyGridState,
    openContact: (String) -> Unit,
) {
    val contacts = remember(snapshot, tab) {
        when (tab) {
            ContactTab.Favorites -> snapshot.contacts.filter(Contact::favorite)
            ContactTab.Photos -> snapshot.contacts.filter(Contact::hasPhoto)
            ContactTab.All -> snapshot.contacts
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                stringResource(tab.title),
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        if (contacts.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Status(tab.emptyTitle, tab.emptyMessage)
            }
        }
        items(contacts, key = Contact::key) { contact ->
            ContactTile(contact, snapshot.photoRevision) { openContact(contact.key) }
        }
    }
}

@Composable
private fun ContactTile(contact: Contact, revision: Long, openContact: () -> Unit) {
    Card(
        onClick = openContact,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
    ) {
        ContactPhoto(contact, revision, Modifier.fillMaxWidth().aspectRatio(1f))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(horizontal = 6.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                contact.displayName(),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
internal fun ContactPhoto(contact: Contact, revision: Long, modifier: Modifier) {
    val context = LocalContext.current
    val sources = remember(contact.photo, contact.thumbnail) {
        listOfNotNull(contact.photo, contact.thumbnail).distinct()
    }
    var sourceIndex by remember(sources, revision) { mutableIntStateOf(0) }
    val source = sources.getOrNull(sourceIndex)
    Box(modifier.background(Lavender), contentAlignment = Alignment.Center) {
        if (source == null) {
            Icon(painterResource(R.drawable.ic_person), null, Modifier.fillMaxSize(0.48f), tint = Indigo)
        } else {
            val request = remember(context, source, revision) {
                ImageRequest.Builder(context)
                    .data(source)
                    .memoryCacheKey("$source:$revision")
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .networkCachePolicy(CachePolicy.DISABLED)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { sourceIndex++ },
            )
        }
    }
}

@Composable
internal fun Contact.displayName(): String = name ?: stringResource(R.string.unnamed_contact)
