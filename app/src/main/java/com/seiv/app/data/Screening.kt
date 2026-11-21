package com.seiv.app.data

// ---------------------------------------------------------------------------
// Skrining kesehatan jiwa — 6 instrumen:
// SRQ-20, lalu instrumen dari "Kuesioner Kesehatan Mental Mahasiswa" (FIKES UB):
// PHQ-9, GAD-7, ASQ, CAPE-15, CD-RISC-10. Teks butir mengikuti Google Form tersebut.
// ---------------------------------------------------------------------------

enum class QuestionType { YES_NO, SCALE }

/**
 * @param options pilihan khusus butir ini (nilai = indeks); null = pakai pilihan kuesioner.
 * @param scored false untuk butir tambahan yang tidak dihitung ke skor total.
 * @param optional butir boleh dilewati.
 * @param visibleIf kapan butir ditampilkan, berdasarkan jawaban butir lain di kuesioner yang sama.
 */
data class Question(
    val text: String,
    val options: List<String>? = null,
    val scored: Boolean = true,
    val optional: Boolean = false,
    val visibleIf: ((List<Int?>) -> Boolean)? = null,
)

data class Questionnaire(
    val code: String,
    val title: String,
    val fullName: String,
    val section: Int,
    val period: String,
    val minutes: Int,
    val description: String,
    val instruction: String,
    val type: QuestionType,
    val options: List<String>,
    val questions: List<Question>,
) {
    /** Jumlah butir utama yang dinilai (untuk tampilan "x pertanyaan"). */
    val mainCount get() = questions.count { it.visibleIf == null }

    fun optionsFor(q: Question): List<Pair<Int, String>> = when {
        q.options != null -> q.options.mapIndexed { i, s -> i to s }
        type == QuestionType.YES_NO -> listOf(1 to "Ya", 0 to "Tidak")
        else -> options.mapIndexed { i, s -> i to s }
    }
}

private val YES_NO = listOf("Ya", "Tidak")
private val FREQ_2W = listOf("Tidak pernah", "Beberapa hari", "Lebih dari separuh waktu", "Hampir setiap hari")
private val DIFFICULTY = listOf("Sangat tidak sulit", "Sedikit sulit", "Sangat sulit", "Luar biasa sulit")

val SRQ20 = Questionnaire(
    code = "SRQ-20", title = "SRQ-20", fullName = "Self-Reporting Questionnaire",
    section = 1, period = "30 hari terakhir", minutes = 5,
    description = "Mengukur kondisi umum kesehatan mental dalam 30 hari terakhir.",
    instruction = "Jawablah sesuai yang kamu rasakan selama 30 hari terakhir.",
    type = QuestionType.YES_NO, options = YES_NO,
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
    ).map { Question(it) },
)

val PHQ9 = Questionnaire(
    code = "PHQ-9", title = "PHQ-9", fullName = "Patient Health Questionnaire-9",
    section = 2, period = "2 minggu terakhir", minutes = 3,
    description = "Mengukur gejala depresi yang mungkin kamu alami.",
    instruction = "Selama 2 minggu terakhir, seberapa sering Anda terganggu oleh masalah-masalah berikut?",
    type = QuestionType.SCALE, options = FREQ_2W,
    questions = listOf(
        "Kurang tertarik atau bergairah dalam melakukan apapun",
        "Merasa murung, muram, atau putus asa",
        "Sulit tidur atau mudah terbangun, atau terlalu banyak tidur",
        "Merasa lelah atau kurang bertenaga",
        "Kurang nafsu makan atau terlalu banyak makan",
        "Kurang percaya diri — atau merasa bahwa Anda adalah orang yang gagal atau telah mengecewakan diri sendiri atau keluarga",
        "Sulit berkonsentrasi pada sesuatu, misalnya membaca koran atau menonton televisi",
        "Bergerak atau berbicara sangat lambat sehingga orang lain memperhatikannya. Atau sebaliknya — merasa resah atau gelisah sehingga Anda lebih sering bergerak dari biasanya",
        "Merasa lebih baik mati atau ingin melukai diri sendiri dengan cara apapun",
    ).map { Question(it) } + Question(
        "Jika Anda mengalami salah satu masalah di atas, seberapa besar kesulitan yang ditimbulkan karenanya dalam melakukan pekerjaan, perkuliahan, mengurus pekerjaan rumah tangga, atau bergaul dengan orang lain?",
        options = DIFFICULTY, scored = false, optional = true,
        visibleIf = { a -> a.take(9).any { (it ?: 0) > 0 } },
    ),
)

