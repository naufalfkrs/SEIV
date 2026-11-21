import { useCallback, useEffect, useState } from "react";
import ProfileDetails from "../ProfileDetails";
import {
  fmtDateTime, rupiah, screeningSummary, STATUS_LABEL, supabase,
  type Profile, type ScreeningRow, type SessionRow, type SessionStatus,
} from "../supabase";

const FILTERS: { id: SessionStatus | "all"; label: string }[] = [
  { id: "all", label: "Semua" },
  { id: "waiting_verification", label: "Menunggu verifikasi" },
  { id: "confirmed", label: "Terkonfirmasi" },
  { id: "completed", label: "Selesai" },
  { id: "cancelled", label: "Dibatalkan" },
];

const MOODS = ["😣 Sangat buruk", "😔 Buruk", "😐 Netral", "🙂 Baik", "😄 Sangat baik"];

interface ClientData {
  checkIns: { created_at: string; mood: number; factors: string[]; feelings: string[]; note: string }[];
  screenings: ScreeningRow[];
}

const SCREENING_COLUMNS = "created_at, srq, gad, phq, phq_item9, asq, asq_acute, asq_attempt, cape, cape_frequent, cdrisc";

export default function Sessions({ isAdmin }: { isAdmin: boolean }) {
  const [rows, setRows] = useState<SessionRow[]>([]);
  const [filter, setFilter] = useState<SessionStatus | "all">(isAdmin ? "waiting_verification" : "confirmed");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState<SessionRow | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    let q = supabase
      .from("sessions")
      .select("*, profiles(name, email), counselors(name, title)")
      .order("starts_at", { ascending: true });
    if (filter !== "all") q = q.eq("status", filter);
    const { data, error } = await q;
    if (error) setError(error.message);
    else setRows((data ?? []) as SessionRow[]);
    setLoading(false);
  }, [filter]);

  useEffect(() => {
    load();
  }, [load]);

  async function setStatus(s: SessionRow, status: SessionStatus, note?: string) {
    const { error } = await supabase.rpc("set_session_status", {
      p_id: s.id,
      p_status: status,
      p_counselor_note: note ?? null,
    });
    if (error) alert(error.message);
    else {
      setOpen(null);
      load();
    }
  }

  return (
    <>
      <h1>Sesi Konseling</h1>
      <div className="tabs">
        {FILTERS.map((f) => (
          <button key={f.id} className={filter === f.id ? "tab active" : "tab"} onClick={() => setFilter(f.id)}>
            {f.label}
          </button>
        ))}
        <button className="btn ghost small" onClick={load}>↻ Muat ulang</button>
      </div>
      {error && <p className="error">{error}</p>}
      {loading ? (
        <p className="muted">Memuat…</p>
      ) : rows.length === 0 ? (
        <div className="card muted">Tidak ada sesi.</div>
      ) : (
        <div className="card table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Jadwal</th>
                <th>Mahasiswa</th>
                <th>Konselor</th>
                <th>Metode</th>
                <th>Pembayaran</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((s) => (
                <tr key={s.id}>
                  <td className="mono">#{s.id}</td>
                  <td>{fmtDateTime(s.starts_at)}</td>
                  <td>
                    {s.profiles?.name || "—"}
                    <div className="muted small">{s.profiles?.email}</div>
                  </td>
                  <td>{s.counselors?.name}</td>
                  <td>{s.method}</td>
                  <td>
                    {rupiah(s.total)}
                    <div className="muted small">{s.payment_method}</div>
                  </td>
                  <td><span className={`badge ${s.status}`}>{STATUS_LABEL[s.status]}</span></td>
                  <td><button className="btn small" onClick={() => setOpen(s)}>Kelola</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {open && <SessionDialog s={open} isAdmin={isAdmin} onClose={() => setOpen(null)} onStatus={setStatus} />}
    </>
  );
}

function SessionDialog({
  s, isAdmin, onClose, onStatus,
}: {
  s: SessionRow;
  isAdmin: boolean;
  onClose: () => void;
  onStatus: (s: SessionRow, status: SessionStatus, note?: string) => void;
}) {
  const [note, setNote] = useState(s.counselor_note);
  const [client, setClient] = useState<ClientData | null>(null);
  const [clientError, setClientError] = useState<string | null>(null);
  const [profile, setProfile] = useState<Partial<Profile> | null>(null);

  useEffect(() => {
    // Data diri mahasiswa (RLS: admin & konselor yang menangani sesi ini)
    supabase.from("profiles")
      .select("full_name, address, occupation, institution, faculty, phone, guardian_phone")
      .eq("id", s.user_id).maybeSingle()
      .then(({ data }) => setProfile((data as Partial<Profile>) ?? {}));
  }, [s.user_id]);

  async function loadClient() {
    const [ci, sc] = await Promise.all([
      supabase.from("check_ins").select("created_at, mood, factors, feelings, note")
        .eq("user_id", s.user_id).order("created_at", { ascending: false }).limit(14),
      supabase.from("screenings").select(SCREENING_COLUMNS)
        .eq("user_id", s.user_id).order("created_at", { ascending: false }).limit(5),
    ]);
    if (ci.error || sc.error) setClientError((ci.error ?? sc.error)!.message);
    else setClient({ checkIns: ci.data ?? [], screenings: (sc.data ?? []) as ScreeningRow[] });
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="card modal" onClick={(e) => e.stopPropagation()}>
        <div className="row-between">
          <h2>Sesi #{s.id}</h2>
          <button className="btn ghost small" onClick={onClose}>✕</button>
        </div>
        <dl className="kv">
          <dt>Mahasiswa</dt><dd>{s.profiles?.name} ({s.profiles?.email})</dd>
          <dt>Konselor</dt><dd>{s.counselors?.name} · {s.counselors?.title}</dd>
          <dt>Jadwal</dt><dd>{fmtDateTime(s.starts_at)} · {s.duration_min} menit · {s.method}</dd>
          <dt>Pembayaran</dt><dd>{rupiah(s.total)} via {s.payment_method}</dd>
          <dt>Status</dt><dd><span className={`badge ${s.status}`}>{STATUS_LABEL[s.status]}</span></dd>
          <dt>Cerita mahasiswa</dt><dd>{s.note || <span className="muted">—</span>}</dd>
          <dt>Berbagi data</dt><dd>{s.share_check_in ? "Ya, ringkasan check-in & skrining dibagikan" : "Tidak"}</dd>
        </dl>

        <h3>Data diri mahasiswa</h3>
        {profile ? <ProfileDetails p={profile} /> : <p className="muted small">Memuat…</p>}

        <label>
          Pesan untuk mahasiswa (tampil di aplikasi)
          <textarea rows={3} value={note} onChange={(e) => setNote(e.target.value)} placeholder="mis. tautan ruang sesi, persiapan, atau ringkasan sesi" />
        </label>

        <div className="actions">
          {isAdmin && s.status === "waiting_verification" && (
            <button className="btn" onClick={() => onStatus(s, "confirmed", note)}>✓ Konfirmasi pembayaran</button>
          )}
          {s.status === "confirmed" && (
            <button className="btn" onClick={() => onStatus(s, "completed", note)}>Tandai selesai</button>
          )}
          {note !== s.counselor_note && (
            <button className="btn secondary" onClick={() => onStatus(s, s.status, note)}>Simpan pesan</button>
          )}
          {s.status !== "cancelled" && s.status !== "completed" && (
            <button className="btn danger" onClick={() => confirm("Batalkan sesi ini?") && onStatus(s, "cancelled", note)}>
              Batalkan
            </button>
          )}
        </div>

        {!isAdmin && s.share_check_in && (
          <div className="client-data">
            {!client && <button className="btn secondary small" onClick={loadClient}>Lihat data yang dibagikan</button>}
            {clientError && <p className="error">{clientError}</p>}
            {client && (
              <>
                <h3>Skrining terakhir</h3>
                {client.screenings.length === 0 && <p className="muted small">Belum ada.</p>}
                {client.screenings.map((r) => {
                  const { text, risk } = screeningSummary(r);
                  return (
                    <div key={r.created_at} className={risk ? "mini warn" : "mini"}>
                      {fmtDateTime(r.created_at)} — {text}
                      {risk && <div><b>⚠ {risk}</b></div>}
                    </div>
                  );
                })}
                <h3>Check-in 14 terakhir</h3>
                {client.checkIns.length === 0 && <p className="muted small">Belum ada.</p>}
                {client.checkIns.map((c) => (
                  <div key={c.created_at} className="mini">
                    {fmtDateTime(c.created_at)} — {MOODS[c.mood]}
                    {[...c.factors, ...c.feelings].length > 0 && <span className="muted"> · {[...c.factors, ...c.feelings].join(", ")}</span>}
                    {c.note && <div className="muted small">“{c.note}”</div>}
                  </div>
                ))}
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
