package com.bugchecking.app

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private data class PendingBackupImport(
    val uri: Uri,
    val inspection: BackupInspection,
)

@Composable
fun BackupSettingsSection(onDataImported: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var preferences by remember { mutableStateOf(AppDataBackup.preferences(context)) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<PendingBackupImport?>(null) }

    fun refresh() {
        preferences = AppDataBackup.preferences(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { AppDataBackup.setFolder(context, uri) }
                .onSuccess {
                    refresh()
                    message = "已启用分层自动备份"
                }
                .onFailure { message = it.message ?: "无法使用这个文件夹" }
        }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            message = null
            runCatching { AppDataBackup.inspect(context, uri) }
                .onSuccess { pendingImport = PendingBackupImport(uri, it) }
                .onFailure { message = it.message ?: "无法读取备份文件" }
            busy = false
        }
    }

    Text("数据备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Card(
        shape = PassportTokens.shapes.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = primaryPageCardColor(),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(PassportTokens.content.cardHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(PassportSpacing.md),
        ) {
            Text(
                "备份包含资料、设备设置、清单、二维码、地点库及全部轨迹。备份含精确轨迹与二维码，请妥善保管。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PassportSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("备份文件夹", style = MaterialTheme.typography.titleSmall)
                    Text(
                        preferences.folderLabel ?: "尚未选择",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { folderPicker.launch(null) },
                    enabled = !busy,
                    shape = PassportTokens.shapes.buttonShape,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                ) {
                    Text(if (preferences.folderUri == null) "选择" else "更改")
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("自动保护数据", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "数据变化后轮换最新与上一份快照；每日另存一份，导入恢复前自动保留关键节点",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = preferences.automaticEnabled,
                    enabled = preferences.folderUri != null && !busy,
                    onCheckedChange = { enabled ->
                        runCatching { AppDataBackup.setAutomaticEnabled(context, enabled) }
                            .onSuccess {
                                refresh()
                                message = if (enabled) "已开启分层自动备份" else "已关闭自动备份"
                            }
                            .onFailure { message = it.message ?: "无法更新自动备份设置" }
                    },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (preferences.lastSuccessAt > 0L) {
                        "上次成功${preferences.lastKind?.let { kind ->
                            "（${kind.displayName}${preferences.lastReason?.let { " · ${it.displayName}" }.orEmpty()}）"
                        }.orEmpty()}：" +
                            formatBackupTime(preferences.lastSuccessAt)
                    } else {
                        "尚无成功备份"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                preferences.lastFileName?.let { fileName ->
                    Text(
                        fileName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                preferences.lastError?.let { error ->
                    Text("上次失败：$error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                message?.let { value ->
                    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PassportSpacing.sm)) {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            message = null
                            runCatching { AppDataBackup.backupNow(context, BackupKind.MANUAL) }
                                .onSuccess {
                                    message = "已写入同步文件夹：${it.fileName}"
                                    refresh()
                                }
                                .onFailure {
                                    message = it.message ?: "备份失败"
                                    refresh()
                                }
                            busy = false
                        }
                    },
                    enabled = preferences.folderUri != null && !busy,
                    modifier = Modifier.weight(1f),
                    shape = PassportTokens.shapes.buttonShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) { Text(if (busy) "处理中…" else "立即备份") }
                OutlinedButton(
                    onClick = { importPicker.launch(arrayOf("application/zip", "application/octet-stream")) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = PassportTokens.shapes.buttonShape,
                ) { Text("导入备份") }
            }
        }
    }

    pendingImport?.let { pending ->
        val summary = pending.inspection
        AlertDialog(
            onDismissRequest = { if (!busy) pendingImport = null },
            title = { Text("导入并替换本机数据？") },
            text = {
                Text(
                    "备份时间：${formatBackupTime(summary.createdAt)}\n" +
                        "类型：${summary.kind?.displayName ?: "兼容旧备份"}" +
                        summary.reason?.let { " · ${it.displayName}" }.orEmpty() + "\n" +
                        "清单 ${summary.destinations} 项 · 二维码 ${summary.qrs} 个 · 场所 ${summary.places} 个 · 轨迹 ${summary.tracks} 条" +
                        (if (summary.hasAvatar) "\n包含头像" else "\n不包含头像") +
                        if (summary.checksumVerified) " · 完整性校验通过" else " · 旧格式无校验值",
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    shape = PassportTokens.shapes.buttonShape,
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching { AppDataBackup.restore(context, pending.uri) }
                                .onSuccess {
                                    pendingImport = null
                                    onDataImported()
                                    refresh()
                                    message = "备份已导入"
                                    Toast.makeText(context, "备份已导入", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure { message = it.message ?: "导入失败" }
                            busy = false
                        }
                    },
                ) { Text(if (busy) "正在导入…" else "替换并导入") }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingImport = null },
                    enabled = !busy,
                    shape = PassportTokens.shapes.buttonShape,
                ) { Text("取消") }
            },
        )
    }
}

private fun formatBackupTime(value: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(value))
