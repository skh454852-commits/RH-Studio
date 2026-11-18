package com.example.ui.social

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Conversation
import com.example.model.DirectMessage
import com.example.model.NotificationItem
import com.example.model.Playlist
import com.example.model.PostComment
import com.example.model.SocialPost
import com.example.ui.components.AvatarCircle
import com.example.ui.components.EmptyFeedState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: SocialPost,
    comments: List<PostComment>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onAddComment: (text: String, parentCommentId: String) -> Unit,
    onEditComment: (commentId: String, newText: String) -> Unit,
    onDeleteComment: (commentId: String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<PostComment?>(null) }
    var editingComment by remember { mutableStateOf<PostComment?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Comments (${comments.size})",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(10.dp))

            if (comments.isEmpty()) {
                Text(
                    text = "No comments yet. Start the conversation!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        val isReply = comment.parentCommentId.isNotBlank()
                        val isMine = comment.authorId == currentUserId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = if (isReply) 24.dp else 0.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${comment.authorName} (@${comment.authorUsername})",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Row {
                                        IconButton(
                                            onClick = { replyTarget = comment },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Rounded.Reply,
                                                contentDescription = "Reply",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        if (isMine) {
                                            IconButton(
                                                onClick = {
                                                    editingComment = comment
                                                    draft = comment.text
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Edit,
                                                    contentDescription = "Edit comment",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteComment(comment.id) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Delete,
                                                    contentDescription = "Delete comment",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            if (replyTarget != null) {
                Text(
                    text = "Replying to @${replyTarget!!.authorUsername} (tap to cancel)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable { replyTarget = null }
                )
            }
            if (editingComment != null) {
                Text(
                    text = "Editing your comment (tap to cancel)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            editingComment = null
                            draft = ""
                        }
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(500) },
                    placeholder = { Text("Add a comment...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val txt = draft.trim()
                        if (txt.isNotEmpty()) {
                            val editing = editingComment
                            if (editing != null) {
                                onEditComment(editing.id, txt)
                                editingComment = null
                            } else {
                                onAddComment(txt, replyTarget?.id.orEmpty())
                                replyTarget = null
                            }
                            draft = ""
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send comment")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    onNotificationClick: (NotificationItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Notifications", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Real-time likes, comments, followers, creator reviews & payout updates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (notifications.isEmpty()) {
            item {
                EmptyFeedState(
                    title = "You're all caught up",
                    subtitle = "Likes, comments, new followers, and payout status updates will appear here."
                )
            }
        } else {
            items(notifications, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNotificationClick(item) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.read) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (item.read) FontWeight.Medium else FontWeight.Bold
                            )
                            Text(
                                text = item.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesInboxScreen(
    currentUserId: String,
    conversations: List<Conversation>,
    onBack: () -> Unit,
    onOpenConversation: (Conversation) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Direct Messages") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        )
        if (conversations.isEmpty()) {
            EmptyFeedState(
                title = "No conversations yet",
                subtitle = "Visit any creator's profile and tap Message to start a private chat."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    val otherName = conv.participantNames.entries
                        .firstOrNull { it.key != currentUserId }?.value ?: "Creator"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenConversation(conv) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarCircle(name = otherName, avatarBase64 = "", size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(otherName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    conv.lastMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    currentUserId: String,
    conversation: Conversation,
    messages: List<DirectMessage>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    val otherName = conversation.participantNames.entries
        .firstOrNull { it.key != currentUserId }?.value ?: "Creator"

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(otherName) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == currentUserId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(1000) },
                placeholder = { Text("Message $otherName...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val t = draft.trim()
                    if (t.isNotEmpty()) {
                        onSendMessage(t)
                        draft = ""
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun ReportContentDialog(
    targetType: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    val reasons = when (targetType) {
        "copyright" -> listOf("Unauthorized audio upload", "Copyright infringement", "Trademark misuse")
        "app_issue" -> listOf("Playback bug", "Upload error", "Billing/Wallet question")
        else -> listOf("Spam or scam", "Inappropriate content", "Harassment", "Misleading info")
    }
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (targetType == "copyright") "Report Copyright Issue" else "Submit Report") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reasons.forEach { r ->
                    FilterChip(
                        selected = selectedReason == r,
                        onClick = { selectedReason = r },
                        label = { Text(r) }
                    )
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(500) },
                    label = { Text("Additional details") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, details) }) { Text("Submit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditPostDialog(
    post: SocialPost,
    onDismiss: () -> Unit,
    onSave: (title: String, caption: String, hashtags: String, location: String) -> Unit
) {
    var title by remember { mutableStateOf(post.title) }
    var caption by remember { mutableStateOf(post.caption) }
    var hashtags by remember { mutableStateOf(post.hashtags.joinToString(", ")) }
    var location by remember { mutableStateOf(post.location) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Post") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text("Caption") })
                OutlinedTextField(value = hashtags, onValueChange = { hashtags = it }, label = { Text("Hashtags") })
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, caption, hashtags, location) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    track: SocialPost,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onAddToExisting: (playlistId: String) -> Unit,
    onCreateNew: (title: String) -> Unit
) {
    var newTitle by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                playlists.forEach { pl ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddToExisting(pl.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlaylistAdd, null)
                            Spacer(Modifier.width(8.dp))
                            Text("${pl.title} (${pl.trackIds.size})")
                        }
                    }
                }
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it.take(80) },
                    label = { Text("Or create new playlist") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateNew(newTitle) },
                enabled = newTitle.isNotBlank()
            ) { Text("Create & Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
