import { useState, type FormEvent } from "react";
import { supabase } from "../supabase";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [forgot, setForgot] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setInfo(null);
    if (forgot) {
      // Kirim tautan atur ulang kata sandi; tautan kembali ke panel ini (lihat ResetPassword)
      const { error } = await supabase.auth.resetPasswordForEmail(email, { redirectTo: window.location.origin });
      if (error) setError(/rate limit/i.test(error.message) ? "Terlalu sering. Coba lagi beberapa menit lagi." : error.message);
      else setInfo("Jika email terdaftar, tautan untuk mengatur ulang kata sandi sudah dikirim. Cek kotak masuk (atau folder spam).");
    } else {
      const { error } = await supabase.auth.signInWithPassword({ email, password });
      if (error) setError(error.message === "Invalid login credentials" ? "Email atau kata sandi salah." : error.message);
    }
    setBusy(false);
  }

  return (
    <div className="center-screen">
      <form className="card narrow" onSubmit={submit}>
        <div className="logo big">SEIV</div>
        <h2>{forgot ? "Lupa kata sandi" : "Panel Admin & Konselor"}</h2>
        {forgot && <p className="muted small">Masukkan email akunmu. Kami akan mengirim tautan untuk membuat kata sandi baru.</p>}
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        {!forgot && (
          <label>
            Kata sandi
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
        )}
        {error && <p className="error">{error}</p>}
        {info && <p className="muted small">{info}</p>}
        <button className="btn" disabled={busy}>
          {busy ? "Memproses…" : forgot ? "Kirim tautan" : "Masuk"}
        </button>
        <button
          type="button" className="btn ghost small"
          onClick={() => { setForgot(!forgot); setError(null); setInfo(null); }}
        >
          {forgot ? "← Kembali ke halaman masuk" : "Lupa kata sandi?"}
        </button>
      </form>
    </div>
  );
}

/** Ditampilkan saat pengguna membuka tautan atur ulang kata sandi dari email. */
export function ResetPassword({ onDone }: { onDone: () => void }) {
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (password.length < 8) return setError("Kata sandi minimal 8 karakter.");
    if (password !== confirm) return setError("Konfirmasi kata sandi tidak sama.");
    setBusy(true);
    const { error } = await supabase.auth.updateUser({ password });
    setBusy(false);
    if (error) setError(error.message);
    else onDone();
  }

  return (
    <div className="center-screen">
      <form className="card narrow" onSubmit={submit}>
        <div className="logo big">SEIV</div>
        <h2>Buat kata sandi baru</h2>
        <label>
          Kata sandi baru
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="new-password" />
        </label>
        <label>
          Ulangi kata sandi baru
          <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required autoComplete="new-password" />
        </label>
        {error && <p className="error">{error}</p>}
        <button className="btn" disabled={busy}>{busy ? "Menyimpan…" : "Simpan kata sandi"}</button>
      </form>
    </div>
  );
}
