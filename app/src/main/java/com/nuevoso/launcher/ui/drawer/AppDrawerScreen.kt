package com.nuevoso.launcher.ui.drawer

import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nuevoso.launcher.R
import com.nuevoso.launcher.data.apps.AppInfo
import com.nuevoso.launcher.ui.chat.DockDestination
import com.nuevoso.launcher.ui.chat.DockNav
import com.nuevoso.launcher.ui.theme.SolBackground
import com.nuevoso.launcher.ui.theme.SolSurface
import com.nuevoso.launcher.ui.theme.SolTextDark
import com.nuevoso.launcher.ui.theme.SolTextFaint
import com.nuevoso.launcher.ui.theme.SolTextSoft

@Composable
fun AppDrawerScreen(onBack: () -> Unit = {}, vm: AppDrawerViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SolBackground)
            .statusBarsPadding(),
    ) {
        // Search pill
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(999.dp),
            color = SolSurface,
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = SolTextFaint,
                    modifier = Modifier.size(20.dp),
                )
                BasicTextField(
                    value = state.query,
                    onValueChange = vm::onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = SolTextDark),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (state.query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_apps),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = SolTextFaint,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }

        // App grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.filtered, key = { it.packageName }) { app ->
                AppCell(app) {
                    val intent = context.packageManager
                        .getLaunchIntentForPackage(app.packageName)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent?.let { context.startActivity(it) }
                }
            }
        }

        // Dock — Home tap goes back to ChatScreen
        DockNav(
            currentDestination = DockDestination.Apps,
            onDestinationSelected = { dest ->
                when (dest) {
                    DockDestination.Home,
                    DockDestination.Conversation,
                    DockDestination.Settings -> onBack()
                    DockDestination.Apps     -> { /* already here */ }
                }
            },
        )
    }
}

@Composable
private fun AppCell(app: AppInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val bitmap = remember(app.packageName) {
            if (app.icon is BitmapDrawable) {
                (app.icon as BitmapDrawable).bitmap
            } else {
                val bmp = android.graphics.Bitmap.createBitmap(
                    app.icon.intrinsicWidth.coerceAtLeast(1),
                    app.icon.intrinsicHeight.coerceAtLeast(1),
                    android.graphics.Bitmap.Config.ARGB_8888,
                )
                app.icon.setBounds(0, 0, bmp.width, bmp.height)
                app.icon.draw(Canvas(bmp))
                bmp
            }
        }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = app.label,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = SolTextSoft,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
