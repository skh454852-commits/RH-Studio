package com.example.ui.create

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AspectCropPreset
import com.example.data.MediaProcessingEngine
import com.example.data.PhotoEditConfig
import com.example.data.PhotoFilterPreset
import com.example.ui.components.InlineExoVideoPlayer
import com.example.ui.components.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CreateStudioScreen(
    onPublishPost: (
        mediaType: String,
        title: String,
        caption: String,
        hashtagsRaw: String,
        location: String,
        mediaPreviewBase64: String,
        localMediaPath: String,
        mimeType: String,
        durationSeconds: Long,
        trimStartMs: Long,
        trimEndMs: Long,
        copyrightConfirmed: Boolean,
        onDone: () -> Unit
    ) -> Unit,
    onPostSuccessNavigateHome: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Photo", "Video", "Music & Audio")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label) }
                )
            }
        }
        when (selectedTab) {
            0 -> PhotoCreatorTab(onPublishPost, onPostSuccessNavigateHome)
            1 -> VideoCreatorTab(onPublishPost, onPostSuccessNavigateHome)
            else -> AudioCreatorTab(onPublishPost, onPostSuccessNavigateHome)
        }
    }
}

@Composable
private fun PhotoCreatorTab(
    onPublishPost: (String, String, String, String, String, String, String, String, Long, Long, Long, Boolean, () -> Unit) -> Unit,
    onPostSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var activeIndex by remember { mutableIntStateOf(0) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editConfig by remember { mutableStateOf(PhotoEditConfig()) }

    var caption by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("photography, creator") }
    var location by remember { mutableStateOf("") }
    var showCameraDialog by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var permissionNotice by remember { mutableStateOf<String?>(null) }

    val multiPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            activeIndex = 0
            editConfig = PhotoEditConfig()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionNotice = null
            showCameraDialog = true
        } else {
            permissionNotice = "Camera permission is needed to take photos directly."
        }
    }

    LaunchedEffect(selectedUris, activeIndex) {
        val uri = selectedUris.getOrNull(activeIndex)
        sourceBitmap = if (uri != null) {
            MediaProcessingEngine.decodeSampledBitmap(context, uri, 960)
        } else null
    }

    LaunchedEffect(sourceBitmap, editConfig) {
        val src = sourceBitmap
        editedBitmap = if (src != null) {
            MediaProcessingEngine.applyPhotoEdits(src, editConfig)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    multiPhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_photos_btn")
            ) {
                Icon(Icons.Rounded.PhotoLibrary, null)
                Spacer(Modifier.width(6.dp))
                Text("Gallery")
            }
            OutlinedButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) showCameraDialog = true
                    else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("camera_photo_btn")
            ) {
                Icon(Icons.Rounded.CameraAlt, null)
                Spacer(Modifier.width(6.dp))
                Text("Camera")
            }
        }

        if (permissionNotice != null) {
            Text(permissionNotice!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (selectedUris.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedUris.indices.toList()) { idx ->
                    FilterChip(
                        selected = idx == activeIndex,
                        onClick = { activeIndex = idx },
                        label = { Text("Photo ${idx + 1}") }
                    )
                }
            }
        }

        if (editedBitmap != null) {
            Image(
                bitmap = editedBitmap!!.asImageBitmap(),
                contentDescription = "Edited photo preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.08f))
            )

            // Rotate & Crop bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        editConfig = editConfig.copy(rotationDegrees = (editConfig.rotationDegrees + 90) % 360)
                    }
                ) {
                    Icon(Icons.AutoMirrored.Rounded.RotateRight, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Rotate")
                }
                Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AspectCropPreset.entries) { preset ->
                        FilterChip(
                            selected = editConfig.cropPreset == preset,
                            onClick = { editConfig = editConfig.copy(cropPreset = preset) },
                            label = { Text(preset.label) }
                        )
                    }
                }
            }

            // Filter presets
            Text("Filters", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PhotoFilterPreset.entries) { filter ->
                    FilterChip(
                        selected = editConfig.filterPreset == filter,
                        onClick = { editConfig = editConfig.copy(filterPreset = filter) },
                        label = { Text(filter.label) }
                    )
                }
            }

            // Sliders
            Text("Brightness", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = editConfig.brightness,
                onValueChange = { editConfig = editConfig.copy(brightness = it) },
                valueRange = -80f..80f
            )
            Text("Contrast", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = editConfig.contrast,
                onValueChange = { editConfig = editConfig.copy(contrast = it) },
                valueRange = 0.5f..1.6f
            )
            Text("Saturation", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = editConfig.saturation,
                onValueChange = { editConfig = editConfig.copy(saturation = it) },
                valueRange = 0f..2f
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Select JPG, PNG, or WebP photos or capture with CameraX", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it.take(1000) },
            label = { Text("Write a caption...") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = hashtags,
            onValueChange = { hashtags = it },
            label = { Text("Hashtags (comma separated)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = location,
            onValueChange = { location = it.take(100) },
            label = { Text("Location (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val bmp = editedBitmap ?: return@Button
                isPublishing = true
                scope.launch {
                    val b64 = MediaProcessingEngine.bitmapToCompressedBase64(bmp, 720, 74)
                    val path = MediaProcessingEngine.saveBitmapToPrivateFile(
                        context,
                        bmp,
                        "photo_${System.currentTimeMillis()}.jpg"
                    )
                    onPublishPost(
                        "photo",
                        "",
                        caption.ifBlank { "Shared a new photo" },
                        hashtags,
                        location,
                        b64,
                        path,
                        "image/jpeg",
                        0L,
                        0L,
                        0L,
                        true
                    ) {
                        isPublishing = false
                        onPostSuccess()
                    }
                }
            },
            enabled = editedBitmap != null && !isPublishing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("publish_photo_btn")
        ) {
            if (isPublishing) {
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Publish Photo Post")
            }
        }
    }

    if (showCameraDialog) {
        CameraXPhotoCaptureDialog(
            onDismiss = { showCameraDialog = false },
            onPhotoCaptured = { uri ->
                selectedUris = listOf(uri) + selectedUris
                activeIndex = 0
                showCameraDialog = false
            }
        )
    }
}

@Composable
private fun VideoCreatorTab(
    onPublishPost: (String, String, String, String, String, String, String, String, Long, Long, Long, Boolean, () -> Unit) -> Unit,
    onPostSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var trimRange by remember { mutableStateOf(0f..1f) }
    var thumbTimeFrac by remember { mutableFloatStateOf(0.1f) }
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("video, creator") }
    var showVideoCamera by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var permissionNotice by remember { mutableStateOf<String?>(null) }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) videoUri = uri
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val camOk = grants[Manifest.permission.CAMERA] == true
        val micOk = grants[Manifest.permission.RECORD_AUDIO] == true
        if (camOk && micOk) {
            permissionNotice = null
            showVideoCamera = true
        } else {
            permissionNotice = "Camera & Microphone permissions are required to record video."
        }
    }

    LaunchedEffect(videoUri, thumbTimeFrac) {
        val uri = videoUri ?: return@LaunchedEffect
        val targetMs = (durationMs * thumbTimeFrac).toLong().coerceAtLeast(300L)
        val (dur, bmp) = MediaProcessingEngine.extractVideoMetadata(context, uri, targetMs)
        if (dur > 0L) durationMs = dur
        thumbnailBitmap = bmp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    videoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.VideoLibrary, null)
                Spacer(Modifier.width(6.dp))
                Text("Pick MP4 Video")
            }
            OutlinedButton(
                onClick = {
                    val cam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (cam && mic) showVideoCamera = true
                    else permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Videocam, null)
                Spacer(Modifier.width(6.dp))
                Text("Record Video")
            }
        }

        if (permissionNotice != null) {
            Text(permissionNotice!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (videoUri != null) {
            val startMs = (durationMs * trimRange.start).toLong()
            val endMs = (durationMs * trimRange.endInclusive).toLong()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
            ) {
                InlineExoVideoPlayer(
                    uri = videoUri!!,
                    trimStartMs = startMs,
                    trimEndMs = endMs
                )
            }

            Text(
                text = "Trim Video: ${formatDuration(startMs / 1000L)} – ${formatDuration(endMs / 1000L)}",
                style = MaterialTheme.typography.labelMedium
            )
            RangeSlider(
                value = trimRange,
                onValueChange = { trimRange = it }
            )

            Text("Select Video Thumbnail Frame", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = "Selected video thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 88.dp, height = 56.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
                Slider(
                    value = thumbTimeFrac,
                    onValueChange = { thumbTimeFrac = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(120) },
            label = { Text("Video title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it.take(1000) },
            label = { Text("Description & caption") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = hashtags,
            onValueChange = { hashtags = it },
            label = { Text("Hashtags") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (isUploading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = {
                val uri = videoUri ?: return@Button
                isUploading = true
                scope.launch {
                    val savedPath = MediaProcessingEngine.copyUriToPrivateMedia(context, uri, "videos", "mp4").orEmpty()
                    val thumbB64 = thumbnailBitmap?.let {
                        MediaProcessingEngine.bitmapToCompressedBase64(it, 640, 70)
                    }.orEmpty()
                    val startMs = (durationMs * trimRange.start).toLong()
                    val endMs = (durationMs * trimRange.endInclusive).toLong()
                    val effectiveSec = ((endMs - startMs).coerceAtLeast(1000L)) / 1000L
                    onPublishPost(
                        "video",
                        title.ifBlank { "Creator Video" },
                        caption.ifBlank { "Check out my latest video!" },
                        hashtags,
                        "",
                        thumbB64,
                        savedPath,
                        "video/mp4",
                        effectiveSec,
                        startMs,
                        endMs,
                        true
                    ) {
                        isUploading = false
                        onPostSuccess()
                    }
                }
            },
            enabled = videoUri != null && !isUploading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(if (isUploading) "Uploading Video..." else "Publish Video")
        }
    }

    if (showVideoCamera) {
        CameraXVideoCaptureDialog(
            onDismiss = { showVideoCamera = false },
            onVideoRecorded = { uri ->
                videoUri = uri
                showVideoCamera = false
            }
        )
    }
}

@Composable
private fun AudioCreatorTab(
    onPublishPost: (String, String, String, String, String, String, String, String, Long, Long, Long, Boolean, () -> Unit) -> Unit,
    onPostSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var audioType by remember { mutableStateOf("music") }
    var durationSec by remember { mutableLongStateOf(0L) }
    var coverBase64 by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("music, original") }
    var copyrightConfirmed by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }

    // Microphone recorder state
    var isRecording by remember { mutableStateOf(false) }
    var recordedSeconds by remember { mutableLongStateOf(0L) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var micNotice by remember { mutableStateOf<String?>(null) }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            audioUri = uri
            audioType = "music"
            scope.launch {
                durationSec = MediaProcessingEngine.extractAudioDurationSeconds(context, uri)
            }
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = MediaProcessingEngine.decodeSampledBitmap(context, uri, 600)
                if (bmp != null) {
                    coverBase64 = MediaProcessingEngine.bitmapToCompressedBase64(bmp, 600, 74)
                }
            }
        }
    }

    fun startRecording() {
        val dir = File(context.filesDir, "media/audio").apply { mkdirs() }
        val file = File(dir, "rec_${System.currentTimeMillis()}.m4a")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            recordingFile = file
            recordedSeconds = 0L
            isRecording = true
            micNotice = null
        } catch (e: Exception) {
            micNotice = "Unable to start microphone: ${e.localizedMessage}"
            try { mr.release() } catch (_: Exception) {}
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        isRecording = false
        recordingFile?.let { f ->
            if (f.exists()) {
                audioUri = Uri.fromFile(f)
                audioType = "audio"
                durationSec = recordedSeconds.coerceAtLeast(1L)
            }
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
        else micNotice = "Microphone permission is required to record audio."
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000L)
            recordedSeconds += 1L
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.release()
            } catch (_: Exception) {
            }
        }
    }

    val pulse = rememberInfiniteTransition(label = "micPulse")
    val barScale by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "barScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { audioPicker.launch(arrayOf("audio/mpeg", "audio/wav", "audio/mp4", "audio/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.AudioFile, null)
                Spacer(Modifier.width(6.dp))
                Text("MP3 / WAV / M4A")
            }
            OutlinedButton(
                onClick = {
                    if (isRecording) {
                        stopRecording()
                    } else {
                        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                        if (ok) startRecording()
                        else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic, null)
                Spacer(Modifier.width(6.dp))
                Text(if (isRecording) "Stop (${formatDuration(recordedSeconds)})" else "Record Mic")
            }
        }

        if (isRecording) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FiberManualRecord, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Recording live audio: ${formatDuration(recordedSeconds)}", style = MaterialTheme.typography.titleSmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(0.6f, 1.0f, 0.7f, 0.9f, 0.5f).forEach { factor ->
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height((24 * factor * barScale).dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }
                }
            }
        }

        if (micNotice != null) {
            Text(micNotice!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (audioUri != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "${if (audioType == "music") "Music File" else "Mic Recording"} ready (${formatDuration(durationSec)})",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.MusicNote, null)
            Spacer(Modifier.width(6.dp))
            Text(if (coverBase64.isBlank()) "Select Cover Art (Optional)" else "Cover Art Selected ✓")
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(120) },
            label = { Text("Track / Audio Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it.take(1000) },
            label = { Text("Description & credits") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = hashtags,
            onValueChange = { hashtags = it },
            label = { Text("Hashtags") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = copyrightConfirmed,
                onCheckedChange = { copyrightConfirmed = it }
            )
            Text(
                text = "I confirm that I own or hold valid distribution rights for this audio and accept copyright moderation rules.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                val uri = audioUri ?: return@Button
                isPublishing = true
                scope.launch {
                    val path = if (uri.scheme == "file") {
                        uri.path.orEmpty()
                    } else {
                        MediaProcessingEngine.copyUriToPrivateMedia(context, uri, "audio", "m4a").orEmpty()
                    }
                    onPublishPost(
                        audioType,
                        title.ifBlank { "Original Audio Track" },
                        caption.ifBlank { "Listen to my latest audio release." },
                        hashtags,
                        "",
                        coverBase64,
                        path,
                        "audio/mp4",
                        durationSec.coerceAtLeast(1L),
                        0L,
                        0L,
                        copyrightConfirmed
                    ) {
                        isPublishing = false
                        onPostSuccess()
                    }
                }
            },
            enabled = audioUri != null && copyrightConfirmed && !isPublishing && !isRecording,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(if (isPublishing) "Publishing Audio..." else "Publish Music / Audio")
        }
    }
}

@Composable
private fun CameraXPhotoCaptureDialog(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx) },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(context))
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close camera", tint = Color.White)
            }
            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Rounded.Cameraswitch, contentDescription = "Switch camera", tint = Color.White)
            }
            FilledIconButton(
                onClick = {
                    val outFile = File(context.cacheDir, "cam_photo_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onPhotoCaptured(Uri.fromFile(outFile))
                            }

                            override fun onError(exception: ImageCaptureException) {
                                onDismiss()
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = "Capture photo", tint = Color.Black)
            }
        }
    }
}

