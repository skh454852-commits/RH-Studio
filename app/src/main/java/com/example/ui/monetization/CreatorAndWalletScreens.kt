package com.example.ui.monetization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.AdminSettings
import com.example.model.ContentReport
import com.example.model.CreatorApplication
import com.example.model.CreatorEarnings
import com.example.model.LedgerTransaction
import com.example.model.SocialPost
import com.example.model.UserPrivateSettings
import com.example.model.UserProfile
import com.example.model.WithdrawalRequest
import com.example.ui.theme.EmeraldSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorDashboardScreen(
    profile: UserProfile?,
    earnings: CreatorEarnings,
    application: CreatorApplication?,
    userPosts: List<SocialPost>,
    adminSettings: AdminSettings,
    transactions: List<LedgerTransaction>,
    onBack: () -> Unit,
    onOpenWallet: () -> Unit,
    onApplyMonetization: (category: String, notes: String, totalViews: Long) -> Unit
) {
    val user = profile ?: UserProfile()
    val totalPostViews = remember(userPosts) { userPosts.sumOf { it.viewsCount } }
    val effectiveViews = maxOf(earnings.eligibleViews, totalPostViews)

    var category by remember { mutableStateOf("Music & Entertainment") }
    var notes by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Creator Monetization") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Creator earnings are calculated strictly from verified, policy-compliant ad revenue (${adminSettings.creatorRevenueSharePercent}% creator share). Watching ads or app usage alone does not guarantee payouts. Invalid traffic is filtered before admin approval.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Metrics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard("Eligible Views", "$effectiveViews", Modifier.weight(1f))
                        MetricCard("Est. Revenue", formatUsd(earnings.estimatedRevenueUsd), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard("Monthly Earnings", formatUsd(earnings.monthlyEarningsUsd), Modifier.weight(1f))
                        MetricCard("Available Balance", formatUsd(earnings.availableBalanceUsd), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard("Pending Balance", formatUsd(earnings.pendingBalanceUsd), Modifier.weight(1f))
                        MetricCard(
                            "Status",
                            earnings.monetizationStatus.replace("_", " ").replaceFirstChar { it.uppercase() },
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onOpenWallet,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Wallet & Payouts")
                }
            }

            // Eligibility Checklist
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Monetization Eligibility", style = MaterialTheme.typography.titleMedium)
                        EligibilityRow(
                            "Followers",
                            user.followersCount,
                            adminSettings.minFollowersForMonetization
                        )
                        EligibilityRow(
                            "Published Posts",
                            user.postsCount,
                            adminSettings.minPostsForMonetization
                        )
                        EligibilityRow(
                            "Valid Views",
                            effectiveViews,
                            adminSettings.minViewsForMonetization
                        )
                    }
                }
            }

            // Application form or status
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Program Application", style = MaterialTheme.typography.titleMedium)
                        when {
                            earnings.monetizationStatus == "approved" -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = EmeraldSuccess)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Approved for verified ad revenue sharing.")
                                }
                            }
                            application != null && application.status == "pending" -> {
                                Text(
                                    "Your application (${application.category}) is currently under administrator review.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            else -> {
                                if (application?.status == "rejected") {
                                    Text(
                                        "Previous review note: ${application.adminNote.ifBlank { "Please grow original content and reapply." }}",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = { category = it.take(40) },
                                    label = { Text("Content Category") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it.take(500) },
                                    label = { Text("Describe your original content & compliance") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = { onApplyMonetization(category, notes, effectiveViews) },
                                    enabled = category.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Submit for Admin Approval")
                                }
                            }
                        }
                    }
                }
            }

            // Revenue History
            item {
                Text("Revenue & Ledger History", style = MaterialTheme.typography.titleMedium)
            }
            if (transactions.isEmpty()) {
                item {
                    Text(
                        "No verified revenue credits yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRow(tx)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    earnings: CreatorEarnings,
    privateSettings: UserPrivateSettings?,
    adminSettings: AdminSettings,
    withdrawals: List<WithdrawalRequest>,
    transactions: List<LedgerTransaction>,
    onBack: () -> Unit,
    onSavePayoutMethod: (provider: String, maskedDest: String) -> Unit,
    onRequestWithdrawal: (amountUsd: Double, provider: String, maskedDest: String) -> Unit
) {
    val prefs = privateSettings ?: UserPrivateSettings()
    var selectedProvider by remember(prefs.payoutProvider) {
        mutableStateOf(if (prefs.payoutProvider == "none") "upi" else prefs.payoutProvider)
    }
    var maskedDest by remember(prefs.maskedPayoutDestination) {
        mutableStateOf(prefs.maskedPayoutDestination)
    }
    var withdrawAmountInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Creator Wallet") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Available Balance", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = formatUsd(earnings.availableBalanceUsd),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pending: ${formatUsd(earnings.pendingBalanceUsd)}", style = MaterialTheme.typography.bodySmall)
                            Text("Withdrawn: ${formatUsd(earnings.totalWithdrawnUsd)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = "Minimum withdrawal threshold: ${formatUsd(adminSettings.minWithdrawalUsd)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Payout method (masked only - never store raw card/bank numbers)
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AccountBalance, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Payout Destination", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "For security, only enter a masked payout identifier (e.g., UPI handle or Bank Acct ****4821). Never store full card numbers or PINs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedProvider == "upi",
                                onClick = { selectedProvider = "upi" },
                                label = { Text("UPI") }
                            )
                            FilterChip(
                                selected = selectedProvider == "bank_transfer",
                                onClick = { selectedProvider = "bank_transfer" },
                                label = { Text("Bank Transfer") }
                            )
                        }
                        OutlinedTextField(
                            value = maskedDest,
                            onValueChange = { maskedDest = it.take(80) },
                            label = { Text(if (selectedProvider == "upi") "UPI ID (e.g. creator@okaxis)" else "Bank Label (e.g. HDFC ****4821)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = { onSavePayoutMethod(selectedProvider, maskedDest) },
                            enabled = maskedDest.trim().length >= 3,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Payout Method")
                        }
                    }
                }
            }

            // Request Withdrawal
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Request Withdrawal", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = withdrawAmountInput,
                            onValueChange = { withdrawAmountInput = it },
                            label = { Text("Amount (USD)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val parsedAmt = withdrawAmountInput.toDoubleOrNull() ?: 0.0
                        Button(
                            onClick = {
                                onRequestWithdrawal(parsedAmt, selectedProvider, maskedDest)
                                withdrawAmountInput = ""
                            },
                            enabled = parsedAmt >= adminSettings.minWithdrawalUsd &&
                                parsedAmt <= earnings.availableBalanceUsd &&
                                maskedDest.trim().length >= 3,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Payout Request")
                        }
                    }
                }
            }

            item { Text("Withdrawal History", style = MaterialTheme.typography.titleMedium) }
            if (withdrawals.isEmpty()) {
                item {
                    Text("No withdrawals requested yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(withdrawals, key = { it.id }) { w ->
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${formatUsd(w.amountUsd)} via ${w.payoutProvider.uppercase()}", style = MaterialTheme.typography.titleSmall)
                                Text(w.maskedDestination, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    w.status.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Text("Transaction History", style = MaterialTheme.typography.titleMedium) }
            items(transactions, key = { it.id }) { tx ->
                TransactionRow(tx)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    isAdmin: Boolean,
    allUsers: List<UserProfile>,
    allPosts: List<SocialPost>,
    reports: List<ContentReport>,
    applications: List<CreatorApplication>,
    withdrawals: List<WithdrawalRequest>,
    adminSettings: AdminSettings,
    onBack: () -> Unit,
    onReviewApplication: (userId: String, approved: Boolean, note: String) -> Unit,
    onReviewWithdrawal: (req: WithdrawalRequest, newStatus: String, note: String) -> Unit,
    onResolveReport: (reportId: String, status: String, removePostId: String?) -> Unit,
    onModerateUser: (userId: String, verified: Boolean, accountStatus: String) -> Unit,
    onCreditVerifiedRevenue: (userId: String, views: Long, amountUsd: Double, desc: String) -> Unit,
    onSaveSettings: (AdminSettings) -> Unit
) {
    if (!isAdmin) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Text("Administrator access required.", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) { Text("Go Back") }
        }
        return
    }

    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Apps", "Payouts", "Reports", "Users", "Settings")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Admin Console") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        )
        ScrollableTabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { idx, label ->
                Tab(selected = tab == idx, onClick = { tab = idx }, text = { Text(label) })
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (tab) {
                0 -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricCard("Users", "${allUsers.size}", Modifier.weight(1f))
                            MetricCard("Active Posts", "${allPosts.size}", Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricCard("Pending Apps", "${applications.count { it.status == "pending" }}", Modifier.weight(1f))
                            MetricCard("Pending Payouts", "${withdrawals.count { it.status == "pending" }}", Modifier.weight(1f))
                        }
                    }
                    item {
                        var selectedUserId by remember { mutableStateOf(allUsers.firstOrNull()?.userId.orEmpty()) }
                        var viewsStr by remember { mutableStateOf("5000") }
                        var usdStr by remember { mutableStateOf("28.50") }
                        var desc by remember { mutableStateOf("Verified AdMob monthly share") }
                        Card(shape = RoundedCornerShape(18.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Credit Verified Ad Revenue", style = MaterialTheme.typography.titleMedium)
                                }
                                Text("Select Creator UID or use first user:", style = MaterialTheme.typography.bodySmall)
                                OutlinedTextField(
                                    value = selectedUserId,
                                    onValueChange = { selectedUserId = it },
                                    label = { Text("Creator UID") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = viewsStr,
                                        onValueChange = { viewsStr = it },
                                        label = { Text("Eligible Views") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = usdStr,
                                        onValueChange = { usdStr = it },
                                        label = { Text("Verified USD") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = desc,
                                    onValueChange = { desc = it },
                                    label = { Text("Ledger Note") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        val v = viewsStr.toLongOrNull() ?: 0L
                                        val usd = usdStr.toDoubleOrNull() ?: 0.0
                                        if (selectedUserId.isNotBlank() && usd > 0) {
                                            onCreditVerifiedRevenue(selectedUserId, v, usd, desc)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Credit Verified Revenue") }
                            }
                        }
                    }
                }
                1 -> {
                    items(applications, key = { it.userId }) { app ->
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("@${app.username} • ${app.category}", style = MaterialTheme.typography.titleSmall)
                                Text("Status: ${app.status} | Followers: ${app.followersSnapshot} | Views: ${app.viewsSnapshot}", style = MaterialTheme.typography.bodySmall)
                                if (app.notes.isNotBlank()) Text(app.notes, style = MaterialTheme.typography.bodySmall)
                                if (app.status == "pending") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { onReviewApplication(app.userId, true, "Approved by admin") }) { Text("Approve") }
                                        OutlinedButton(onClick = { onReviewApplication(app.userId, false, "Eligibility not met") }) { Text("Reject") }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    items(withdrawals, key = { it.id }) { req ->
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("@${req.username} — ${formatUsd(req.amountUsd)}", style = MaterialTheme.typography.titleSmall)
                                Text("${req.payoutProvider.uppercase()}: ${req.maskedDestination} (${req.status})", style = MaterialTheme.typography.bodySmall)
                                if (req.status == "pending" || req.status == "approved") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { onReviewWithdrawal(req, "paid", "Payout completed") }) { Text("Mark Paid") }
                                        OutlinedButton(onClick = { onReviewWithdrawal(req, "rejected", "Returned to balance") }) { Text("Reject") }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    items(reports, key = { it.id }) { rep ->
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${rep.targetType.uppercase()} • ${rep.reason}", style = MaterialTheme.typography.titleSmall)
                                Text("Target: ${rep.targetId} | Status: ${rep.status}", style = MaterialTheme.typography.bodySmall)
                                if (rep.details.isNotBlank()) Text(rep.details, style = MaterialTheme.typography.bodySmall)
                                if (rep.status == "pending") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (rep.targetType == "post" || rep.targetType == "copyright") {
                                            Button(
                                                onClick = { onResolveReport(rep.id, "resolved", rep.targetId) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            ) { Text("Remove Post") }
                                        }
                                        OutlinedButton(onClick = { onResolveReport(rep.id, "dismissed", null) }) { Text("Dismiss") }
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    items(allUsers, key = { it.userId }) { u ->
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${u.displayName} (@${u.username})", style = MaterialTheme.typography.titleSmall)
                                Text("Status: ${u.accountStatus} | Verified: ${u.verified} | Monetization: ${u.monetizationStatus}", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onModerateUser(u.userId, !u.verified, u.accountStatus) }) {
                                        Text(if (u.verified) "Unverify" else "Verify Badge")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val next = if (u.accountStatus == "active") "suspended" else "active"
                                            onModerateUser(u.userId, u.verified, next)
                                        }
                                    ) {
                                        Text(if (u.accountStatus == "active") "Suspend" else "Restore")
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    item {
                        AdminSettingsForm(adminSettings, onSaveSettings)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSettingsForm(
    current: AdminSettings,
    onSave: (AdminSettings) -> Unit
) {
    var minFollowers by remember(current) { mutableStateOf(current.minFollowersForMonetization.toString()) }
    var minPosts by remember(current) { mutableStateOf(current.minPostsForMonetization.toString()) }
    var minViews by remember(current) { mutableStateOf(current.minViewsForMonetization.toString()) }
    var sharePct by remember(current) { mutableStateOf(current.creatorRevenueSharePercent.toString()) }
    var minWithdraw by remember(current) { mutableStateOf(current.minWithdrawalUsd.toString()) }
    var appId by remember(current) { mutableStateOf(current.adMobAppId) }
    var bannerId by remember(current) { mutableStateOf(current.bannerAdUnitId) }
    var nativeId by remember(current) { mutableStateOf(current.nativeAdUnitId) }
    var rewardedId by remember(current) { mutableStateOf(current.rewardedAdUnitId) }
    var adsEnabled by remember(current) { mutableStateOf(current.adsEnabled) }
    var autoMod by remember(current) { mutableStateOf(current.autoModerationEnabled) }

    Card(shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Monetization & AdMob Config", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = minFollowers, onValueChange = { minFollowers = it }, label = { Text("Min Followers") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = minPosts, onValueChange = { minPosts = it }, label = { Text("Min Posts") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = minViews, onValueChange = { minViews = it }, label = { Text("Min Views") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = sharePct, onValueChange = { sharePct = it }, label = { Text("Creator Share % (0-90)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = minWithdraw, onValueChange = { minWithdraw = it }, label = { Text("Min Withdrawal (USD)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = appId, onValueChange = { appId = it }, label = { Text("AdMob App ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bannerId, onValueChange = { bannerId = it }, label = { Text("Banner Ad Unit ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = nativeId, onValueChange = { nativeId = it }, label = { Text("Native Ad Unit ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = rewardedId, onValueChange = { rewardedId = it }, label = { Text("Rewarded Ad Unit ID") }, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Ads Enabled")
                Switch(checked = adsEnabled, onCheckedChange = { adsEnabled = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Auto Copyright/Spam Moderation")
                Switch(checked = autoMod, onCheckedChange = { autoMod = it })
            }
            Button(
                onClick = {
                    onSave(
                        current.copy(
                            minFollowersForMonetization = minFollowers.toLongOrNull() ?: current.minFollowersForMonetization,
                            minPostsForMonetization = minPosts.toLongOrNull() ?: current.minPostsForMonetization,
                            minViewsForMonetization = minViews.toLongOrNull() ?: current.minViewsForMonetization,
                            creatorRevenueSharePercent = sharePct.toLongOrNull() ?: current.creatorRevenueSharePercent,
                            minWithdrawalUsd = minWithdraw.toDoubleOrNull() ?: current.minWithdrawalUsd,
                            adMobAppId = appId,
                            bannerAdUnitId = bannerId,
                            nativeAdUnitId = nativeId,
                            rewardedAdUnitId = rewardedId,
                            adsEnabled = adsEnabled,
                            autoModerationEnabled = autoMod
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Platform Settings")
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EligibilityRow(label: String, current: Long, target: Long) {
    val progress = if (target <= 0L) 1f else (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$current / $target", style = MaterialTheme.typography.labelSmall)
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TransactionRow(tx: LedgerTransaction) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.description, style = MaterialTheme.typography.titleSmall)
                Text("${tx.type.replace("_", " ")} • ${tx.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = (if (tx.type == "withdrawal") "-" else "+") + formatUsd(tx.amountUsd),
                style = MaterialTheme.typography.titleSmall,
                color = if (tx.type == "withdrawal") MaterialTheme.colorScheme.secondary else EmeraldSuccess
            )
        }
    }
}

private fun formatUsd(amount: Double): String = "$" + String.format("%.2f", amount)
