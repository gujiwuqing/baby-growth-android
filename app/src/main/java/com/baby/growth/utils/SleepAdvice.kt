package com.baby.growth.utils

/**
 * AAP 睡眠时长建议及评估
 * 数据来源：American Academy of Pediatrics
 */
object SleepAdvice {

    data class SleepRecommendation(
        val minHours: Float,
        val maxHours: Float,
        val recommendedHours: Float,
        val warning: String,
        val recommendation: String
    )

    private val SLEEP_RECOMMENDATIONS = mapOf(
        0 to SleepRecommendation(14f, 17f, 16f, "新生儿每天需要14-17小时睡眠", "频繁小睡是正常的，无需担心"),
        1 to SleepRecommendation(14f, 17f, 15.5f, "1月龄宝宝每天需要14-17小时睡眠", "昼夜颠倒属于正常现象"),
        2 to SleepRecommendation(14f, 17f, 15f, "2月龄宝宝每天需要14-17小时睡眠", "开始建立昼夜节律"),
        3 to SleepRecommendation(14f, 16f, 15f, "3月龄宝宝每天需要14-16小时睡眠", "夜间睡眠开始变长"),
        4 to SleepRecommendation(12f, 16f, 14.5f, "4月龄宝宝每天需要12-16小时睡眠", "可以开始培养规律作息"),
        5 to SleepRecommendation(12f, 16f, 14f, "5月龄宝宝每天需要12-16小时睡眠", "夜醒频率开始降低"),
        6 to SleepRecommendation(12f, 15f, 14f, "6月龄宝宝每天需要12-15小时睡眠", "夜醒可能增加（睡眠倒退期）"),
        7 to SleepRecommendation(12f, 15f, 13.5f, "7月龄宝宝每天需要12-15小时睡眠", "坚持规律作息很重要"),
        8 to SleepRecommendation(12f, 15f, 13.5f, "8月龄宝宝每天需要12-15小时睡眠", "白天小睡2-3次"),
        9 to SleepRecommendation(12f, 15f, 13f, "9月龄宝宝每天需要12-15小时睡眠", "夜间应能连续6-8小时"),
        10 to SleepRecommendation(12f, 15f, 13f, "10月龄宝宝每天需要12-15小时睡眠", "白天小睡2次"),
        11 to SleepRecommendation(12f, 15f, 13f, "11月龄宝宝每天需要12-15小时睡眠", "白天小睡可能减为1-2次"),
        12 to SleepRecommendation(11f, 14f, 13f, "1岁宝宝每天需要11-14小时睡眠", "白天小睡1-2次"),
        18 to SleepRecommendation(11f, 14f, 12.5f, "18月龄宝宝每天需要11-14小时睡眠", "白天小睡1次"),
        24 to SleepRecommendation(11f, 14f, 12f, "2岁宝宝每天需要11-14小时睡眠", "白天小睡1次"),
        36 to SleepRecommendation(10f, 13f, 12f, "3岁宝宝每天需要10-13小时睡眠", "白天小睡可能取消")
    )

    fun getSleepAdvice(monthAge: Int): SleepRecommendation {
        val clampedAge = monthAge.coerceIn(0, 36)
        val key = SLEEP_RECOMMENDATIONS.keys.filter { it <= clampedAge }.maxOrNull() ?: 0
        return SLEEP_RECOMMENDATIONS[key] ?: SLEEP_RECOMMENDATIONS[0]!!
    }

    fun evaluateSleep(monthAge: Int, actualHours: Float): String {
        val advice = getSleepAdvice(monthAge)
        return when {
            actualHours < advice.minHours -> "睡眠不足 ⚠️ 建议增加${(advice.minHours - actualHours).toInt()}小时"
            actualHours > advice.maxHours -> "睡眠偏多，可能影响白天活动"
            else -> "睡眠正常 ✅"
        }
    }
}