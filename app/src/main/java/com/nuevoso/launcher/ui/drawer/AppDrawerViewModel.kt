package com.nuevoso.launcher.ui.drawer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nuevoso.launcher.App
import com.nuevoso.launcher.data.apps.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DrawerUiState(
    val apps: List<AppInfo> = emptyList(),
    val filtered: List<AppInfo> = emptyList(),
    val query: String = "",
)

class AppDrawerViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepo = App.get(application).appRepository
    private val _state = MutableStateFlow(DrawerUiState())
    val state: StateFlow<DrawerUiState> = _state.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appRepo.getAllApps()
            _state.update { it.copy(apps = apps, filtered = apps) }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { state ->
            val filtered = if (query.isBlank()) state.apps
            else state.apps.filter { it.label.contains(query, ignoreCase = true) }
            state.copy(query = query, filtered = filtered)
        }
    }
}
