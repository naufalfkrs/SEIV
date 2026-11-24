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
    /** Nilai skor = indeks pilihan + offset (CAPE-P15 dinilai 1–5, lainnya mulai 0). */
    val scoreOffset: Int = 0,
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
    code = "CAPE-15", title = "CAPE-15", fullName = "Community Assessment of Psychic Experiences-Positive 15",
    section = 5, period = "sepanjang hidup", minutes = 4,
    description = "Mengenali pengalaman psikologis tertentu yang mungkin pernah kamu alami.",
    instruction = "Untuk setiap pernyataan, tunjukkan seberapa sering Anda mengalaminya sepanjang hidup Anda.",
    type = QuestionType.SCALE, options = FREQ_LIFE, scoreOffset = 1,
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
        srqItems = srq.foldIndexed(0) { i, acc, v -> if (v == 1) acc or (1 shl i) else acc },
        phq = phq.take(9).sumOf { it ?: 0 },
        gad = gad.take(7).sumOf { it ?: 0 },
        phqItem9 = phq[8] ?: 0,
        phqDifficulty = phq[9],
        gadDifficulty = gad[7],
        asq = asq.take(4).count { it == 1 },
        asqAttempt = if (asq[3] == 1) asq[4] else null,
        asqAcute = if (asq.take(4).any { it == 1 }) asq[5] == 1 else false,
        // CAPE-P15: butir 1 (tidak pernah) – 5 (sangat sering)
        cape = cape.sumOf { (it ?: 0) + 1 },
        capeFrequent = cape.count { (it ?: 0) >= 3 },
        capePi = cape.subList(0, 5).sumOf { (it ?: 0) + 1 },   // ide paranoid, butir 1–5
        capeBe = cape.subList(5, 12).sumOf { (it ?: 0) + 1 },  // pengalaman aneh, butir 6–12
        capePa = cape.subList(12, 15).sumOf { (it ?: 0) + 1 }, // anomali persepsi, butir 13–15
        cdrisc = cd.sumOf { it ?: 0 },
    )
}

private operator fun <T> List<T>.component6() = this[5]

// ---------------------------------------------------------------------------
// Interpretasi — mengikuti sumber resmi tiap instrumen:
//  SRQ-20        : Pedoman SRQ-20 / Riskesdas — Ya = 1; ≥6 "Ya" = gangguan mental emosional; 5 subskala
//  PHQ-9 & GAD-7 : University of Washington, Mental Health Screening (hiv.uw.edu)
//  ASQ           : NIMH ASQ Suicide Risk Screening Tool (2025)
//  CAPE-P15      : Núñez et al. 2021, PLoS One (PMC8389461) — cut-off rata-rata 1,47
//  CD-RISC-10    : Yan et al. 2023 (J Affect Disord Rep) — 0–4 per butir, total 0–40; rerata acuan 24,85 / 32,36
// ---------------------------------------------------------------------------

enum class Severity(val label: String, val level: Int) {
    NORMAL("Normal", 0), MINIMAL("Minimal", 0), RINGAN("Ringan", 1), SEDANG("Sedang", 2),
    SEDANG_BERAT("Sedang-berat", 3), BERAT("Berat", 3), INDIKASI("Ada indikasi", 2),
    // Label yang tampil ke pengguna: bahasa awam, tanpa istilah teknis
    NEGATIF("Tidak berisiko", 0), POSITIF("Perlu perhatian", 3), AKUT("Butuh bantuan segera", 3),
    CAPE_NEGATIF("Jarang dialami", 0), CAPE_POSITIF("Cukup sering", 2),
    RESILIEN_TINGGI("Tinggi", 0), RESILIEN_SEDANG("Sedang", 0), RESILIEN_RENDAH("Rendah", 1),
}

/** SRQ-20 (Riskesdas): menjawab "Ya" pada ≥6 butir = gangguan mental emosional/distres. */
fun srqSeverity(score: Int) = if (score >= 6) Severity.INDIKASI else Severity.NORMAL

/** Subskala SRQ-20 menurut pedoman (nomor butir 1-based). */
val SRQ_SUBSCALES = listOf(
    "Suasana hati" to listOf(6, 9, 10, 14, 15, 16, 17),
    "Cemas" to listOf(3, 4, 5),
    "Keluhan fisik" to listOf(1, 2, 7, 19),
    "Berpikir & konsentrasi" to listOf(8, 12, 13),
    "Energi" to listOf(8, 11, 12, 13, 18, 20),
)

/** Jumlah "Ya" per subskala: (nama, jumlah Ya, jumlah butir). */
fun srqSubscales(items: Int): List<Triple<String, Int, Int>> = SRQ_SUBSCALES.map { (name, nums) ->
    Triple(name, nums.count { items and (1 shl (it - 1)) != 0 }, nums.size)
}

