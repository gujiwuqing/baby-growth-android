package com.baby.growth.ui.growth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.baby.growth.data.entity.GrowthRecord
import com.baby.growth.ui.components.*
import com.baby.growth.ui.theme.*
import com.baby.growth.utils.DateUtils
import com.baby.growth.utils.GrowthCurveData
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthScreen(
    navController: NavController,
    viewModel: GrowthViewModel = viewModel()
) {
    val records by viewModel.records.collectAsState()
    val latest by viewModel.latestRecord.collectAsState()
    val gender by viewModel.babyGender.collectAsState()
    var selectedType by remember { mutableStateOf("height") }

    Scaffold(
        topBar = {
            BabyTopBar(title = "成长曲线")
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("record/growth") },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加记录")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                LatestDataCard(latest, gender)
            }

            item {
                ChartTypeSelector(selectedType) { selectedType = it }
            }

            item {
                GrowthCurveChart(records, selectedType, gender)
            }

            item {
                SectionHeader(title = "历史记录")
            }

            items(records.sortedByDescending { it.recordTime }) { record ->
                HistoryRecordItem(record)
            }
        }
    }
}

@Composable
fun LatestDataCard(latest: GrowthRecord?, gender: Int) {
    BabyTitledCard(title = "最新指标") {
        if (latest != null) {
            val ageMonths = DateUtils.getMonthAge(latest.recordTime)
            val isMale = gender == 1
            
            val heightPercentile = latest.height?.let {
                GrowthCurveData.calculatePercentile(it, ageMonths, isMale, "height")
            }
            val weightPercentile = latest.weight?.let {
                GrowthCurveData.calculatePercentile(it, ageMonths, isMale, "weight")
            }
            val headPercentile = latest.headCircumference?.let {
                GrowthCurveData.calculatePercentile(it, ageMonths, isMale, "head")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                GrowthIndicatorWithPercentile(
                    "身高", 
                    "${latest.height} cm", 
                    heightPercentile,
                    MaterialTheme.colorScheme.primary
                )
                GrowthIndicatorWithPercentile(
                    "体重", 
                    "${latest.weight} kg", 
                    weightPercentile,
                    StatusColor.Success
                )
                GrowthIndicatorWithPercentile(
                    "头围", 
                    "${latest.headCircumference} cm", 
                    headPercentile,
                    Color(0xFFFFB74D)
                )
            }
        } else {
            EmptyState(
                icon = Icons.Outlined.TrendingUp,
                title = "暂无记录",
                subtitle = "点击右下角按钮添加第一条成长记录",
                emoji = "📏",
            )
        }
    }
}

@Composable
fun GrowthIndicatorWithPercentile(label: String, value: String, percentile: Float?, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        percentile?.let {
            Text(
                text = "P${it.toInt()}",
                fontSize = 12.sp,
                color = color.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = BabyGrowthTheme.colors.textSecondary)
    }
}

