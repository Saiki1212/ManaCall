package com.saiki1212.manacall.ui

import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.remember
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
internal fun DetailHeader(back: () -> Unit) {
    Surface(color = Paper) {
        Box(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
        }
    }
}

@Composable
internal fun ContactActions(
    contact: Contact,
    photoRevision: Long,
    callHandoff: CallHandoff,
    whatsappAvailable: Boolean,
) {
    var chosenNumber by rememberSaveable(contact.key) { mutableStateOf<String?>(null) }
    var message by remember(contact.key) { mutableStateOf<Int?>(null) }
    val number = contact.numbers.firstOrNull { it.value == chosenNumber } ?: contact.numbers.firstOrNull()
    val whatsappNumber = remember(number, callHandoff, photoRevision) {
        number?.let(callHandoff::whatsappNumber)
    }
    val openWhatsApp = {
        if (whatsappNumber != null && !callHandoff.openWhatsApp(whatsappNumber)) {
            message = if (callHandoff.hasWhatsApp()) {
                R.string.whatsapp_open_failed
            } else {
                R.string.whatsapp_unavailable
            }
        }
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
                Modifier.widthIn(max = 248.dp).fillMaxWidth(0.77f).aspectRatio(1f)
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

                CallButton(
                    icon = R.drawable.ic_phone,
                    label = R.string.normal_call,
                    help = R.string.normal_call_help,
                    primary = true,
                ) {
                    if (!callHandoff.openPhone(number)) message = R.string.phone_unavailable
                }
                if (whatsappAvailable && whatsappNumber != null) {
                    CallButton(
                        icon = R.drawable.ic_chat_phone,
                        label = R.string.whatsapp_voice,
                        help = R.string.whatsapp_voice_help,
                        onClick = openWhatsApp,
                    )
                    CallButton(
                        icon = R.drawable.ic_video,
                        label = R.string.whatsapp_video,
                        help = R.string.whatsapp_video_help,
                        onClick = openWhatsApp,
                    )
                } else {
                    Text(
                        stringResource(
                            if (whatsappAvailable) R.string.whatsapp_number_unavailable else R.string.whatsapp_unavailable
                        ),
                        Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MutedInk,
                    )
                }
            }
        }
    }
    message?.let { MessageDialog(it) { message = null } }
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
    @StringRes help: Int,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (primary) Indigo else PaleGreen,
        contentColor = if (primary) Color.White else Green,
        border = if (primary) null else BorderStroke(1.dp, Color(0xFFC8DDD2)),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 13.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(33.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(stringResource(help), style = MaterialTheme.typography.bodySmall)
            }
            Icon(painterResource(R.drawable.ic_next), contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}
