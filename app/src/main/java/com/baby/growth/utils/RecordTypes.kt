package com.baby.growth.utils

/**
 * 记录类型元信息
 * 定义所有记录类型的label、icon、颜色等
 */
object RecordTypes {

    data class RecordTypeMeta(
        val key: String,
        val label: String,
        val icon: String,
        val colorHex: String
    )

    val ALL_TYPES = listOf(
        RecordTypeMeta("breast", "母乳", "🤱", "#E8857A"),
        RecordTypeMeta("formula", "配方奶", "🍼", "#5B8DEF"),
        RecordTypeMeta("bottle", "瓶喂母乳", "🍼", "#9DC4E0"),
        RecordTypeMeta("diaper", "换纸尿裤", "👶", "#8CC9B0"),
        RecordTypeMeta("sleep", "睡眠", "😴", "#B8A9D4"),
        RecordTypeMeta("food", "辅食", "🥣", "#F5C5A3"),
        RecordTypeMeta("supplement", "营养补剂", "💊", "#A8D8EA"),
        RecordTypeMeta("growth", "成长指标", "📏", "#FFB6C1")
    )

    fun getByKey(key: String): RecordTypeMeta? =
        ALL_TYPES.find { it.key == key }

    fun getLabel(key: String): String =
        getByKey(key)?.label ?: key

    fun getIcon(key: String): String =
        getByKey(key)?.icon ?: "📝"

    fun getFeedTypes(): List<RecordTypeMeta> =
        ALL_TYPES.filter { it.key in listOf("breast", "formula", "bottle") }

    fun getRecordTypesForHome(): List<RecordTypeMeta> =
        ALL_TYPES.filter { it.key != "bottle" }

    /** 大便颜色选项 */
    val POO_COLORS = listOf(
        "黄色" to "#F5C518",
        "金黄色" to "#FFD700",
        "浅黄色" to "#FFFACD",
        "绿色" to "#90EE90",
        "深绿色" to "#006400",
        "棕色" to "#8B4513",
        "浅棕色" to "#D2B48C",
        "黑色" to "#1A1A1A",
        "灰白色" to "#D3D3D3",
        "红色" to "#FF4444",
        "橙黄色" to "#FFA500",
        "黄绿色" to "#9ACD32"
    )

    /** 大便形状选项 */
    val POO_SHAPES = listOf(
        "糊状", "软便", "成形软便", "硬便", "水样",
        "蛋花汤样", "黏液便", "泡沫便", "颗粒状",
        "条状", "稀便", "脓血便", "柏油样",
        "干硬粒状", "冻胶状"
    )

    /** 食物分类 */
    val FOOD_CATEGORIES = listOf("谷物", "蔬菜", "水果", "肉类", "蛋类", "奶制品")

    /** 营养补剂类型 */
    val SUPPLEMENT_TYPES = listOf(
        "维生素AD" to "vitamin_ad",
        "DHA" to "dha",
        "钙" to "calcium",
        "益生菌" to "probiotics",
        "铁" to "iron",
        "锌" to "zinc"
    )

    /** 睡眠质量选项 */
    val SLEEP_QUALITIES = listOf(
        "好" to "good",
        "一般" to "average",
        "差" to "poor"
    )

    /** 食物反应选项 */
    val FOOD_REACTIONS = listOf(
        "正常" to "normal",
        "过敏" to "allergy",
        "拒绝" to "reject"
    )
}