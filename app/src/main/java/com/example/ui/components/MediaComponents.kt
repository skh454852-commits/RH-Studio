package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.AudioPlaybackState
import com.example.data.MediaProcessingEngine
import com.example.model.AdminSettings
import com.example.model.SocialPost
import java.io.File

@Composable
fun Base64Image(
    base64: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackResId: Int? = null
) {
    val bitmap = remember(base64) {
        MediaProcessingEngine.decodeBase64ToBitmap(base64)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else if (fallbackResId != null) {
        Image(
            painter = painterResource(id = fallbackResId),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            )
        )
    }
}

@Composable
fun AvatarCircle(
    name: String,
    avatarBase64: String,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(avatarBase64) {
        MediaProcessingEngine.decodeBase64ToBitmap(avatarBase64)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "$name avatar",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: "P",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostCard(
    post: SocialPost,
    currentUserId: String,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowingAuthor: Boolean,
    isAuthorVerified: Boolean,
    isPlayingThisAudio: Boolean,
    onAuthorClick: () -> Unit,
    onFollowClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onPlayAudioClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: (String) -> Unit,
    onBlockClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var playInlineVideo by remember { mutableStateOf(false) }
    val isMine = post.authorId == currentUserId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAuthorClick)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarCircle(
                        name = post.authorName,
                        avatarBase64 = post.authorAvatarBase64,
                        size = 42.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName.ifBlank { "Creator" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isAuthorVerified) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "Verified creator",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "@${post.authorUsername}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (post.location.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = "Location",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = post.location,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (!isMine) {
                    OutlinedButton(
                        onClick = onFollowClick,
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = if (isFollowingAuthor) "Following" else "Follow",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Post options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (isMine) {
                            DropdownMenuItem(
                                text = { Text("Edit Post") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Post") },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report Content") },
                                onClick = {
                                    menuExpanded = false
                                    onReportClick("post")
                                }
                            )
                            if (post.mediaType == "music" || post.mediaType == "audio") {
                                DropdownMenuItem(
                                    text = { Text("Report Copyright Issue") },
                                    onClick = {
                                        menuExpanded = false
                                        onReportClick("copyright")
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Block @${post.authorUsername}") },
                                onClick = {
                                    menuExpanded = false
                                    onBlockClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Media Area
            when (post.mediaType) {
                "photo" -> {
                    if (post.mediaPreviewBase64.isNotBlank()) {
                        Base64Image(
                            base64 = post.mediaPreviewBase64,
                            contentDescription = post.caption.ifBlank { "Photo post" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
                "video" -> {
                    val localFile = remember(post.localMediaPath) {
                        post.localMediaPath.takeIf { it.isNotBlank() }?.let(::File)
                    }
                    val canPlayLocal = localFile != null && localFile.exists()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playInlineVideo && canPlayLocal) {
                            InlineExoVideoPlayer(
                                uri = Uri.fromFile(localFile!!),
                                trimStartMs = post.trimStartMs,
                                trimEndMs = post.trimEndMs
                            )
                        } else {
                            Base64Image(
                                base64 = post.mediaPreviewBase64,
                                contentDescription = "Video thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                fallbackResId = R.drawable.img_banner_creator
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f))
                            )
                            FilledIconButton(
                                onClick = { if (canPlayLocal) playInlineVideo = true },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play video",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Videocam,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = formatDuration(post.durationSeconds),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                "music", "audio" -> {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Base64Image(
                                base64 = post.mediaPreviewBase64,
                                contentDescription = "Audio cover art",
                                fallbackResId = if (post.mediaType == "music") R.drawable.img_cover_lofi else R.drawable.img_cover_synth,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (post.mediaType == "music") "MUSIC TRACK" else "AUDIO RECORDING",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = post.title.ifBlank { "Untitled Audio" },
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatDuration(post.durationSeconds)} • Verified Original/Licensed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onAddToPlaylistClick) {
                                Icon(Icons.Rounded.PlaylistAdd, contentDescription = "Add to playlist")
                            }
                            FilledIconButton(
                                onClick = onPlayAudioClick,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThisAudio) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlayingThisAudio) "Pause audio" else "Play audio"
                                )
                            }
                        }
                    }
                }
            }

            if (post.title.isNotBlank() && post.mediaType != "music" && post.mediaType != "audio") {
                Spacer(Modifier.height(10.dp))
                Text(text = post.title, style = MaterialTheme.typography.titleMedium)
            }

            if (post.caption.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (post.hashtags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.hashtags.take(6).forEach { tag ->
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLikeClick) {
                        Icon(
                            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Like post",
                            tint = if (isLiked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${post.likesCount}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onCommentClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Comment,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${post.commentsCount}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share post",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${post.viewsCount} views",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onSaveClick) {
                        Icon(
                            imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Save post",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InlineExoVideoPlayer(
    uri: Uri,
    trimStartMs: Long = 0L,
    trimEndMs: Long = 0L,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val context = LocalContext.current
    val player = remember(uri, trimStartMs, trimEndMs) {
        ExoPlayer.Builder(context).build().apply {
            val builder = MediaItem.Builder().setUri(uri)
            if (trimEndMs > trimStartMs && trimStartMs >= 0L) {
                builder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(trimStartMs)
                        .setEndPositionMs(trimEndMs)
                        .build()
                )
            }
            setMediaItem(builder.build())
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun SponsoredSlotCard(
    placementLabel: String,
    settings: AdminSettings
) {
    if (!settings.adsEnabled) return
    val unitId = when (placementLabel) {
        "Explore" -> settings.bannerAdUnitId
        else -> settings.nativeAdUnitId
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "SPONSORED • ADMOB TEST PLACEMENT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = placementLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "AdMob Native / Banner Slot Ready",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Configured Unit: $unitId • Policy-compliant placement (no forced clicks or simulated impressions; revenue is shared only from verified ad network earnings).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MiniAudioPlayerBar(
    audioState: AudioPlaybackState,
    onTogglePlay: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit
) {
    val track = audioState.activeTrack ?: return
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onExpand)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = "Now playing",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title.ifBlank { "Audio Track" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = audioState.statusNotice ?: "${formatDuration(audioState.positionMs / 1000L)} / ${formatDuration(audioState.durationMs / 1000L)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (audioState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play or pause"
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close player")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullAudioPlayerSheet(
    audioState: AudioPlaybackState,
    onDismiss: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val track = audioState.activeTrack ?: return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Base64Image(
                base64 = track.mediaPreviewBase64,
                contentDescription = "Track artwork",
                fallbackResId = R.drawable.img_cover_lofi,
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = track.title.ifBlank { "Untitled Audio" },
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "@${track.authorUsername}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (audioState.statusNotice != null) {
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(audioState.statusNotice, style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Rounded.GraphicEq, null) }
                )
            }
            Spacer(Modifier.height(16.dp))
            val maxDur = audioState.durationMs.coerceAtLeast(1000L).toFloat()
            Slider(
                value = audioState.positionMs.toFloat().coerceIn(0f, maxDur),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..maxDur
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(audioState.positionMs / 1000L), style = MaterialTheme.typography.labelSmall)
                Text(formatDuration(audioState.durationMs / 1000L), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                IconButton(onClick = { onSeekBy(-10_000L) }) {
                    Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10 seconds", modifier = Modifier.size(32.dp))
                }
                FilledIconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (audioState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play or pause",
                        modifier = Modifier.size(34.dp)
                    )
                }
                IconButton(onClick = { onSeekBy(10_000L) }) {
                    Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10 seconds", modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolumeUp, contentDescription = "Volume")
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = audioState.volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SkeletonPostCard() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun EmptyFeedState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_social),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(22.dp))
        )
        Spacer(Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onAction, shape = RoundedCornerShape(14.dp)) {
                Text(actionLabel)
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    val mins = s / 60L
    val rem = s % 60L
    return String.format("%d:%02d", mins, rem)
}
