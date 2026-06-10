package com.baby.growth.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.baby.growth.ui.theme.BabyGrowthTheme
import com.baby.growth.ui.theme.Radius
import com.baby.growth.ui.theme.Spacing

/**
 * 统一卡片组件 - 项目中所有卡片的基础
 */
@Composable
fun BabyCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radius.lg,
    hasBorder: Boolean = false,
    backgroundColor: Color = BabyGrowthTheme.colors.cardBackground,
    elevation: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = if (hasBorder) BorderStroke(1.dp, BabyGrowthTheme.colors.cardBorder) else null,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * 带标题的卡片
 */
@Composable
fun BabyTitledCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BabyCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailing?.invoke()
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        content()
    }
}

/**
 * 渐变强调卡片 - 用于统计概览等
 */
@Composable
fun BabyAccentCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    BabyCard(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        cornerRadius = Radius.xl,
        contentPadding = PaddingValues(Spacing.xl),
        content = content
    )
}
