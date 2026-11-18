package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AdminSettings
import com.example.model.ContentReport
import com.example.model.CreatorApplication
import com.example.model.CreatorEarnings
import com.example.model.LedgerTransaction
import com.example.model.UserProfile
import com.example.model.WithdrawalRequest
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

class MonetizationAdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun isCurrentUserAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email == "parmanwithout@gmail.com" && user.isEmailVerified
    }

    fun observeAdminSettings(): Flow<AdminSettings> {
        val path = "admin_settings/global"
        return db.collection("admin_settings").document("global")
            .snapshots()
            .map { snap ->
                snap.toObject(AdminSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: AdminSettings()
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun saveAdminSettings(settings: AdminSettings): Result<Unit> {
        val path = "admin_settings/global"
        return try {
            val payload = mapOf(
                "minFollowersForMonetization" to settings.minFollowersForMonetization.coerceAtLeast(0L),
                "minPostsForMonetization" to settings.minPostsForMonetization.coerceAtLeast(0L),
                "minViewsForMonetization" to settings.minViewsForMonetization.coerceAtLeast(0L),
                "creatorRevenueSharePercent" to settings.creatorRevenueSharePercent.coerceIn(0L, 90L),
                "minWithdrawalUsd" to settings.minWithdrawalUsd.coerceIn(1.0, 10000.0),
                "adMobAppId" to settings.adMobAppId.trim().take(128),
                "bannerAdUnitId" to settings.bannerAdUnitId.trim().take(128),
                "nativeAdUnitId" to settings.nativeAdUnitId.trim().take(128),
                "rewardedAdUnitId" to settings.rewardedAdUnitId.trim().take(128),
                "adsEnabled" to settings.adsEnabled,
                "autoModerationEnabled" to settings.autoModerationEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("admin_settings").document("global").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeCreatorEarnings(userId: String): Flow<CreatorEarnings> {
        val path = "creator_earnings/$userId"
        return db.collection("creator_earnings").document(userId)
            .snapshots()
            .map { snap ->
                snap.toObject(CreatorEarnings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: CreatorEarnings(userId = userId)
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeCreatorApplication(userId: String): Flow<CreatorApplication?> {
        val path = "creator_applications/$userId"
        return db.collection("creator_applications").document(userId)
            .snapshots()
            .map { snap ->
                snap.toObject(CreatorApplication::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun submitCreatorApplication(
        profile: UserProfile,
        category: String,
        notes: String,
        totalViews: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = db.collection("creator_applications").document(uid)
        return try {
            val payload = mapOf(
                "userId" to uid,
                "username" to profile.username.ifBlank { "creator" }.take(32),
                "category" to category.trim().take(40),
                "notes" to notes.trim().take(500),
                "followersSnapshot" to profile.followersCount.coerceAtLeast(0L),
                "viewsSnapshot" to totalViews.coerceAtLeast(0L),
                "status" to "pending",
                "adminNote" to "",
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

    fun observeUserTransactions(userId: String): Flow<List<LedgerTransaction>> {
        val path = "transactions"
        return db.collection("transactions")
            .whereEqualTo("userId", userId)
            .limit(50)
            .snapshots()
            .map { snap ->
                snap.toObjects(LedgerTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeUserWithdrawals(userId: String): Flow<List<WithdrawalRequest>> {
        val path = "withdrawal_requests"
        return db.collection("withdrawal_requests")
            .whereEqualTo("userId", userId)
            .limit(40)
            .snapshots()
            .map { snap ->
                snap.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun requestWithdrawal(
        profile: UserProfile,
        earnings: CreatorEarnings,
        amountUsd: Double,
        payoutProvider: String,
        maskedDestination: String,
        minWithdrawalUsd: Double
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val reqRef = db.collection("withdrawal_requests").document()
        val txRef = db.collection("transactions").document()
        val earningsRef = db.collection("creator_earnings").document(uid)
        return try {
            require(amountUsd >= minWithdrawalUsd) {
                "Minimum withdrawal threshold is $${String.format("%.2f", minWithdrawalUsd)}"
            }
            require(amountUsd <= earnings.availableBalanceUsd) {
                "Requested amount exceeds available balance."
            }
            require(maskedDestination.trim().length >= 3) {
                "Please configure a valid masked UPI or Bank payout destination."
            }

            val newAvailable = earnings.availableBalanceUsd - amountUsd
            val newPending = earnings.pendingBalanceUsd + amountUsd

            val batch = db.batch()
            batch.set(
                reqRef,
                mapOf(
                    "userId" to uid,
                    "username" to profile.username.ifBlank { "creator" }.take(32),
                    "amountUsd" to amountUsd,
                    "payoutProvider" to payoutProvider,
                    "maskedDestination" to maskedDestination.trim().take(100),
                    "status" to "pending",
                    "adminNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "type" to "withdrawal",
                    "amountUsd" to amountUsd,
                    "description" to "Withdrawal request (${payoutProvider.uppercase()})",
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                earningsRef,
                mapOf(
                    "availableBalanceUsd" to newAvailable,
                    "pendingBalanceUsd" to newPending,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, reqRef.path)
            Result.failure(e)
        }
    }

    // Admin queries and mutations
    fun observeAllReports(): Flow<List<ContentReport>> {
        return db.collection("reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .snapshots()
            .map { it.toObjects(ContentReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "reports")
                throw error
            }
    }

    fun observeAllCreatorApplications(): Flow<List<CreatorApplication>> {
        return db.collection("creator_applications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .snapshots()
            .map { it.toObjects(CreatorApplication::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "creator_applications")
                throw error
            }
    }

    fun observeAllWithdrawals(): Flow<List<WithdrawalRequest>> {
        return db.collection("withdrawal_requests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .snapshots()
            .map { it.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "withdrawal_requests")
                throw error
            }
    }

    suspend fun resolveReport(reportId: String, newStatus: String): Result<Unit> {
        val ref = db.collection("reports").document(reportId)
        return try {
            ref.update(
                mapOf(
                    "status" to newStatus,
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

    suspend fun adminRemovePost(postId: String): Result<Unit> {
        val ref = db.collection("posts").document(postId)
        return try {
            ref.update(
                mapOf(
                    "status" to "removed",
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

    suspend fun reviewCreatorApplication(
        targetUserId: String,
        approved: Boolean,
        adminNote: String
    ): Result<Unit> {
        val appRef = db.collection("creator_applications").document(targetUserId)
        val userRef = db.collection("users").document(targetUserId)
        val earnRef = db.collection("creator_earnings").document(targetUserId)
        val notifRef = userRef.collection("notifications").document()
        val actorId = auth.currentUser?.uid ?: targetUserId
        val newStatus = if (approved) "approved" else "rejected"
        return try {
            val batch = db.batch()
            batch.update(
                appRef,
                mapOf(
                    "status" to newStatus,
                    "adminNote" to adminNote.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "monetizationStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                earnRef,
                mapOf(
                    "monetizationStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "userId" to targetUserId,
                    "actorId" to actorId,
                    "actorName" to "PulseStream Monetization",
                    "type" to "monetization",
                    "title" to if (approved) "Monetization Approved" else "Monetization Update",
                    "body" to if (approved) "Your creator account is approved for verified ad revenue sharing." else "Your application status: $newStatus. $adminNote",
                    "targetPostId" to "",
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, appRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminCreditVerifiedRevenue(
        targetUserId: String,
        verifiedViewsAdded: Long,
        amountUsd: Double,
        description: String
    ): Result<Unit> {
        val earnRef = db.collection("creator_earnings").document(targetUserId)
        val txRef = db.collection("transactions").document()
        return try {
            require(amountUsd > 0.0) { "Amount must be positive" }
            val current = earnRef.get().await().toObject(CreatorEarnings::class.java)
                ?: CreatorEarnings(userId = targetUserId)
            val batch = db.batch()
            batch.set(
                earnRef,
                mapOf(
                    "userId" to targetUserId,
                    "eligibleViews" to (current.eligibleViews + verifiedViewsAdded.coerceAtLeast(0L)),
                    "estimatedRevenueUsd" to (current.estimatedRevenueUsd + amountUsd),
                    "monthlyEarningsUsd" to (current.monthlyEarningsUsd + amountUsd),
                    "availableBalanceUsd" to (current.availableBalanceUsd + amountUsd),
                    "pendingBalanceUsd" to current.pendingBalanceUsd,
                    "totalWithdrawnUsd" to current.totalWithdrawnUsd,
                    "monetizationStatus" to current.monetizationStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                txRef,
                mapOf(
                    "userId" to targetUserId,
                    "type" to "ad_revenue",
                    "amountUsd" to amountUsd,
                    "description" to description.trim().take(200),
                    "status" to "completed",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, earnRef.path)
            Result.failure(e)
        }
    }

    suspend fun reviewWithdrawalRequest(
        req: WithdrawalRequest,
        newStatus: String,
        adminNote: String
    ): Result<Unit> {
        val reqRef = db.collection("withdrawal_requests").document(req.id)
        val earnRef = db.collection("creator_earnings").document(req.userId)
        val notifRef = db.collection("users").document(req.userId).collection("notifications").document()
        val actorId = auth.currentUser?.uid ?: req.userId
        return try {
            val current = earnRef.get().await().toObject(CreatorEarnings::class.java)
                ?: CreatorEarnings(userId = req.userId)
            val batch = db.batch()
            batch.update(
                reqRef,
                mapOf(
                    "status" to newStatus,
                    "adminNote" to adminNote.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            when (newStatus) {
                "paid" -> {
                    val nextPending = (current.pendingBalanceUsd - req.amountUsd).coerceAtLeast(0.0)
                    batch.update(
                        earnRef,
                        mapOf(
                            "pendingBalanceUsd" to nextPending,
                            "totalWithdrawnUsd" to (current.totalWithdrawnUsd + req.amountUsd),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                "rejected" -> {
                    val nextPending = (current.pendingBalanceUsd - req.amountUsd).coerceAtLeast(0.0)
                    batch.update(
                        earnRef,
                        mapOf(
                            "pendingBalanceUsd" to nextPending,
                            "availableBalanceUsd" to (current.availableBalanceUsd + req.amountUsd),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.set(
                notifRef,
                mapOf(
                    "userId" to req.userId,
                    "actorId" to actorId,
                    "actorName" to "PulseStream Payouts",
                    "type" to "payout",
                    "title" to "Payout ${newStatus.replaceFirstChar { it.uppercase() }}",
                    "body" to "Withdrawal of $${String.format("%.2f", req.amountUsd)} is now $newStatus.",
                    "targetPostId" to "",
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUserModeration(
        targetUserId: String,
        verified: Boolean,
        accountStatus: String
    ): Result<Unit> {
        val ref = db.collection("users").document(targetUserId)
        return try {
            ref.update(
                mapOf(
                    "verified" to verified,
                    "accountStatus" to accountStatus,
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