val GAD7 = Questionnaire(
    code = "GAD-7", title = "GAD-7", fullName = "Generalized Anxiety Disorder-7",
    section = 3, period = "2 minggu terakhir", minutes = 3,
    description = "Mengukur tingkat kecemasan yang kamu rasakan.",
    instruction = "Selama 2 minggu terakhir, seberapa sering Anda merasa terganggu oleh masalah-masalah berikut?",
    type = QuestionType.SCALE, options = FREQ_2W,
    questions = listOf(
        "Merasa gugup, cemas, atau gelisah",
        "Merasa tidak mampu menghentikan atau mengendalikan rasa khawatir",
        "Terlalu mengkhawatirkan berbagai hal",
        "Sulit untuk merasa tenang/rileks",
        "Merasa sangat gelisah sehingga sulit untuk duduk diam",
        "Menjadi mudah terganggu, mudah kesal, atau mudah marah",
        "Merasa khawatir seakan-akan sesuatu yang buruk akan terjadi",
    ).map { Question(it) } + Question(
        "Jika Anda mengalami salah satu masalah di atas, seberapa besar kesulitan yang ditimbulkan karenanya dalam menjalani pekerjaan, perkuliahan, aktivitas rumah tangga, atau hubungan dengan orang lain?",
        options = DIFFICULTY, scored = false, optional = true,
        visibleIf = { a -> a.take(7).any { (it ?: 0) > 0 } },
    ),
)

val ASQ = Questionnaire(
    code = "ASQ", title = "ASQ", fullName = "Ask Suicide-Screening Questions",
    section = 4, period = "beberapa minggu terakhir", minutes = 2,
    description = "Menyaring risiko bunuh diri agar kamu bisa segera mendapat dukungan.",
    instruction = "Pilih \"Ya\" jika pernyataan sesuai dengan yang Anda alami, \"Tidak\" jika tidak. Jawablah dengan jujur — tidak ada jawaban benar atau salah.",
    type = QuestionType.YES_NO, options = YES_NO,
    questions = listOf(
        Question("Dalam beberapa minggu terakhir, apakah Anda berharap Anda sudah meninggal?"),
        Question("Dalam beberapa minggu terakhir, apakah Anda merasa bahwa Anda atau keluarga Anda akan menjadi lebih baik jika Anda meninggal?"),
        Question("Dalam satu minggu terakhir, apakah Anda memiliki pikiran untuk mengakhiri hidup Anda?"),
        Question("Apakah Anda pernah mencoba mengakhiri hidup Anda?"),
        Question(
            "Kapan percobaan paling akhir dilakukan?",
            options = listOf("Dalam 12 bulan terakhir", "Lebih dari 1 tahun lalu"), scored = false,
            visibleIf = { a -> a[3] == 1 },
        ),
        Question(
            "Apakah saat ini Anda memiliki pikiran untuk mengakhiri hidup Anda?", scored = false,
            visibleIf = { a -> a.take(4).any { it == 1 } },
        ),
    ),
)

private val FREQ_LIFE = listOf("Tidak pernah", "Jarang", "Kadang-kadang", "Sering", "Sangat sering")

