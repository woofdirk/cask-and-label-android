package be.webweaver.apps.caskandlabel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.webweaver.apps.caskandlabel.R
import be.webweaver.apps.caskandlabel.data.Repository
import kotlinx.coroutines.launch

/** E-mail and password; "Account maken" or "Aanmelden" (spec: account.md, 0.25). */
@Composable
fun AuthScreen(online: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var signUp by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var info by rememberSaveable { mutableStateOf<String?>(null) }
    val canSubmit = online && !busy && email.contains("@") && password.length >= 8

    if (!online) OfflineBanner()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.auth_intro))
        Text(stringResource(R.string.auth_email_private), color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !signUp, onClick = { signUp = false; error = null }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                Text(stringResource(R.string.sign_in))
            }
            SegmentedButton(selected = signUp, onClick = { signUp = true; error = null }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                Text(stringResource(R.string.create_account))
            }
        }
        OutlinedTextField(
            value = email, onValueChange = { email = it.trim() }, singleLine = true,
            label = { Text(stringResource(R.string.email)) },
            placeholder = { Text("naam@voorbeeld.be") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it }, singleLine = true,
            label = { Text(stringResource(R.string.password_hint)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                busy = true
                scope.launch {
                    try {
                        if (signUp) {
                            Repository.signUp(email, password)
                            // With e-mail confirmation off (temporarily, DECISIONS.md) this already signs you in.
                            if (Repository.currentUserId == null) {
                                info = context.getString(R.string.auth_confirm_mail)
                                signUp = false
                            }
                        } else {
                            Repository.signIn(email, password)
                        }
                        error = null
                    } catch (e: Exception) {
                        error = errorMessage(context, e)
                    } finally {
                        busy = false
                    }
                }
            },
        ) { Text(stringResource(if (signUp) R.string.create_account else R.string.sign_in)) }
        if (signUp) Text(stringResource(R.string.auth_signup_footer), style = MaterialTheme.typography.bodySmall)
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        ErrorText(error)
        if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
    }
}

/** First sign-in on the server: the name group mates see and the 18+ confirmation (profiles row). */
@Composable
fun ProfileSetupScreen(vm: AppViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var adult by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val loadError by vm.error.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.profile_intro))
        OutlinedTextField(
            value = name, onValueChange = { if (it.length <= 40) name = it }, singleLine = true,
            label = { Text(stringResource(R.string.your_name)) }, modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = adult, onCheckedChange = { adult = it })
            Text(stringResource(R.string.adult_confirm))
        }
        Text(stringResource(R.string.adult_footer), style = MaterialTheme.typography.bodySmall)
        Button(
            enabled = !busy && adult && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                busy = true
                vm.createProfile(name.trim()) { error = it; busy = false }
            },
        ) { Text(stringResource(R.string.start)) }
        ErrorText(error ?: loadError)
        if (loadError != null) TextButton(onClick = { vm.retryProfile() }) { Text(stringResource(R.string.retry)) }
        TextButton(onClick = { vm.signOut() }) { Text(stringResource(R.string.sign_out)) }
    }
}
