package com.seiv.app.data

// ---------------------------------------------------------------------------
// Kuesioner skrining (instrumen klinis tervalidasi)
// ---------------------------------------------------------------------------

enum class QuestionType { YES_NO, FREQUENCY }

data class Questionnaire(
    val code: String,
    val title: String,
    val fullName: String,
    val section: Int,
    val period: String,
    val minutes: Int,
    val description: String,
    val type: QuestionType,
    val stem: String?,
    val questions: List<String>,
)

val SRQ20 = Questionnaire(
    code = "SRQ-20",
    title = "SRQ-20",
    fullName = "Self-Reporting Questionnaire",
    section = 1,
    period = "30 hari terakhir",
    minutes = 5,
    description = "Mengukur kondisi umum kesehatan mental dalam 30 hari terakhir.",
    type = QuestionType.YES_NO,
    stem = null,
    questions = listOf(
        "Apakah kamu sering merasa sakit kepala?",
        "Apakah nafsu makan kamu berkurang?",
        "Apakah tidurmu tidak nyenyak?",
        "Apakah kamu mudah merasa takut?",
        "Apakah kamu merasa cemas, tegang, atau khawatir?",
        "Apakah tanganmu gemetar?",
        "Apakah pencernaanmu terganggu?",
        "Apakah kamu sulit berpikir dengan jernih?",
        "Apakah kamu merasa tidak bahagia?",
        "Apakah kamu menangis lebih sering dari biasanya?",
        "Apakah kamu sulit menikmati kegiatan sehari-hari?",
        "Apakah kamu sulit mengambil keputusan?",
        "Apakah tugas atau kegiatan sehari-harimu terbengkalai?",
        "Apakah kamu merasa tidak mampu berperan dalam hidupmu?",
        "Apakah kamu kehilangan minat terhadap banyak hal?",
        "Apakah kamu merasa tidak berharga?",
        "Apakah kamu pernah berpikir untuk mengakhiri hidup?",
        "Apakah kamu merasa lelah sepanjang waktu?",
        "Apakah kamu merasa tidak enak di perut?",
        "Apakah kamu mudah merasa lelah?",
    ),
)

val GAD7 = Questionnaire(
    code = "GAD-7",
    title = "GAD-7",
    fullName = "Generalized Anxiety Disorder",
    section = 2,
    period = "2 minggu terakhir",
    minutes = 3,
    description = "Mengukur tingkat kecemasan yang kamu rasakan.",
    type = QuestionType.FREQUENCY,
    stem = "Seberapa sering kamu mengalami:",
    questions = listOf(
        "Merasa gugup, cemas, atau sangat tegang",
        "Tidak mampu menghentikan atau mengendalikan rasa khawatir",
        "Terlalu khawatir tentang berbagai hal",
        "Sulit untuk merasa santai",
        "Sangat gelisah sehingga sulit untuk duduk diam",
        "Mudah kesal atau mudah tersinggung",
        "Merasa takut seolah-olah sesuatu yang buruk akan terjadi",
    ),
)

val PHQ9 = Questionnaire(
    code = "PHQ-9",
    title = "PHQ-9",
    fullName = "Patient Health Questionnaire",
    section = 3,
    period = "2 minggu terakhir",
    minutes = 3,
    description = "Mengukur gejala depresi yang mungkin kamu alami.",
    type = QuestionType.FREQUENCY,
    stem = "Seberapa sering kamu mengalami:",
    questions = listOf(
        "Sedikit tertarik atau tidak merasa senang dalam melakukan sesuatu",
        "Merasa sedih, murung, atau putus asa",
        "Sulit tidur, mudah terbangun, atau tidur terlalu banyak",
        "Merasa lelah atau kurang bertenaga",
        "Nafsu makan berkurang atau makan berlebihan",
        "Merasa buruk tentang diri sendiri, merasa gagal, atau mengecewakan diri sendiri maupun keluarga",
        "Sulit berkonsentrasi, misalnya saat membaca atau menonton",
        "Bergerak atau berbicara sangat lambat, atau justru sangat gelisah hingga sulit diam",
        "Berpikir bahwa lebih baik mati atau ingin menyakiti diri sendiri",
    ),
)

val QUESTIONNAIRES = listOf(SRQ20, GAD7, PHQ9)

val FREQUENCY_OPTIONS = listOf("Tidak pernah", "Beberapa hari", "Lebih dari setengah waktu", "Hampir setiap hari")

enum class Severity(val label: String, val level: Int) {
    NORMAL("Normal", 0), MINIMAL("Minimal", 0), RINGAN("Ringan", 1), SEDANG("Sedang", 2),
    SEDANG_BERAT("Sedang-berat", 3), BERAT("Berat", 3), INDIKASI("Ada indikasi", 2)
}

fun srqSeverity(score: Int) = if (score >= 6) Severity.INDIKASI else Severity.NORMAL
fun gadSeverity(score: Int) = when {
    score <= 4 -> Severity.MINIMAL
    score <= 9 -> Severity.RINGAN
    score <= 14 -> Severity.SEDANG
    else -> Severity.BERAT
}
fun phqSeverity(score: Int) = when {
    score <= 4 -> Severity.MINIMAL
    score <= 9 -> Severity.RINGAN
    score <= 14 -> Severity.SEDANG
    score <= 19 -> Severity.SEDANG_BERAT
    else -> Severity.BERAT
}

fun srqDescription(score: Int) = if (score >= 6)
    "Ada indikasi gangguan mental emosional. Sebaiknya bicarakan kondisimu dengan tenaga profesional."
else "Kondisi mentalmu secara umum tampak baik. Tetap jaga keseimbangan hidup dan rutinitas positifmu."

fun gadDescription(score: Int) = when (gadSeverity(score)) {
    Severity.MINIMAL -> "Tingkat kecemasanmu minimal. Pertahankan kebiasaan baik yang menenangkanmu."
    Severity.RINGAN -> "Ada kecemasan ringan. Latihan napas dan menulis jurnal bisa membantu meredakannya."
    Severity.SEDANG -> "Kecemasanmu berada di tingkat sedang. Pertimbangkan untuk berbicara dengan konselor."
    else -> "Kecemasanmu cukup berat. Kami sangat menyarankan kamu berkonsultasi dengan profesional."
}

fun phqDescription(score: Int) = when (phqSeverity(score)) {
    Severity.MINIMAL -> "Gejala depresi minimal. Terus rawat dirimu dengan aktivitas yang kamu sukai."
    Severity.RINGAN -> "Ada gejala depresi ringan. Pantau suasana hatimu lewat check-in harian."
    Severity.SEDANG -> "Gejala depresi tingkat sedang. Berbicara dengan konselor dapat sangat membantu."
    else -> "Gejala depresimu cukup berat. Segera hubungi tenaga profesional untuk pendampingan."
}

/** Tingkat kebutuhan bantuan: 0 = promotif, 1 = preventif, 2 = kuratif (konseling), 3 = darurat. */
fun ScreeningResult.careLevel(): Int = when {
    phqItem9 > 0 -> 3
    phqSeverity(phq).level >= 2 || gadSeverity(gad).level >= 2 || srq >= 6 -> 2
    phqSeverity(phq).level == 1 || gadSeverity(gad).level == 1 -> 1
    else -> 0
}

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

fun counselorById(id: String) = COUNSELORS.firstOrNull { it.id == id } ?: COUNSELORS.first()

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
