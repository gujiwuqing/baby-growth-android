package com.baby.growth.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baby.growth.ui.components.pickers.BabyDatePicker
import com.baby.growth.ui.components.pickers.BabyTimePicker
import com.baby.growth.ui.components.pickers.PickerDialog
import com.baby.growth.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DateTimeInput(
    dateTime: Long,
    onDateTimeChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "记录时间",
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
) {
    var showDialog by remember { mutableStateOf(false) }

    BabyCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = BabyGrowthTheme.colors.textSecondary,
                )
                Text(
                    text = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINESE).format(Date(dateTime)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = "修改时间",
                tint = BabyGrowthTheme.colors.textHint,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    if (showDialog) {
        var tempDateTime by remember { mutableLongStateOf(dateTime) }

        PickerDialog(
            onDismiss = { showDialog = false },
            onConfirm = {
                onDateTimeChange(tempDateTime)
                showDialog = false
            },
            title = "选择日期和时间",
        ) {
            BabyDatePicker(
                selectedDateMillis = tempDateTime,
                onDateSelected = { newDate ->
                    val cal = Calendar.getInstance().apply { timeInMillis = tempDateTime }
                    val newCal = Calendar.getInstance().apply { timeInMillis = newDate }
                    cal.set(Calendar.YEAR, newCal.get(Calendar.YEAR))
                    cal.set(Calendar.MONTH, newCal.get(Calendar.MONTH))
                    cal.set(Calendar.DAY_OF_MONTH, newCal.get(Calendar.DAY_OF_MONTH))
                    tempDateTime = cal.timeInMillis
                },
                minDateMillis = minDateMillis,
                maxDateMillis = maxDateMillis,
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            HorizontalDivider(
                color = BabyGrowthTheme.colors.dividerColor,
                modifier = Modifier.padding(horizontal = Spacing.xxl),
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            val cal = Calendar.getInstance().apply { timeInMillis = tempDateTime }
            BabyTimePicker(
                selectedHour = cal.get(Calendar.HOUR_OF_DAY),
                selectedMinute = cal.get(Calendar.MINUTE),
                onTimeChanged = { hour, minute ->
                    val newCal = Calendar.getInstance().apply { timeInMillis = tempDateTime }
                    newCal.set(Calendar.HOUR_OF_DAY, hour)
                    newCal.set(Calendar.MINUTE, minute)
                    tempDateTime = newCal.timeInMillis
                },
            )
        }
    }
}
