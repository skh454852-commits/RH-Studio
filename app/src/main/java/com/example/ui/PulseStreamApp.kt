package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.AppPreferences
import com.example.data.AudioPlayerManager
import com.example.data.MediaProcessingEngine
import com.example.data.MonetizationAdminRepository
import com.example.data.SocialRepository
import com.example.data.UserRepository
import com.example.data.authStateFlow
import com.example.data.signOutUser
import com.example.model.SocialPost
import com.example.model.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.components.FullAudioPlayerSheet
import com.example.ui.components.MiniAudioPlayerBar
import com.example.ui.create.CreateStudioScreen
import com.example.ui.home.ExploreScreen
import com.example.ui.home.HomeScreen
import com.example.ui.monetization.AdminDashboardScreen
import com.example.ui.monetization.CreatorDashboardScreen
import com.example.ui.monetization.WalletScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.SettingsScreen
import com.example.ui.social.AddToPlaylistDialog
import com.example.ui.social.ChatThreadScreen
import com.example.ui.social.CommentsBottomSheet
import com.example.ui.social.EditPostDialog
import com.example.ui.social.MessagesInboxScreen
import com.example.ui.social.NotificationsScreen
import com.example.ui.social.ReportContentDialog
import com.example.ui.theme.PulseStreamTheme
import com.example.viewmodel.PulseViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun PulseStreamApp() {
    val context = LocalContext.current
    val appPrefs = remember(context) { AppPreferences(context.applicationContext) }
    val themeMode by appPrefs.themeMode.collectAsStateWithLifecycle(initialValue = "system")

    val auth: FirebaseAuth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    PulseStreamTheme(themeMode = themeMode) {
        val user = currentUser
        if (user == null) {
            AuthScreen(onAuthSuccess = {})
        } else {
            key(user.uid) {
                AuthenticatedPulseShell(
                    currentUserId = user.uid,
                    appPrefs = appPrefs,
                    themeMode = themeMode
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedPulseShell(
    currentUserId: String,
    appPrefs: AppPreferences,
    themeMode: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val appContext = context.applicationContext
    val vm: PulseViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val databaseId = appContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PulseViewModel(
                    userRepo = UserRepository(db),
                    socialRepo = SocialRepository(db),
                    monetizationRepo = MonetizationAdminRepository(db),
                    appPrefs = appPrefs,
                    audioPlayer = AudioPlayerManager(appContext),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    val snackbarHostState = remember { SnackbarHostState() }

    val actionBanner by vm.actionBanner.collectAsStateWithLifecycle()
    LaunchedEffect(actionBanner) {
        val msg = actionBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            vm.clearBanner()
        }
    }

    val currentUserProfile by vm.currentUserProfile.collectAsStateWithLifecycle()
    val privateSettings by vm.privateSettings.collectAsStateWithLifecycle()
    val allUsers by vm.allUsers.collectAsStateWithLifecycle()
    val blockedUsers by vm.blockedUsers.collectAsStateWithLifecycle()
    val feedState by vm.feedState.collectAsStateWithLifecycle()
    val stories by vm.stories.collectAsStateWithLifecycle()
    val followingIds by vm.followingIds.collectAsStateWithLifecycle()
    val likedPostIds by vm.likedPostIds.collectAsStateWithLifecycle()
    val savedPostIds by vm.savedPostIds.collectAsStateWithLifecycle()
    val notifications by vm.notifications.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val adminSettings by vm.adminSettings.collectAsStateWithLifecycle()
    val creatorEarnings by vm.creatorEarnings.collectAsStateWithLifecycle()
    val creatorApplication by vm.creatorApplication.collectAsStateWithLifecycle()
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val userWithdrawals by vm.userWithdrawals.collectAsStateWithLifecycle()
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val activeComments by vm.activeComments.collectAsStateWithLifecycle()
    val activeMessages by vm.activeMessages.collectAsStateWithLifecycle()
    val audioState by vm.audioState.collectAsStateWithLifecycle()

    val adminReports by vm.adminReports.collectAsStateWithLifecycle()
    val adminApplications by vm.adminApplications.collectAsStateWithLifecycle()
    val adminWithdrawals by vm.adminWithdrawals.collectAsStateWithLifecycle()

    val unreadCount = remember(notifications) { notifications.count { !it.read } }
    val allPosts = (feedState as? UiState.Success)?.data ?: emptyList()

    // Modal states
    var commentsPost by remember { mutableStateOf<SocialPost?>(null) }
    var editingPost by remember { mutableStateOf<SocialPost?>(null) }
    var reportTarget by remember { mutableStateOf<Pair<SocialPost, String>?>(null) }
    var playlistTargetPost by remember { mutableStateOf<SocialPost?>(null) }
    var showFullAudioSheet by remember { mutableStateOf(false) }

    val topLevelRoutes = setOf("home", "explore", "create", "notifications", "profile")
    val showMainNav = currentRoute in topLevelRoutes

    val navItems = listOf(
        Triple("home", "Home", Icons.Rounded.Home),
        Triple("explore", "Explore", Icons.Rounded.Explore),
        Triple("create", "Create", Icons.Rounded.AddCircle),
        Triple("notifications", "Alerts", Icons.Rounded.Notifications),
        Triple("profile", "Profile", Icons.Rounded.Person)
    )

    val useRail = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 640

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Column {
                if (audioState.activeTrack != null) {
                    MiniAudioPlayerBar(
                        audioState = audioState,
                        onTogglePlay = { vm.audioPlayer.togglePlayPause() },
                        onExpand = { showFullAudioSheet = true },
                        onClose = { vm.audioPlayer.stop() }
                    )
                }
                if (showMainNav && !useRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        navItems.forEach { (route, label, icon) ->
                            NavigationBarItem(
                                selected = currentRoute == route,
                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (route == "notifications" && unreadCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                            Icon(icon, contentDescription = label)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_$route")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showMainNav && useRail) {
                NavigationRail {
                    navItems.forEach { (route, label, icon) ->
                        NavigationRailItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            currentUser = currentUserProfile,
                            currentUserId = currentUserId,
                            feedState = feedState,
                            stories = stories,
                            allUsers = allUsers,
                            followingIds = followingIds,
                            likedPostIds = likedPostIds,
                            savedPostIds = savedPostIds,
                            unreadNotificationsCount = unreadCount,
                            audioState = audioState,
                            adminSettings = adminSettings,
                            onSearchClick = { navController.navigate("explore") },
                            onNotificationsClick = { navController.navigate("notifications") },
                            onMessagesClick = { navController.navigate("messages") },
                            onProfileClick = { uid ->
                                if (uid == currentUserId) navController.navigate("profile")
                                else navController.navigate("user_profile/$uid")
                            },
                            onCreateClick = { navController.navigate("create") },
                            onPublishStory = { cap, img, hex -> vm.publishStory(cap, img, hex) },
                            onFollowToggle = { target -> vm.toggleFollow(target) },
                            onLikeToggle = { post -> vm.toggleLike(post) },
                            onSaveToggle = { id -> vm.toggleSave(id) },
                            onCommentClick = { post ->
                                vm.setActiveCommentsPost(post.id)
                                commentsPost = post
                            },
                            onShareClick = { post -> MediaProcessingEngine.sharePostNatively(context, post) },
                            onPlayAudioClick = { post ->
                                if (audioState.activeTrack?.id == post.id) vm.audioPlayer.togglePlayPause()
                                else vm.audioPlayer.playTrack(post)
                            },
                            onAddToPlaylistClick = { post -> playlistTargetPost = post },
                            onEditPost = { post -> editingPost = post },
                            onDeletePost = { post -> vm.deletePost(post.id) },
                            onReportPost = { post, type -> reportTarget = post to type },
                            onBlockUser = { post -> vm.blockOrMuteUser(post.authorId, post.authorUsername, false) }
                        )
                    }

                    composable("explore") {
                        ExploreScreen(
                            currentUserId = currentUserId,
                            feedState = feedState,
                            allUsers = allUsers,
                            playlists = playlists,
                            followingIds = followingIds,
                            likedPostIds = likedPostIds,
                            savedPostIds = savedPostIds,
                            audioState = audioState,
                            adminSettings = adminSettings,
                            onProfileClick = { uid ->
                                if (uid == currentUserId) navController.navigate("profile")
                                else navController.navigate("user_profile/$uid")
                            },
                            onFollowToggle = { vm.toggleFollow(it) },
                            onLikeToggle = { vm.toggleLike(it) },
                            onSaveToggle = { vm.toggleSave(it) },
                            onCommentClick = { post ->
                                vm.setActiveCommentsPost(post.id)
                                commentsPost = post
                            },
                            onShareClick = { MediaProcessingEngine.sharePostNatively(context, it) },
                            onPlayAudioClick = { post ->
                                if (audioState.activeTrack?.id == post.id) vm.audioPlayer.togglePlayPause()
                                else vm.audioPlayer.playTrack(post)
                            },
                            onAddToPlaylistClick = { playlistTargetPost = it },
                            onCreatePlaylist = { t, d -> vm.createPlaylist(t, d) },
                            onDeletePlaylist = { vm.deletePlaylist(it) },
                            onEditPost = { editingPost = it },
                            onDeletePost = { vm.deletePost(it.id) },
                            onReportPost = { post, type -> reportTarget = post to type },
                            onBlockUser = { vm.blockOrMuteUser(it.authorId, it.authorUsername, false) }
                        )
                    }

                    composable("create") {
                        CreateStudioScreen(
                            onPublishPost = { mediaType, title, caption, tags, loc, prevB64, path, mime, dur, tStart, tEnd, copyOk, done ->
                                vm.publishPost(
                                    mediaType, title, caption, tags, loc, prevB64, path, mime, dur, tStart, tEnd, copyOk, done
                                )
                            },
                            onPostSuccessNavigateHome = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                        )
                    }

                    composable("notifications") {
                        NotificationsScreen(
                            notifications = notifications,
                            onNotificationClick = { notif ->
                                vm.markNotificationRead(notif.id)
                            }
                        )
                    }

                    composable("profile") {
                        val myPosts = remember(allPosts, currentUserId) {
                            allPosts.filter { it.authorId == currentUserId }
                        }
                        val saved = remember(allPosts, savedPostIds) {
                            allPosts.filter { it.id in savedPostIds }
                        }
                        val followersList by vm.observeFollowersList(currentUserId).collectAsStateWithLifecycle(emptyList())
                        val followingList by vm.observeFollowingList(currentUserId).collectAsStateWithLifecycle(emptyList())
                        ProfileScreen(
                            profile = currentUserProfile,
                            isOwnProfile = true,
                            isAdmin = vm.isAdmin,
                            isFollowing = false,
                            userPosts = myPosts,
                            savedPosts = saved,
                            followersList = followersList,
                            followingList = followingList,
                            likedPostIds = likedPostIds,
                            savedPostIds = savedPostIds,
                            followingIds = followingIds,
                            audioState = audioState,
                            onOpenSettings = { navController.navigate("settings") },
                            onOpenCreatorDashboard = { navController.navigate("creator_dashboard") },
                            onOpenWallet = { navController.navigate("wallet") },
                            onOpenAdminDashboard = { navController.navigate("admin_dashboard") },
                            onFollowToggle = {},
                            onMessageUser = {},
                            onUpdateProfile = { name, handle, bio, av, cov, priv, dms ->
                                vm.updateProfile(name, handle, bio, av, cov, priv, dms)
                            },
                            onLikeToggle = { vm.toggleLike(it) },
                            onSaveToggle = { vm.toggleSave(it) },
                            onCommentClick = { post ->
                                vm.setActiveCommentsPost(post.id)
                                commentsPost = post
                            },
                            onShareClick = { MediaProcessingEngine.sharePostNatively(context, it) },
                            onPlayAudioClick = { post ->
                                if (audioState.activeTrack?.id == post.id) vm.audioPlayer.togglePlayPause()
                                else vm.audioPlayer.playTrack(post)
                            },
                            onAddToPlaylistClick = { playlistTargetPost = it },
                            onEditPost = { editingPost = it },
                            onDeletePost = { vm.deletePost(it.id) },
                            onReportPost = { post, type -> reportTarget = post to type },
                            onBlockUser = { vm.blockOrMuteUser(it.authorId, it.authorUsername, false) }
                        )
                    }

                    composable(
                        route = "user_profile/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { entry ->
                        val targetUid = entry.arguments?.getString("userId") ?: return@composable
                        val targetUser = allUsers.firstOrNull { it.userId == targetUid }
                        val targetPosts = remember(allPosts, targetUid) { allPosts.filter { it.authorId == targetUid } }
                        val followersList by vm.observeFollowersList(targetUid).collectAsStateWithLifecycle(emptyList())
                        val followingList by vm.observeFollowingList(targetUid).collectAsStateWithLifecycle(emptyList())
                        ProfileScreen(
                            profile = targetUser,
                            isOwnProfile = false,
                            isAdmin = false,
                            isFollowing = targetUid in followingIds,
                            userPosts = targetPosts,
                            savedPosts = emptyList(),
                            followersList = followersList,
                            followingList = followingList,
                            likedPostIds = likedPostIds,
                            savedPostIds = savedPostIds,
                            followingIds = followingIds,
                            audioState = audioState,
                            onBack = { navController.popBackStack() },
                            onOpenSettings = {},
                            onOpenCreatorDashboard = {},
                            onOpenWallet = {},
                            onOpenAdminDashboard = {},
                            onFollowToggle = { targetUser?.let { vm.toggleFollow(it) } },
                            onMessageUser = {
                                targetUser?.let {
                                    vm.startDirectMessage(it) { convId ->
                                        vm.setActiveConversation(convId)
                                        navController.navigate("chat/$convId")
                                    }
                                }
                            },
                            onUpdateProfile = { _, _, _, _, _, _, _ -> },
                            onLikeToggle = { vm.toggleLike(it) },
                            onSaveToggle = { vm.toggleSave(it) },
                            onCommentClick = { post ->
                                vm.setActiveCommentsPost(post.id)
                                commentsPost = post
                            },
                            onShareClick = { MediaProcessingEngine.sharePostNatively(context, it) },
                            onPlayAudioClick = { post ->
                                if (audioState.activeTrack?.id == post.id) vm.audioPlayer.togglePlayPause()
                                else vm.audioPlayer.playTrack(post)
                            },
                            onAddToPlaylistClick = { playlistTargetPost = it },
                            onEditPost = {},
                            onDeletePost = {},
                            onReportPost = { post, type -> reportTarget = post to type },
                            onBlockUser = { vm.blockOrMuteUser(it.authorId, it.authorUsername, false) }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            profile = currentUserProfile,
                            privateSettings = privateSettings,
                            blockedUsers = blockedUsers,
                            themeMode = themeMode,
                            onBack = { navController.popBackStack() },
                            onThemeChange = { vm.setThemeMode(it) },
                            onUpdateProfilePrivacy = { priv, dms ->
                                val u = currentUserProfile ?: return@SettingsScreen
                                vm.updateProfile(u.displayName, u.username, u.bio, null, null, priv, dms)
                            },
                            onUpdateNotificationPrefs = { likes, comments, follows, earnings ->
                                val p = privateSettings
                                vm.updateSettings(
                                    p?.payoutProvider ?: "none",
                                    p?.maskedPayoutDestination ?: "",
                                    likes, comments, follows, earnings, themeMode
                                )
                            },
                            onUnblockUser = { vm.unblockUser(it) },
                            onSubmitProblemReport = { reason, details ->
                                vm.submitReport("app_issue", currentUserId, reason, details)
                            },
                            onDeleteAccount = {
                                vm.deleteAccount {
                                    signOutUser(credentialManager, scope)
                                }
                            },
                            onLogout = {
                                signOutUser(credentialManager, scope)
                            }
                        )
                    }

                    composable("creator_dashboard") {
                        val myPosts = remember(allPosts, currentUserId) { allPosts.filter { it.authorId == currentUserId } }
                        CreatorDashboardScreen(
                            profile = currentUserProfile,
                            earnings = creatorEarnings,
                            application = creatorApplication,
                            userPosts = myPosts,
                            adminSettings = adminSettings,
                            transactions = transactions,
                            onBack = { navController.popBackStack() },
                            onOpenWallet = { navController.navigate("wallet") },
                            onApplyMonetization = { cat, notes, views ->
                                vm.applyForMonetization(cat, notes, views)
                            }
                        )
                    }

                    composable("wallet") {
                        WalletScreen(
                            earnings = creatorEarnings,
                            privateSettings = privateSettings,
                            adminSettings = adminSettings,
                            withdrawals = userWithdrawals,
                            transactions = transactions,
                            onBack = { navController.popBackStack() },
                            onSavePayoutMethod = { provider, masked ->
                                val p = privateSettings
                                vm.updateSettings(
                                    provider,
                                    masked,
                                    p?.notifyLikes ?: true,
                                    p?.notifyComments ?: true,
                                    p?.notifyFollows ?: true,
                                    p?.notifyEarnings ?: true,
                                    themeMode
                                )
                            },
                            onRequestWithdrawal = { amt, provider, dest ->
                                vm.requestWithdrawal(amt, provider, dest)
                            }
                        )
                    }

                    composable("admin_dashboard") {
                        AdminDashboardScreen(
                            isAdmin = vm.isAdmin,
                            allUsers = allUsers,
                            allPosts = allPosts,
                            reports = adminReports,
                            applications = adminApplications,
                            withdrawals = adminWithdrawals,
                            adminSettings = adminSettings,
                            onBack = { navController.popBackStack() },
                            onReviewApplication = { uid, ok, note -> vm.reviewCreatorApp(uid, ok, note) },
                            onReviewWithdrawal = { req, status, note -> vm.reviewWithdrawal(req, status, note) },
                            onResolveReport = { repId, status, postId -> vm.resolveReport(repId, status, postId) },
                            onModerateUser = { uid, ver, status -> vm.moderateUser(uid, ver, status) },
                            onCreditVerifiedRevenue = { uid, views, amt, desc ->
                                vm.creditVerifiedRevenue(uid, views, amt, desc)
                            },
                            onSaveSettings = { vm.saveAdminSettings(it) }
                        )
                    }

                    composable("messages") {
                        MessagesInboxScreen(
                            currentUserId = currentUserId,
                            conversations = conversations,
                            onBack = { navController.popBackStack() },
                            onOpenConversation = { conv ->
                                vm.setActiveConversation(conv.id)
                                navController.navigate("chat/${conv.id}")
                            }
                        )
                    }

                    composable(
                        route = "chat/{convId}",
                        arguments = listOf(navArgument("convId") { type = NavType.StringType })
                    ) { entry ->
                        val convId = entry.arguments?.getString("convId") ?: return@composable
                        val conv = conversations.firstOrNull { it.id == convId }
                        if (conv != null) {
                            ChatThreadScreen(
                                currentUserId = currentUserId,
                                conversation = conv,
                                messages = activeMessages,
                                onBack = {
                                    vm.setActiveConversation(null)
                                    navController.popBackStack()
                                },
                                onSendMessage = { text -> vm.sendDirectMessage(conv, text) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Active Sheets & Dialogs
    commentsPost?.let { post ->
        CommentsBottomSheet(
            post = post,
            comments = activeComments,
            currentUserId = currentUserId,
            onDismiss = {
                vm.setActiveCommentsPost(null)
                commentsPost = null
            },
            onAddComment = { text, parentId -> vm.addComment(post, text, parentId) },
            onEditComment = { cid, newText -> vm.editComment(post.id, cid, newText) },
            onDeleteComment = { cid -> vm.deleteComment(post.id, cid) }
        )
    }

    editingPost?.let { post ->
        EditPostDialog(
            post = post,
            onDismiss = { editingPost = null },
            onSave = { title, cap, tags, loc ->
                vm.editPost(post.id, title, cap, tags, loc)
                editingPost = null
            }
        )
    }

    reportTarget?.let { (post, type) ->
        ReportContentDialog(
            targetType = type,
            onDismiss = { reportTarget = null },
            onSubmit = { reason, details ->
                vm.submitReport(type, post.id, reason, details)
                reportTarget = null
            }
        )
    }

    playlistTargetPost?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = playlists,
            onDismiss = { playlistTargetPost = null },
            onAddToExisting = { plId ->
                vm.addTrackToPlaylist(plId, track.id)
                playlistTargetPost = null
            },
            onCreateNew = { title ->
                vm.createPlaylist(title, "Curated tracks", track.id)
                playlistTargetPost = null
            }
        )
    }

    if (showFullAudioSheet && audioState.activeTrack != null) {
        FullAudioPlayerSheet(
            audioState = audioState,
            onDismiss = { showFullAudioSheet = false },
            onTogglePlay = { vm.audioPlayer.togglePlayPause() },
            onSeekTo = { vm.audioPlayer.seekTo(it) },
            onSeekBy = { vm.audioPlayer.seekBy(it) },
            onVolumeChange = { vm.audioPlayer.setVolume(it) }
        )
    }
}