/** PHQ-9: 0–4 tidak ada–minimal · 5–9 ringan · 10–14 sedang · 15–19 sedang-berat · 20–27 berat. */
fun phqSeverity(score: Int) = when {
    score <= 4 -> Severity.MINIMAL
    score <= 9 -> Severity.RINGAN
    score <= 14 -> Severity.SEDANG
    score <= 19 -> Severity.SEDANG_BERAT
    else -> Severity.BERAT
}

/** GAD-7: 0–4 minimal · 5–9 ringan · 10–14 sedang · 15–21 berat. */
fun gadSeverity(score: Int) = when {
    score <= 4 -> Severity.MINIMAL
    score <= 9 -> Severity.RINGAN
    score <= 14 -> Severity.SEDANG
    else -> Severity.BERAT
}

/** GAD-7 ≥ 8: kemungkinan gangguan kecemasan umum, perlu asesmen diagnostik lanjutan (UW). */
const val GAD_FURTHER_ASSESSMENT = 8

/**
 * ASQ (NIMH): "Tidak" pada butir 1–4 = negatif. "Ya" pada salah satu butir 1–4 lalu
 * "Ya" pada butir akut (pikiran bunuh diri saat ini) = positif akut; "Tidak" = positif non-akut.
 */
fun asqSeverity(r: ScreeningResult) = when {
    r.asqAcute == true -> Severity.AKUT
    (r.asq ?: 0) > 0 -> Severity.POSITIF
    else -> Severity.NEGATIF
}

/** Pengecualian NIMH: positif hanya karena butir 4 dan percobaan terakhir > 1 tahun lalu. */
fun asqOnlyPastAttempt(r: ScreeningResult) = r.asq == 1 && r.asqAttempt == 1 && r.asqAcute != true

/** CAPE-P15: rata-rata butir (1–5). Positif bila rata-rata ≥ 1,47. */
const val CAPE_CUTOFF = 1.47
fun capeMean(total: Int) = total / 15.0
fun capeSeverity(total: Int) = if (capeMean(total) >= CAPE_CUTOFF) Severity.CAPE_POSITIF else Severity.CAPE_NEGATIF

/**
 * CD-RISC-10 (Yan et al. 2023, bag. 2.2): butir 0 "sama sekali tidak benar" – 4 "hampir selalu benar",
 * total 0–40, makin tinggi makin mampu menghadapi kesulitan. Artikel tidak memberi cut-off; kategori
 * memakai rerata acuan artikel: kelompok tanpa PTSD 32,36 (SD 7,20) dan dengan gejala PTSD 24,85 (SD 7,54).
 */
const val CDRISC_REF_HEALTHY = 32.36
const val CDRISC_REF_PTSD = 24.85
fun cdriscSeverity(score: Int) = when {
    score > CDRISC_REF_HEALTHY -> Severity.RESILIEN_TINGGI   // ≥ 33: di atas rerata kelompok tanpa PTSD
    score > CDRISC_REF_PTSD -> Severity.RESILIEN_SEDANG      // 25–32: di antara kedua rerata acuan
    else -> Severity.RESILIEN_RENDAH                         // ≤ 24: setara/di bawah rerata kelompok dgn gejala PTSD
}

fun srqDescription(score: Int, items: Int? = null): String {
    val base = if (score >= 6)
        "Kamu mengalami cukup banyak keluhan dalam sebulan terakhir — tanda adanya gangguan mental emosional atau stres yang sebaiknya dibicarakan dengan tenaga profesional."
    else "Keluhanmu dalam sebulan terakhir masih sedikit. Kondisimu secara umum tampak baik — tetap jaga rutinitas positifmu."
    if (items == null) return base
    val felt = srqSubscales(items).filter { it.second > 0 }
    return if (felt.isEmpty()) base
    else base + "\n\nKeluhan yang kamu rasakan: " + felt.sortedByDescending { it.second.toFloat() / it.third }
        .joinToString(" · ") { "${it.first.lowercase()} (${it.second} dari ${it.third})" } + "."
}

