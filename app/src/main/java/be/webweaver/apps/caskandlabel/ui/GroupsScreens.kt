package be.webweaver.apps.caskandlabel.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.webweaver.apps.caskandlabel.R
import be.webweaver.apps.caskandlabel.core.InviteCode
import be.webweaver.apps.caskandlabel.data.Repository
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/** Your groups, a new group, and joining with a code (spec: groepen.md, 0.28). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(vm: AppViewModel, online: Boolean, onGroup: (String) -> Unit) {
    val groups by vm.groups.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val loadError by vm.error.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    var joining by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_groups)) }) },
        floatingActionButton = {
            if (online) ExtendedFloatingActionButton(onClick = { newName = ""; creating = true },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.new_group)) })
        },
    ) { padding ->
        PullToRefreshBox(loading, onRefresh = { vm.refresh() }, modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(groups, key = { it.id }) { g ->
                    ListItem(
                        headlineContent = { Text(g.name) },
                        supportingContent = {
                            Text(LocalContext.current.resources.getQuantityString(R.plurals.members_count, g.members.size, g.members.size))
                        },
                        modifier = Modifier.clickable { onGroup(g.id) },
                    )
                    HorizontalDivider()
                }
                item {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (groups.isEmpty() && !loading) Text(stringResource(R.string.groups_empty))
                        OutlinedButton(enabled = online, onClick = { joining = true }) {
                            Icon(Icons.Outlined.GroupAdd, null)
                            Text(stringResource(R.string.join_with_code), Modifier.padding(start = 8.dp))
                        }
                        Text(stringResource(R.string.groups_footer), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ErrorText(error ?: loadError)
                    }
                }
            }
        }
    }

    if (creating) {
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text(stringResource(R.string.new_group)) },
            text = {
                OutlinedTextField(newName, { if (it.length <= 60) newName = it }, singleLine = true,
                    label = { Text(stringResource(R.string.name)) }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(enabled = newName.isNotBlank(), onClick = {
                    creating = false
                    vm.run({ Repository.createGroup(newName.trim()) }, onError = { error = it })
                }) { Text(stringResource(R.string.create)) }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (joining) JoinDialog(vm, initialCode = "", onDismiss = { joining = false })
}

/** Opens the join dialog when the app was opened with `caskandlabel://join/<code>` (QR scanned with the camera). */
@Composable
fun JoinFromLink(vm: AppViewModel) {
    val code by vm.pendingJoinCode.collectAsStateWithLifecycle()
    code?.let { JoinDialog(vm, it, onDismiss = { vm.pendingJoinCode.value = null }) }
}

@Composable
fun JoinDialog(vm: AppViewModel, initialCode: String, onDismiss: () -> Unit) {
    var code by rememberSaveable { mutableStateOf(initialCode) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scanner = androidx.activity.compose.rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { code = InviteCode.formatted(InviteCode.parse(it)) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.join_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    code, { code = it.uppercase().take(8) }, singleLine = true,
                    label = { Text(stringResource(R.string.code)) }, placeholder = { Text("K7M-2QX") },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = {
                    scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setBeepEnabled(false)
                        .setOrientationLocked(false).setPrompt(""))
                }) {
                    Icon(Icons.Outlined.QrCodeScanner, null)
                    Text(stringResource(R.string.scan_qr), Modifier.padding(start = 8.dp))
                }
                Text(stringResource(R.string.join_footer), style = MaterialTheme.typography.bodySmall)
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && InviteCode.isComplete(code), onClick = {
                busy = true
                vm.run({ Repository.joinWithCode(InviteCode.parse(code)) }, onDone = onDismiss,
                    onError = { error = it; busy = false })
            }) { Text(stringResource(R.string.join)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Members, the group's tastings (newest first), invite for admins, and leaving. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    vm: AppViewModel, online: Boolean, groupId: String,
    onBack: () -> Unit, onBottle: (String) -> Unit, onNewTasting: (String) -> Unit,
) {
    val context = LocalContext.current
    val groups by vm.groups.collectAsStateWithLifecycle()
    val all by vm.tastings.collectAsStateWithLifecycle()
    val group = groups.firstOrNull { it.id == groupId }
    val tastings = remember(all, groupId) { all.filter { it.groupId == groupId } }
    var invite by remember { mutableStateOf<String?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val me = Repository.currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
        floatingActionButton = {
            if (online && group != null) ExtendedFloatingActionButton(onClick = { onNewTasting(groupId) },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.new_tasting)) })
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(stringResource(R.string.members), style = MaterialTheme.typography.titleMedium) }
            items(group?.members.orEmpty(), key = { it.userId }) { m ->
                Row(Modifier.fillMaxWidth()) {
                    Text(m.profile?.displayName ?: "—", Modifier.weight(1f))
                    if (m.role == "admin") Text(stringResource(R.string.admin), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (group?.isAdmin(me) == true) item {
                Button(enabled = online, onClick = {
                    vm.run({ invite = Repository.createInvite(groupId) }, onError = { error = it })
                }) {
                    Icon(Icons.Outlined.QrCode, null)
                    Text(stringResource(R.string.invite), Modifier.padding(start = 8.dp))
                }
            }
            item { Text(stringResource(R.string.tastings), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            if (tastings.isEmpty()) item { Text(stringResource(R.string.group_no_tastings), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(tastings, key = { it.id }) { t ->
                Column(Modifier.clickable { onBottle(t.bottleId) }) {
                    TastingCard(t, groupName = group?.name, canDelete = false, onDelete = {}, title = bottleTitle(context, t.bottle))
                }
            }
            item {
                TextButton(enabled = online, onClick = { confirmLeave = true }) {
                    Text(stringResource(R.string.leave_group), color = MaterialTheme.colorScheme.error)
                }
            }
            item { ErrorText(error) }
        }
    }

    invite?.let { code ->
        InviteDialog(group?.name.orEmpty(), code, onDismiss = { invite = null }, onRevoke = {
            invite = null
            vm.run({ Repository.revokeInvite(code) }, onDone = { error = context.getString(R.string.code_revoked) },
                onError = { error = it })
        })
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.leave_group_confirm)) },
            text = { Text(stringResource(R.string.leave_group_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    vm.run({ Repository.leaveGroup(groupId) }, onDone = onBack, onError = { error = it })
                }) { Text(stringResource(R.string.leave_group), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** The code in large letters, its QR code (`caskandlabel://join/<code>`) and sharing the code with an explanation. */
@Composable
fun InviteDialog(groupName: String, code: String, onDismiss: () -> Unit, onRevoke: () -> Unit) {
    val context = LocalContext.current
    val qr = remember(code) { qrBitmap(InviteCode.link(code)) }
    val formatted = InviteCode.formatted(code)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.invite_title, groupName)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.invite_explain))
                Image(qr.asImageBitmap(), stringResource(R.string.qr_for, groupName), filterQuality = FilterQuality.None,
                    modifier = Modifier.size(220.dp))
                Text(formatted, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                TextButton(onClick = onRevoke) { Text(stringResource(R.string.revoke_code), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val text = context.getString(R.string.invite_share_text, groupName, formatted)
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                context.startActivity(Intent.createChooser(send, null))
            }) {
                Icon(Icons.Outlined.Share, null)
                Text(stringResource(R.string.share_code), Modifier.padding(start = 8.dp))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

private fun qrBitmap(text: String, size: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1))
    val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}