val CAPE15 = Questionnaire(
    code = "CAPE-15", title = "CAPE-15", fullName = "Community Assessment of Psychic Experiences-15",
    section = 5, period = "sepanjang hidup", minutes = 4,
    description = "Mengenali pengalaman psikologis tertentu yang mungkin pernah kamu alami.",
    instruction = "Untuk setiap pernyataan, tunjukkan seberapa sering Anda mengalaminya sepanjang hidup Anda.",
    type = QuestionType.SCALE, options = FREQ_LIFE,
    questions = listOf(
        "Apakah Anda pernah merasa seolah-olah orang lain memberi petunjuk atau mengatakan sesuatu yang bermakna ganda mengenai Anda?",
        "Apakah Anda pernah merasa seolah-olah beberapa orang menampilkan dirinya berbeda dari yang sebenarnya?",
        "Apakah Anda pernah merasa bahwa Anda sedang dikejar-kejar atau diperlakukan tidak adil dengan cara tertentu?",
        "Apakah Anda pernah merasa seolah-olah ada konspirasi (persekongkolan) terhadap diri Anda?",
        "Apakah Anda pernah merasa orang lain memandang Anda dengan aneh karena penampilan Anda?",
        "Apakah Anda pernah merasa seolah-olah peralatan elektronik seperti komputer dapat mempengaruhi cara Anda berpikir?",
        "Apakah Anda pernah merasa seolah-olah pikiran di kepala Anda diambil dari Anda (dikeluarkan begitu saja)?",
        "Apakah Anda pernah merasa seolah-olah pikiran di kepala Anda bukan milik Anda sendiri?",
        "Apakah pikiran Anda pernah muncul dengan sangat jelas hingga Anda khawatir orang lain dapat mendengarnya?",
        "Apakah Anda pernah mendengar pikiran Anda sendiri seolah-olah bergema kembali di kepala Anda?",
        "Apakah Anda pernah merasa seolah-olah berada di bawah kendali suatu kekuatan atau daya selain diri Anda sendiri?",
        "Apakah Anda pernah merasa seolah-olah seorang penyamar telah mengambil tempat anggota keluarga, teman, atau kenalan Anda?",
        "Apakah Anda pernah mendengar suara-suara padahal Anda sedang sendirian?",
        "Apakah Anda pernah mendengar suara-suara yang seolah-olah sedang berbicara satu sama lain, padahal Anda sedang sendirian?",
        "Apakah Anda pernah melihat benda-benda, orang-orang, atau binatang-binatang yang tidak dapat dilihat oleh orang lain?",
    ).map { Question(it) },
)

val CDRISC10 = Questionnaire(
    code = "CD-RISC-10", title = "CD-RISC-10", fullName = "Connor-Davidson Resilience Scale-10",
    section = 6, period = "1 bulan terakhir", minutes = 3,
    description = "Mengukur resiliensi — kemampuanmu bangkit menghadapi tekanan.",
    instruction = "Pilih jawaban yang paling sesuai dengan kondisi Anda dalam satu bulan terakhir.",
    type = QuestionType.SCALE,
    options = listOf("Sama sekali tidak benar", "Jarang benar", "Kadang-kadang benar", "Sering benar", "Hampir selalu benar"),
    questions = listOf(
        "Saya mampu beradaptasi ketika terjadi perubahan",
        "Saya dapat menghadapi apa pun yang datang kepada saya",
        "Saya berusaha melihat sisi humor/lucu dari suatu masalah",
        "Menghadapi tekanan/stres dapat membuat saya menjadi lebih kuat",
        "Saya cenderung bangkit kembali setelah mengalami sakit, cedera, atau kesulitan hidup lainnya",
        "Saya percaya bahwa saya dapat mencapai tujuan saya meskipun ada hambatan",
        "Di bawah tekanan, saya tetap dapat fokus dan berpikir jernih",
        "Saya tidak mudah patah semangat oleh kegagalan",
        "Saya menganggap diri saya sebagai orang yang kuat",
        "Saya mampu menghadapi perasaan-perasaan yang tidak menyenangkan",
    ).map { Question(it) },
)

val QUESTIONNAIRES = listOf(SRQ20, PHQ9, GAD7, ASQ, CAPE15, CDRISC10)

/** Hitung hasil dari jawaban mentah (indeks kuesioner sesuai [QUESTIONNAIRES]). */
fun scoreScreening(answers: List<List<Int?>>): ScreeningResult {
    val (srq, phq, gad, asq, cape, cd) = answers
    return ScreeningResult(
        srq = srq.count { it == 1 },
        phq = phq.take(9).sumOf { it ?: 0 },
        gad = gad.take(7).sumOf { it ?: 0 },
        phqItem9 = phq[8] ?: 0,
        phqDifficulty = phq[9],
        gadDifficulty = gad[7],
        asq = asq.take(4).count { it == 1 },
        asqAttempt = if (asq[3] == 1) asq[4] else null,
        asqAcute = if (asq.take(4).any { it == 1 }) asq[5] == 1 else false,
        cape = cape.sumOf { it ?: 0 },
        capeFrequent = cape.count { (it ?: 0) >= 3 },
        cdrisc = cd.sumOf { it ?: 0 },
    )
}

private operator fun <T> List<T>.component6() = this[5]

// ---------------------------------------------------------------------------
// Interpretasi
// ---------------------------------------------------------------------------

