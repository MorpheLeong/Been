package com.bugchecking.app

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal fun clipboardMapShareCandidate(text: CharSequence?): String? = text
    ?.toString()
    ?.trim()
    ?.takeIf { it.isNotEmpty() && PlaceLinkParser.firstUrl(it) != null }

@Composable
internal fun ClipboardMapLinkPrompt(
    promptKey: String,
    ignoredText: String? = null,
    onRecognize: (String) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    var pendingText by rememberSaveable(promptKey) { mutableStateOf<String?>(null) }
    var editorText by rememberSaveable(promptKey) { mutableStateOf("") }
    var lastSeenClipboardToken by rememberSaveable(promptKey) { mutableStateOf<String?>(null) }
    var ignoreInitialTextOnce by rememberSaveable(promptKey) { mutableStateOf(!ignoredText.isNullOrBlank()) }

    DisposableEffect(clipboard, lifecycleOwner, view, promptKey) {
        fun inspectClipboard() {
            if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
            val item = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0) ?: return
            val candidate = clipboardMapShareCandidate(item.coerceToText(context)) ?: return
            val timestamp = clipboard.primaryClipDescription?.timestamp ?: 0L
            val token = "$timestamp:${candidate.hashCode()}"
            if (ignoreInitialTextOnce) {
                ignoreInitialTextOnce = false
                if (candidate == ignoredText?.trim()) {
                    lastSeenClipboardToken = token
                    return
                }
            }
            if (token == lastSeenClipboardToken) return
            lastSeenClipboardToken = token
            editorText = candidate
            pendingText = candidate
        }

        val clipListener = ClipboardManager.OnPrimaryClipChangedListener(::inspectClipboard)
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) inspectClipboard()
        }
        val windowFocusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) inspectClipboard()
        }
        clipboard.addPrimaryClipChangedListener(clipListener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        view.viewTreeObserver.addOnWindowFocusChangeListener(windowFocusListener)
        inspectClipboard()
        onDispose {
            clipboard.removePrimaryClipChangedListener(clipListener)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnWindowFocusChangeListener(windowFocusListener)
            }
        }
    }

    if (pendingText != null) {
        AlertDialog(
            onDismissRequest = { pendingText = null },
            title = { Text("从地图分享内容识别") },
            text = {
                OutlinedTextField(
                    value = editorText,
                    onValueChange = { editorText = it },
                    label = { Text("地图分享内容") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingText = null }) { Text("取消") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val content = editorText
                        pendingText = null
                        onRecognize(content)
                    },
                    enabled = editorText.isNotBlank(),
                ) { Text("识别名称") }
            },
        )
    }
}
