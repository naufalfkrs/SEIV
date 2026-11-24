// SEIV — AI Companion (Ruang Cerita)
// Edge Function Supabase yang meneruskan percakapan ke Google Gemini.
// API key Gemini disimpan sebagai secret Supabase (GEMINI_API_KEY),
// tidak pernah dikirim ke aplikasi.
import { createClient } from "npm:@supabase/supabase-js@2.119.2";

// Bisa diganti tanpa ubah kode: npx supabase secrets set GEMINI_MODEL=<nama-model>
// gemini-3.1-flash-lite: stabil & cepat (umumnya 2–4 detik). Alias gemini-flash-latest sempat macet >25 detik.
const MODEL = Deno.env.get("GEMINI_MODEL") || "gemini-3.1-flash-lite";
// Model cadangan bila model utama lambat/penuh (bisa diganti: GEMINI_BACKUP_MODEL)
const BACKUP_MODEL = Deno.env.get("GEMINI_BACKUP_MODEL") || "gemini-3.1-flash-lite-preview";
const HEDGE_MS = 5_000; // jeda sebelum percobaan berikutnya bila belum ada balasan
const MAX_HISTORY = 24; // pesan terakhir yang dikirim ke model
const MAX_CHARS = 2000; // panjang maksimum per pesan

const SYSTEM_PROMPT = `Kamu adalah SEIV AI, teman bercerita di aplikasi kesehatan mental SEIV untuk mahasiswa Indonesia.

Peranmu:
- Menjadi ruang aman: dengarkan, validasi perasaan, dan tanggapi dengan hangat, empatik, dan tidak menghakimi.
- Gunakan bahasa Indonesia santai yang sopan (aku/kamu), seperti teman yang peduli. Boleh memakai emoji secukupnya (maksimal satu per pesan).
- Jawab singkat: 2–4 kalimat. Biasanya akhiri dengan satu pertanyaan terbuka yang membantu pengguna bercerita lebih jauh. Jangan memberi daftar panjang kecuali diminta.
- Tulis sebagai teks biasa seperti pesan chat: jangan memakai format markdown (tanpa tanda *, **, _, #, atau daftar berpoin), karena aplikasi menampilkan teks apa adanya.
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
type GeminiContent = { role: "user" | "model"; parts: { text: string }[] };
type GeminiResponse = {
  candidates?: { content?: { parts?: { text?: string; thought?: boolean }[] }; finishReason?: string }[];
  promptFeedback?: { blockReason?: string };
  error?: { code?: number; message?: string; status?: string };
};

const SAFE_FALLBACK =
  "Maaf, aku belum bisa menanggapi itu. Tapi aku tetap di sini untukmu 🌿 Kalau kamu sedang dalam bahaya, segera hubungi SEJIWA 119 ext. 8 atau 112.";
const BLOCKED = new Set(["SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII"]);

function withTimeout<T>(p: Promise<T>, ms: number, label: string): Promise<T> {
  return Promise.race([
    p,
    new Promise<T>((_, reject) => setTimeout(() => reject(new Error(`${label} timeout ${ms}ms`)), ms)),
  ]);
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const apiKey = Deno.env.get("GEMINI_API_KEY");
  if (!apiKey) {
    console.error("GEMINI_API_KEY belum di-set");
    return json({ error: "Layanan AI belum dikonfigurasi." }, 503);
  }

  // Pastikan pemanggil adalah pengguna SEIV yang login
  // (verifikasi di kode agar kompatibel dengan JWT signing key baru maupun lama)
  const token = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  if (!token) return json({ error: "Unauthorized" }, 401);
  const serverKey = Deno.env.get("SUPABASE_ANON_KEY") || Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, serverKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const t0 = Date.now();
  let auth;
  try {
    auth = await withTimeout(supabase.auth.getUser(token), 10_000, "auth");
  } catch (e) {
    console.error("auth error:", e);
    return json({ error: "Layanan sedang bermasalah." }, 502);
  }
  const { data: userData, error: userError } = auth;
  if (userError || !userData.user) {
    console.warn("auth gagal:", userError?.message);
    return json({ error: "Unauthorized" }, 401);
  }

  let payload: { messages?: IncomingMessage[]; name?: string };
  try {
    payload = await req.json();
  } catch {
    return json({ error: "Body harus JSON" }, 400);
  }

  const contents: GeminiContent[] = (payload.messages ?? [])
    .filter((m) => (m.role === "user" || m.role === "assistant") && typeof m.content === "string" && m.content.trim())
    .slice(-MAX_HISTORY)
    .map((m) => ({ role: m.role === "user" ? "user" : "model", parts: [{ text: m.content.slice(0, MAX_CHARS) }] }));

  // Percakapan harus diawali pesan pengguna (salam pembuka dari aplikasi dibuang)
  while (contents.length && contents[0].role !== "user") contents.shift();
  if (!contents.length || contents[contents.length - 1].role !== "user") {
    return json({ error: "Pesan terakhir harus dari pengguna" }, 400);
  }

  const name = (payload.name ?? "").slice(0, 40).replace(/[^\p{L}\p{N} .'-]/gu, "");
  const system = name ? `${SYSTEM_PROMPT}\n\nNama panggilan pengguna: ${name}.` : SYSTEM_PROMPT;

  // Satu permintaan ke Gemini; melempar error (berisi status) bila gagal agar bisa dicoba model lain.
  const body = JSON.stringify({
    systemInstruction: { parts: [{ text: system }] },
    contents,
    // Chat ringan: tanpa "thinking" (minimal) agar balasan secepat mungkin
    generationConfig: { maxOutputTokens: 1024, thinkingConfig: { thinkingLevel: "minimal" } },
  });
  const cancel = new AbortController();
  type Attempt = { model: string; status: number; data: GeminiResponse };
  const call = async (model: string): Promise<Attempt> => {
    const s = Date.now();
    const res = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": apiKey },
        signal: AbortSignal.any([cancel.signal, AbortSignal.timeout(35_000)]),
        body,
      },
    );
    const data: GeminiResponse = await res.json().catch(() => ({}));
    console.log(`gemini ${model} status ${res.status} ${Date.now() - s}ms finish=${data.candidates?.[0]?.finishReason}`);
    const attempt = { model, status: res.status, data };
    if (!res.ok) throw Object.assign(new Error(data.error?.message ?? `HTTP ${res.status}`), { attempt });
    return attempt;
  };

  // Server Gemini kadang lambat/penuh (503 "high demand"). Kirim percobaan bertahap:
  // model utama → model cadangan → ulang model utama. Percobaan berikutnya dimulai begitu
  // yang sebelumnya gagal atau setelah HEDGE_MS; balasan yang selesai duluan dipakai.
  const plan = [MODEL, BACKUP_MODEL, MODEL];
  /** Jalankan percobaan ke-i; hasilnya = yang tercepat berhasil di antara percobaan i dan seterusnya. */
  const launch = (i: number): Promise<Attempt> => {
    const p = call(plan[i]);
    if (i + 1 >= plan.length) return p;
    const rest = new Promise<Attempt>((resolve, reject) => {
      let started = false;
      const start = () => {
        if (started) return;
        started = true;
        launch(i + 1).then(resolve, reject);
      };
      const timer = setTimeout(start, HEDGE_MS);
      p.then(
        () => { if (!started) { clearTimeout(timer); reject(new Error("tidak diperlukan")); } },
        () => { clearTimeout(timer); start(); },
      );
    });
    return Promise.any([p, rest]);
  };

  let result: Attempt;
  try {
    result = await launch(0);
  } catch (error) {
    // AggregateError bisa bertingkat (satu per percobaan berantai): ratakan
    const flat = (e: unknown): unknown[] => (e instanceof AggregateError ? e.errors.flatMap(flat) : [e]);
    const attempts = flat(error)
      .map((e) => (e as { attempt?: Attempt }).attempt).filter((a): a is Attempt => !!a);
    console.error("Semua percobaan Gemini gagal:", String(error), attempts.map((a) => `${a.model}:${a.status}`).join(","));
    if (attempts.some((a) => a.status === 401 || a.status === 403 || /api key/i.test(a.data.error?.message ?? ""))) {
      return json({ error: "Layanan AI belum dikonfigurasi." }, 503);
    }
    if (attempts.length && attempts.every((a) => a.status === 429)) {
      return json({ error: "Terlalu banyak permintaan, coba sebentar lagi." }, 429);
    }
    return json({ error: "Layanan AI sedang sibuk. Coba lagi sebentar." }, 502);
  } finally {
    cancel.abort(); // hentikan permintaan yang kalah cepat
  }
  console.log(`balasan dari ${result.model} total ${Date.now() - t0}ms`);
  const data = result.data;

  // Diblokir oleh filter keamanan Gemini → balasan aman + arahkan ke bantuan
  const candidate = data.candidates?.[0];
  if (data.promptFeedback?.blockReason || !candidate || BLOCKED.has(candidate.finishReason ?? "")) {
    return json({ reply: SAFE_FALLBACK, refused: true });
  }

  const reply = (candidate.content?.parts ?? [])
    .filter((p) => !p.thought && typeof p.text === "string")
    .map((p) => p.text)
    .join("")
    .trim();

  return json({ reply: reply || "Aku mendengarkan. Ceritakan lebih banyak, ya?" });
});
