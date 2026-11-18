package com.example.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AudioPlaybackState
import com.example.data.MediaProcessingEngine
import com.example.model.BlockedUserEdge
import com.example.model.FollowEdge
import com.example.model.SocialPost
import com.example.model.UserPrivateSettings
import com.example.model.UserProfile
import com.example.ui.components.AvatarCircle
import com.example.ui.components.Base64Image
import com.example.ui.components.EmptyFeedState
import com.example.ui.components.PostCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile?,
    isOwnProfile: Boolean,
    isAdmin: Boolean,
    isFollowing: Boolean,
    userPosts: List<SocialPost>,
    savedPosts: List<SocialPost>,
    followersList: List<FollowEdge>,
    followingList: List<FollowEdge>,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    followingIds: Set<String>,
    audioState: AudioPlaybackState,
    onBack: (() -> Unit)? = null,
    onOpenSettings: () -> Unit,
    onOpenCreatorDashboard: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onFollowToggle: () -> Unit,
    onMessageUser: () -> Unit,
    onUpdateProfile: (displayName: String, username: String, bio: String, avatarB64: String?, coverB64: String?, privateProfile: Boolean, allowDms: Boolean) -> Unit,
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
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var followModalTitle by remember { mutableStateOf<String?>(null) }
    var followModalEntries by remember { mutableStateOf<List<FollowEdge>>(emptyList()) }

    val user = profile ?: UserProfile(displayName = "Pulse Creator", username = "creator")
    val tabs = if (isOwnProfile) {
        listOf("All (${userPosts.size})", "Photos", "Videos", "Music", "Saved")
    } else {
        listOf("All (${userPosts.size})", "Photos", "Videos", "Music")
    }

    val displayedPosts = when (selectedTab) {
        1 -> userPosts.filter { it.mediaType == "photo" }
        2 -> userPosts.filter { it.mediaType == "video" }
        3 -> userPosts.filter { it.mediaType == "music" || it.mediaType == "audio" }
        4 -> savedPosts
        else -> userPosts
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            item {
                TopAppBar(
                    title = { Text("@${user.username}") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }

        // Cover & Avatar header
        item {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    if (user.coverBase64.isNotBlank()) {
                        Base64Image(
                            base64 = user.coverBase64,
                            contentDescription = "Cover photo",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.img_banner_creator),
                            contentDescription = "Default creator cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x88000000))))
                    )
                    if (isOwnProfile) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .testTag("profile_settings_btn")
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AvatarCircle(
                                name = user.displayName,
                                avatarBase64 = user.avatarBase64,
                                size = 64.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.displayName, style = MaterialTheme.typography.headlineSmall)
                                    if (user.verified) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Rounded.Verified,
                                            contentDescription = "Verified",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    "@${user.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isOwnProfile) {
                            OutlinedButton(
                                onClick = { showEditDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit")
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onFollowToggle) {
                                    Text(if (isFollowing) "Following" else "Follow")
                                }
                                if (user.allowDms) {
                                    OutlinedButton(onClick = onMessageUser) {
                                        Icon(Icons.AutoMirrored.Rounded.Send, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (user.bio.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(user.bio, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatBox("Posts", "${user.postsCount}") {}
                        ProfileStatBox("Followers", "${user.followersCount}") {
                            followModalTitle = "Followers"
                            followModalEntries = followersList
                        }
                        ProfileStatBox("Following", "${user.followingCount}") {
                            followModalTitle = "Following"
                            followModalEntries = followingList
                        }
                        ProfileStatBox(
                            "Tracks",
                            "${userPosts.count { it.mediaType == "music" || it.mediaType == "audio" }}"
                        ) {}
                    }

                    if (isOwnProfile) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenCreatorDashboard,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_creator_dashboard_btn")
                            ) {
                                Icon(Icons.Rounded.AutoGraph, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Creator Studio")
                            }
                            OutlinedButton(
                                onClick = onOpenWallet,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_wallet_btn")
                            ) {
                                Icon(Icons.Rounded.AccountBalanceWallet, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Wallet")
                            }
                        }
                        if (isAdmin) {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = onOpenAdminDashboard,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_admin_dashboard_btn")
                            ) {
                                Icon(Icons.Rounded.AdminPanelSettings, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Open Administrator Console")
                            }
                        }
                    }
                }
            }
        }

        item {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label, maxLines = 1) }
                    )
                }
            }
        }

        if (displayedPosts.isEmpty()) {
            item {
                EmptyFeedState(
                    title = "No posts in this tab",
                    subtitle = "Uploaded photos, videos, music, and saved posts appear here."
                )
            }
        } else {
            items(displayedPosts, key = { "${selectedTab}_${it.id}" }) { post ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PostCard(
                        post = post,
                        currentUserId = user.userId,
                        isLiked = post.id in likedPostIds,
                        isSaved = post.id in savedPostIds,
                        isFollowingAuthor = post.authorId in followingIds,
                        isAuthorVerified = user.verified,
                        isPlayingThisAudio = audioState.activeTrack?.id == post.id && audioState.isPlaying,
                        onAuthorClick = {},
                        onFollowClick = onFollowToggle,
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

    if (showEditDialog && isOwnProfile) {
        EditProfileDialog(
            profile = user,
            onDismiss = { showEditDialog = false },
            onSave = { name, handle, bio, avatarB64, coverB64 ->
                onUpdateProfile(name, handle, bio, avatarB64, coverB64, user.privateProfile, user.allowDms)
                showEditDialog = false
            }
        )
    }

    if (followModalTitle != null) {
        AlertDialog(
            onDismissRequest = { followModalTitle = null },
            title = { Text(followModalTitle!!) },
            text = {
                if (followModalEntries.isEmpty()) {
                    Text("No entries yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        followModalEntries.take(25).forEach { edge ->
                            Text("• ${edge.actorName.ifBlank { edge.targetId }}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { followModalTitle = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ProfileStatBox(label: String, value: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (displayName: String, username: String, bio: String, avatarB64: String?, coverB64: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf(profile.displayName) }
    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio) }
    var avatarB64 by remember { mutableStateOf<String?>(null) }
    var coverB64 by remember { mutableStateOf<String?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = MediaProcessingEngine.decodeSampledBitmap(context, uri, 360)
                if (bmp != null) avatarB64 = MediaProcessingEngine.bitmapToCompressedBase64(bmp, 360, 72)
            }
        }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = MediaProcessingEngine.decodeSampledBitmap(context, uri, 720)
                if (bmp != null) coverB64 = MediaProcessingEngine.bitmapToCompressedBase64(bmp, 720, 72)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = displayName, onValueChange = { displayName = it.take(60) }, label = { Text("Display Name") })
                OutlinedTextField(value = username, onValueChange = { username = it.take(32) }, label = { Text("Username") })
                OutlinedTextField(value = bio, onValueChange = { bio = it.take(300) }, label = { Text("Bio") }, minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (avatarB64 == null) "Avatar" else "Avatar ✓") }
                    OutlinedButton(
                        onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (coverB64 == null) "Cover" else "Cover ✓") }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(displayName, username, bio, avatarB64, coverB64) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: UserProfile?,
    privateSettings: UserPrivateSettings?,
    blockedUsers: List<BlockedUserEdge>,
    themeMode: String,
    onBack: () -> Unit,
    onThemeChange: (String) -> Unit,
    onUpdateProfilePrivacy: (privateProfile: Boolean, allowDms: Boolean) -> Unit,
    onUpdateNotificationPrefs: (likes: Boolean, comments: Boolean, follows: Boolean, earnings: Boolean) -> Unit,
    onUnblockUser: (String) -> Unit,
    onSubmitProblemReport: (reason: String, details: String) -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val prefs = privateSettings ?: UserPrivateSettings()
    val user = profile ?: UserProfile()
    var showBlockedDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf<String?>(null) }
    var showProblemDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings & Privacy") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Appearance / Theme
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Theme & Language", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                            FilterChip(
                                selected = themeMode == key,
                                onClick = { onThemeChange(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Text(
                        "Language: English (US) • Account verified via Google Sign-In (${prefs.email.ifBlank { "Google Account" }})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Privacy Controls
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Privacy & Safety", style = MaterialTheme.typography.titleMedium)
                    }
                    SwitchSettingRow("Private Profile", user.privateProfile) {
                        onUpdateProfilePrivacy(it, user.allowDms)
                    }
                    SwitchSettingRow("Allow Direct Messages", user.allowDms) {
                        onUpdateProfilePrivacy(user.privateProfile, it)
                    }
                    SettingsActionRow(
                        icon = Icons.Rounded.Block,
                        title = "Blocked & Muted Accounts (${blockedUsers.size})",
                        onClick = { showBlockedDialog = true }
                    )
                }
            }

            // Notifications
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Notification Preferences", style = MaterialTheme.typography.titleMedium)
                    }
                    SwitchSettingRow("Likes", prefs.notifyLikes) {
                        onUpdateNotificationPrefs(it, prefs.notifyComments, prefs.notifyFollows, prefs.notifyEarnings)
                    }
                    SwitchSettingRow("Comments & Replies", prefs.notifyComments) {
                        onUpdateNotificationPrefs(prefs.notifyLikes, it, prefs.notifyFollows, prefs.notifyEarnings)
                    }
                    SwitchSettingRow("New Followers", prefs.notifyFollows) {
                        onUpdateNotificationPrefs(prefs.notifyLikes, prefs.notifyComments, it, prefs.notifyEarnings)
                    }
                    SwitchSettingRow("Creator Earnings & Payouts", prefs.notifyEarnings) {
                        onUpdateNotificationPrefs(prefs.notifyLikes, prefs.notifyComments, prefs.notifyFollows, it)
                    }
                }
            }

            // Legal, Support & Account Lifecycle
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingsActionRow(Icons.Rounded.HelpOutline, "Help & Support") {
                        showPolicyDialog = "Help & Support"
                    }
                    SettingsActionRow(Icons.Rounded.ReportProblem, "Report a Problem") {
                        showProblemDialog = true
                    }
                    SettingsActionRow(Icons.Rounded.Policy, "Terms, Privacy & Monetization Policy") {
                        showPolicyDialog = "Terms & Privacy Policy"
                    }
                    SettingsActionRow(Icons.Rounded.DeleteForever, "Delete Account & Cloud Data") {
                        showDeleteConfirm = true
                    }
                }
            }

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text("Blocked Accounts") },
            text = {
                if (blockedUsers.isEmpty()) {
                    Text("You have not blocked or muted anyone.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        blockedUsers.forEach { b ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("@${b.blockedUsername}")
                                TextButton(onClick = { onUnblockUser(b.blockedUserId) }) { Text("Unblock") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBlockedDialog = false }) { Text("Close") } }
        )
    }

    if (showPolicyDialog != null) {
        AlertDialog(
            onDismissRequest = { showPolicyDialog = null },
            title = { Text(showPolicyDialog!!) },
            text = {
                Text(
                    "PulseStream operates under Google Play Developer & AdMob Policies:\n\n" +
                        "• Security & Auth: Protected by Google Sign-In and per-user Firestore security rules. Never share sensitive banking card numbers.\n" +
                        "• Copyright: Only upload music/audio you own or have distribution rights for.\n" +
                        "• Monetization: Ad revenue sharing requires eligibility approval and verified ad network revenue. Viewing ads does not guarantee earnings.\n" +
                        "• Support: Use 'Report a Problem' to reach moderation administrators."
                )
            },
            confirmButton = { TextButton(onClick = { showPolicyDialog = null }) { Text("Got It") } }
        )
    }

    if (showProblemDialog) {
        var issueText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showProblemDialog = false },
            title = { Text("Report a Problem") },
            text = {
                OutlinedTextField(
                    value = issueText,
                    onValueChange = { issueText = it.take(500) },
                    label = { Text("Describe the issue or feedback") },
                    minLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitProblemReport("App Support / Bug Report", issueText)
                        showProblemDialog = false
                    },
                    enabled = issueText.isNotBlank()
                ) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { showProblemDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account?") },
            text = {
                Text("This permanently deletes your PulseStream profile, posts, and earnings data from Firestore and removes your account.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Forever") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SwitchSettingRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}
