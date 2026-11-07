package be.webweaver.apps.caskandlabel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.webweaver.apps.caskandlabel.R
import be.webweaver.apps.caskandlabel.core.GroupScope
import be.webweaver.apps.caskandlabel.core.Journal
import be.webweaver.apps.caskandlabel.core.JournalRow
import be.webweaver.apps.caskandlabel.core.JournalSort
import be.webweaver.apps.caskandlabel.core.JournalTasting
import be.webweaver.apps.caskandlabel.core.Score
import be.webweaver.apps.caskandlabel.data.TastingRow

fun TastingRow.toJournal(title: String) = JournalTasting(
    id = id, bottleId = bottleId, bottleTitle = title, userId = userId, groupId = groupId,
    scoreHalfSteps = scoreHalfSteps, tastedAt = parseInstant(tastedAt), labelPhotoPath = bottle?.labelPhotoPath,
    flavorProfile = bottle?.flavorProfile, caskTypes = bottle?.caskTypes.orEmpty(),
)

/** One row per bottle with your tastings and those of your groups (spec 0.12, 0.30), filtered by group. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(vm: AppViewModel, online: Boolean, onBottle: (String) -> Unit, onNewTasting: () -> Unit) {
    val context = LocalContext.current
    val tastings by vm.tastings.collectAsStateWithLifecycle()
    val groups by vm.groups.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var scopeKey by rememberSaveable { mutableStateOf("all") }
    var sort by rememberSaveable { mutableStateOf(JournalSort.DATE) }
    var sortMenu by remember { mutableStateOf(false) }
    val scope = when (scopeKey) {
        "all" -> GroupScope.All
        "private" -> GroupScope.Private
        else -> GroupScope.Group(scopeKey)
    }
    val rows = remember(tastings, scope, query, sort) {
        Journal.rows(tastings.map { it.toJournal(bottleTitle(context, it.bottle)) }, scope, query, sort)
    }
    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_journal)) },
                actions = {
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort)) }
                    DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                        listOf(JournalSort.DATE to R.string.sort_date, JournalSort.SCORE to R.string.sort_score,
                            JournalSort.NAME to R.string.sort_name).forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text((if (sort == value) "✓ " else "") + stringResource(label)) },
                                onClick = { sort = value; sortMenu = false },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (online) ExtendedFloatingActionButton(onClick = onNewTasting, icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.new_tasting)) })
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text(stringResource(R.string.search_name)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(scopeKey == "all", { scopeKey = "all" }, label = { Text(stringResource(R.string.scope_all)) })
                FilterChip(scopeKey == "private", { scopeKey = "private" }, label = { Text(stringResource(R.string.scope_private)) })
                groups.forEach { g -> FilterChip(scopeKey == g.id, { scopeKey = g.id }, label = { Text(g.name) }) }
            }
            ErrorText(error, Modifier.padding(horizontal = 16.dp))
            PullToRefreshBox(isRefreshing = loading, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
                if (rows.isEmpty() && !loading) {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(if (tastings.isEmpty()) R.string.journal_empty else R.string.journal_no_match),
                            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(rows, key = { it.bottleId }) { row ->
                        JournalRowView(row, groupNames = row.groupIds.mapNotNull { vm.groupName(it) }) { onBottle(row.bottleId) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalRowView(row: JournalRow, groupNames: List<String>, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelPhoto(row.labelPhotoPath, 56.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(row.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(
                        row.flavorProfile,
                        LocalContext.current.resources.getQuantityString(R.plurals.tastings_count, row.count, row.count),
                        formatDate(row.lastTastedAt),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "★ " + (Score.formatAverage(row.scores) ?: ""),
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
            )
        }
        val tags = groupNames + row.caskTypes
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                tags.forEach { SuggestionChip(onClick = {}, label = { Text(it, style = MaterialTheme.typography.labelSmall) }) }
            }
        }
    }
}

