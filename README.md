# SEIV — Ruang Aman Kesehatan Mental Mahasiswa

SEIV adalah aplikasi kesehatan mental untuk mahasiswa (SDGs 3). Fiturnya: daily check-in, insight awal, skrining
kesehatan jiwa 6 instrumen (SRQ-20, PHQ-9, GAD-7, ASQ, CAPE-15, CD-RISC-10), rekomendasi tindakan, AI Companion (Ruang Cerita), jurnal refleksi, latihan napas,
konseling dengan nurse counselor/psikolog, serta rujukan bantuan profesional.

| Folder | Isi |
|---|---|
| `app/` | Aplikasi Android (Kotlin + Jetpack Compose) |
| `supabase/` | Backend: skema database + RLS (`migrations/`) dan Edge Function AI Companion (`functions/companion-chat`) |
| `admin/` | Panel web admin & konselor (React + Vite) |

Tanpa konfigurasi backend, aplikasi tetap berjalan penuh dalam **mode offline**: data hanya tersimpan di HP dan
AI Companion memakai balasan berbasis aturan.

---

## 1. Siapkan Supabase

1. Buat project di <https://supabase.com/dashboard> (region **Southeast Asia (Singapore)**).
2. **Buat tabel:** buka **SQL Editor**, salin seluruh isi
   `supabase/migrations/20261121000000_init_schema.sql`, lalu **Run**.
3. **Auth:** di **Authentication → Sign In / Providers → Email**, matikan *Confirm email* agar pendaftaran langsung
   bisa dipakai (saat demo). Jika tetap aktif, pengguna harus klik tautan verifikasi di email sebelum masuk.
4. Catat **Project URL** dan **anon public key** dari **Project Settings → API**.

### Edge Function AI Companion (Google Gemini)

