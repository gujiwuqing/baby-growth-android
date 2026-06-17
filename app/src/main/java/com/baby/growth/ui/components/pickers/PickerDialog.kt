package com.baby.growth.ui.components.pickers

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.baby.growth.ui.components.BabyCard
import com.baby.growth.ui.theme.*

@Composable
fun PickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 }),
        ) {
            BabyCard(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(Spacing.lg),
                cornerRadius = Radius.xl,
                contentPadding = PaddingValues(Spacing.xl),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                content()
                Spacer(modifier = Modifier.height(Spacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissText, color = BabyGrowthTheme.colors.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    FilledTonalButton(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(Radius.md),
                    ) {
                        Text(confirmText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
