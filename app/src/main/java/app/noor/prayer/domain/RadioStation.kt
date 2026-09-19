package app.noor.prayer.domain

data class RadioStation(val id: String, val name: String, val url: String)

/** Curated from mp3quran.net's public radios API; every URL verified live (HTTP 200, audio/mpeg) before inclusion. */
val RadioStations = listOf(
    RadioStation("yasser_aldosari","ياسر الدوسري","https://backup.qurango.net/radio/yasser_aldosari"),
    RadioStation("maher","ماهر المعيقلي","https://backup.qurango.net/radio/maher"),
    RadioStation("abdulrahman_alsudaes","عبدالرحمن السديس","https://backup.qurango.net/radio/abdulrahman_alsudaes"),
    RadioStation("mishary_alafasi","مشاري العفاسي","https://backup.qurango.net/radio/mishary_alafasi"),
    RadioStation("mahmoud_khalil_alhussary","محمود خليل الحصري","https://backup.qurango.net/radio/mahmoud_khalil_alhussary"),
    RadioStation("mohammed_siddiq_alminshawi","محمد صديق المنشاوي","https://backup.qurango.net/radio/mohammed_siddiq_alminshawi"),
    RadioStation("ahmad_alajmy","أحمد العجمي","https://backup.qurango.net/radio/ahmad_alajmy"),
    RadioStation("shaik_abu_bakr_al_shatri","أبو بكر الشاطري","https://backup.qurango.net/radio/shaik_abu_bakr_al_shatri"),
    RadioStation("mix","منوعة قراء","https://backup.qurango.net/radio/mix"),
    RadioStation("tafseer","تفسير القرآن الكريم","https://backup.qurango.net/radio/tafseer"),
)