Butuh [Supabase CLI](https://supabase.com/docs/guides/cli) (`npx supabase ...`, perlu Node.js) dan API key Gemini
gratis dari <https://aistudio.google.com/apikey>.

```bash
npx supabase login
npx supabase link --project-ref <project-ref>
npx supabase secrets set GEMINI_API_KEY=<api-key-gemini>
npx supabase functions deploy companion-chat
```

`<project-ref>` adalah bagian acak di Project URL (`https://<project-ref>.supabase.co`).

API key Gemini hanya tersimpan di server Supabase, **tidak pernah** masuk ke APK. Model bawaan adalah
`gemini-3.1-flash-lite` (stabil, balasan ~2 detik). Alias `gemini-flash-latest` sempat macet >25 detik, jadi tidak dipakai. Untuk mengganti model:
`npx supabase secrets set GEMINI_MODEL=<nama-model>` (daftar di <https://ai.google.dev/gemini-api/docs/models>).
Bila filter keamanan Gemini memblokir balasan, aplikasi menampilkan pesan aman beserta nomor hotline.

### Akun admin pertama

Daftar lewat aplikasi (atau Authentication → Add user), lalu jalankan di SQL Editor:

```sql
update public.profiles set role = 'admin' where email = 'email-admin@contoh.com';
```

## 2. Build aplikasi Android

Tambahkan ke `local.properties` (file ini tidak ikut ke git):

```properties
SUPABASE_URL=https://xxxxxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOi...
# Opsional, untuk tombol "Masuk dengan Google":
GOOGLE_WEB_CLIENT_ID=xxxxxxxx.apps.googleusercontent.com
```

```bash
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`.

### Login Google (opsional)

1. Di Google Cloud Console buat **OAuth client ID** tipe *Web application* → salin Client ID & Secret.
2. Buat juga OAuth client tipe *Android* dengan package `com.seiv.app` dan SHA-1 sertifikat penandatangan
   (`./gradlew signingReport`).
3. Di Supabase **Authentication → Providers → Google**: aktifkan, isi Client ID & Secret dari langkah 1.
4. Isi `GOOGLE_WEB_CLIENT_ID` di `local.properties` dengan Client ID *Web* lalu build ulang.

Tanpa langkah ini, tombol Google menampilkan pesan bahwa login Google belum diaktifkan.

## 3. Panel admin & konselor

```bash
cd admin
cp .env.example .env   # isi VITE_SUPABASE_URL dan VITE_SUPABASE_ANON_KEY
npm install
npm run dev            # http://localhost:5173
```

- **Admin:** dasbor statistik, verifikasi pembayaran sesi, kelola konselor, atur peran pengguna.
- **Konselor:** melihat sesi miliknya, mengirim pesan/tautan sesi ke mahasiswa, menandai selesai, dan melihat
  ringkasan check-in & skrining **hanya** dari klien yang memilih membagikannya.
- Membuat akun konselor: konselor mendaftar lewat aplikasi → admin membuka menu **Pengguna**, mengubah perannya menjadi
  *Konselor* dan menautkannya ke profil konselor.

Untuk produksi: `npm run build` lalu deploy folder `admin/dist` ke Vercel/Netlify (isi variabel `VITE_...` di sana).

---

## Alur data & privasi

- Data ditulis ke HP dulu, lalu disinkronkan ke Supabase. Jika offline, data masuk antrean dan dikirim ulang otomatis.
- Setiap tabel memakai **Row Level Security**: mahasiswa hanya bisa melihat datanya sendiri; konselor hanya melihat
  check-in & skrining klien yang mengaktifkan *bagikan ringkasan*; **jurnal tidak pernah bisa dilihat siapa pun**
  selain pemiliknya, termasuk admin. Status sesi hanya bisa diubah lewat fungsi khusus admin/konselor.
- Riwayat chat AI Companion tidak disimpan di database.
- Pembayaran (QRIS/VA) masih **simulasi**: mahasiswa menekan "Saya Sudah Bayar", lalu admin memverifikasi di panel.
  Untuk pembayaran sungguhan, integrasikan payment gateway (mis. Midtrans/Xendit) lewat Edge Function + webhook.

## Pengujian skema

Skema dan kebijakan RLS sudah diuji (32 skenario: isolasi data antar mahasiswa, akses konselor, peran admin,
bentrok jadwal, dll.) memakai PGlite dengan stub skema `auth` Supabase.

## Skrining kesehatan jiwa (6 instrumen)

Urutan: **SRQ-20 → PHQ-9 → GAD-7 → ASQ → CAPE-15 → CD-RISC-10**. Butir PHQ-9 s.d. CD-RISC-10 mengikuti
*Kuesioner Kesehatan Mental Mahasiswa* (FIKES UB). Logika ada di `app/src/main/java/com/seiv/app/data/Screening.kt`.

| Instrumen | Skor | Interpretasi di aplikasi |
|---|---|---|
| SRQ-20 | 0–20 (jumlah "Ya") | ≥6 ada indikasi |
| PHQ-9 | 0–27 | 0–4 minimal · 5–9 ringan · 10–14 sedang · 15–19 sedang-berat · ≥20 berat; butir 9 > 0 = darurat |
| GAD-7 | 0–21 | 0–4 minimal · 5–9 ringan · 10–14 sedang · ≥15 berat |
| ASQ | jumlah "Ya" butir 1–4 | 0 negatif · ≥1 positif (butuh bantuan segera) · butir 6 "Ya" = positif akut |
| CAPE-15 | 0–60 (0–4 per butir) | *sementara*: perlu ditinjau bila ≥2 butir "Sering/Sangat sering" atau total ≥30 |
| CD-RISC-10 | 0–40 | *sementara*: ≥33 tinggi · 26–32 sedang · ≤25 rendah |

Ambang CAPE-15 dan CD-RISC-10 belum memiliki titik potong baku untuk versi ini — sebaiknya dikonfirmasi tim ahli
lalu ubah fungsi `capeSeverity` / `cdriscSeverity`. Butir "tingkat kesulitan" PHQ-9/GAD-7 dan butir lanjutan ASQ
(5–6) hanya muncul bila relevan dan tidak dihitung ke skor total.

## Data diri pengguna

Nama lengkap, alamat domisili, status (mahasiswa/bekerja/lainnya), asal instansi & fakultas (bila mahasiswa),
nomor HP, dan nomor HP wali/kontak darurat. Diminta setelah mendaftar (bisa "Nanti saja") dan dapat diubah di
Profil → Data Diri. Hanya dapat dilihat pemiliknya, admin, dan konselor yang menangani sesinya.
