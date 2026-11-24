import { useEffect, useState } from "react";
import type { Session } from "@supabase/supabase-js";
import { configured, supabase, type Profile } from "./supabase";
import Login, { ResetPassword } from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Sessions from "./pages/Sessions";
import Counselors from "./pages/Counselors";
import Users from "./pages/Users";

type Page = "dashboard" | "sessions" | "counselors" | "users";

export default function App() {
  const [session, setSession] = useState<Session | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState<Page>("sessions");
  // true saat dibuka dari tautan "atur ulang kata sandi" di email
  const [recovery, setRecovery] = useState(() => window.location.hash.includes("type=recovery"));

  useEffect(() => {
    supabase.auth.getSession().then(({ data }) => {
      setSession(data.session);
      if (!data.session) setLoading(false);
    });
    const { data: sub } = supabase.auth.onAuthStateChange((event, s) => {
      if (event === "PASSWORD_RECOVERY") setRecovery(true);
      setSession(s);
    });
    return () => sub.subscription.unsubscribe();
  }, []);

  useEffect(() => {
    if (!session) {
      setProfile(null);
      return;
    }
    setLoading(true);
    supabase
      .from("profiles")
      .select("*")
      .eq("id", session.user.id)
      .single()
      .then(({ data }) => {
        const p = data as Profile | null;
        setProfile(p);
        if (p?.role === "admin") setPage("dashboard");
        setLoading(false);
      });
  }, [session]);

  if (!configured) {
    return (
      <div className="center-screen">
        <div className="card narrow">
          <h2>Admin panel belum dikonfigurasi</h2>
          <p className="muted">
            Salin <code>admin/.env.example</code> menjadi <code>admin/.env</code>, isi URL &amp; anon key Supabase,
            lalu jalankan ulang <code>npm run dev</code>.
          </p>
        </div>
      </div>
    );
  }
  if (recovery && session) {
    return <ResetPassword onDone={() => { setRecovery(false); window.history.replaceState(null, "", "/"); }} />;
  }
  if (loading) return <div className="center-screen muted">Memuat…</div>;
  if (!session) return <Login />;
  if (!profile || profile.role === "student") {
    return (
      <div className="center-screen">
        <div className="card narrow">
          <h2>Akses ditolak</h2>
          <p className="muted">
            Akun <b>{session.user.email}</b> bukan admin atau konselor. Minta admin SEIV untuk mengatur peranmu.
          </p>
          <button className="btn secondary" onClick={() => supabase.auth.signOut()}>Keluar</button>
        </div>
      </div>
    );
  }

  const isAdmin = profile.role === "admin";
  const nav: { id: Page; label: string; admin?: boolean }[] = [
    { id: "dashboard", label: "Dasbor", admin: true },
    { id: "sessions", label: "Sesi Konseling" },
    { id: "counselors", label: "Konselor", admin: true },
    { id: "users", label: "Pengguna", admin: true },
  ];

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="logo">SEIV</div>
        <div className="role-chip">{isAdmin ? "Admin" : "Konselor"}</div>
        <nav>
          {nav
            .filter((n) => !n.admin || isAdmin)
            .map((n) => (
              <button key={n.id} className={page === n.id ? "nav active" : "nav"} onClick={() => setPage(n.id)}>
                {n.label}
              </button>
            ))}
        </nav>
        <div className="sidebar-foot">
          <div className="muted small">{profile.name || profile.email}</div>
          <button className="btn ghost small" onClick={() => supabase.auth.signOut()}>Keluar</button>
        </div>
      </aside>
      <main className="content">
        {page === "dashboard" && isAdmin && <Dashboard />}
        {page === "sessions" && <Sessions isAdmin={isAdmin} />}
        {page === "counselors" && isAdmin && <Counselors />}
        {page === "users" && isAdmin && <Users />}
      </main>
    </div>
  );
}
