package com.example.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AudioPlaybackState
import com.example.data.MediaProcessingEngine
import com.example.model.AdminSettings
import com.example.model.Playlist
import com.example.model.SocialPost
import com.example.model.StoryItem
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.AvatarCircle
import com.example.ui.components.Base64Image
import com.example.ui.components.EmptyFeedState
import com.example.ui.components.PostCard
import com.example.ui.components.SkeletonPostCard
import com.example.ui.components.SponsoredSlotCard
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    currentUser: UserProfile?,
    currentUserId: String,
    feedState: UiState<List<SocialPost>>,
    stories: List<StoryItem>,
    allUsers: List<UserProfile>,
    followingIds: Set<String>,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    unreadNotificationsCount: Int,
    audioState: AudioPlaybackState,
    adminSettings: AdminSettings,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onPublishStory: (caption: String, imageBase64: String, accentHex: String) -> Unit,
    onFollowToggle: (UserProfile) -> Unit,
    onLikeToggle: (SocialPost) -> Unit,
    onSaveToggle: (String) -> Unit,
    onCommentClick: (SocialPost) -> Unit,
    onShareClick: (SocialPost) -> Unit,
    onPlayAudioClick: (SocialPost) -> Unit,
    onAddToPlaylistClick: (SocialPost) -> Unit,
    onEditPost: (SocialPost) -> Unit,
    onDeletePost: (SocialPost) -> Unit,
    onReportPost: (SocialPost, String) -> Unit,
    onBlockUser: (SocialPost) -> Unit
) {
    var feedFilter by remember { mutableStateOf("All") }
    var activeStory by remember { mutableStateOf<StoryItem?>(null) }
    var showStoryComposer by remember { mutableStateOf(false) }

    val usersById = remember(allUsers) { allUsers.associateBy { it.userId } }
    val recommendedCreators = remember(allUsers, currentUserId) {
        allUsers.filter { it.userId != currentUserId && it.accountStatus == "active" }.take(10)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Branded Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "PulseStream logo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PulseStream",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Photos · Video · Music · Creators",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSearchClick, modifier = Modifier.testTag("home_search_btn")) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onMessagesClick, modifier = Modifier.testTag("home_messages_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Messages")
                    }
                    IconButton(onClick = onNotificationsClick, modifier = Modifier.testTag("home_notif_btn")) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge { Text("$unreadNotificationsCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "Notifications")
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onProfileClick(currentUserId) }
                    ) {
                        AvatarCircle(
                            name = currentUser?.displayName ?: "You",
                            avatarBase64 = currentUser?.avatarBase64.orEmpty(),
                            size = 34.dp
                        )
                    }
                }
            }
        }

        // Stories strip
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clickable { showStoryComposer = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add story",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Your Story",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                items(stories, key = { it.id }) { story ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clickable { activeStory = story }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.5.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                        ) {
                            AvatarCircle(
                                name = story.authorName,
                                avatarBase64 = story.authorAvatarBase64,
                                size = 56.dp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = story.authorName,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Feed filter pills
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf("All", "Trending", "Following", "Photos", "Videos", "Music & Audio")
                items(tabs) { tab ->
                    FilterChip(
                        selected = feedFilter == tab,
                        onClick = { feedFilter = tab },
                        label = { Text(tab) }
                    )
                }
            }
        }

        // Recommended Creators strip
        if (recommendedCreators.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Recommended Creators",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recommendedCreators, key = { it.userId }) { creator ->
                            val isFollowing = creator.userId in followingIds
                            Card(
                                modifier = Modifier
                                    .width(152.dp)
                                    .clickable { onProfileClick(creator.userId) },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AvatarCircle(
                                        name = creator.displayName,
                                        avatarBase64 = creator.avatarBase64,
                                        size = 48.dp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = creator.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (creator.verified) {
                                            Spacer(Modifier.width(3.dp))
                                            Icon(
                                                Icons.Rounded.Verified,
                                                contentDescription = "Verified",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${creator.followersCount} followers",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { onFollowToggle(creator) },
                                        shape = RoundedCornerShape(99.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                    ) {
                                        Text(
                                            text = if (isFollowing) "Following" else "Follow",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Feed items
        when (feedState) {
            is UiState.Loading -> {
                items(3) { SkeletonPostCard() }
            }
            is UiState.Error -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val filtered = when (feedFilter) {
                    "Trending" -> feedState.data.sortedByDescending { it.likesCount + it.commentsCount + it.viewsCount }
                    "Following" -> feedState.data.filter { it.authorId in followingIds || it.authorId == currentUserId }
                    "Photos" -> feedState.data.filter { it.mediaType == "photo" }
                    "Videos" -> feedState.data.filter { it.mediaType == "video" }
                    "Music & Audio" -> feedState.data.filter { it.mediaType == "music" || it.mediaType == "audio" }
                    else -> feedState.data
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyFeedState(
                            title = "Your feed is ready for creativity",
                            subtitle = "Upload an edited photo, trimmed video, or original audio track to kick off the stream.",
                            actionLabel = "Create First Post",
                            onAction = onCreateClick
                        )
                    }
                } else {
                    itemsIndexed(filtered, key = { _, item -> item.id }) { index, post ->
                        val authorProfile = usersById[post.authorId]
                        val isPlayingThis = audioState.activeTrack?.id == post.id && audioState.isPlaying
                        PostCard(
                            post = post,
                            currentUserId = currentUserId,
                            isLiked = post.id in likedPostIds,
                            isSaved = post.id in savedPostIds,
                            isFollowingAuthor = post.authorId in followingIds,
                            isAuthorVerified = authorProfile?.verified == true,
                            isPlayingThisAudio = isPlayingThis,
                            onAuthorClick = { onProfileClick(post.authorId) },
                            onFollowClick = {
                                val target = authorProfile ?: UserProfile(
                                    userId = post.authorId,
                                    username = post.authorUsername,
                                    displayName = post.authorName
                                )
                                onFollowToggle(target)
                            },
                            onLikeClick = { onLikeToggle(post) },
                            onCommentClick = { onCommentClick(post) },
                            onShareClick = { onShareClick(post) },
                            onSaveClick = { onSaveToggle(post.id) },
                            onPlayAudioClick = { onPlayAudioClick(post) },
                            onAddToPlaylistClick = { onAddToPlaylistClick(post) },
                            onEditClick = { onEditPost(post) },
                            onDeleteClick = { onDeletePost(post) },
                            onReportClick = { reportType -> onReportPost(post, reportType) },
                            onBlockClick = { onBlockUser(post) }
                        )

                        if (index == 1 || (index > 1 && index % 5 == 0)) {
                            Spacer(Modifier.height(14.dp))
                            SponsoredSlotCard(placementLabel = "Home Feed", settings = adminSettings)
                        }
                    }
                }
            }
        }
    }

    if (activeStory != null) {
        val s = activeStory!!
        AlertDialog(
            onDismissRequest = { activeStory = null },
            title = { Text("${s.authorName}'s Story") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (s.imageBase64.isNotBlank()) {
                        Base64Image(
                            base64 = s.imageBase64,
                            contentDescription = s.caption,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                    Text(text = s.caption, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(onClick = { activeStory = null }) { Text("Close") }
            }
        )
    }

    if (showStoryComposer) {
        StoryComposerDialog(
            onDismiss = { showStoryComposer = false },
            onPublish = { caption, imgB64 ->
                onPublishStory(caption, imgB64, "#6366F1")
                showStoryComposer = false
            }
        )
    }
}

@Composable
private fun StoryComposerDialog(
    onDismiss: () -> Unit,
    onPublish: (caption: String, imageBase64: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var caption by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = MediaProcessingEngine.decodeSampledBitmap(context, uri, 640)
                if (bmp != null) {
                    imageBase64 = MediaProcessingEngine.bitmapToCompressedBase64(bmp, 640, 70)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post a Story") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(140) },
                    label = { Text("What are you creating today?") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (imageBase64.isBlank()) "Attach Story Photo (Optional)" else "Photo Attached ✓")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(caption, imageBase64) },
                enabled = caption.isNotBlank()
            ) {
                Text("Share Story")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExploreScreen(
    currentUserId: String,
    feedState: UiState<List<SocialPost>>,
    allUsers: List<UserProfile>,
    playlists: List<Playlist>,
    followingIds: Set<String>,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    audioState: AudioPlaybackState,
    adminSettings: AdminSettings,
    onProfileClick: (String) -> Unit,
    onFollowToggle: (UserProfile) -> Unit,
    onLikeToggle: (SocialPost) -> Unit,
    onSaveToggle: (String) -> Unit,
    onCommentClick: (SocialPost) -> Unit,
    onShareClick: (SocialPost) -> Unit,
    onPlayAudioClick: (SocialPost) -> Unit,
    onAddToPlaylistClick: (SocialPost) -> Unit,
    onCreatePlaylist: (title: String, description: String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onEditPost: (SocialPost) -> Unit,
    onDeletePost: (SocialPost) -> Unit,
    onReportPost: (SocialPost, String) -> Unit,
    onBlockUser: (SocialPost) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCreatePlaylist by remember { mutableStateOf(false) }

    val posts = (feedState as? UiState.Success)?.data ?: emptyList()
    val usersById = remember(allUsers) { allUsers.associateBy { it.userId } }

    val filteredPosts = remember(posts, query, selectedCategory) {
        posts.filter { post ->
            val matchesCat = when (selectedCategory) {
                "Photos" -> post.mediaType == "photo"
                "Videos" -> post.mediaType == "video"
                "Music & Audio" -> post.mediaType == "music" || post.mediaType == "audio"
                else -> true
            }
            val q = query.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                post.title.lowercase().contains(q) ||
                post.caption.lowercase().contains(q) ||
                post.authorName.lowercase().contains(q) ||
                post.hashtags.any { it.lowercase().contains(q) }
            matchesCat && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Explore & Music Hub",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Search photos, videos, music, #hashtags, creators") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Photos", "Videos", "Music & Audio", "Playlists")) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        if (selectedCategory == "Playlists") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Personal Playlists", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { showCreatePlaylist = true }) {
                        Icon(Icons.Rounded.PlaylistAdd, null)
                        Spacer(Modifier.width(6.dp))
                        Text("New Playlist")
                    }
                }
            }
            if (playlists.isEmpty()) {
                item {
                    EmptyFeedState(
                        title = "No playlists yet",
                        subtitle = "Create a personal playlist and save your favorite music and voice recordings.",
                        actionLabel = "Create Playlist",
                        onAction = { showCreatePlaylist = true }
                    )
                }
            } else {
                items(playlists, key = { it.id }) { pl ->
                    val playlistTracks = posts.filter { it.id in pl.trackIds }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(pl.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${pl.trackIds.size} tracks • ${pl.description.ifBlank { "Personal mix" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row {
                                    if (playlistTracks.isNotEmpty()) {
                                        IconButton(onClick = { onPlayAudioClick(playlistTracks.first()) }) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play playlist")
                                        }
                                    }
                                    IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete playlist")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Featured Audio Artwork Showcase
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCategory = "Music & Audio" },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column {
                            Image(
                                painter = painterResource(R.drawable.img_cover_lofi),
                                contentDescription = "Lo-Fi & Indie Audio",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                            )
                            Text(
                                "Music & Beats",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCategory = "Videos" },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column {
                            Image(
                                painter = painterResource(R.drawable.img_cover_synth),
                                contentDescription = "Creator Clips & Podcasts",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                            )
                            Text(
                                "Creator Reels & Audio",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            item {
                SponsoredSlotCard(placementLabel = "Explore", settings = adminSettings)
            }

            if (filteredPosts.isEmpty()) {
                item {
                    EmptyFeedState(
                        title = "No matching posts",
                        subtitle = "Try clearing your search or switching media filters."
                    )
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    val authorProfile = usersById[post.authorId]
                    PostCard(
                        post = post,
                        currentUserId = currentUserId,
                        isLiked = post.id in likedPostIds,
                        isSaved = post.id in savedPostIds,
                        isFollowingAuthor = post.authorId in followingIds,
                        isAuthorVerified = authorProfile?.verified == true,
                        isPlayingThisAudio = audioState.activeTrack?.id == post.id && audioState.isPlaying,
                        onAuthorClick = { onProfileClick(post.authorId) },
                        onFollowClick = {
                            onFollowToggle(
                                authorProfile ?: UserProfile(
                                    userId = post.authorId,
                                    username = post.authorUsername,
                                    displayName = post.authorName
                                )
                            )
                        },
                        onLikeClick = { onLikeToggle(post) },
                        onCommentClick = { onCommentClick(post) },
                        onShareClick = { onShareClick(post) },
                        onSaveClick = { onSaveToggle(post.id) },
                        onPlayAudioClick = { onPlayAudioClick(post) },
                        onAddToPlaylistClick = { onAddToPlaylistClick(post) },
                        onEditClick = { onEditPost(post) },
                        onDeleteClick = { onDeletePost(post) },
                        onReportClick = { type -> onReportPost(post, type) },
                        onBlockClick = { onBlockUser(post) }
                    )
                }
            }
        }
    }

    if (showCreatePlaylist) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text("Create Audio Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(80) },
                        label = { Text("Playlist title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it.take(240) },
                        label = { Text("Description (optional)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePlaylist(title, desc)
                        showCreatePlaylist = false
                    },
                    enabled = title.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylist = false }) { Text("Cancel") }
            }
        )
    }
}