/** Deskripsi + tindak lanjut yang disarankan (UW). */
fun phqDescription(score: Int, item9: Int = 0): String {
    val base = when (phqSeverity(score)) {
        Severity.MINIMAL -> "Gejala depresi tidak ada atau minimal. Tidak perlu tindakan khusus — terus rawat dirimu."
        Severity.RINGAN -> "Gejala depresi ringan. Pantau suasana hatimu lewat check-in harian dan ulangi skrining beberapa minggu lagi."
        Severity.SEDANG -> "Gejala depresi sedang. Disarankan menyusun rencana penanganan, misalnya konseling dan tindak lanjut berkala."
        Severity.SEDANG_BERAT -> "Gejala depresi cukup berat. Sebaiknya kamu mendapat pendampingan aktif dari psikolog atau tenaga kesehatan jiwa."
        else -> "Gejala depresi berat. Segera temui psikolog, psikiater, atau tenaga kesehatan jiwa untuk mendapat penanganan."
    }
    return if (item9 > 0) "$base Kamu juga menyebut pernah berpikir lebih baik mati atau ingin melukai diri — mohon bicarakan ini dengan konselor atau tenaga profesional secepatnya."
    else base
}

fun gadDescription(score: Int): String {
    val base = when (gadSeverity(score)) {
        Severity.MINIMAL -> "Kecemasan minimal. Pertahankan kebiasaan baik yang menenangkanmu."
        Severity.RINGAN -> "Kecemasan ringan. Latihan napas dan menulis jurnal bisa membantu meredakannya."
        Severity.SEDANG -> "Kecemasan sedang. Pertimbangkan untuk berbicara dengan konselor."
        else -> "Kecemasan berat. Sangat disarankan berkonsultasi dengan tenaga profesional."
    }
    return if (score >= GAD_FURTHER_ASSESSMENT && gadSeverity(score) != Severity.BERAT) "$base Rasa cemasmu cukup sering muncul, jadi sebaiknya diperiksa lebih lanjut bersama konselor."
    else base
}

fun asqDescription(r: ScreeningResult) = when (asqSeverity(r)) {
    Severity.AKUT -> "Saat ini kamu sedang memiliki pikiran untuk mengakhiri hidup. Keselamatanmu yang paling penting — hubungi SEJIWA 119 ext. 8, 112, atau orang terdekat sekarang juga."
    Severity.POSITIF -> if (asqOnlyPastAttempt(r))
        "Kamu pernah melewati masa yang sangat berat lebih dari setahun lalu. Tetap baik untuk bercerita ke konselor agar kamu mendapat dukungan yang tepat."
    else "Ada tanda kamu sedang menyimpan pikiran yang berat. Kamu tidak perlu menghadapinya sendiri — mohon segera bicara dengan konselor atau tenaga profesional."
    else -> "Tidak ditemukan tanda risiko keselamatan pada jawabanmu."
}

fun capeDescription(total: Int) = if (capeSeverity(total) == Severity.CAPE_POSITIF)
    "Beberapa pengalaman tidak biasa (misalnya merasa dicurigai, pikiran terasa aneh, atau mendengar/melihat sesuatu) cukup sering kamu alami. Ini bukan diagnosis, tapi sebaiknya dibicarakan dengan tenaga profesional."
else "Pengalaman tidak biasa seperti merasa dicurigai atau mendengar/melihat sesuatu yang tidak dialami orang lain jarang kamu alami."

/** Frekuensi rata-rata (skala 1–5) dalam bahasa awam. */
fun capeFrequencyWord(mean: Double) = when {
    mean < 1.5 -> "tidak pernah"
    mean < 2.5 -> "jarang"
    mean < 3.5 -> "kadang-kadang"
    mean < 4.5 -> "sering"
    else -> "sangat sering"
}

fun cdriscDescription(score: Int) = when (cdriscSeverity(score)) {
    Severity.RESILIEN_TINGGI -> "Kamu cenderung mampu bangkit dan beradaptasi saat menghadapi tekanan. Pertahankan!"
    Severity.RESILIEN_SEDANG -> "Kemampuanmu menghadapi tekanan cukup baik. Dukungan teman dan kebiasaan sehat bisa membuatmu makin kuat."
    else -> "Saat ini kamu mungkin merasa sulit bangkit dari tekanan. Latihan kecil seperti jurnal, napas, bercerita, dan dukungan konselor bisa membantumu lebih kuat."
}

/** Tingkat kebutuhan bantuan: 0 = promotif, 1 = preventif, 2 = kuratif (konseling), 3 = segera/darurat. */
fun ScreeningResult.careLevel(): Int = when {
    // ASQ positif (akut/non-akut) atau PHQ-9 butir 9 > 0: perlu penilaian risiko bunuh diri
    phqItem9 > 0 || asqAcute == true || (asq ?: 0) > 0 -> 3
    phq >= 10 || gad >= GAD_FURTHER_ASSESSMENT || srq >= 6 || (cape != null && capeSeverity(cape) == Severity.CAPE_POSITIF) -> 2
    phq >= 5 || gad >= 5 || (cdrisc != null && cdriscSeverity(cdrisc) == Severity.RESILIEN_RENDAH) -> 1
    else -> 0
}