enum class Severity(val label: String, val level: Int) {
    NORMAL("Normal", 0), MINIMAL("Minimal", 0), RINGAN("Ringan", 1), SEDANG("Sedang", 2),
    SEDANG_BERAT("Sedang-berat", 3), BERAT("Berat", 3), INDIKASI("Ada indikasi", 2),
    NEGATIF("Negatif", 0), POSITIF("Positif", 3), AKUT("Positif akut", 3),
    RENDAH_CAPE("Rendah", 0), PERLU_TINJAU("Perlu ditinjau", 2),
    RESILIEN_TINGGI("Tinggi", 0), RESILIEN_SEDANG("Sedang", 0), RESILIEN_RENDAH("Rendah", 1),
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

/** ASQ: positif bila ada "Ya" pada butir 1–4; akut bila butir 6 juga "Ya". */
fun asqSeverity(r: ScreeningResult) = when {
    r.asqAcute == true -> Severity.AKUT
    (r.asq ?: 0) > 0 -> Severity.POSITIF
    else -> Severity.NEGATIF
}

/**
 * CAPE-15 (dimensi positif). Belum ada titik potong baku untuk versi 5 pilihan ini;
 * aplikasi menandai "perlu ditinjau" bila ≥2 pengalaman dialami "Sering"/"Sangat sering"
 * atau total ≥30 (rata-rata "Kadang-kadang"). Ambang ini bersifat sementara.
 */
fun capeSeverity(total: Int, frequent: Int) =
    if (frequent >= 2 || total >= 30) Severity.PERLU_TINJAU else Severity.RENDAH_CAPE

/** CD-RISC-10 (0–40): makin tinggi makin resilien. Rujukan umum: rerata populasi ±32. */
fun cdriscSeverity(score: Int) = when {
    score >= 33 -> Severity.RESILIEN_TINGGI
    score >= 26 -> Severity.RESILIEN_SEDANG
    else -> Severity.RESILIEN_RENDAH
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

fun asqDescription(r: ScreeningResult) = when (asqSeverity(r)) {
    Severity.AKUT -> "Kamu sedang memiliki pikiran untuk mengakhiri hidup. Keselamatanmu yang utama — hubungi SEJIWA 119 ext. 8, 112, atau orang terdekat sekarang juga."
    Severity.POSITIF -> "Ada tanda risiko yang perlu ditindaklanjuti. Mohon segera bicara dengan konselor atau tenaga profesional — kamu tidak perlu menghadapinya sendiri."
    else -> "Tidak ditemukan tanda risiko bunuh diri pada skrining ini."
}

fun capeDescription(total: Int, frequent: Int) = if (capeSeverity(total, frequent) == Severity.PERLU_TINJAU)
    "Beberapa pengalaman psikologis cukup sering kamu alami. Ini bukan diagnosis — namun sebaiknya ditinjau lebih lanjut bersama tenaga profesional."
else "Pengalaman psikologis tidak biasa jarang kamu alami."

fun cdriscDescription(score: Int) = when (cdriscSeverity(score)) {
    Severity.RESILIEN_TINGGI -> "Resiliensimu tinggi — kamu cenderung mampu bangkit dari tekanan. Pertahankan!"
    Severity.RESILIEN_SEDANG -> "Resiliensimu cukup baik. Dukungan sosial dan kebiasaan sehat bisa memperkuatnya."
    else -> "Resiliensimu sedang menurun. Latihan kecil seperti jurnal, napas, dan bercerita bisa membantumu lebih kuat."
}

/** Tingkat kebutuhan bantuan: 0 = promotif, 1 = preventif, 2 = kuratif (konseling), 3 = darurat. */
fun ScreeningResult.careLevel(): Int {
    val cape = cape?.let { capeSeverity(it, capeFrequent ?: 0) }
    return when {
        phqItem9 > 0 || asqAcute == true || (asq ?: 0) > 0 -> 3
        phqSeverity(phq).level >= 2 || gadSeverity(gad).level >= 2 || srq >= 6 || cape == Severity.PERLU_TINJAU -> 2
        phqSeverity(phq).level == 1 || gadSeverity(gad).level == 1 ||
            (cdrisc != null && cdriscSeverity(cdrisc) == Severity.RESILIEN_RENDAH) -> 1
        else -> 0
    }
}
