package be.webweaver.apps.caskandlabel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import be.webweaver.apps.caskandlabel.core.InviteCode
import be.webweaver.apps.caskandlabel.data.Backend
import be.webweaver.apps.caskandlabel.ui.App
import be.webweaver.apps.caskandlabel.ui.AppViewModel
import be.webweaver.apps.caskandlabel.ui.CaskTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handle(intent)
        setContent {
            CaskTheme {
                Surface(Modifier.fillMaxSize()) { App(vm) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    /** `caskandlabel://join/<code>` opens the join dialog; `caskandlabel://login-callback` completes a mail link. */
    private fun handle(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "caskandlabel") return
        when (data.host) {
            "join" -> InviteCode.parse(data.toString()).takeIf { it.length == InviteCode.LENGTH }
                ?.let { vm.pendingJoinCode.value = InviteCode.formatted(it) }
            "login-callback" -> Backend.client?.handleDeeplinks(intent)
        }
    }
}
