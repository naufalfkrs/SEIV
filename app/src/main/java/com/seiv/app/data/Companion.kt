package com.seiv.app.data

data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCrisis: Boolean = false,
)

/**
 * Mesin respons empatik berbasis aturan (offline).
 * Tidak menggantikan profesional — hanya ruang aman untuk bercerita.
 * Titik integrasi LLM dapat ditambahkan di [CompanionEngine.reply].
 */
object CompanionEngine {
    private val crisisWords = listOf(
        "bunuh diri", "mengakhiri hidup", "ingin mati", "pengen mati", "mau mati", "menyakiti diri",
        "self harm", "selfharm", "tidak ingin hidup", "gak mau hidup", "ga mau hidup", "nyakitin diri",
    )

    private data class Rule(val keys: List<String>, val replies: List<String>)

    private val rules = listOf(
        Rule(listOf("cemas", "khawatir", "takut", "panik", "deg-degan", "gelisah"), listOf(
            "Rasa cemas itu melelahkan, ya. Terima kasih sudah mau cerita 🌿 Boleh aku tahu, apa yang paling kamu khawatirkan sekarang?",
            "Wajar banget merasa cemas. Coba tarik napas pelan 4 hitungan, tahan 7, lalu hembuskan 8 hitungan. Setelah itu, mau cerita lebih lanjut apa yang bikin kamu gelisah?",
        )),
        Rule(listOf("tidur", "insomnia", "begadang", "ngantuk"), listOf(
            "Susah tidur bisa bikin semuanya terasa lebih berat. Biasanya apa yang ada di pikiranmu waktu mau tidur?",
            "Tubuh dan pikiranmu butuh istirahat. Coba jauhkan layar 30 menit sebelum tidur dan tuliskan isi pikiranmu di jurnal dulu. Menurutmu, apa yang paling bikin kamu terjaga?",
        )),
        Rule(listOf("tugas", "kuliah", "skripsi", "ujian", "dosen", "nilai", "deadline", "akademik", "ipk"), listOf(
            "Tekanan akademik memang nyata dan berat. Kamu sudah berusaha sejauh ini, itu patut dihargai 💛 Bagian mana yang paling bikin kamu kewalahan?",
            "Kadang tugas terasa seperti gunung. Bagaimana kalau kita pecah jadi langkah kecil? Satu hal kecil apa yang bisa kamu selesaikan hari ini?",
        )),
        Rule(listOf("sedih", "nangis", "menangis", "kecewa", "hancur", "patah hati"), listOf(
            "Aku ikut merasakan sedihmu. Tidak apa-apa untuk menangis, itu cara hatimu melepas beban. Mau ceritakan apa yang terjadi?",
            "Perasaan sedihmu valid. Kamu nggak harus kuat setiap saat. Aku di sini mendengarkan 🌿",
        )),
        Rule(listOf("sendiri", "kesepian", "sepi", "nggak punya teman", "gak punya teman", "dikucilkan"), listOf(
            "Merasa kesepian itu menyakitkan. Terima kasih sudah mau berbagi denganku. Siapa satu orang yang biasanya membuatmu merasa nyaman?",
            "Kamu tidak sendirian, meskipun rasanya begitu. Aku di sini. Mau cerita kapan rasa sepi itu paling terasa?",
        )),
        Rule(listOf("keluarga", "orang tua", "ortu", "ayah", "ibu", "mama", "papa"), listOf(
            "Hubungan dengan keluarga bisa jadi rumit, ya. Apa yang sedang terjadi di rumah?",
        )),
        Rule(listOf("pacar", "teman", "sahabat", "relasi", "putus"), listOf(
            "Hubungan dengan orang terdekat memang sangat memengaruhi perasaan kita. Bagaimana perasaanmu tentang hubungan itu sekarang?",
        )),
        Rule(listOf("capek", "lelah", "burnout", "cape", "letih"), listOf(
            "Kedengarannya kamu sudah memikul banyak hal. Istirahat bukan berarti menyerah 🌿 Apa yang bisa kamu lepaskan sejenak hari ini?",
        )),
        Rule(listOf("motivasi", "malas", "semangat"), listOf(
            "Kehilangan motivasi itu manusiawi. Coba mulai dari hal yang sangat kecil, misalnya 5 menit saja. Apa satu hal yang dulu membuatmu bersemangat?",
        )),
        Rule(listOf("senang", "bahagia", "lega", "bersyukur", "baik"), listOf(
            "Ikut senang mendengarnya! 😊 Apa yang membuat harimu terasa baik?",
            "Wah, itu kabar yang menyenangkan. Jangan lupa abadikan momen ini di jurnalmu, ya!",
        )),
        Rule(listOf("terima kasih", "makasih", "thanks"), listOf(
            "Sama-sama 💛 Aku senang bisa menemanimu. Kapan pun kamu butuh tempat bercerita, aku di sini.",
        )),
    )

    private val fallback = listOf(
        "Aku mendengarkan. Ceritakan lebih banyak, ya — bagaimana perasaanmu tentang hal itu?",
        "Terima kasih sudah percaya untuk bercerita. Apa yang paling kamu rasakan saat ini?",
        "Hmm, aku paham. Kalau boleh tahu, sejak kapan kamu merasakan hal ini?",
        "Perasaanmu penting. Apa yang menurutmu bisa sedikit membantumu saat ini?",
    )

    fun greeting(name: String) = listOf(
        ChatMessage(false, "Hai $name 👋 Aku SEIV AI — teman digitalmu untuk kesehatan mental. Aku di sini buat menemani dan mendengarkan, kapan pun kamu butuh."),
        ChatMessage(false, "Ada yang ingin kamu ceritakan hari ini? Nggak ada yang terlalu besar atau terlalu kecil — semua boleh diceritain di sini 🌿"),
    )

    val quickStarters = listOf("Aku lagi cemas 😟", "Susah tidur 😴", "Butuh motivasi 💪", "Aku merasa kesepian 🌙")

    fun reply(input: String, turn: Int): ChatMessage {
        val text = input.lowercase()
        if (crisisWords.any { text.contains(it) }) {
            return ChatMessage(
                false,
                "Aku sangat peduli dengan keselamatanmu. Yang kamu rasakan sekarang sangat berat, dan kamu tidak harus menghadapinya sendirian. " +
                    "Tolong segera hubungi layanan SEJIWA 119 ext. 8 atau 112 jika dalam bahaya, atau bicara dengan konselor SEIV. Aku tetap di sini menemanimu.",
                isCrisis = true,
            )
        }
        val rule = rules.firstOrNull { r -> r.keys.any { text.contains(it) } }
        val pool = rule?.replies ?: fallback
        return ChatMessage(false, pool[turn % pool.size])
    }
}