@Composable
private fun CameraXVideoCaptureDialog(
    onDismiss: () -> Unit,
    onVideoRecorded: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }

    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.SD))
            .build()
    }
    val videoCapture = remember(recorder) { VideoCapture.withOutput(recorder) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    videoCapture
                                )
                            } catch (_: Exception) {
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = {
                    activeRecording?.stop()
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close video camera", tint = Color.White)
            }
            FilledIconButton(
                onClick = {
                    if (isRecording) {
                        activeRecording?.stop()
                    } else {
                        val outFile = File(context.cacheDir, "cam_vid_${System.currentTimeMillis()}.mp4")
                        val options = FileOutputOptions.Builder(outFile).build()
                        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                        var pending = videoCapture.output.prepareRecording(context, options)
                        if (hasMic) {
                            pending = pending.withAudioEnabled()
                        }
                        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                            when (event) {
                                is VideoRecordEvent.Start -> isRecording = true
                                is VideoRecordEvent.Finalize -> {
                                    isRecording = false
                                    if (!event.hasError()) {
                                        onVideoRecorded(Uri.fromFile(outFile))
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isRecording) Color.Red else Color.White
                )
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord,
                    contentDescription = if (isRecording) "Stop recording" else "Start recording",
                    tint = if (isRecording) Color.White else Color.Red
                )
            }
        }
    }
}
