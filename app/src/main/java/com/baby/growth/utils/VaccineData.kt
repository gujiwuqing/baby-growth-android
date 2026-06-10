package com.baby.growth.utils

/**
 * 中国儿童免疫规划疫苗数据
 * 包含一类（免费）和二类（自费）疫苗
 */
object VaccineData {

    data class VaccineInfo(
        val name: String,
        val fullName: String,
        val type: String,       // "free" or "paid"
        val dose: String,       // 剂次
        val ageMonths: Int,     // 推荐月龄
        val description: String,
        val diseases: String,
        val isOral: Boolean = false
    )

    val FREE_VACCINES = listOf(
        VaccineInfo("乙肝疫苗", "重组乙型肝炎疫苗（乙肝疫苗）", "free", "第1剂", 0, "出生24小时内接种", "预防乙型肝炎"),
        VaccineInfo("卡介苗", "卡介苗", "free", "第1剂", 0, "出生24小时内接种", "预防结核病"),
        VaccineInfo("乙肝疫苗", "重组乙型肝炎疫苗（乙肝疫苗）", "free", "第2剂", 1, "1月龄接种", "预防乙型肝炎"),
        VaccineInfo("脊灰灭活疫苗", "脊髓灰质炎灭活疫苗(IPV)", "free", "第1剂", 2, "2月龄接种", "预防脊髓灰质炎"),
        VaccineInfo("百白破疫苗", "百白破疫苗（DTaP）", "free", "第1剂", 3, "3月龄接种", "预防百日咳、白喉、破伤风"),
        VaccineInfo("脊灰减毒活疫苗", "脊髓灰质炎减毒活疫苗(OPV)", "free", "第2剂", 3, "3月龄接种，口服", "预防脊髓灰质炎", true),
        VaccineInfo("脊灰灭活疫苗", "脊髓灰质炎灭活疫苗(IPV)", "free", "第3剂", 4, "4月龄接种", "预防脊髓灰质炎"),
        VaccineInfo("百白破疫苗", "百白破疫苗（DTaP）", "free", "第2剂", 4, "4月龄接种", "预防百日咳、白喉、破伤风"),
        VaccineInfo("脊灰减毒活疫苗", "脊髓灰质炎减毒活疫苗(OPV)", "free", "第3剂", 4, "4月龄接种，口服", "预防脊髓灰质炎", true),
        VaccineInfo("百白破疫苗", "百白破疫苗（DTaP）", "free", "第3剂", 5, "5月龄接种", "预防百日咳、白喉、破伤风"),
        VaccineInfo("乙肝疫苗", "重组乙型肝炎疫苗（乙肝疫苗）", "free", "第3剂", 6, "6月龄接种", "预防乙型肝炎"),
        VaccineInfo("流脑A群疫苗", "A群脑膜炎球菌多糖疫苗", "free", "第1剂", 6, "6月龄接种", "预防A群脑膜炎"),
        VaccineInfo("麻腮风疫苗", "麻腮风联合疫苗（MMR）", "free", "第1剂", 8, "8月龄接种", "预防麻疹、腮腺炎、风疹"),
        VaccineInfo("乙脑减毒活疫苗", "乙型脑炎减毒活疫苗", "free", "第1剂", 8, "8月龄接种", "预防乙型脑炎"),
        VaccineInfo("流脑A群疫苗", "A群脑膜炎球菌多糖疫苗", "free", "第2剂", 9, "9月龄接种", "预防A群脑膜炎"),
        VaccineInfo("甲肝减毒活疫苗", "甲型肝炎减毒活疫苗", "free", "第1剂", 18, "18月龄接种", "预防甲型肝炎"),
        VaccineInfo("百白破疫苗", "百白破疫苗（DTaP）", "free", "第4剂", 18, "18月龄接种", "预防百日咳、白喉、破伤风"),
        VaccineInfo("麻腮风疫苗", "麻腮风联合疫苗（MMR）", "free", "第2剂", 18, "18月龄接种", "预防麻疹、腮腺炎、风疹"),
        VaccineInfo("乙脑减毒活疫苗", "乙型脑炎减毒活疫苗", "free", "第2剂", 24, "24月龄接种", "预防乙型脑炎"),
        VaccineInfo("流脑A+C群疫苗", "A+C群脑膜炎球菌多糖疫苗", "free", "第1剂", 36, "3周岁接种", "预防A+C群脑膜炎"),
        VaccineInfo("脊灰减毒活疫苗", "脊髓灰质炎减毒活疫苗(OPV)", "free", "第4剂", 48, "4周岁接种，口服", "预防脊髓灰质炎", true),
        VaccineInfo("流脑A+C群疫苗", "A+C群脑膜炎球菌多糖疫苗", "free", "第2剂", 72, "6周岁接种", "预防A+C群脑膜炎"),
        VaccineInfo("白破疫苗", "白喉破伤风联合疫苗", "free", "第1剂", 72, "6周岁接种", "预防白喉、破伤风")
    )

    val PAID_VACCINES = listOf(
        VaccineInfo("13价肺炎疫苗", "13价肺炎球菌结合疫苗", "paid", "第1剂", 2, "2月龄接种，共4剂", "预防肺炎球菌感染"),
        VaccineInfo("五联疫苗", "五联疫苗（DTaP-IPV-Hib）", "paid", "第1剂", 2, "2月龄接种，替代百白破+脊灰+Hib", "预防百日咳、白喉、破伤风、脊灰、Hib"),
        VaccineInfo("Hib疫苗", "b型流感嗜血杆菌疫苗", "paid", "第1剂", 2, "2月龄接种", "预防Hib感染"),
        VaccineInfo("轮状病毒疫苗", "轮状病毒疫苗", "paid", "第1剂", 2, "2月龄接种，口服", "预防轮状病毒腹泻", true),
        VaccineInfo("流感疫苗", "流感疫苗", "paid", "每年1剂", 6, "6月龄以上每年接种", "预防流感"),
        VaccineInfo("EV71手足口疫苗", "肠道病毒71型疫苗", "paid", "第1剂", 6, "6月龄接种，共2剂", "预防手足口病"),
        VaccineInfo("水痘疫苗", "水痘疫苗", "paid", "第1剂", 12, "12月龄接种", "预防水痘"),
        VaccineInfo("23价肺炎疫苗", "23价肺炎球菌多糖疫苗", "paid", "第1剂", 24, "24月龄接种", "预防肺炎球菌感染"),
        VaccineInfo("甲肝灭活疫苗", "甲型肝炎灭活疫苗", "paid", "第1剂", 18, "18月龄接种，共2剂", "预防甲型肝炎"),
        VaccineInfo("狂犬病疫苗", "狂犬病疫苗", "paid", "暴露后接种", 0, "暴露后接种，不限月龄", "预防狂犬病")
    )

    fun getAllVaccines(): List<VaccineInfo> = FREE_VACCINES + PAID_VACCINES

    fun getVaccinesByType(type: String): List<VaccineInfo> =
        getAllVaccines().filter { it.type == type }

    fun getVaccinesByAge(ageMonths: Int): List<VaccineInfo> =
        getAllVaccines().filter { it.ageMonths <= ageMonths + 1 }
}