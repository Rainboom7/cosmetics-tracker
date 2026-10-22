package io.github.rainboom7.cosmetics.ui.about

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rainboom7.cosmetics.BuildConfig
import io.github.rainboom7.cosmetics.CosmeticsApp
import io.github.rainboom7.cosmetics.R
import io.github.rainboom7.cosmetics.core.collection.CollectionBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** "О приложении": data source attribution (required by ODbL / CC BY-SA), backups, notifications. */
@Composable
fun AboutRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as CosmeticsApp).container }
    val scope = rememberCoroutineScope()

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val json = CollectionBackup.export(container.collection.observeAll().first(), container.usageLog.observeAll().first())
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) }
                }
            }.isSuccess
            Toast.makeText(context, if (ok) R.string.backup_saved else R.string.backup_failed, Toast.LENGTH_SHORT).show()
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                }
                CollectionBackup.restore(text, container.repository, container.usageLog)
            }.isSuccess
            Toast.makeText(context, if (ok) R.string.backup_restored else R.string.backup_failed, Toast.LENGTH_SHORT).show()
        }
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.app_name) + " " + BuildConfig.VERSION_NAME, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.about_data_source))
            Text(stringResource(R.string.about_photos_license), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.about_on_device), style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.backup_body), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { export.launch("cosmetics-backup.json") }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.backup_export)) }
            OutlinedButton(onClick = { restore.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.backup_import)) }
            if (Build.VERSION.SDK_INT >= 33) {
                HorizontalDivider()
                Text(stringResource(R.string.notifications_body), style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { notifications.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.notifications_enable))
                }
            }
        }
    }
}
