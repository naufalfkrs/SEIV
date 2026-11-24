// SEIV — kirim push notification lewat Firebase Cloud Messaging (FCM HTTP v1).
// Dipanggil oleh trigger database (pg_net) dengan header rahasia x-push-secret.
// Secret yang dibutuhkan:
//   PUSH_WEBHOOK_SECRET      — sama dengan private.push_config.secret di database
//   FIREBASE_SERVICE_ACCOUNT — isi file JSON service account Firebase (rahasia)
import { createClient } from "npm:@supabase/supabase-js@2.119.2";

type ServiceAccount = { project_id: string; client_email: string; private_key: string };
type Payload = { user_id?: string; title?: string; body?: string; route?: string };

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

const b64url = (data: ArrayBuffer | Uint8Array | string) => {
  const bytes = typeof data === "string" ? new TextEncoder().encode(data) : new Uint8Array(data);
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
};

let cachedToken: { value: string; exp: number } | null = null;

/** Tukar service account menjadi access token OAuth2 (JWT RS256 → oauth2.googleapis.com). */
async function accessToken(sa: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cachedToken && cachedToken.exp - 60 > now) return cachedToken.value;
  const header = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = b64url(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const pem = sa.private_key.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${claims}`));
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${header}.${claims}.${b64url(sig)}`,
    }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(`OAuth gagal ${res.status}: ${JSON.stringify(data).slice(0, 200)}`);
  cachedToken = { value: data.access_token, exp: now + (data.expires_in ?? 3600) };
  return cachedToken.value;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);
  const secret = Deno.env.get("PUSH_WEBHOOK_SECRET");
  if (!secret || req.headers.get("x-push-secret") !== secret) return json({ error: "Unauthorized" }, 401);

  const raw = Deno.env.get("FIREBASE_SERVICE_ACCOUNT");
  if (!raw) {
    console.error("FIREBASE_SERVICE_ACCOUNT belum di-set");
    return json({ error: "Push belum dikonfigurasi" }, 503);
  }
  let sa: ServiceAccount;
  try {
    sa = JSON.parse(raw);
  } catch {
    console.error("FIREBASE_SERVICE_ACCOUNT bukan JSON yang valid");
    return json({ error: "Konfigurasi push tidak valid" }, 503);
  }

  const p: Payload = await req.json().catch(() => ({}));
  if (!p.user_id || !p.title) return json({ error: "user_id dan title wajib" }, 400);

  const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: rows, error } = await db.from("push_tokens").select("token").eq("user_id", p.user_id);
  if (error) {
    console.error("Gagal membaca token:", error.message);
    return json({ error: "Gagal membaca token" }, 500);
  }
  if (!rows?.length) return json({ sent: 0, note: "pengguna belum punya perangkat terdaftar" });

  const token = await accessToken(sa);
  let sent = 0;
  const stale: string[] = [];
  await Promise.all(rows.map(async ({ token: device }) => {
    const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({
        message: {
          token: device,
          // Pesan "data" agar aplikasi yang menampilkan notifikasi (ikon, warna, rute tujuan)
          data: { title: p.title, body: p.body ?? "", route: p.route ?? "" },
          android: { priority: "HIGH" },
        },
      }),
    });
    if (res.ok) { sent++; return; }
    const err = await res.text();
    console.error(`FCM ${res.status}: ${err.slice(0, 200)}`);
    if (res.status === 404 || /UNREGISTERED|INVALID_ARGUMENT/.test(err)) stale.push(device);
  }));
  if (stale.length) await db.from("push_tokens").delete().in("token", stale);
  console.log(`push ke ${p.user_id}: terkirim ${sent}/${rows.length}`);
  return json({ sent, removed: stale.length });
});
