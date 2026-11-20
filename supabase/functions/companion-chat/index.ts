// SEIV — AI Companion (Ruang Cerita)
// Edge Function Supabase yang meneruskan percakapan ke Claude.
// API key Anthropic disimpan sebagai secret Supabase (ANTHROPIC_API_KEY),
// tidak pernah dikirim ke aplikasi.
import Anthropic from "npm:@anthropic-ai/sdk@1.12.0";
import { createClient } from "npm:@supabase/supabase-js@2.119.2";

const MODEL = "claude-opus-5-5";
const MAX_HISTORY = 24; // pesan terakhir yang dikirim ke model
const MAX_CHARS = 2000; // panjang maksimum per pesan

const SYSTEM_PROMPT = `Kamu adalah SEIV AI, teman bercerita di aplikasi kesehatan mental SEIV untuk mahasiswa Indonesia.

Peranmu:
- Menjadi ruang aman: dengarkan, validasi perasaan, dan tanggapi dengan hangat, empatik, dan tidak menghakimi.
- Gunakan bahasa Indonesia santai yang sopan (aku/kamu), seperti teman yang peduli. Boleh memakai emoji secukupnya (maksimal satu per pesan).
- Jawab singkat: 2–4 kalimat. Biasanya akhiri dengan satu pertanyaan terbuka yang membantu pengguna bercerita lebih jauh. Jangan memberi daftar panjang kecuali diminta.
- Kamu boleh menawarkan teknik sederhana yang aman (napas 4-7-8, grounding 5-4-3-2-1, menulis jurnal, memecah tugas jadi langkah kecil, istirahat) bila relevan.
- Kamu BUKAN psikolog atau dokter. Jangan mendiagnosis, jangan menyebut nama gangguan sebagai label untuk pengguna, dan jangan memberi saran obat.
- Bila keluhan terdengar berat, berlangsung lama, atau mengganggu kuliah/aktivitas, sarankan dengan lembut fitur Skrining dan Konseling di aplikasi SEIV (nurse counselor & psikolog).

Keselamatan (paling penting):
- Bila pengguna menyiratkan pikiran bunuh diri, menyakiti diri, atau berada dalam bahaya, utamakan keselamatannya: tanggapi dengan serius dan penuh kepedulian, tanyakan apakah ia aman saat ini, dan anjurkan segera menghubungi layanan SEJIWA 119 ekstensi 8 (24 jam), 112 untuk keadaan darurat, orang terdekat yang dipercaya, atau Konseling SEIV. Jangan menghakimi dan jangan meninggalkan percakapan.
- Jangan memberikan informasi yang dapat digunakan untuk menyakiti diri sendiri atau orang lain.

Tetap pada topik kesejahteraan emosional dan kehidupan mahasiswa. Bila diminta hal di luar itu (misalnya mengerjakan tugas kuliah), tolak dengan ramah dan arahkan kembali ke perasaan pengguna.`;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

type IncomingMessage = { role: "user" | "assistant"; content: string };

const anthropic = new Anthropic(); // membaca ANTHROPIC_API_KEY dari environment

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  // Pastikan pemanggil adalah pengguna SEIV yang login
  const authHeader = req.headers.get("Authorization") ?? "";
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userError } = await supabase.auth.getUser();
  if (userError || !userData.user) return json({ error: "Unauthorized" }, 401);

  let payload: { messages?: IncomingMessage[]; name?: string };
  try {
    payload = await req.json();
  } catch {
    return json({ error: "Body harus JSON" }, 400);
  }

  const messages: Anthropic.MessageParam[] = (payload.messages ?? [])
    .filter((m) => (m.role === "user" || m.role === "assistant") && typeof m.content === "string" && m.content.trim())
    .slice(-MAX_HISTORY)
    .map((m) => ({ role: m.role, content: m.content.slice(0, MAX_CHARS) }));

  // Percakapan harus diawali pesan pengguna (salam pembuka dari aplikasi dibuang)
  while (messages.length && messages[0].role !== "user") messages.shift();
  if (!messages.length || messages[messages.length - 1].role !== "user") {
    return json({ error: "Pesan terakhir harus dari pengguna" }, 400);
  }

  const name = (payload.name ?? "").slice(0, 40).replace(/[^\p{L}\p{N} .'-]/gu, "");
  const system = name ? `${SYSTEM_PROMPT}\n\nNama panggilan pengguna: ${name}.` : SYSTEM_PROMPT;

  try {
    const response = await anthropic.beta.messages.create({
      model: MODEL,
      max_tokens: 16000,
      system,
      messages,
      output_config: { effort: "low" }, // percakapan ringan: respons cepat
      // Bila model utama menolak karena kebijakan keamanan, API otomatis mencoba model cadangan
      betas: ["server-side-fallback-2026-07-01"],
      fallbacks: "default",
    });

    if (response.stop_reason === "refusal") {
      return json({
        reply:
          "Maaf, aku belum bisa menanggapi itu. Tapi aku tetap di sini untukmu 🌿 Kalau kamu sedang dalam bahaya, segera hubungi SEJIWA 119 ext. 8 atau 112.",
        refused: true,
      });
    }

    const reply = response.content
      .filter((b): b is Anthropic.Beta.BetaTextBlock => b.type === "text")
      .map((b) => b.text)
      .join("\n")
      .trim();

    return json({ reply: reply || "Aku mendengarkan. Ceritakan lebih banyak, ya?" });
  } catch (error) {
    if (error instanceof Anthropic.RateLimitError) {
      return json({ error: "Terlalu banyak permintaan, coba sebentar lagi." }, 429);
    } else if (error instanceof Anthropic.AuthenticationError) {
      console.error("ANTHROPIC_API_KEY tidak valid");
      return json({ error: "Layanan AI belum dikonfigurasi." }, 503);
    } else if (error instanceof Anthropic.APIError) {
      console.error(`Anthropic API error ${error.status}:`, error.message);
      return json({ error: "Layanan AI sedang bermasalah." }, 502);
    }
    console.error(error);
    return json({ error: "Terjadi kesalahan." }, 500);
  }
});
