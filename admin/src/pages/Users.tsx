import { useEffect, useState } from "react";
import { supabase, type Counselor, type Profile, type Role } from "../supabase";

const ROLE_LABEL: Record<Role, string> = { student: "Mahasiswa", counselor: "Konselor", admin: "Admin" };

export default function Users() {
  const [users, setUsers] = useState<Profile[]>([]);
  const [counselors, setCounselors] = useState<Counselor[]>([]);
  const [query, setQuery] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function load() {
    const [u, c] = await Promise.all([
      supabase.from("profiles").select("*").order("created_at", { ascending: false }),
      supabase.from("counselors").select("*").order("name"),
    ]);
    if (u.error || c.error) setError((u.error ?? c.error)!.message);
    else {
      setUsers((u.data ?? []) as Profile[]);
      setCounselors((c.data ?? []) as Counselor[]);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function changeRole(u: Profile, role: Role, counselorId?: string) {
    if (role === "counselor" && !counselorId) {
      alert("Pilih profil konselor yang ditautkan ke akun ini.");
      return;
    }
    const { error } = await supabase.rpc("admin_set_role", {
      p_user: u.id,
      p_role: role,
      p_counselor: role === "counselor" ? counselorId : null,
    });
    if (error) alert(error.message);
    else load();
  }

  const shown = users.filter((u) => `${u.name} ${u.email}`.toLowerCase().includes(query.toLowerCase()));

  return (
    <>
      <h1>Pengguna</h1>
      <p className="muted small">
        Untuk membuat akun konselor: minta konselor mendaftar lewat aplikasi SEIV, lalu ubah perannya di sini menjadi
        Konselor dan tautkan ke profil konselornya. Konselor kemudian bisa masuk ke panel ini.
      </p>
      <input className="search" placeholder="Cari nama atau email…" value={query} onChange={(e) => setQuery(e.target.value)} />
      {error && <p className="error">{error}</p>}
      <div className="card table-wrap">
        <table>
          <thead>
            <tr><th>Nama</th><th>Email</th><th>Terdaftar</th><th>Peran</th><th>Profil konselor</th></tr>
          </thead>
          <tbody>
            {shown.map((u) => {
              const linked = counselors.find((c) => c.user_id === u.id);
              return (
                <tr key={u.id}>
                  <td>{u.name || "—"}</td>
                  <td>{u.email}</td>
                  <td>{new Date(u.created_at).toLocaleDateString("id-ID")}</td>
                  <td>
                    <select value={u.role} onChange={(e) => changeRole(u, e.target.value as Role, linked?.id)}>
                      {(Object.keys(ROLE_LABEL) as Role[]).map((r) => <option key={r} value={r}>{ROLE_LABEL[r]}</option>)}
                    </select>
                  </td>
                  <td>
                    <select value={linked?.id ?? ""} onChange={(e) => e.target.value && changeRole(u, "counselor", e.target.value)}>
                      <option value="">—</option>
                      {counselors.map((c) => (
                        <option key={c.id} value={c.id} disabled={!!c.user_id && c.user_id !== u.id}>{c.name}</option>
                      ))}
                    </select>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </>
  );
}
