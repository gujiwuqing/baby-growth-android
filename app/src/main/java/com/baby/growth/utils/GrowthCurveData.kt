package com.baby.growth.utils

/**
 * WHO 儿童生长标准数据及百分位数计算
 * 数据来源：WHO Child Growth Standards
 */
object GrowthCurveData {

    data class PercentilePoint(
        val p3: Float,
        val p50: Float,
        val p97: Float
    )

    // 男童身高标准 (cm) - 0-36个月
    val BOY_HEIGHT = mapOf(
        0 to PercentilePoint(46.1f, 49.9f, 53.7f),
        1 to PercentilePoint(50.8f, 54.7f, 58.6f),
        2 to PercentilePoint(54.4f, 58.4f, 62.4f),
        3 to PercentilePoint(57.3f, 61.4f, 65.5f),
        4 to PercentilePoint(59.7f, 63.9f, 68.0f),
        5 to PercentilePoint(61.7f, 65.9f, 70.1f),
        6 to PercentilePoint(63.3f, 67.6f, 71.9f),
        7 to PercentilePoint(64.8f, 69.2f, 73.5f),
        8 to PercentilePoint(66.2f, 70.6f, 75.0f),
        9 to PercentilePoint(67.5f, 72.0f, 76.5f),
        10 to PercentilePoint(68.7f, 73.3f, 77.9f),
        11 to PercentilePoint(69.9f, 74.5f, 79.2f),
        12 to PercentilePoint(71.0f, 75.7f, 80.5f),
        15 to PercentilePoint(74.0f, 79.1f, 84.2f),
        18 to PercentilePoint(76.5f, 82.3f, 87.9f),
        21 to PercentilePoint(78.8f, 84.9f, 91.1f),
        24 to PercentilePoint(81.0f, 87.1f, 93.6f),
        30 to PercentilePoint(84.8f, 92.0f, 98.7f),
        36 to PercentilePoint(88.0f, 96.1f, 103.4f)
    )

    // 女童身高标准 (cm)
    val GIRL_HEIGHT = mapOf(
        0 to PercentilePoint(45.4f, 49.1f, 52.9f),
        1 to PercentilePoint(49.8f, 53.7f, 57.6f),
        2 to PercentilePoint(53.0f, 57.1f, 61.1f),
        3 to PercentilePoint(55.8f, 59.8f, 63.8f),
        4 to PercentilePoint(58.0f, 62.1f, 66.2f),
        5 to PercentilePoint(59.9f, 64.0f, 68.2f),
        6 to PercentilePoint(61.5f, 65.7f, 70.0f),
        7 to PercentilePoint(62.9f, 67.3f, 71.6f),
        8 to PercentilePoint(64.3f, 68.7f, 73.1f),
        9 to PercentilePoint(65.6f, 70.1f, 74.5f),
        10 to PercentilePoint(66.8f, 71.5f, 75.9f),
        11 to PercentilePoint(68.0f, 72.8f, 77.3f),
        12 to PercentilePoint(69.2f, 74.0f, 78.6f),
        15 to PercentilePoint(72.2f, 77.5f, 82.4f),
        18 to PercentilePoint(74.8f, 80.7f, 86.1f),
        21 to PercentilePoint(77.1f, 83.4f, 89.3f),
        24 to PercentilePoint(79.3f, 86.4f, 92.6f),
        30 to PercentilePoint(83.2f, 91.1f, 97.8f),
        36 to PercentilePoint(86.5f, 95.1f, 102.2f)
    )

    // 男童体重标准 (kg)
    val BOY_WEIGHT = mapOf(
        0 to PercentilePoint(2.5f, 3.3f, 4.4f),
        1 to PercentilePoint(3.2f, 4.2f, 5.5f),
        2 to PercentilePoint(3.9f, 5.1f, 6.6f),
        3 to PercentilePoint(4.5f, 5.8f, 7.5f),
        4 to PercentilePoint(4.9f, 6.4f, 8.2f),
        5 to PercentilePoint(5.3f, 6.9f, 8.8f),
        6 to PercentilePoint(5.7f, 7.4f, 9.3f),
        7 to PercentilePoint(6.0f, 7.8f, 9.8f),
        8 to PercentilePoint(6.2f, 8.1f, 10.2f),
        9 to PercentilePoint(6.5f, 8.4f, 10.5f),
        10 to PercentilePoint(6.7f, 8.6f, 10.9f),
        11 to PercentilePoint(6.9f, 8.9f, 11.2f),
        12 to PercentilePoint(7.1f, 9.2f, 11.5f),
        15 to PercentilePoint(7.6f, 9.8f, 12.4f),
        18 to PercentilePoint(8.1f, 10.5f, 13.2f),
        21 to PercentilePoint(8.6f, 11.1f, 14.0f),
        24 to PercentilePoint(9.0f, 11.7f, 14.8f),
        30 to PercentilePoint(9.8f, 12.8f, 16.2f),
        36 to PercentilePoint(10.5f, 13.9f, 17.6f)
    )

