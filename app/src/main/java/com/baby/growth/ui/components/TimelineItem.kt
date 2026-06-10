package com.baby.growth.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baby.growth.ui.theme.*
import com.baby.growth.utils.RecordTypes

/**
 * 时间轴列表项 - 首页和记录页复用
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineItem(
    time: String,
    title: String,
    subtitle: String,
    typeKey: String,
    isLast: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val dotColor = RecordColor.fromKey(typeKey)
    val meta = RecordTypes.getByKey(typeKey)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(Radius.md))
                        .combinedClickable(
                            onClick = { onClick?.invoke() },
                            onLongClick = { onLongClick?.invoke() },
                        )
                } else Modifier
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        // 左侧时间
        Text(
            text = time,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.width(48.dp),
        )

        // 中间时间轴线 + 圆点
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(IntrinsicSize.Max),
            contentAlignment = Alignment.TopCenter,
        ) {
            // 竖线
            if (!isLast) {
                Canvas(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(top = 12.dp),
                ) {
                    drawLine(
                        color = dotColor.copy(alpha = 0.2f),
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                    )
                }
            }
            // 圆点
            Surface(
                modifier = Modifier.size(10.dp),
                shape = CircleShape,
                color = dotColor,
                shadowElevation = 2.dp,
            ) {}
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        // 右侧内容卡片
        BabyCard(
            modifier = Modifier.weight(1f),
            cornerRadius = Radius.md,
            contentPadding = PaddingValues(Spacing.md),
            hasBorder = true,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 类型图标
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(Radius.sm),
                    color = dotColor.copy(alpha = 0.1f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = meta?.icon ?: "📝",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }
}
