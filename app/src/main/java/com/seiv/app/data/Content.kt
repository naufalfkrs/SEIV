package com.seiv.app.data
// ---------------------------------------------------------------------------
// Check-in & insight
// ---------------------------------------------------------------------------

val MOODS = listOf(
    "😣" to "Sangat buruk",
    "😔" to "Buruk",
    "😐" to "Netral",
    "🙂" to "Baik",
    "😄" to "Sangat baik",
)

val FACTORS = listOf(
    "📚" to "Akademik", "👨‍👩‍👧" to "Keluarga", "💛" to "Relasi",
    "🤝" to "Organisasi", "💼" to "Pekerjaan", "✦" to "Lainnya",
)

val FEELINGS = listOf(
    "Cemas", "Lelah", "Tenang", "Overthinking", "Senang", "Sedih",
    "Bersemangat", "Stres", "Bingung", "Bersyukur",
)

data class Insight(val title: String, val message: String, val tip: String)

fun buildInsight(c: CheckIn, recent: List<CheckIn>): Insight {
    val lowStreak = recent.take(3).count { it.mood <= 1 }
    val title = when (c.mood) {
        0 -> "Hari ini terasa berat, ya"
        1 -> "Terima kasih sudah jujur pada dirimu"
        2 -> "Hari yang biasa juga berharga"
        3 -> "Senang mendengarnya!"
        else -> "Wah, harimu menyenangkan!"
    }
    val factorText = if (c.factors.isNotEmpty())
        " Sepertinya ${c.factors.joinToString(", ").lowercase()} cukup memengaruhi harimu." else ""
    val message = when (c.mood) {
        0, 1 -> "Perasaanmu valid dan tidak apa-apa untuk tidak baik-baik saja.$factorText Kamu tidak sendirian."
        2 -> "Tidak semua hari harus luar biasa.$factorText Coba sisihkan waktu sejenak untuk dirimu sendiri."
        else -> "Rayakan hal-hal kecil yang membuatmu merasa baik hari ini.$factorText"
    }
    val tip = when {
        lowStreak >= 3 -> "Suasana hatimu menurun beberapa hari terakhir. Coba lakukan skrining untuk memahami kondisimu lebih dalam, atau bicara dengan konselor."
        "Cemas" in c.feelings || "Overthinking" in c.feelings || "Stres" in c.feelings ->
            "Coba latihan napas 4-7-8 selama 2 menit untuk menenangkan pikiran."
        "Sedih" in c.feelings || c.mood <= 1 -> "Menuliskan perasaan di jurnal atau bercerita di Ruang Cerita bisa membantu meringankan beban."
        "Lelah" in c.feelings -> "Tubuhmu mungkin butuh istirahat. Usahakan tidur 7–8 jam malam ini."
        else -> "Tuliskan satu hal yang kamu syukuri hari ini di jurnal refleksimu."
    }
    return Insight(title, message, tip)
}

val FACTS = listOf(
    "Menghabiskan 20 menit di alam terbuka terbukti menurunkan kadar hormon stres kortisol." to "Frontiers in Psychology",
    "Menulis jurnal ekspresif 15 menit sehari dapat membantu mengurangi gejala kecemasan." to "Advances in Psychiatric Treatment",
    "Tidur kurang dari 6 jam meningkatkan risiko emosi negatif keesokan harinya." to "Journal of Experimental Psychology",
    "Napas lambat dan dalam dapat mengaktifkan sistem saraf parasimpatis yang menenangkan tubuh." to "Frontiers in Human Neuroscience",
    "Berbicara tentang perasaan dengan orang yang dipercaya dapat menurunkan intensitas emosi negatif." to "Psychological Science",
)

val JOURNAL_PROMPTS = listOf(
    "Apa satu hal kecil yang membuatmu tersenyum hari ini?",
    "Apa yang sedang paling memenuhi pikiranmu saat ini?",
    "Jika kamu bisa berbicara dengan dirimu setahun lalu, apa yang ingin kamu katakan?",
    "Tuliskan tiga hal yang kamu syukuri hari ini.",
    "Apa yang kamu butuhkan dari dirimu sendiri minggu ini?",
    "Momen apa hari ini yang membuatmu merasa lelah? Apa yang bisa membantumu?",
)

// ---------------------------------------------------------------------------
// Konselor & bantuan profesional
// ---------------------------------------------------------------------------

data class Counselor(
    val id: String,
    val name: String,
    val role: String,
    val rating: Double,
    val reviews: Int,
    val tags: List<String>,
    val bio: String,
    val price: Int,
    val initials: String,
    val colorIndex: Int,
)

val COUNSELORS = listOf(
    Counselor("dinda", "Ns. Dinda Aulia", "Nurse Counselor", 4.9, 128, listOf("Stres akademik", "Kecemasan"),
        "Spesialis dukungan kesehatan mental remaja dan gaya hidup sehat.", 150_000, "DA", 0),
    Counselor("anisa", "Dr. Anisa Rahma", "Psikolog", 4.9, 152, listOf("Kecemasan", "Self-esteem"),
        "Berpengalaman dalam terapi kognitif dan manajemen emosi.", 200_000, "AR", 1),
    Counselor("fajar", "M. Fajar Ramadhan", "Psikolog", 4.8, 86, listOf("Stres akademik", "Motivasi"),
        "Fokus pada produktivitas, motivasi, dan pengembangan diri.", 175_000, "FR", 2),
)

fun counselorById(id: String) = SeivStore.counselorById(id)

const val PLATFORM_FEE = 5_000

data class Hotline(val name: String, val number: String, val desc: String)

val HOTLINES = listOf(
    Hotline("Layanan SEJIWA (Kemenkes)", "119", "Tekan ekstensi 8 untuk layanan kesehatan jiwa. 24 jam."),
    Hotline("Gawat Darurat", "112", "Layanan darurat nasional jika kamu atau orang lain dalam bahaya."),
    Hotline("Into The Light Indonesia", "intothelightid.org", "Informasi dan direktori layanan pencegahan bunuh diri."),
)

fun rupiah(value: Int): String {
    val s = value.toString().reversed().chunked(3).joinToString(".").reversed()
    return "Rp $s"
}
