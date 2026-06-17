package com.baby.growth.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baby.growth.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val VISIBLE_ITEMS = 5
private const val ITEM_HEIGHT_DP = 44

@Composable
fun BabyTimePicker(
    selectedHour: Int,
    selectedMinute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hours = (0..23).toList()
    val minutes = (0..59).toList()

    Row(
        modifier = modifier.fillMaxWidth().height((ITEM_HEIGHT_DP * VISIBLE_ITEMS).dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        WheelPickerColumn(
            items = hours,
            initialIndex = selectedHour,
            onSelected = { hour -> onTimeChanged(hour, selectedMinute) },
            labelFormatter = { "%02d".format(it) },
            modifier = Modifier.weight(1f),
        )

        Text(
            text = ":",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = Spacing.sm),
        )

        WheelPickerColumn(
            items = minutes,
            initialIndex = selectedMinute,
            onSelected = { minute -> onTimeChanged(selectedHour, minute) },
            labelFormatter = { "%02d".format(it) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WheelPickerColumn(
    items: List<Int>,
    initialIndex: Int,
    onSelected: (Int) -> Unit,
    labelFormatter: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val paddingCount = VISIBLE_ITEMS / 2

    val centerIndex by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex + paddingCount).coerceIn(0, items.size - 1)
        }
    }

    LaunchedEffect(centerIndex) {
        onSelected(items[centerIndex])
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIdx = listState.firstVisibleItemIndex + paddingCount
            val targetScroll = (centerIdx - paddingCount).coerceIn(0, items.size - 1)
            listState.animateScrollToItem(targetScroll)
        }
    }

    Box(modifier = modifier.fillMaxHeight()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = (ITEM_HEIGHT_DP * paddingCount).dp),
        ) {
            items(items.size) { index ->
                val isSelected = index == centerIndex
                val itemAlpha = if (isSelected) 1f else 0.4f
                val itemScale = if (isSelected) 1.3f else 0.9f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT_DP.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height((ITEM_HEIGHT_DP - 8).dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(Radius.md),
                                )
                        )
                    }
                    Text(
                        text = labelFormatter(items[index]),
                        fontSize = if (isSelected) 22.sp else 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            BabyGrowthTheme.colors.textSecondary,
                        modifier = Modifier
                            .scale(itemScale)
                            .alpha(itemAlpha),
                    )
                }
            }
        }

        // 上下渐变遮罩
        val fadeGradient = MaterialTheme.colorScheme.background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((ITEM_HEIGHT_DP * paddingCount).dp)
                .background(fadeGradient)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((ITEM_HEIGHT_DP * paddingCount).dp)
                .align(Alignment.BottomCenter)
                .background(fadeGradient)
        )
    }
}
