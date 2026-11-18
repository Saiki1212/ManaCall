package com.saiki1212.manacall.ui

import android.content.Intent
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.saiki1212.manacall.R
import com.saiki1212.manacall.calling.CallHandoff
import com.saiki1212.manacall.contacts.Contact
import com.saiki1212.manacall.contacts.PhoneNumber

@Composable
internal fun DetailHeader(favorite: Boolean, changeFavorite: (Boolean) -> Unit, back: () -> Unit) {
    Surface(color = Paper) {
        Row(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                onClick = back,
                color = Lavender,
                contentColor = Indigo,
                shape = RoundedCornerShape(15.dp),
            ) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.back_to_contacts),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Box(
                Modifier.size(56.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (favorite) Indigo else Lavender)
                    .toggleable(favorite, role = Role.Checkbox, onValueChange = changeFavorite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (favorite) R.drawable.ic_star else R.drawable.ic_star_outline),
                    contentDescription = stringResource(
                        if (favorite) R.string.remove_favorite else R.string.add_favorite
                    ),
                    modifier = Modifier.size(36.dp),
                    tint = if (favorite) Color.White else Indigo,
                )
            }
        }
    }
}

@Composable
internal fun ContactActions(
    contact: Contact,
    photoRevision: Long,
    callHandoff: CallHandoff,
    requestPhoneCall: (String) -> Unit,
    requestWhatsAppVideo: (Intent) -> Unit,
) {
    var chosenNumber by rememberSaveable(contact.key) { mutableStateOf<String?>(null) }
    val number = contact.numbers.firstOrNull { it.value == chosenNumber } ?: contact.numbers.firstOrNull()
    val videoAction = key(contact.id, number, photoRevision) {
        produceState<Intent?>(null) {
            if (number != null) value = callHandoff.findWhatsAppVideo(contact.id, number)
        }.value
    }

    Box(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ContactPhoto(
                contact, photoRevision,
                Modifier.widthIn(max = 380.dp).fillMaxWidth().aspectRatio(1f)
                    .clip(RoundedCornerShape(22.dp)),
            )
            Text(
                contact.displayName(),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold,
            )
            if (number == null) {
                Text(
                    stringResource(R.string.no_phone_number),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedInk,
                    textAlign = TextAlign.Center,
                )
            } else {
                if (contact.numbers.size > 1) {
                    NumberChoices(contact.numbers, number) { chosenNumber = it.value }
                } else {
                    Text(number.value, style = MaterialTheme.typography.bodyLarge.digits(), color = MutedInk)
                }

                Row(
                    Modifier.widthIn(max = 380.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CallButton(
                        icon = R.drawable.ic_phone,
                        label = R.string.normal_call,
                        modifier = Modifier.weight(1f),
                        primary = true,
                    ) { requestPhoneCall(number.value) }
                    if (videoAction != null) {
                        CallButton(
                            icon = R.drawable.ic_video,
                            label = R.string.whatsapp_video,
                            modifier = Modifier.weight(1f),
                            showLabel = true,
                        ) { requestWhatsAppVideo(videoAction) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberChoices(
    numbers: List<PhoneNumber>,
    chosen: PhoneNumber,
    select: (PhoneNumber) -> Unit,
) {
    val resources = LocalResources.current
    Column(Modifier.fillMaxWidth().selectableGroup()) {
        Text(stringResource(R.string.choose_number), style = MaterialTheme.typography.titleMedium)
        numbers.forEach { number ->
            val selected = number == chosen
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .selectable(selected, role = Role.RadioButton, onClick = { select(number) })
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected, onClick = null)
                Column {
                    Text(number.value, style = MaterialTheme.typography.titleMedium.digits())
                    val label = Phone.getTypeLabel(resources, number.labelType, number.customLabel)
                    if (label.isNotBlank()) {
                        Text(label.toString(), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                    }
                }
            }
        }
    }
}

private fun TextStyle.digits() = copy(textDirection = TextDirection.Ltr)

@Composable
private fun CallButton(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    modifier: Modifier,
    primary: Boolean = false,
    showLabel: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        shape = RoundedCornerShape(16.dp),
        color = if (primary) Indigo else PaleGreen,
        contentColor = if (primary) Color.White else Green,
        border = if (primary) null else BorderStroke(1.dp, Color(0xFFC8DDD2)),
    ) {
        Column(
            Modifier.heightIn(min = 104.dp).padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
        ) {
            val spokenLabel = stringResource(label)
            Icon(
                painterResource(icon),
                contentDescription = if (showLabel) null else spokenLabel,
                modifier = Modifier.size(44.dp),
            )
            if (showLabel) {
                Text(
                    spokenLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
