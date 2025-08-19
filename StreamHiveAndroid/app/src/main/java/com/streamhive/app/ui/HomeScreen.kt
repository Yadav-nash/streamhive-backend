package com.streamhive.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.streamhive.app.net.ApiClient
import com.streamhive.app.net.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@Composable
fun HomeScreen(nav: NavController) {
    val filesState = remember { mutableStateOf<List<FileItem>>(emptyList()) }
    val loading = remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading.value = true
        val result = ApiClient.api.listFiles()
        filesState.value = result.files
        loading.value = false
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    loading.value = true
                    val meta = ApiClient.api.requestUpload(mapOf(
                        "name" to (uri.lastPathSegment ?: "video.mp4"),
                        "size" to 0,
                        "mime" to "video/mp4"
                    ))
                    val tempFile = withContext(Dispatchers.IO) {
                        val input = ApiClient.appContext.contentResolver.openInputStream(uri)!!
                        val tmp = File.createTempFile("upload", ".bin", ApiClient.appContext.cacheDir)
                        tmp.outputStream().use { out -> input.copyTo(out) }
                        tmp
                    }
                    val body = tempFile.asRequestBody("application/octet-stream".toMediaType())
                    ApiClient.putPresigned(meta.uploadUrl, body)
                    ApiClient.api.commitUpload(mapOf("uploadId" to meta.uploadId))
                    val refreshed = ApiClient.api.listFiles()
                    filesState.value = refreshed.files
                } catch (e: Exception) {
                    snack.showSnackbar("Upload failed: ${e.message}")
                } finally {
                    loading.value = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SnackbarHost(hostState = snack)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("StreamHive", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { picker.launch("video/*") }) { Text("Upload") }
        }
        Spacer(Modifier.height(12.dp))
        if (loading.value) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
            }
        } else {
            FileList(filesState, nav)
        }
    }
}

@Composable
private fun FileList(filesState: MutableState<List<FileItem>>, nav: NavController) {
    LazyColumn {
        items(filesState.value) { item ->
            Row(modifier = Modifier.fillMaxWidth().clickable { nav.navigate("player/${Uri.encode(item.key)}") }.padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${item.size} bytes")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val urlIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://streamhive.app/watch?key=${Uri.encode(item.key)}")
                        }
                        nav.context.startActivity(Intent.createChooser(urlIntent, "Share video"))
                    }) { Text("Share") }
                }
            }
        }
    }
}