@Composable
fun ChartTypeSelector(selectedType: String, onTypeChanged: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        FilterTag(
            text = "身高曲线",
            selected = selectedType == "height",
            onClick = { onTypeChanged("height") },
            modifier = Modifier.weight(1f)
        )
        FilterTag(
            text = "体重曲线",
            selected = selectedType == "weight",
            onClick = { onTypeChanged("weight") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun GrowthCurveChart(records: List<GrowthRecord>, type: String, gender: Int) {
    val validRecords = records.filter {
        when (type) {
            "height" -> it.height != null
            "weight" -> it.weight != null
            else -> false
        }
    }.sortedBy { it.recordTime }

    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = BabyGrowthTheme.colors.textHint
    )
    val legendStyle = TextStyle(
        fontSize = 10.sp,
        color = BabyGrowthTheme.colors.textSecondary
    )

    if (validRecords.isEmpty()) {
        BabyCard {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Outlined.TrendingUp,
                    title = "暂无数据",
                    subtitle = "添加成长记录后即可查看曲线图",
                    emoji = "📊",
                )
            }
        }
        return
    }

    val isMale = gender == 1
    val maxAge = validRecords.maxOfOrNull { DateUtils.getMonthAge(it.recordTime) } ?: 36
    val displayMaxAge = (maxAge + 6).coerceAtMost(36)

    BabyCard {
        Canvas(modifier = Modifier.fillMaxWidth().height(250.dp).padding(Spacing.lg)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val paddingLeft = 40.dp.toPx()
            val paddingRight = 20.dp.toPx()
            val paddingTop = 20.dp.toPx()
            val paddingBottom = 30.dp.toPx()

            val chartWidth = canvasWidth - paddingLeft - paddingRight
            val chartHeight = canvasHeight - paddingTop - paddingBottom

            val minValue = when (type) {
                "height" -> 45f
                "weight" -> 2f
                else -> 0f
            }
            val maxValue = when (type) {
                "height" -> 105f
                "weight" -> 18f
                else -> 0f
            }

            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(paddingLeft, paddingTop),
                end = Offset(paddingLeft, canvasHeight - paddingBottom),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(paddingLeft, canvasHeight - paddingBottom),
                end = Offset(canvasWidth - paddingRight, canvasHeight - paddingBottom),
                strokeWidth = 1.dp.toPx()
            )

            for (age in 0..displayMaxAge step 6) {
                val x = paddingLeft + (age.toFloat() / displayMaxAge) * chartWidth
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(x, paddingTop),
                    end = Offset(x, canvasHeight - paddingBottom),
                    strokeWidth = 0.5.dp.toPx()
                )
                val ageTextLayout = textMeasurer.measure("${age}月", labelStyle.copy(textAlign = TextAlign.Center))
                drawText(
                    textLayoutResult = ageTextLayout,
                    topLeft = Offset(x - 15.dp.toPx(), canvasHeight - paddingBottom + 5.dp.toPx())
                )
            }

            for (i in 0..4) {
                val value = minValue + (maxValue - minValue) * i / 4
                val y = canvasHeight - paddingBottom - (value - minValue) / (maxValue - minValue) * chartHeight
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(paddingLeft, y),
                    end = Offset(canvasWidth - paddingRight, y),
                    strokeWidth = 0.5.dp.toPx()
                )
                val valueTextLayout = textMeasurer.measure(String.format("%.0f", value), labelStyle.copy(textAlign = TextAlign.Right))
                drawText(
                    textLayoutResult = valueTextLayout,
                    topLeft = Offset(0f, y - 5.dp.toPx())
                )
            }

            val p3Points = mutableListOf<Offset>()
            val p50Points = mutableListOf<Offset>()
            val p97Points = mutableListOf<Offset>()

            for (age in 0..displayMaxAge) {
                val x = paddingLeft + (age.toFloat() / displayMaxAge) * chartWidth

                val p3 = when (type) {
                    "height" -> if (isMale) GrowthCurveData.BOY_HEIGHT[age]?.p3 ?: 50f else GrowthCurveData.GIRL_HEIGHT[age]?.p3 ?: 50f
                    "weight" -> if (isMale) GrowthCurveData.BOY_WEIGHT[age]?.p3 ?: 3f else GrowthCurveData.GIRL_WEIGHT[age]?.p3 ?: 3f
                    else -> 0f
                }
                val p50 = when (type) {
                    "height" -> if (isMale) GrowthCurveData.BOY_HEIGHT[age]?.p50 ?: 50f else GrowthCurveData.GIRL_HEIGHT[age]?.p50 ?: 50f
                    "weight" -> if (isMale) GrowthCurveData.BOY_WEIGHT[age]?.p50 ?: 3f else GrowthCurveData.GIRL_WEIGHT[age]?.p50 ?: 3f
                    else -> 0f
                }
                val p97 = when (type) {
                    "height" -> if (isMale) GrowthCurveData.BOY_HEIGHT[age]?.p97 ?: 50f else GrowthCurveData.GIRL_HEIGHT[age]?.p97 ?: 50f
                    "weight" -> if (isMale) GrowthCurveData.BOY_WEIGHT[age]?.p97 ?: 3f else GrowthCurveData.GIRL_WEIGHT[age]?.p97 ?: 3f
                    else -> 0f
                }

                val y3 = canvasHeight - paddingBottom - (p3 - minValue) / (maxValue - minValue) * chartHeight
                val y50 = canvasHeight - paddingBottom - (p50 - minValue) / (maxValue - minValue) * chartHeight
                val y97 = canvasHeight - paddingBottom - (p97 - minValue) / (maxValue - minValue) * chartHeight

                p3Points.add(Offset(x, y3))
                p50Points.add(Offset(x, y50))
                p97Points.add(Offset(x, y97))
            }

            fun drawDashedLine(points: List<Offset>, lineColor: Color) {
                if (points.size < 2) return
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = lineColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)),
                        cap = StrokeCap.Round
                    )
                }
            }

            drawDashedLine(p3Points, Color(0xFF90CAF9))
            drawDashedLine(p50Points, Color(0xFFA5D6A7))
            drawDashedLine(p97Points, Color(0xFFFFAB91))

            val dataPoints = validRecords.map { record ->
                val age = DateUtils.getMonthAge(record.recordTime).toFloat()
                val x = paddingLeft + (age / displayMaxAge) * chartWidth
                val value = when (type) {
                    "height" -> record.height ?: 0f
                    "weight" -> record.weight ?: 0f
                    else -> 0f
                }
                val y = canvasHeight - paddingBottom - (value - minValue) / (maxValue - minValue) * chartHeight
                Offset(x, y)
            }

            if (dataPoints.size > 1) {
                for (i in 0 until dataPoints.size - 1) {
                    drawLine(
                        color = primaryColor,
                        start = dataPoints[i],
                        end = dataPoints[i + 1],
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            dataPoints.forEach { point ->
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = backgroundColor,
                    radius = 2.dp.toPx(),
                    center = point
                )
            }

            val legendY = paddingTop + 10.dp.toPx()
            val legendStartX = canvasWidth - 120.dp.toPx()

            val legends = listOf(
                LegendItem("P3", Color(0xFF90CAF9), 0),
                LegendItem("P50", Color(0xFFA5D6A7), 1),
                LegendItem("P97", Color(0xFFFFAB91), 2)
            )
            legends.forEach { (label, lineColor, indexOffset) ->
                val x = legendStartX
                val y = legendY + indexOffset * 15.dp.toPx()
                drawLine(
                    color = lineColor,
                    start = Offset(x, y),
                    end = Offset(x + 20.dp.toPx(), y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                )
                val legendTextLayout = textMeasurer.measure(label, legendStyle)
                drawText(
                    textLayoutResult = legendTextLayout,
                    topLeft = Offset(x + 25.dp.toPx(), y - 5.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun HistoryRecordItem(record: GrowthRecord) {
    BabyCard(contentPadding = PaddingValues(Spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(record.recordTime)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "身高${record.height}cm 体重${record.weight}kg 头围${record.headCircumference}cm",
                    style = MaterialTheme.typography.bodySmall,
                    color = BabyGrowthTheme.colors.textSecondary
                )
            }
        }
    }
}
