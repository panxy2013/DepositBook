package com.pxy.depositbook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.pxy.depositbook.data.entity.Profile
import com.pxy.depositbook.data.repo.AccountStore
import com.pxy.depositbook.util.I18n
import java.io.File

/** 头像色板：按昵称 hash 选一组渐变，每个账户固定一个颜色 */
private val AVATAR_PALETTE = listOf(
    listOf(Color(0xFF7DA7F5), Color(0xFF5B6FD6)),
    listOf(Color(0xFF66BB9A), Color(0xFF43A57F)),
    listOf(Color(0xFFF2A65A), Color(0xFFE07B54)),
    listOf(Color(0xFFB98BD9), Color(0xFF8A63C9)),
    listOf(Color(0xFF6FBBD6), Color(0xFF3F8FB8)),
    listOf(Color(0xFFE48FA9), Color(0xFFC55E83)),
)

private fun paletteOf(name: String): List<Color> {
    val h = (name.ifBlank { "D" }).fold(31) { acc, c -> acc * 31 + c.code }
    return AVATAR_PALETTE[((h % AVATAR_PALETTE.size) + AVATAR_PALETTE.size) % AVATAR_PALETTE.size]
}

/**
 * 系统默认设计的存款用户头像：渐变圆 + 昵称首字。
 */
@Composable
fun DefaultAvatar(name: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val colors = remember(name) { paletteOf(name) }
    val first = remember(name) { name.trim().firstOrNull()?.toString() ?: "存" }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            first,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
            maxLines = 1,
        )
    }
}

/**
 * 账户头像：有自定义头像显示图片，否则显示默认设计的渐变首字头像。
 */
@Composable
fun ProfileAvatar(profile: Profile?, accounts: AccountStore, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val file: File? = remember(profile?.avatarPath) { accounts.avatarFile(profile) }
    if (file != null) {
        AsyncImageCircle(file, size, profile?.name.orEmpty(), modifier)
    } else {
        DefaultAvatar(profile?.name.orEmpty(), size, modifier)
    }
}

@Composable
private fun AsyncImageCircle(file: File, size: Dp, fallbackName: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = file,
        contentDescription = fallbackName,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}

/**
 * 账户切换弹窗：列出全部账户（头像+昵称），点选即切换；底部「新建账户」入口。
 */
@Composable
fun AccountSwitchDialog(
    accounts: AccountStore,
    profiles: List<Profile>,
    currentId: Long,
    onPick: (Long) -> Unit,
    onCreateClick: () -> Unit,
    onManageClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    I18n.s("切换账户", "Switch account"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(profiles, key = { it.id }) { p ->
                        val isCurrent = p.id == currentId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .then(
                                    if (isCurrent) Modifier.background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    ) else Modifier
                                )
                                .clickable { onPick(p.id) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ProfileAvatar(p, accounts, size = 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                p.name,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                            )
                            if (isCurrent) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = I18n.s("当前账户", "Current"),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onManageClick) {
                        Text(I18n.s("管理账户", "Manage"))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onCreateClick) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(I18n.s("新建账户", "New account"))
                    }
                }
            }
        }
    }
}

/**
 * 新建 / 重命名账户弹窗（同一个组件，标题与初始值不同）。
 */
@Composable
fun AccountNameDialog(
    title: String,
    initial: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val trimmed = text.trim()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 20) text = it },
                    singleLine = true,
                    label = { Text(I18n.s("账户昵称", "Account name")) },
                    placeholder = { Text(I18n.s("例如：我的存款", "e.g. My savings")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(I18n.s("取消", "Cancel")) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { if (trimmed.isNotEmpty()) onConfirm(trimmed) },
                        enabled = trimmed.isNotEmpty(),
                    ) {
                        Text(I18n.s("确定", "OK"))
                    }
                }
            }
        }
    }
}

/**
 * 删除账户确认弹窗：明确提示账户内所有存款/日志/历史数据都会被删除。
 */
@Composable
fun AccountDeleteDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    I18n.s("删除账户「$name」？", "Delete account \"$name\"?"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    I18n.s(
                        "该账户下的所有存款、操作日志和资产历史都会被永久删除，无法恢复。",
                        "All deposits, logs and asset history in this account will be permanently deleted.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(I18n.s("取消", "Cancel")) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onConfirm) {
                        Text(
                            I18n.s("删除", "Delete"),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
