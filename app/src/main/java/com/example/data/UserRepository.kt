package com.example.data

import android.content.Context
import com.example.R
import com.example.model.BlockedUserEdge
import com.example.model.NotificationItem
import com.example.model.Playlist
import com.example.model.UserPrivateSettings
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    suspend fun ensureUserBootstrap(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val userPath = "users/$uid"
        return try {
            val userRef = db.collection("users").document(uid)
            val existingUser = userRef.get().await()
            if (!existingUser.exists()) {
                val baseHandle = (user.email?.substringBefore("@") ?: user.displayName ?: "creator")
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]"), "")
                    .take(20)
                    .ifBlank { "creator" }
                val handle = "${baseHandle}_${uid.take(4).lowercase()}"
                val displayName = user.displayName?.take(50)?.ifBlank { "Pulse Creator" } ?: "Pulse Creator"

                val profilePayload = mapOf(
                    "userId" to uid,
                    "username" to handle,
                    "displayName" to displayName,
                    "bio" to "Sharing photos, videos, music & audio on PulseStream.",
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "postsCount" to 0L,
                    "verified" to false,
                    "monetizationStatus" to "not_applied",
                    "accountStatus" to "active",
                    "privateProfile" to false,
                    "allowDms" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(profilePayload).await()
            }

            val privateRef = userRef.collection("private").document("settings")
            if (!privateRef.get().await().exists()) {
                val privatePayload = mapOf(
                    "userId" to uid,
                    "email" to (user.email ?: ""),
                    "payoutProvider" to "none",
                    "maskedPayoutDestination" to "",
                    "notifyLikes" to true,
                    "notifyComments" to true,
                    "notifyFollows" to true,
                    "notifyEarnings" to true,
                    "themeMode" to "system",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                privateRef.set(privatePayload).await()
            }

            val earningsRef = db.collection("creator_earnings").document(uid)
            if (!earningsRef.get().await().exists()) {
                val earningsPayload = mapOf(
                    "userId" to uid,
                    "eligibleViews" to 0L,
                    "estimatedRevenueUsd" to 0.0,
                    "monthlyEarningsUsd" to 0.0,
                    "availableBalanceUsd" to 0.0,
                    "pendingBalanceUsd" to 0.0,
                    "totalWithdrawnUsd" to 0.0,
                    "monetizationStatus" to "not_applied",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                earningsRef.set(earningsPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logFlowError(error, OperationType.GET, path, userId)
                throw error
            }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection("users")
            .limit(100)
            .snapshots()
            .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, auth.currentUser?.uid)
                throw error
            }
    }

    fun observePrivateSettings(userId: String): Flow<UserPrivateSettings?> {
        val path = "users/$userId/private/settings"
        return db.collection("users").document(userId)
            .collection("private").document("settings")
            .snapshots()
            .map { snap -> snap.toObject(UserPrivateSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logFlowError(error, OperationType.GET, path, userId)
                throw error
            }
    }

    suspend fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        avatarBase64: String? = null,
        coverBase64: String? = null,
        privateProfile: Boolean,
        allowDms: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_]"), "").take(32)
            require(cleanUsername.length >= 2) { "Username must be at least 2 alphanumeric/underscore characters" }
            val cleanDisplay = displayName.trim().take(60)
            require(cleanDisplay.isNotEmpty()) { "Display name cannot be empty" }

            val updates = mutableMapOf<String, Any>(
                "username" to cleanUsername,
                "displayName" to cleanDisplay,
                "bio" to bio.trim().take(300),
                "privateProfile" to privateProfile,
                "allowDms" to allowDms,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (avatarBase64 != null) updates["avatarBase64"] = avatarBase64.take(340_000)
            if (coverBase64 != null) updates["coverBase64"] = coverBase64.take(440_000)

            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePrivateSettings(
        payoutProvider: String,
        maskedDestination: String,
        notifyLikes: Boolean,
        notifyComments: Boolean,
        notifyFollows: Boolean,
        notifyEarnings: Boolean,
        themeMode: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/private/settings"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "email" to (auth.currentUser?.email ?: ""),
                "payoutProvider" to payoutProvider,
                "maskedPayoutDestination" to maskedDestination.trim().take(100),
                "notifyLikes" to notifyLikes,
                "notifyComments" to notifyComments,
                "notifyFollows" to notifyFollows,
                "notifyEarnings" to notifyEarnings,
                "themeMode" to themeMode,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("private").document("settings")
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> {
        val path = "users/$userId/notifications"
        return db.collection("users").document(userId)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(60)
            .snapshots()
            .map { snap -> snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/notifications/$notifId"
        return try {
            db.collection("users").document(uid)
                .collection("notifications").document(notifId)
                .update("read", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeBlockedUsers(userId: String): Flow<List<BlockedUserEdge>> {
        val path = "users/$userId/blocked"
        return db.collection("users").document(userId)
            .collection("blocked")
            .snapshots()
            .map { snap -> snap.toObjects(BlockedUserEdge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    suspend fun blockOrMuteUser(targetUid: String, targetUsername: String, muteOnly: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/blocked/$targetUid"
        return try {
            require(targetUid != uid) { "Cannot block yourself" }
            val payload = mapOf(
                "userId" to uid,
                "blockedUserId" to targetUid,
                "blockedUsername" to targetUsername.take(60),
                "muteOnly" to muteOnly,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("blocked").document(targetUid)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUid: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/blocked/$targetUid"
        return try {
            db.collection("users").document(uid)
                .collection("blocked").document(targetUid)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observePlaylists(userId: String): Flow<List<Playlist>> {
        val path = "users/$userId/playlists"
        return db.collection("users").document(userId)
            .collection("playlists")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(Playlist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    suspend fun createPlaylist(title: String, description: String, initialTrackId: String? = null): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("users").document(uid).collection("playlists").document()
        return try {
            val cleanTitle = title.trim().take(80)
            require(cleanTitle.isNotEmpty()) { "Playlist title cannot be empty" }
            val tracks = if (initialTrackId != null) listOf(initialTrackId) else emptyList()
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "description" to description.trim().take(240),
                "trackIds" to tracks,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("users").document(uid).collection("playlists").document(playlistId)
        return try {
            ref.update(
                mapOf(
                    "trackIds" to FieldValue.arrayUnion(trackId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("users").document(uid).collection("playlists").document(playlistId)
        return try {
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountAndData(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        return try {
            val userPosts = db.collection("posts").whereEqualTo("authorId", uid).get().await()
            for (doc in userPosts.documents) {
                doc.reference.delete().await()
            }
            db.collection("users").document(uid).collection("private").document("settings").delete().await()
            db.collection("creator_earnings").document(uid).delete().await()
            db.collection("users").document(uid).delete().await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }

    private fun logFlowError(error: Throwable, op: OperationType, path: String, expectedUid: String?) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val ex = firestoreError ?: (error as? Exception)
        if (ex != null && expectedUid != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(ex, op, path)
        }
    }
}
