package be.webweaver.apps.caskandlabel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.webweaver.apps.caskandlabel.BuildConfig
import be.webweaver.apps.caskandlabel.R
import be.webweaver.apps.caskandlabel.data.Repository

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val profile = (state as? AppState.Ready)?.profile
    var renaming by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.account), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.signed_in_as, Repository.currentEmail.orEmpty()))
        Text(stringResource(R.string.display_name_value, profile?.displayName.orEmpty()))
        Text(stringResource(R.string.display_name_footer), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { newName = profile?.displayName.orEmpty(); renaming = true }) {
            Text(stringResource(R.string.change_name))
        }
        Button(onClick = { confirmSignOut = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sign_out)) }
        ErrorText(error)
        HorizontalDivider()
        Text(stringResource(R.string.about), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.about_text))
        Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.drink_responsibly), style = MaterialTheme.typography.bodySmall)
    }

    if (renaming) {
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.change_name)) },
            text = {
                OutlinedTextField(newName, { if (it.length <= 40) newName = it }, singleLine = true,
                    label = { Text(stringResource(R.string.your_name)) }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(enabled = newName.isNotBlank(), onClick = {
                    renaming = false
                    vm.rename(newName.trim()) { error = it }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.sign_out_confirm)) },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; vm.signOut() }) { Text(stringResource(R.string.sign_out)) } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
