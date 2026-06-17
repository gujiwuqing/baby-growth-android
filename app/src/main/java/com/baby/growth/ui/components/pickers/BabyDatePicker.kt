package com.baby.growth.ui.components.pickers

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baby.growth.ui.theme.*
import java.util.*

@Composable
fun BabyDatePicker(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
) {
    var displayYear by remember {
        mutableIntStateOf(
            Calendar.getInstance().apply { timeInMillis = selectedDateMillis }.get(Calendar.YEAR)
        )
    }
    var displayMonth by remember {
        mutableIntStateOf(
            Calendar.getInstance().apply { timeInMillis = selectedDateMillis }.get(Calendar.MONTH)
        )
    }

    val selectedCal = remember(selectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    }

    val todayCal = remember {
        Calendar.getInstance()
    }

    val weekHeaders = listOf("日", "一", "二", "三", "四", "五", "六")

    Column(modifier = modifier.fillMaxWidth()) {
        // 年月导航
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (displayMonth == 0) {
                        displayYear -= 1
                        displayMonth = 11
                    } else {
                        displayMonth -= 1
                    }
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上个月",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = "${displayYear}年${displayMonth + 1}月",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            IconButton(
                onClick = {
                    if (displayMonth == 11) {
                        displayYear += 1
                        displayMonth = 0
                    } else {
                        displayMonth += 1
                    }
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下个月",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // 星期标题
        Row(modifier = Modifier.fillMaxWidth()) {
            weekHeaders.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = BabyGrowthTheme.colors.textSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // 日历网格
        val daysInMonth = getDaysInMonth(displayYear, displayMonth)
        val firstDayOfWeek = getFirstDayOfWeek(displayYear, displayMonth)

        val weeks = mutableListOf<List<Int?>>()
        var currentWeek = mutableListOf<Int?>()
        repeat(firstDayOfWeek) { currentWeek.add(null) }
        for (day in 1..daysInMonth) {
            currentWeek.add(day)
            if (currentWeek.size == 7) {
                weeks.add(currentWeek.toList())
                currentWeek = mutableListOf()
            }
        }
        if (currentWeek.isNotEmpty()) {
            while (currentWeek.size < 7) currentWeek.add(null)
            weeks.add(currentWeek)
        }

        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            val isSelected = selectedCal.get(Calendar.YEAR) == displayYear
                                && selectedCal.get(Calendar.MONTH) == displayMonth
                                && selectedCal.get(Calendar.DAY_OF_MONTH) == day

                            val isToday = todayCal.get(Calendar.YEAR) == displayYear
                                && todayCal.get(Calendar.MONTH) == displayMonth
                                && todayCal.get(Calendar.DAY_OF_MONTH) == day

                            val dayTimestamp = Calendar.getInstance().apply {
                                set(displayYear, displayMonth, day, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis

                            val isInRange = (minDateMillis == null || dayTimestamp >= minDateMillis)
                                && (maxDateMillis == null || dayTimestamp <= maxDateMillis)

                            val bgColor by animateColorAsState(
                                targetValue = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> androidx.compose.ui.graphics.Color.Transparent
                                },
                                label = "dayBgColor"
                            )

                            val textColor by animateColorAsState(
                                targetValue = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    !isInRange -> BabyGrowthTheme.colors.textHint
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                label = "dayTextColor"
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .then(
                                        if (isInRange) {
                                            Modifier.clickable {
                                                val newCal = Calendar.getInstance().apply {
                                                    timeInMillis = selectedDateMillis
                                                }
                                                newCal.set(Calendar.YEAR, displayYear)
                                                newCal.set(Calendar.MONTH, displayMonth)
                                                newCal.set(Calendar.DAY_OF_MONTH, day)
                                                onDateSelected(newCal.timeInMillis)
                                            }
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$day",
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                                    color = textColor,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getDaysInMonth(year: Int, month: Int): Int {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun getFirstDayOfWeek(year: Int, month: Int): Int {
    return Calendar.getInstance().apply {
        set(year, month, 1)
    }.get(Calendar.DAY_OF_WEEK) - 1
}
