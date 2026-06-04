package com.nuevoso.launcher.ui.settings

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nuevoso.launcher.R
import com.nuevoso.launcher.ui.chat.DockDestination
import com.nuevoso.launcher.ui.chat.DockNav
import com.nuevoso.launcher.ui.theme.SolBackground
import com.nuevoso.launcher.ui.theme.SolGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDockDestinationSelected: (DockDestination) -> Unit = {},
    vm: SettingsViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var apiKeyInput by remember { mutableStateOf("") }
    var modelExpanded by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val roleRequestLauncher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    } else null

    Scaffold(
        containerColor = SolBackground,
        bottomBar = {
            DockNav(
                currentDestination = DockDestination.Settings,
                onDestinationSelected = onDockDestinationSelected,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SolBackground)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("screen_settings"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp),
            )

            // API Key
            Text(stringResource(R.string.api_key_label), style = MaterialTheme.typography.labelLarge)
            if (state.hasApiKey) {
                Text(
                    stringResource(R.string.api_key_saved_securely),
                    style = MaterialTheme.typography.bodySmall,
                    color = SolGreen,
                )
            }
            if (state.credentialError) {
                Text(
                    stringResource(R.string.api_key_secure_storage_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.api_key_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            Button(
                onClick = {
                    vm.saveApiKey(apiKeyInput.trim())
                    apiKeyInput = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.api_key_save))
            }
            if (state.hasApiKey) {
                OutlinedButton(
                    onClick = { vm.clearApiKey() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.api_key_clear))
                }
            }

            Spacer(Modifier.height(4.dp))

            // Model
            Text(stringResource(R.string.model_label), style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = it },
            ) {
                OutlinedTextField(
                    value = GEMINI_MODELS.find { it.first == state.modelId }?.second ?: state.modelId,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false },
                ) {
                    GEMINI_MODELS.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                vm.saveModel(id)
                                modelExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Set as home
            Text(stringResource(R.string.set_as_home), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.set_as_home_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val roleManager = context.getSystemService(RoleManager::class.java)
                        if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                            roleRequestLauncher?.launch(
                                roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                            )
                        }
                    } else {
                        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.set_as_home))
            }

            Spacer(Modifier.height(4.dp))

            // Memory
            Text(stringResource(R.string.remember_facts_label), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.facts_count, state.facts.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.clear_memory))
            }

            Spacer(Modifier.height(4.dp))

            // Accessibility service
            val accessibilityEnabled =
                com.nuevoso.launcher.accessibility.NuevoSOAccessibilityService.isEnabled()
            Text(stringResource(R.string.accessibility_enable), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.accessibility_enable_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (accessibilityEnabled) {
                Text(
                    stringResource(R.string.accessibility_enabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Button(
                    onClick = {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.accessibility_enable))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_memory)) },
            text = { Text(stringResource(R.string.clear_memory_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearMemory()
                    showClearDialog = false
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
