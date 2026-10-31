package com.pxy.depositbook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.pxy.depositbook.data.model.BankCatalog
import kotlin.math.abs

private val BANK_COLORS = listOf(
    Color(0xFF3B6FE0), // 蓝
    Color(0xFFE04F4F), // 红
    Color(0xFFD97706), // 橙
    Color(0xFF059669), // 绿
    Color(0xFF7C3AED), // 紫
    Color(0xFF0891B2), // 青
    Color(0xFFB45309), // 棕
    Color(0xFFBE185D), // 玫红
    Color(0xFF475569), // 灰蓝
    Color(0xFF2563EB), // 亮蓝
)

/**
 * 银行图标：
 * - 内置银行且有真 logo（assets 下 bank_logos 目录的 SVG）→ 渲染矢量 logo
 * - 其余 → 彩色文字圆标（按银行名固定底色 + 首字）
 * 用户自定义银行永远走文字圆标。
 */
@Composable
fun BankIcon(bankName: String, size: Dp = 26.dp, modifier: Modifier = Modifier) {
    val logoFile = remember(bankName) { BankCatalog.logoFile(bankName) }

    if (logoFile != null) {
        val context = LocalContext.current
        val loader = remember(context) {
            ImageLoader.Builder(context)
                .components { add(SvgDecoder.Factory()) }
                .build()
        }
        val model = remember(context, logoFile) {
            ImageRequest.Builder(context)
                .data("file:///android_asset/bank_logos/$logoFile")
                .build()
        }
        // 纯标志图形为方形，用等边容器 + 1dp 内边距，去掉左右留白
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF4F6F8)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = model,
                imageLoader = loader,
                contentDescription = bankName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp),
            )
        }
        return
    }

    // 文字圆标回退
    val color = remember(bankName) {
        val idx = abs(bankName.hashCode()) % BANK_COLORS.size
        BANK_COLORS[idx]
    }
    val ch = remember(bankName) { BankCatalog.iconChar(bankName) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            ch,
            color = Color.White,
            fontSize = (size.value * 0.52f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
