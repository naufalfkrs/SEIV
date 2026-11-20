import { useState, type FormEvent } from "react";
import { supabase } from "../supabase";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const { error } = await supabase.auth.signInWithPassword({ email, password });
    if (error) setError(error.message === "Invalid login credentials" ? "Email atau kata sandi salah." : error.message);
    setBusy(false);
  }

  return (
    <div className="center-screen">
      <form className="card narrow" onSubmit={submit}>
        <div className="logo big">SEIV</div>
        <h2>Panel Admin &amp; Konselor</h2>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Kata sandi
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        {error && <p className="error">{error}</p>}
        <button className="btn" disabled={busy}>{busy ? "Memproses…" : "Masuk"}</button>
      </form>
    </div>
  );
}
