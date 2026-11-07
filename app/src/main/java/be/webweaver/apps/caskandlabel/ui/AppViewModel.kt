package be.webweaver.apps.caskandlabel.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import be.webweaver.apps.caskandlabel.data.Backend
import be.webweaver.apps.caskandlabel.data.Connectivity
import be.webweaver.apps.caskandlabel.data.GroupSummary
import be.webweaver.apps.caskandlabel.data.ProfileRow
import be.webweaver.apps.caskandlabel.data.Repository
import be.webweaver.apps.caskandlabel.data.TastingRow
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Where the app stands: which screen the root shows. */
sealed interface AppState {
    data object NotConfigured : AppState
    data object Loading : AppState
    data object SignedOut : AppState
    data object NeedsProfile : AppState
    data class Ready(val profile: ProfileRow) : AppState
}

/** Session, profile, groups and visible tastings, shared by all screens; everything comes from the server. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<AppState>(if (Backend.isConfigured) AppState.Loading else AppState.NotConfigured)
    val state: StateFlow<AppState> = _state.asStateFlow()

    val online: StateFlow<Boolean> = Connectivity.online(app)
        .stateIn(viewModelScope, SharingStarted.Eagerly, Connectivity.isOnline(app))

    private val _groups = MutableStateFlow<List<GroupSummary>>(emptyList())
    val groups: StateFlow<List<GroupSummary>> = _groups.asStateFlow()

    private val _tastings = MutableStateFlow<List<TastingRow>>(emptyList())
    val tastings: StateFlow<List<TastingRow>> = _tastings.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Code from a scanned invite QR or a `caskandlabel://join/<code>` link, shown in the join dialog. */
    val pendingJoinCode = MutableStateFlow<String?>(null)

    init {
        Backend.client?.let { client ->
            viewModelScope.launch {
                client.auth.sessionStatus.collect { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> loadProfile()
                        is SessionStatus.NotAuthenticated -> {
                            _groups.value = emptyList()
                            _tastings.value = emptyList()
                            _state.value = AppState.SignedOut
                        }
                        is SessionStatus.Initializing -> _state.value = AppState.Loading
                        is SessionStatus.RefreshFailure -> if (_state.value == AppState.Loading) loadProfile()
                    }
                }
            }
        }
    }

    private suspend fun loadProfile() {
        try {
            val profile = Repository.myProfile()
            _state.value = if (profile == null) AppState.NeedsProfile else AppState.Ready(profile)
            _error.value = null
            if (profile != null) refresh()
        } catch (e: Exception) {
            _error.value = errorMessage(getApplication(), e)
            if (_state.value == AppState.Loading) _state.value = AppState.NeedsProfile
        }
    }

    fun retryProfile() { viewModelScope.launch { loadProfile() } }

    fun createProfile(name: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Repository.createProfile(name)
                loadProfile()
            } catch (e: Exception) {
                onError(errorMessage(getApplication(), e))
            }
        }
    }

    fun rename(name: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Repository.renameProfile(name)
                loadProfile()
            } catch (e: Exception) {
                onError(errorMessage(getApplication(), e))
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _groups.value = Repository.groups()
                _tastings.value = Repository.visibleTastings()
                _error.value = null
            } catch (e: Exception) {
                _error.value = errorMessage(getApplication(), e)
            } finally {
                _loading.value = false
            }
        }
    }

    /** Runs a server action, refreshes on success, and reports a readable error otherwise. */
    fun run(action: suspend () -> Unit, onDone: () -> Unit = {}, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                action()
                refresh()
                onDone()
            } catch (e: Exception) {
                onError(errorMessage(getApplication(), e))
            }
        }
    }

    fun signOut() { viewModelScope.launch { runCatching { Repository.signOut() } } }

    fun groupName(id: String?): String? = id?.let { gid -> _groups.value.firstOrNull { it.id == gid }?.name }
}
