package be.webweaver.apps.caskandlabel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.webweaver.apps.caskandlabel.R
import be.webweaver.apps.caskandlabel.core.Score
import be.webweaver.apps.caskandlabel.data.Bottle
import be.webweaver.apps.caskandlabel.data.Repository
import be.webweaver.apps.caskandlabel.data.TastingRow

/** A bottle's facts, label photo and every tasting of it you may see, with who tasted and where it was shared. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BottleScreen(vm: AppViewModel, online: Boolean, bottleId: String, onBack: () -> Unit, onNewTasting: (String) -> Unit) {
    val context = LocalContext.current
    val all by vm.tastings.collectAsStateWithLifecycle()
    val tastings = remember(all, bottleId) { all.filter { it.bottleId == bottleId } }
    var bottle by remember { mutableStateOf<Bottle?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<TastingRow?>(null) }
    LaunchedEffect(bottleId, tastings) {
        bottle = tastings.firstNotNullOfOrNull { it.bottle }
            ?: runCatching { Repository.bottle(bottleId) }.onFailure { error = errorMessage(context, it) }.getOrNull()
    }
    val me = Repository.currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bottleTitle(context, bottle), maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
        floatingActionButton = {
            if (online && bottle != null) ExtendedFloatingActionButton(onClick = { onNewTasting(bottleId) },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.taste_again)) })
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            bottle?.let { b ->
                item {
                    Row(verticalAlignment = Alignment.Top) {
                        LabelPhoto(b.labelPhotoPath, 120.dp)
                        Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(bottleTitle(context, b), style = MaterialTheme.typography.titleLarge)
                            factsLine(context, b).takeIf { it.isNotEmpty() }?.let { Text(it) }
                            Score.formatAverage(tastings.map { it.scoreHalfSteps })?.let {
                                Text(stringResource(R.string.average_score, it, tastings.size), color = MaterialTheme.colorScheme.primary)
                            }
                            if (b.status == "pending") Text(stringResource(R.string.bottle_pending), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { BottleFacts(b) }
            }
            item { Text(stringResource(R.string.tastings), style = MaterialTheme.typography.titleMedium) }
            if (tastings.isEmpty()) item { Text(stringResource(R.string.no_tastings_bottle), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(tastings, key = { it.id }) { t ->
                TastingCard(t, groupName = t.group?.name ?: vm.groupName(t.groupId),
                    canDelete = online && t.userId == me, onDelete = { toDelete = t })
            }
            item { ErrorText(error) }
        }
    }

    toDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_tasting_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    toDelete = null
                    vm.run({ Repository.deleteTasting(t.id) }, onError = { error = it })
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BottleFacts(b: Bottle) {
    val facts = listOfNotNull(
        b.brand?.let { stringResource(R.string.field_brand) to it },
        b.distillery?.let { stringResource(R.string.field_distillery) to it },
        b.kind?.let { stringResource(R.string.field_kind) to it },
        b.country?.let { stringResource(R.string.field_country) to it },
        b.region?.let { stringResource(R.string.field_region) to it },
        b.volumeCl?.let { stringResource(R.string.field_volume) to "${formatNumber(it)} cl" },
        b.peated?.let { stringResource(R.string.field_peated) to stringResource(if (it) R.string.yes else R.string.no) },
        b.flavorProfile?.let { stringResource(R.string.field_flavor_profile) to it },
        b.place?.let { stringResource(R.string.field_place) to it },
        b.founded?.let { stringResource(R.string.field_founded) to it.toString() },
        b.owner?.let { stringResource(R.string.field_owner) to it },
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        facts.forEach { (label, value) ->
            Row { Text("$label: ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value) }
        }
        val tags = b.caskTypes + b.caskDetails + b.features + b.flavorWords
        if (tags.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tags.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
        }
        if (b.sources.isNotEmpty()) Text(stringResource(R.string.sources, b.sources.joinToString(", ")),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TastingCard(t: TastingRow, groupName: String?, canDelete: Boolean, onDelete: () -> Unit, title: String? = null) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                    Text(
                        listOfNotNull(t.profile?.displayName, formatDateTime(parseInstant(t.tastedAt))).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(groupName ?: stringResource(R.string.scope_private), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                ScoreLabel(t.scoreHalfSteps)
                if (canDelete) IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete)) }
            }
            t.nose?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.nose_value, it)) }
            t.palate?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.palate_value, it)) }
            t.finish?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.finish_value, it)) }
            if (t.flavorWords.isNotEmpty()) Text(t.flavorWords.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            t.pricePaidEur?.let { Text(stringResource(R.string.price_value, formatNumber(it)), style = MaterialTheme.typography.bodySmall) }
        }
    }
}