    // 女童体重标准 (kg)
    val GIRL_WEIGHT = mapOf(
        0 to PercentilePoint(2.4f, 3.2f, 4.2f),
        1 to PercentilePoint(3.0f, 3.9f, 5.1f),
        2 to PercentilePoint(3.7f, 4.8f, 6.1f),
        3 to PercentilePoint(4.2f, 5.4f, 6.9f),
        4 to PercentilePoint(4.6f, 5.9f, 7.5f),
        5 to PercentilePoint(5.0f, 6.3f, 8.0f),
        6 to PercentilePoint(5.3f, 6.7f, 8.4f),
        7 to PercentilePoint(5.6f, 7.0f, 8.8f),
        8 to PercentilePoint(5.8f, 7.3f, 9.2f),
        9 to PercentilePoint(6.0f, 7.5f, 9.5f),
        10 to PercentilePoint(6.2f, 7.8f, 9.8f),
        11 to PercentilePoint(6.4f, 8.0f, 10.1f),
        12 to PercentilePoint(6.6f, 8.2f, 10.4f),
        15 to PercentilePoint(7.0f, 8.8f, 11.2f),
        18 to PercentilePoint(7.4f, 9.4f, 12.0f),
        21 to PercentilePoint(7.9f, 10.0f, 12.8f),
        24 to PercentilePoint(8.3f, 10.6f, 13.6f),
        30 to PercentilePoint(9.1f, 11.7f, 15.0f),
        36 to PercentilePoint(9.8f, 12.8f, 16.4f)
    )

    // 男童头围标准 (cm)
    val BOY_HEAD = mapOf(
        0 to PercentilePoint(31.5f, 34.5f, 37.5f),
        3 to PercentilePoint(37.9f, 40.5f, 43.1f),
        6 to PercentilePoint(40.7f, 43.3f, 45.9f),
        9 to PercentilePoint(42.2f, 44.8f, 47.4f),
        12 to PercentilePoint(43.3f, 45.9f, 48.5f),
        18 to PercentilePoint(44.8f, 47.5f, 50.2f),
        24 to PercentilePoint(45.8f, 48.6f, 51.4f),
        36 to PercentilePoint(47.2f, 50.0f, 52.8f)
    )

    // 女童头围标准 (cm)
    val GIRL_HEAD = mapOf(
        0 to PercentilePoint(31.0f, 33.9f, 36.7f),
        3 to PercentilePoint(37.1f, 39.7f, 42.2f),
        6 to PercentilePoint(39.6f, 42.2f, 44.7f),
        9 to PercentilePoint(41.1f, 43.7f, 46.2f),
        12 to PercentilePoint(42.2f, 44.8f, 47.3f),
        18 to PercentilePoint(43.6f, 46.3f, 48.9f),
        24 to PercentilePoint(44.7f, 47.4f, 50.1f),
        36 to PercentilePoint(46.0f, 48.8f, 51.6f)
    )

    /**
     * 计算百分位数
     * @param value 实际测量值
     * @param ageMonths 月龄
     * @param isMale 是否男童
     * @param type 类型：height/weight/head
     * @return 百分位数 (0-100)
     */
    fun calculatePercentile(value: Float, ageMonths: Int, isMale: Boolean, type: String): Float {
        val data = when (type) {
            "height" -> if (isMale) BOY_HEIGHT else GIRL_HEIGHT
            "weight" -> if (isMale) BOY_WEIGHT else GIRL_WEIGHT
            "head" -> if (isMale) BOY_HEAD else GIRL_HEAD
            else -> return 50f
        }

        val clampedAge = ageMonths.coerceIn(0, 36)
        val lowerKey = data.keys.filter { it <= clampedAge }.maxOrNull() ?: return 50f
        val upperKey = data.keys.filter { it >= clampedAge }.minOrNull() ?: return 50f

        val lower = data[lowerKey] ?: return 50f
        val upper = data[upperKey] ?: return 50f

        if (lowerKey == upperKey) {
            return estimatePercentile(value, lower)
        }

        val ratio = if (upperKey != lowerKey) {
            (clampedAge - lowerKey).toFloat() / (upperKey - lowerKey)
        } else 0f

        val p3 = lower.p3 + (upper.p3 - lower.p3) * ratio
        val p50 = lower.p50 + (upper.p50 - lower.p50) * ratio
        val p97 = lower.p97 + (upper.p97 - lower.p97) * ratio

        return estimatePercentile(value, PercentilePoint(p3, p50, p97))
    }

    private fun estimatePercentile(value: Float, point: PercentilePoint): Float {
        return when {
            value <= point.p3 -> 3f
            value >= point.p97 -> 97f
            value <= point.p50 -> {
                3f + (value - point.p3) / (point.p50 - point.p3) * 47f
            }
            else -> {
                50f + (value - point.p50) / (point.p97 - point.p50) * 47f
            }
        }
    }

    fun getPercentileDesc(percentile: Float): String {
        return when {
            percentile < 3f -> "偏低"
            percentile < 15f -> "偏低下"
            percentile < 85f -> "正常"
            percentile < 97f -> "偏高中"
            else -> "偏高"
        }
    }
}
