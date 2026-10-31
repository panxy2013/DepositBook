package com.pxy.depositbook.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.pxy.depositbook.data.db.AppDatabase
import com.pxy.depositbook.data.entity.Profile
import com.pxy.depositbook.data.prefs.OptionsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 账户中枢：多账户支持。
 * - profiles: 全部账户档案（按 sortOrder、创建顺序）
 * - currentId: 当前账户，切换后所有页面数据自动跟随（repository 内按该 id 过滤）
 * - 头像文件保存在 filesDir/avatars/，文件名带时间戳避免图片缓存
 */
class AccountStore(
    private val context: Context,
    private val db: AppDatabase,
    private val prefs: OptionsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles

    private val _currentId = MutableStateFlow(prefs.currentProfileId())
    val currentId: StateFlow<Long> = _currentId

    init {
        scope.launch {
            ensureDefault()
            refresh()
        }
    }

    val avatarDir: File
        get() = File(context.filesDir, "avatars").apply { mkdirs() }

    /** profile.avatarPath 是相对文件名 → 绝对文件 */
    fun avatarFile(profile: Profile?): File? {
        val name = profile?.avatarPath ?: return null
        val f = File(avatarDir, name)
        return if (f.exists()) f else null
    }

    private suspend fun ensureDefault() {
        if (db.profileDao().count() == 0) {
            db.profileDao().insert(Profile(name = com.pxy.depositbook.util.I18n.s("默认账户", "Default")))
        }
        // 当前 id 在库里不存在（如清库）→ 回到第一个账户
        val all = db.profileDao().getAllOnce()
        if (all.none { it.id == _currentId.value }) {
            _currentId.value = all.firstOrNull()?.id ?: 1L
            prefs.setCurrentProfileId(_currentId.value)
        }
    }

    private suspend fun refresh() {
        _profiles.value = db.profileDao().getAllOnce()
    }

    /** 切换账户（所有数据立即隔离切换） */
    fun switchTo(id: Long) {
        if (_currentId.value == id) return
        _currentId.value = id
        prefs.setCurrentProfileId(id)
    }

    /** 新建账户并切换过去 */
    suspend fun add(name: String): Long {
        val clean = name.trim()
        val id = db.profileDao().insert(Profile(name = clean, sortOrder = _profiles.value.size))
        switchTo(id)
        refresh()
        return id
    }

    /** 修改昵称 */
    suspend fun rename(id: Long, name: String) {
        val p = db.profileDao().findById(id) ?: return
        val clean = name.trim()
        if (clean.isEmpty() || clean == p.name) return
        db.profileDao().update(p.copy(name = clean))
        refresh()
    }

    /** 按名字取已有账户 id，不存在则创建（导入/恢复用） */
    suspend fun ensureProfile(name: String, sortOrder: Int = 0): Long {
        val clean = name.trim()
        val existing = db.profileDao().findByName(clean)
        if (existing != null) return existing.id
        val id = db.profileDao().insert(Profile(name = clean, sortOrder = sortOrder))
        refresh()
        return id
    }

    /**
     * 从相册/文件选择器设置头像：解码缩到 512px 内，保存为 JPEG。
     * 文件名带时间戳，Coil 不会吃旧缓存。
     */
    suspend fun setAvatarFromUri(id: Long, uri: android.net.Uri): Boolean {
        val p = db.profileDao().findById(id) ?: return false
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: return false
            val sample = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 512)
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return false
            val name = "a${id}_${System.currentTimeMillis()}.jpg"
            avatarDir.mkdirs()
            val out = File(avatarDir, name)
            out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            bmp.recycle()
            // 删掉旧头像文件，省空间
            p.avatarPath?.let { old -> File(avatarDir, old).takeIf { it.name != name }?.delete() }
            db.profileDao().update(p.copy(avatarPath = name))
            refresh()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 删除账户：数据一并清除；若是当前账户自动切到另一个 */
    suspend fun deleteProfile(id: Long) {
        val all = _profiles.value
        if (all.size <= 1) return // 至少保留一个账户
        db.depositDao().deleteByProfile(id)
        db.operationLogDao().deleteByProfile(id)
        db.assetSnapshotDao().deleteByProfile(id)
        db.profileDao().findById(id)?.avatarPath?.let { File(avatarDir, it).delete() }
        db.profileDao().delete(db.profileDao().findById(id) ?: return)
        if (_currentId.value == id) {
            switchTo(all.first { it.id != id }.id)
        }
        refresh()
    }
}
