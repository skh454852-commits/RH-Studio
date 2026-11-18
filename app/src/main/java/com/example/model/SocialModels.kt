package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarBase64: String = "",
    val coverBase64: String = "",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    val verified: Boolean = false,
    val monetizationStatus: String = "not_applied",
    val accountStatus: String = "active",
    val privateProfile: Boolean = false,
    val allowDms: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPrivateSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val payoutProvider: String = "none",
    val maskedPayoutDestination: String = "",
    val notifyLikes: Boolean = true,
    val notifyComments: Boolean = true,
    val notifyFollows: Boolean = true,
    val notifyEarnings: Boolean = true,
    val themeMode: String = "system",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorAvatarBase64: String = "",
    val mediaType: String = "photo", // photo, video, music, audio
    val title: String = "",
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val location: String = "",
    val mediaPreviewBase64: String = "",
    val localMediaPath: String = "",
    val mimeType: String = "",
    val durationSeconds: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val chunkCount: Long = 0L,
    val copyrightConfirmed: Boolean = true,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val viewsCount: Long = 0L,
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val text: String = "",
    val parentCommentId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoryItem(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarBase64: String = "",
    val caption: String = "",
    val imageBase64: String = "",
    val accentHex: String = "#6366F1",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val type: String = "system",
    val title: String = "",
    val body: String = "",
    val targetPostId: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Playlist(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val trackIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BlockedUserEdge(
    @DocumentId val id: String = "",
    val userId: String = "",
    val blockedUserId: String = "",
    val blockedUsername: String = "",
    val muteOnly: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowEdge(
    @DocumentId val id: String = "",
    val actorId: String = "",
    val targetId: String = "",
    val actorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CreatorApplication(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val category: String = "",
    val notes: String = "",
    val followersSnapshot: Long = 0L,
    val viewsSnapshot: Long = 0L,
    val status: String = "pending",
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CreatorEarnings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val eligibleViews: Long = 0L,
    val estimatedRevenueUsd: Double = 0.0,
    val monthlyEarningsUsd: Double = 0.0,
    val availableBalanceUsd: Double = 0.0,
    val pendingBalanceUsd: Double = 0.0,
    val totalWithdrawnUsd: Double = 0.0,
    val monetizationStatus: String = "not_applied",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LedgerTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "ad_revenue",
    val amountUsd: Double = 0.0,
    val description: String = "",
    val status: String = "completed",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val amountUsd: Double = 0.0,
    val payoutProvider: String = "upi",
    val maskedDestination: String = "",
    val status: String = "pending",
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ContentReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "post",
    val targetId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminSettings(
    @DocumentId val id: String = "",
    val minFollowersForMonetization: Long = 100L,
    val minPostsForMonetization: Long = 5L,
    val minViewsForMonetization: Long = 1000L,
    val creatorRevenueSharePercent: Long = 55L,
    val minWithdrawalUsd: Double = 25.0,
    // Google official AdMob Test IDs used during development
    val adMobAppId: String = "ca-app-pub-3940256099942544~3347511713",
    val bannerAdUnitId: String = "ca-app-pub-3940256099942544/6300978111",
    val nativeAdUnitId: String = "ca-app-pub-3940256099942544/2247696110",
    val rewardedAdUnitId: String = "ca-app-pub-3940256099942544/5224354917",
    val adsEnabled: Boolean = true,
    val autoModerationEnabled: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
