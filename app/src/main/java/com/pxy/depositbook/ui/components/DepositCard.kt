package com.pxy.depositbook.ui.components

import com.pxy.depositbook.util.I18n

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pxy.depositbook.data.model.BankCatalog
import com.pxy.depositbook.data.model.DepositUi
import com.pxy.depositbook.ui.components.BankIcon
import com.pxy.depositbook.ui.theme.Accent
import com.pxy.depositbook.ui.theme.AccentLight
import com.pxy.depositbook.ui.theme.Danger
import com.pxy.depositbook.ui.theme.DangerLight
import com.pxy.depositbook.ui.theme.Warning
import com.pxy.depositbook.ui.theme.WarningLight
import com.pxy.depositbook.util.FormatUtils

@Composable
fun DepositCard(
    ui: DepositUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = ui.deposit
    val statusColor: Color = when {
        ui.expired -> Color(0xFF8A97A3)
        ui.urgent -> Danger
        ui.expiringSoon -> Warning
        else -> Accent
    }
    val statusBg: Color = when {
        ui.expired -> Color(0xFFEDEFF2)
        ui.urgent -> DangerLight
        ui.expiringSoon -> WarningLight
        else -> AccentLight
    }
    val statusText = when {
        ui.expired -> I18n.s("已到期", "Matured")
        ui.daysLeft == 0L -> I18n.s("今天到期", "Matures today")
        else -> I18n.s("${ui.daysLeft} 天后到期", "${ui.daysLeft} days to maturity")
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 内置银行显示 logo；自定义银行不带图标
                        if (BankCatalog.isBuiltIn(d.accountName)) {
                            BankIcon(d.accountName)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            d.accountName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                    if (!d.productName.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            d.productName!!,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Pill(statusText, statusColor, statusBg)
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    FormatUtils.money(d.principal),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    I18n.s("年化 ${FormatUtils.rate(d.annualRate)}", "Rate ${FormatUtils.rate(d.annualRate)}"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { ui.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = statusBg,
            )

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InfoCol(I18n.s("日利息", "Daily interest"), "+" + FormatUtils.moneyPlain(ui.dailyInterest), Accent)
                InfoCol(I18n.s("已获利息", "Interest earned"), "+" + FormatUtils.moneyPlain(ui.interestEarned), Accent)
                InfoCol(I18n.s("本金+利息", "Principal + interest"), FormatUtils.moneyPlain(ui.valueNow), MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AccessTime,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${FormatUtils.date(d.startDate)} → ${FormatUtils.date(d.endDate)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InfoCol(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
