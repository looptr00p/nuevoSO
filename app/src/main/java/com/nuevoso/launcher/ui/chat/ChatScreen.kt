package com.nuevoso.launcher.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nuevoso.launcher.R
import com.nuevoso.launcher.agent.security.ApprovalPrompt
import com.nuevoso.launcher.ui.theme.Newsreader
import com.nuevoso.launcher.ui.theme.SolBackground
import com.nuevoso.launcher.ui.theme.SolSurface
import com.nuevoso.launcher.ui.theme.SolTerracotta
import com.nuevoso.launcher.ui.theme.SolTextDark
import com.nuevoso.launcher.ui.theme.SolTextFaint
import com.nuevoso.launcher.ui.theme.SolTextSoft
import kotlinx.coroutines.launch
import java.util.Calendar

private val SUGGESTION_CHIPS = listOf(
    "¿Qué tengo hoy?",
    "Abre Spotify",
    "Recuérdame algo",
    "Busca en la web",
)

@Composable
fun ChatScreen(
    onNavigateToDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    vm: ChatViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    val isConversationActive = state.messages.isNotEmpty() || state.isThinking
    val orbState = when {
        state.isThinking && state.streamingText == null -> OrbState.Thinking
        state.streamingText != null                     -> OrbState.Speaking
        else                                            -> OrbState.Idle
    }

    LaunchedEffect(state.messages.size, state.streamingText, state.isThinking) {
        val lastIndex = state.messages.size
        if (lastIndex > 0 || state.streamingText != null || state.isThinking) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { err ->
            scope.launch {
                snackbarHostState.showSnackbar(err)
                vm.dismissError()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SolBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            // Main content: crossfade between idle hero and conversation thread
            AnimatedContent(
                targetState = isConversationActive,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "chat-content",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { isActive ->
                if (!isActive) {
                    IdleContent(
                        orbState = orbState,
                        onChipSelected = {
                            vm.sendMessage(it)
                            keyboard?.hide()
                        },
                    )
                } else {
                    ConversationContent(
                        state = state,
                        listState = listState,
                        orbState = orbState,
                        onApprove = vm::approvePendingConfirmation,
                        onReject = vm::rejectPendingConfirmation,
                    )
                }
            }

            ComposerRow(
                input = input,
                onInputChange = { input = it },
                onSend = {
                    if (input.isNotBlank()) {
                        vm.sendMessage(input)
                        input = ""
                        keyboard?.hide()
                    }
                },
                enabled = state.pendingConfirmation == null,
                modifier = Modifier.fillMaxWidth(),
            )

            DockNav(
                currentDestination = DockDestination.Home,
                onDestinationSelected = { dest ->
                    when (dest) {
                        DockDestination.Apps        -> onNavigateToDrawer()
                        DockDestination.Settings    -> onNavigateToSettings()
                        DockDestination.Conversation,
                        DockDestination.Home        -> { /* already on home */ }
                    }
                },
                isOrbActive = orbState != OrbState.Idle,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 160.dp),
        )
    }
}

// ── Idle hero ─────────────────────────────────────────────────────────────

@Composable
private fun IdleContent(
    orbState: OrbState,
    onChipSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))
        GreetingHeader()
        Spacer(Modifier.height(40.dp))
        SolOrb(state = orbState, sizeDp = 136.dp)
        Spacer(Modifier.height(32.dp))
        SuggestionChipsRow(onChipSelected = onChipSelected)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun GreetingHeader() {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when {
        hour < 12 -> "Buenos días"
        hour < 19 -> "Buenas tardes"
        else      -> "Buenas noches"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = greeting.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = SolTextFaint,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Sol",
            style = MaterialTheme.typography.headlineMedium,
            color = SolTextDark,
        )
    }
}

@Composable
private fun SuggestionChipsRow(onChipSelected: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(SUGGESTION_CHIPS) { suggestion ->
            SuggestionChip(
                onClick = { onChipSelected(suggestion) },
                label = {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.labelMedium,
                        color = SolTextSoft,
                    )
                },
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, SolTextFaint),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = SolSurface,
                ),
            )
        }
    }
}

// ── Conversation ──────────────────────────────────────────────────────────

@Composable
private fun ConversationContent(
    state: ChatUiState,
    listState: LazyListState,
    orbState: OrbState,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SolOrb(state = orbState, sizeDp = 40.dp)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages, key = { it.id }) { msg ->
                MessageBubble(msg)
            }
            state.streamingText?.let { streaming ->
                item {
                    MessageBubble(ChatMessage(role = "assistant", text = streaming))
                }
            }
            if (state.isThinking && state.streamingText == null) {
                item { ThinkingIndicator() }
            }
        }

        state.pendingConfirmation?.let { prompt ->
            ConfirmationPanel(
                prompt = prompt,
                onApprove = onApprove,
                onReject = onReject,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

// ── Composer ──────────────────────────────────────────────────────────────

@Composable
private fun ComposerRow(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(999.dp),
        color = SolSurface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = SolTextDark),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isNotBlank() && enabled) onSend()
                }),
                enabled = enabled,
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box {
                        if (input.isEmpty()) {
                            Text(
                                text = stringResource(R.string.chat_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = SolTextFaint,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voz",
                    tint = SolTextSoft,
                )
            }
            IconButton(
                onClick = onSend,
                enabled = input.isNotBlank() && enabled,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                    tint = if (input.isNotBlank() && enabled) SolTerracotta else SolTextFaint,
                )
            }
        }
    }
}

// ── Thinking dots ─────────────────────────────────────────────────────────

@Composable
private fun ThinkingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            ThinkingDot(delayMillis = index * 200)
        }
    }
}

@Composable
private fun ThinkingDot(delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "thinking-dot")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot-alpha",
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(SolTerracotta.copy(alpha = alpha)),
    )
}

// ── Confirmation panel ────────────────────────────────────────────────────

@Composable
private fun ConfirmationPanel(
    prompt: ApprovalPrompt,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = SolSurface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, SolTextFaint.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 156.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.confirmation_title),
                style = MaterialTheme.typography.titleSmall,
                color = SolTextDark,
            )
            ConfirmationField(
                label = stringResource(R.string.confirmation_tool),
                value = prompt.toolName,
            )
            ConfirmationField(
                label = stringResource(R.string.confirmation_risk),
                value = prompt.riskLevel.name,
            )
            ConfirmationField(
                label = stringResource(R.string.confirmation_summary),
                value = prompt.sanitizedSummary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onReject,
                    border = BorderStroke(1.dp, SolTextFaint),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SolTextSoft),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.confirmation_reject))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onApprove) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.confirmation_approve))
                }
            }
        }
    }
}

@Composable
private fun ConfirmationField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SolTextFaint,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = SolTextSoft,
        )
    }
}

// ── Message bubble ────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = screenWidth * 0.80f)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp,
                    )
                )
                .background(if (isUser) SolTerracotta else SolSurface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = msg.text,
                style = if (isUser) {
                    MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                } else {
                    TextStyle(
                        fontFamily = Newsreader,
                        fontWeight = FontWeight.Normal,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = SolTextDark,
                    )
                },
            )
        }
    }
}
