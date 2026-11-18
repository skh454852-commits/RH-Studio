package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Conversation
import com.example.model.DirectMessage
import com.example.model.FollowEdge
import com.example.model.PostComment
import com.example.model.SocialPost
import com.example.model.StoryItem
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
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

class SocialRepository(
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

    fun observeFeedPosts(): Flow<List<SocialPost>> {
        val path = "posts"
        return db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snap ->
                snap.toObjects(SocialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.status == "active" }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeStories(): Flow<List<StoryItem>> {
        val path = "stories"
        return db.collection("stories")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snap -> snap.toObjects(StoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createStory(
        author: UserProfile,
        caption: String,
        imageBase64: String,
        accentHex: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("stories").document()
        return try {
            val cleanCaption = caption.trim().take(140)
            require(cleanCaption.isNotEmpty()) { "Story caption cannot be empty" }
            val payload = mutableMapOf<String, Any>(
                "authorId" to uid,
                "authorName" to author.displayName.ifBlank { "Creator" }.take(60),
                "caption" to cleanCaption,
                "accentHex" to accentHex.take(16),
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (author.avatarBase64.isNotBlank()) payload["authorAvatarBase64"] = author.avatarBase64.take(340_000)
            if (imageBase64.isNotBlank()) payload["imageBase64"] = imageBase64.take(440_000)
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun createPost(
        author: UserProfile,
        mediaType: String,
        title: String,
        caption: String,
        hashtags: List<String>,
        location: String,
        mediaPreviewBase64: String,
        localMediaPath: String,
        mimeType: String,
        durationSeconds: Long,
        trimStartMs: Long = 0L,
        trimEndMs: Long = 0L,
        copyrightConfirmed: Boolean
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("posts").document()
        return try {
            require(copyrightConfirmed) { "Copyright confirmation is required." }
            val cleanTags = hashtags.map { it.trim().removePrefix("#").take(30) }.filter { it.isNotEmpty() }.take(15)
            val payload = mutableMapOf<String, Any>(
                "authorId" to uid,
                "authorName" to author.displayName.ifBlank { "Creator" }.take(60),
                "authorUsername" to author.username.ifBlank { "creator" }.take(32),
                "mediaType" to mediaType,
                "title" to title.trim().take(120),
                "caption" to caption.trim().take(1000),
                "hashtags" to cleanTags,
                "location" to location.trim().take(100),
                "localMediaPath" to localMediaPath.take(400),
                "mimeType" to mimeType.take(80),
                "durationSeconds" to durationSeconds.coerceAtLeast(0L),
                "trimStartMs" to trimStartMs.coerceAtLeast(0L),
                "trimEndMs" to trimEndMs.coerceAtLeast(0L),
                "chunkCount" to 0L,
                "copyrightConfirmed" to copyrightConfirmed,
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "viewsCount" to 0L,
                "status" to "active",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (author.avatarBase64.isNotBlank()) {
                payload["authorAvatarBase64"] = author.avatarBase64.take(340_000)
            }
            if (mediaPreviewBase64.isNotBlank()) {
                payload["mediaPreviewBase64"] = mediaPreviewBase64.take(440_000)
            }

            val batch = db.batch()
            batch.set(ref, payload)
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "postsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun updatePost(
        postId: String,
        title: String,
        caption: String,
        hashtags: List<String>,
        location: String
    ): Result<Unit> {
        requireUserId()
        val ref = db.collection("posts").document(postId)
        return try {
            val cleanTags = hashtags.map { it.trim().removePrefix("#").take(30) }.filter { it.isNotEmpty() }.take(15)
            ref.update(
                mapOf(
                    "title" to title.trim().take(120),
                    "caption" to caption.trim().take(1000),
                    "hashtags" to cleanTags,
                    "location" to location.trim().take(100),
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

    suspend fun deletePost(postId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("posts").document(postId)
        return try {
            val batch = db.batch()
            batch.delete(ref)
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "postsCount" to FieldValue.increment(-1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun toggleLike(
        post: SocialPost,
        actorName: String,
        currentlyLiked: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val postRef = db.collection("posts").document(post.id)
        val likeRef = postRef.collection("likes").document(uid)
        return try {
            val batch = db.batch()
            if (currentlyLiked) {
                batch.delete(likeRef)
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    likeRef,
                    mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())
                )
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (post.authorId != uid) {
                    val notifRef = db.collection("users").document(post.authorId)
                        .collection("notifications").document()
                    batch.set(
                        notifRef,
                        mapOf(
                            "userId" to post.authorId,
                            "actorId" to uid,
                            "actorName" to actorName.ifBlank { "Someone" }.take(60),
                            "type" to "like",
                            "title" to "New Like",
                            "body" to "$actorName liked your ${post.mediaType} post.",
                            "targetPostId" to post.id,
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, postRef.path)
            Result.failure(e)
        }
    }

    suspend fun isPostLikedByMe(postId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("posts").document(postId)
                .collection("likes").document(uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    fun observeSavedPostIds(userId: String): Flow<Set<String>> {
        val path = "users/$userId/saved_posts"
        return db.collection("users").document(userId)
            .collection("saved_posts")
            .snapshots()
            .map { snap -> snap.documents.map { it.id }.toSet() }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun toggleSavePost(postId: String, currentlySaved: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("users").document(uid).collection("saved_posts").document(postId)
        return try {
            if (currentlySaved) {
                ref.delete().await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "postId" to postId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    fun observeComments(postId: String): Flow<List<PostComment>> {
        val path = "posts/$postId/comments"
        return db.collection("posts").document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addComment(
        post: SocialPost,
        author: UserProfile,
        text: String,
        parentCommentId: String = ""
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val postRef = db.collection("posts").document(post.id)
        val commentRef = postRef.collection("comments").document()
        return try {
            val cleanText = text.trim().take(500)
            require(cleanText.isNotEmpty()) { "Comment cannot be empty" }
            val batch = db.batch()
            val payload = mutableMapOf<String, Any>(
                "postId" to post.id,
                "authorId" to uid,
                "authorName" to author.displayName.ifBlank { "Creator" }.take(60),
                "authorUsername" to author.username.ifBlank { "creator" }.take(32),
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (parentCommentId.isNotBlank()) {
                payload["parentCommentId"] = parentCommentId.take(128)
            }
            batch.set(commentRef, payload)
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (post.authorId != uid) {
                val notifRef = db.collection("users").document(post.authorId)
                    .collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "userId" to post.authorId,
                        "actorId" to uid,
                        "actorName" to author.displayName.ifBlank { "Someone" }.take(60),
                        "type" to "comment",
                        "title" to "New Comment",
                        "body" to "${author.displayName}: ${cleanText.take(80)}",
                        "targetPostId" to post.id,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateComment(postId: String, commentId: String, newText: String): Result<Unit> {
        requireUserId()
        val ref = db.collection("posts").document(postId).collection("comments").document(commentId)
        return try {
            val cleanText = newText.trim().take(500)
            require(cleanText.isNotEmpty()) { "Comment cannot be empty" }
            ref.update(
                mapOf(
                    "text" to cleanText,
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

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        requireUserId()
        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document(commentId)
        return try {
            val batch = db.batch()
            batch.delete(commentRef)
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(-1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun incrementViewCount(postId: String) {
        if (auth.currentUser == null) return
        try {
            db.collection("posts").document(postId).update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
        }
    }

    fun observeFollowingIds(userId: String): Flow<Set<String>> {
        val path = "users/$userId/following"
        return db.collection("users").document(userId)
            .collection("following")
            .snapshots()
            .map { snap -> snap.documents.map { it.id }.toSet() }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeFollowers(userId: String): Flow<List<FollowEdge>> {
        return db.collection("users").document(userId).collection("followers")
            .snapshots()
            .map { it.toObjects(FollowEdge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    fun observeFollowing(userId: String): Flow<List<FollowEdge>> {
        return db.collection("users").document(userId).collection("following")
            .snapshots()
            .map { it.toObjects(FollowEdge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
    }

    suspend fun toggleFollow(
        targetUser: UserProfile,
        actorName: String,
        currentlyFollowing: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (targetUser.userId == uid) return Result.failure(IllegalArgumentException("Cannot follow yourself"))
        val myFollowingRef = db.collection("users").document(uid).collection("following").document(targetUser.userId)
        val targetFollowerRef = db.collection("users").document(targetUser.userId).collection("followers").document(uid)
        val myUserRef = db.collection("users").document(uid)
        val targetUserRef = db.collection("users").document(targetUser.userId)

        return try {
            val batch = db.batch()
            if (currentlyFollowing) {
                batch.delete(myFollowingRef)
                batch.delete(targetFollowerRef)
                batch.update(myUserRef, mapOf("followingCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp()))
                batch.update(targetUserRef, mapOf("followersCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp()))
            } else {
                val edge = mapOf(
                    "actorId" to uid,
                    "targetId" to targetUser.userId,
                    "actorName" to actorName.ifBlank { "Creator" }.take(60),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                batch.set(myFollowingRef, edge)
                batch.set(targetFollowerRef, edge)
                batch.update(myUserRef, mapOf("followingCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp()))
                batch.update(targetUserRef, mapOf("followersCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp()))

                val notifRef = db.collection("users").document(targetUser.userId).collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "userId" to targetUser.userId,
                        "actorId" to uid,
                        "actorName" to actorName.ifBlank { "Someone" }.take(60),
                        "type" to "follow",
                        "title" to "New Follower",
                        "body" to "$actorName started following you.",
                        "targetPostId" to "",
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, targetUserRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        reason: String,
        details: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("reports").document()
        return try {
            val payload = mapOf(
                "reporterId" to uid,
                "targetType" to targetType,
                "targetId" to targetId.take(128),
                "reason" to reason.trim().take(100),
                "details" to details.trim().take(500),
                "status" to "pending",
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

    fun observeConversations(userId: String): Flow<List<Conversation>> {
        val path = "conversations"
        return db.collection("conversations")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMessages(conversationId: String): Flow<List<DirectMessage>> {
        val path = "conversations/$conversationId/messages"
        return db.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getOrCreateConversation(myProfile: UserProfile, otherUser: UserProfile): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val sortedIds = listOf(uid, otherUser.userId).sorted()
        val convId = "${sortedIds[0]}_${sortedIds[1]}"
        val ref = db.collection("conversations").document(convId)
        return try {
            if (!ref.get().await().exists()) {
                ref.set(
                    mapOf(
                        "participantIds" to sortedIds,
                        "participantNames" to mapOf(
                            uid to myProfile.displayName.ifBlank { "Creator" },
                            otherUser.userId to otherUser.displayName.ifBlank { "Creator" }
                        ),
                        "lastMessage" to "Conversation started",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        participantIds: List<String>,
        text: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val convRef = db.collection("conversations").document(conversationId)
        val msgRef = convRef.collection("messages").document()
        return try {
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotEmpty()) { "Message cannot be empty" }
            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "conversationId" to conversationId,
                    "senderId" to uid,
                    "participantIds" to participantIds,
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                convRef,
                mapOf(
                    "lastMessage" to cleanText.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    private fun logFlowError(error: Throwable, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val ex = firestoreError ?: (error as? Exception)
        if (ex != null && auth.currentUser != null) {
            handleFirestoreError(ex, op, path)
        }
    }
}
