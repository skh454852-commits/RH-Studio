package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.data.AudioPlaybackState
import com.example.data.AudioPlayerManager
import com.example.data.MonetizationAdminRepository
import com.example.data.SocialRepository
import com.example.data.UserRepository
import com.example.model.AdminSettings
import com.example.model.BlockedUserEdge
import com.example.model.ContentReport
import com.example.model.Conversation
import com.example.model.CreatorApplication
import com.example.model.CreatorEarnings
import com.example.model.DirectMessage
import com.example.model.FollowEdge
import com.example.model.LedgerTransaction
import com.example.model.NotificationItem
import com.example.model.Playlist
import com.example.model.PostComment
import com.example.model.SocialPost
import com.example.model.StoryItem
import com.example.model.UiState
import com.example.model.UserPrivateSettings
import com.example.model.UserProfile
import com.example.model.WithdrawalRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PulseViewModel(
    private val userRepo: UserRepository,
    private val socialRepo: SocialRepository,
    private val monetizationRepo: MonetizationAdminRepository,
    private val appPrefs: AppPreferences,
    val audioPlayer: AudioPlayerManager,
    val currentUserId: String
) : ViewModel() {

    val isAdmin: Boolean = monetizationRepo.isCurrentUserAdmin()

    private val _actionBanner = MutableStateFlow<String?>(null)
    val actionBanner: StateFlow<String?> = _actionBanner.asStateFlow()

    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    private val _activePostIdForComments = MutableStateFlow<String?>(null)
    private val _activeConversationId = MutableStateFlow<String?>(null)

    val audioState: StateFlow<AudioPlaybackState> = audioPlayer.state

    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                userRepo.ensureUserBootstrap(user)
            }
        }
    }

    private fun <T> Flow<T>.asUiState(label: String, initial: UiState<T> = UiState.Loading): StateFlow<UiState<T>> {
        return this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = initial
            )
    }

    val currentUserProfile: StateFlow<UserProfile?> = userRepo.observeUserProfile(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val privateSettings: StateFlow<UserPrivateSettings?> = userRepo.observePrivateSettings(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val allUsers: StateFlow<List<UserProfile>> = userRepo.observeAllUsers()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val blockedUsers: StateFlow<List<BlockedUserEdge>> = userRepo.observeBlockedUsers(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val feedState: StateFlow<UiState<List<SocialPost>>> = combine(
        socialRepo.observeFeedPosts(),
        blockedUsers
    ) { posts, blocked ->
        val blockedIds = blocked.filter { !it.muteOnly }.map { it.blockedUserId }.toSet()
        posts.filter { it.authorId !in blockedIds && it.status == "active" }
    }.asUiState("feed")

    val stories: StateFlow<List<StoryItem>> = socialRepo.observeStories()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val followingIds: StateFlow<Set<String>> = socialRepo.observeFollowingIds(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    val savedPostIds: StateFlow<Set<String>> = socialRepo.observeSavedPostIds(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    val notifications: StateFlow<List<NotificationItem>> = userRepo.observeNotifications(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val playlists: StateFlow<List<Playlist>> = userRepo.observePlaylists(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminSettings: StateFlow<AdminSettings> = monetizationRepo.observeAdminSettings()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AdminSettings())

    val creatorEarnings: StateFlow<CreatorEarnings> = monetizationRepo.observeCreatorEarnings(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CreatorEarnings(userId = currentUserId))

    val creatorApplication: StateFlow<CreatorApplication?> = monetizationRepo.observeCreatorApplication(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val transactions: StateFlow<List<LedgerTransaction>> = monetizationRepo.observeUserTransactions(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val userWithdrawals: StateFlow<List<WithdrawalRequest>> = monetizationRepo.observeUserWithdrawals(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val conversations: StateFlow<List<Conversation>> = socialRepo.observeConversations(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @Suppress("OPT_IN_USAGE")
    val activeComments: StateFlow<List<PostComment>> = _activePostIdForComments
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) flowOf(emptyList())
            else socialRepo.observeComments(postId)
        }
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @Suppress("OPT_IN_USAGE")
    val activeMessages: StateFlow<List<DirectMessage>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) flowOf(emptyList())
            else socialRepo.observeMessages(convId)
        }
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // Admin-only streams (never attached for non-admin sessions)
    val adminReports: StateFlow<List<ContentReport>> = if (isAdmin) {
        monetizationRepo.observeAllReports()
            .catch { if (it is CancellationException) throw it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
    } else MutableStateFlow(emptyList())

    val adminApplications: StateFlow<List<CreatorApplication>> = if (isAdmin) {
        monetizationRepo.observeAllCreatorApplications()
            .catch { if (it is CancellationException) throw it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
    } else MutableStateFlow(emptyList())

    val adminWithdrawals: StateFlow<List<WithdrawalRequest>> = if (isAdmin) {
        monetizationRepo.observeAllWithdrawals()
            .catch { if (it is CancellationException) throw it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
    } else MutableStateFlow(emptyList())

    fun clearBanner() {
        _actionBanner.value = null
    }

    fun showBanner(msg: String) {
        _actionBanner.value = msg
    }

    fun setActiveCommentsPost(postId: String?) {
        _activePostIdForComments.value = postId
        if (!postId.isNullOrBlank()) {
            viewModelScope.launch {
                val liked = socialRepo.isPostLikedByMe(postId)
                _likedPostIds.value = if (liked) _likedPostIds.value + postId else _likedPostIds.value - postId
                socialRepo.incrementViewCount(postId)
            }
        }
    }

    fun setActiveConversation(convId: String?) {
        _activeConversationId.value = convId
    }

    fun syncLikeState(postId: String) {
        viewModelScope.launch {
            val liked = socialRepo.isPostLikedByMe(postId)
            _likedPostIds.value = if (liked) _likedPostIds.value + postId else _likedPostIds.value - postId
        }
    }

    fun toggleLike(post: SocialPost) {
        val currentlyLiked = post.id in _likedPostIds.value
        val actorName = currentUserProfile.value?.displayName ?: "Creator"
        _likedPostIds.value = if (currentlyLiked) _likedPostIds.value - post.id else _likedPostIds.value + post.id
        viewModelScope.launch {
            val res = socialRepo.toggleLike(post, actorName, currentlyLiked)
            if (res.isFailure) {
                _likedPostIds.value = if (currentlyLiked) _likedPostIds.value + post.id else _likedPostIds.value - post.id
                _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update like"
            }
        }
    }

    fun toggleSave(postId: String) {
        val saved = postId in savedPostIds.value
        viewModelScope.launch {
            val res = socialRepo.toggleSavePost(postId, saved)
            if (res.isFailure) {
                _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save post"
            } else {
                _actionBanner.value = if (saved) "Removed from saved" else "Saved to your library"
            }
        }
    }

    fun toggleFollow(targetUser: UserProfile) {
        val following = targetUser.userId in followingIds.value
        val actorName = currentUserProfile.value?.displayName ?: "Creator"
        viewModelScope.launch {
            val res = socialRepo.toggleFollow(targetUser, actorName, following)
            if (res.isFailure) {
                _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update follow"
            }
        }
    }

    fun publishPost(
        mediaType: String,
        title: String,
        caption: String,
        hashtagsRaw: String,
        location: String,
        mediaPreviewBase64: String,
        localMediaPath: String,
        mimeType: String,
        durationSeconds: Long,
        trimStartMs: Long = 0L,
        trimEndMs: Long = 0L,
        copyrightConfirmed: Boolean,
        onPublished: () -> Unit
    ) {
        val profile = currentUserProfile.value ?: UserProfile(
            userId = currentUserId,
            username = "creator_${currentUserId.take(4)}",
            displayName = Firebase.auth.currentUser?.displayName ?: "Creator"
        )
        val tags = hashtagsRaw.split(",", " ", "#")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        viewModelScope.launch {
            val res = socialRepo.createPost(
                author = profile,
                mediaType = mediaType,
                title = title,
                caption = caption,
                hashtags = tags,
                location = location,
                mediaPreviewBase64 = mediaPreviewBase64,
                localMediaPath = localMediaPath,
                mimeType = mimeType,
                durationSeconds = durationSeconds,
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                copyrightConfirmed = copyrightConfirmed
            )
            if (res.isSuccess) {
                _actionBanner.value = "Published ${mediaType.replaceFirstChar { it.uppercase() }} post!"
                onPublished()
            } else {
                _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to publish post"
            }
        }
    }

    fun publishStory(caption: String, imageBase64: String, accentHex: String) {
        val profile = currentUserProfile.value ?: return
        viewModelScope.launch {
            val res = socialRepo.createStory(profile, caption, imageBase64, accentHex)
            _actionBanner.value = if (res.isSuccess) "Story posted!" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun editPost(postId: String, title: String, caption: String, hashtagsRaw: String, location: String) {
        val tags = hashtagsRaw.split(",", " ", "#").map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch {
            val res = socialRepo.updatePost(postId, title, caption, tags, location)
            _actionBanner.value = if (res.isSuccess) "Post updated" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            val res = socialRepo.deletePost(postId)
            _actionBanner.value = if (res.isSuccess) "Post deleted" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun addComment(post: SocialPost, text: String, parentCommentId: String = "") {
        val profile = currentUserProfile.value ?: return
        viewModelScope.launch {
            val res = socialRepo.addComment(post, profile, text, parentCommentId)
            if (res.isFailure) {
                _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to post comment"
            }
        }
    }

    fun editComment(postId: String, commentId: String, newText: String) {
        viewModelScope.launch {
            val res = socialRepo.updateComment(postId, commentId, newText)
            if (res.isFailure) _actionBanner.value = res.exceptionOrNull()?.localizedMessage
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            val res = socialRepo.deleteComment(postId, commentId)
            if (res.isFailure) _actionBanner.value = res.exceptionOrNull()?.localizedMessage
        }
    }

    fun submitReport(targetType: String, targetId: String, reason: String, details: String) {
        viewModelScope.launch {
            val res = socialRepo.submitReport(targetType, targetId, reason, details)
            _actionBanner.value = if (res.isSuccess) {
                "Report submitted for moderation review."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to submit report"
            }
        }
    }

    fun blockOrMuteUser(targetUid: String, targetUsername: String, muteOnly: Boolean) {
        viewModelScope.launch {
            val res = userRepo.blockOrMuteUser(targetUid, targetUsername, muteOnly)
            _actionBanner.value = if (res.isSuccess) {
                if (muteOnly) "Muted @$targetUsername" else "Blocked @$targetUsername"
            } else {
                res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun unblockUser(targetUid: String) {
        viewModelScope.launch {
            val res = userRepo.unblockUser(targetUid)
            _actionBanner.value = if (res.isSuccess) "User unblocked" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun createPlaylist(title: String, description: String, initialTrackId: String? = null) {
        viewModelScope.launch {
            val res = userRepo.createPlaylist(title, description, initialTrackId)
            _actionBanner.value = if (res.isSuccess) "Playlist created" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            val res = userRepo.addTrackToPlaylist(playlistId, trackId)
            _actionBanner.value = if (res.isSuccess) "Added to playlist" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            val res = userRepo.deletePlaylist(playlistId)
            _actionBanner.value = if (res.isSuccess) "Playlist deleted" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            userRepo.markNotificationRead(notifId)
        }
    }

    fun startDirectMessage(otherUser: UserProfile, onReady: (String) -> Unit) {
        val me = currentUserProfile.value ?: return
        viewModelScope.launch {
            val res = socialRepo.getOrCreateConversation(me, otherUser)
            res.onSuccess(onReady)
            res.onFailure { _actionBanner.value = it.localizedMessage ?: "Unable to start chat" }
        }
    }

    fun sendDirectMessage(conversation: Conversation, text: String) {
        viewModelScope.launch {
            val res = socialRepo.sendMessage(conversation.id, conversation.participantIds, text)
            if (res.isFailure) _actionBanner.value = res.exceptionOrNull()?.localizedMessage
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        avatarBase64: String? = null,
        coverBase64: String? = null,
        privateProfile: Boolean,
        allowDms: Boolean
    ) {
        viewModelScope.launch {
            val res = userRepo.updateProfile(displayName, username, bio, avatarBase64, coverBase64, privateProfile, allowDms)
            _actionBanner.value = if (res.isSuccess) "Profile saved" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun updateSettings(
        payoutProvider: String,
        maskedDestination: String,
        notifyLikes: Boolean,
        notifyComments: Boolean,
        notifyFollows: Boolean,
        notifyEarnings: Boolean,
        themeMode: String
    ) {
        viewModelScope.launch {
            appPrefs.setThemeMode(themeMode)
            val res = userRepo.updatePrivateSettings(
                payoutProvider, maskedDestination, notifyLikes, notifyComments, notifyFollows, notifyEarnings, themeMode
            )
            _actionBanner.value = if (res.isSuccess) "Preferences updated" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            appPrefs.setThemeMode(mode)
            val current = privateSettings.value
            if (current != null) {
                userRepo.updatePrivateSettings(
                    current.payoutProvider,
                    current.maskedPayoutDestination,
                    current.notifyLikes,
                    current.notifyComments,
                    current.notifyFollows,
                    current.notifyEarnings,
                    mode
                )
            }
        }
    }

    fun applyForMonetization(category: String, notes: String, totalViews: Long) {
        val profile = currentUserProfile.value ?: return
        viewModelScope.launch {
            val res = monetizationRepo.submitCreatorApplication(profile, category, notes, totalViews)
            _actionBanner.value = if (res.isSuccess) {
                "Application submitted for admin review."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Application failed"
            }
        }
    }

    fun requestWithdrawal(amountUsd: Double, provider: String, maskedDest: String) {
        val profile = currentUserProfile.value ?: return
        val earnings = creatorEarnings.value
        val minLimit = adminSettings.value.minWithdrawalUsd
        viewModelScope.launch {
            val res = monetizationRepo.requestWithdrawal(profile, earnings, amountUsd, provider, maskedDest, minLimit)
            _actionBanner.value = if (res.isSuccess) {
                "Withdrawal request submitted for admin approval."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Withdrawal request failed"
            }
        }
    }

    // Admin actions
    fun saveAdminSettings(settings: AdminSettings) {
        viewModelScope.launch {
            val res = monetizationRepo.saveAdminSettings(settings)
            _actionBanner.value = if (res.isSuccess) "Platform settings saved" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun reviewCreatorApp(targetUserId: String, approved: Boolean, note: String) {
        viewModelScope.launch {
            val res = monetizationRepo.reviewCreatorApplication(targetUserId, approved, note)
            _actionBanner.value = if (res.isSuccess) {
                if (approved) "Creator approved for monetization" else "Creator application rejected"
            } else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun creditVerifiedRevenue(targetUserId: String, views: Long, amountUsd: Double, desc: String) {
        viewModelScope.launch {
            val res = monetizationRepo.adminCreditVerifiedRevenue(targetUserId, views, amountUsd, desc)
            _actionBanner.value = if (res.isSuccess) {
                "Verified revenue credited"
            } else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun reviewWithdrawal(req: WithdrawalRequest, newStatus: String, note: String) {
        viewModelScope.launch {
            val res = monetizationRepo.reviewWithdrawalRequest(req, newStatus, note)
            _actionBanner.value = if (res.isSuccess) "Withdrawal marked $newStatus" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun resolveReport(reportId: String, status: String, removePostId: String? = null) {
        viewModelScope.launch {
            if (!removePostId.isNullOrBlank()) {
                monetizationRepo.adminRemovePost(removePostId)
            }
            val res = monetizationRepo.resolveReport(reportId, status)
            _actionBanner.value = if (res.isSuccess) "Report $status" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun moderateUser(targetUserId: String, verified: Boolean, accountStatus: String) {
        viewModelScope.launch {
            val res = monetizationRepo.adminUpdateUserModeration(targetUserId, verified, accountStatus)
            _actionBanner.value = if (res.isSuccess) "User updated" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            val res = userRepo.deleteAccountAndData()
            if (res.isSuccess) onComplete()
            else _actionBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Account deletion may require recent sign-in"
        }
    }

    fun observeFollowersList(userId: String): Flow<List<FollowEdge>> = socialRepo.observeFollowers(userId)
    fun observeFollowingList(userId: String): Flow<List<FollowEdge>> = socialRepo.observeFollowing(userId)

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    private companion object {
        const val TAG = "PulseViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
