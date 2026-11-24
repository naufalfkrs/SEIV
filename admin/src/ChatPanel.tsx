import { useEffect, useRef, useState, type FormEvent } from "react";
import { supabase } from "./supabase";

interface Msg {
  id: string;
  from_counselor: boolean;
  body: string;
  created_at: string;
}

/**
 * Ruang chat sesi (metode "Chat") di panel web. Konselor sesi bisa membalas;
 * admin hanya bisa membaca. Pesan baru masuk lewat Supabase Realtime (+ cadangan polling).
 */
/** Ruang sesi terbuka tepat jam mulai sampai akhir durasi + 15 menit (sama dengan aturan server). */
export function roomWindow(startsAt: string, durationMin: number) {
  const start = new Date(startsAt).getTime();
  return { start, closes: start + (durationMin + 15) * 60_000 };
}

export default function ChatPanel({
  sessionId, canSend, confirmed, startsAt, durationMin,
}: { sessionId: string; canSend: boolean; confirmed: boolean; startsAt: string; durationMin: number }) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => { const t = setInterval(() => setNow(Date.now()), 15_000); return () => clearInterval(t); }, []);
  const { start, closes } = roomWindow(startsAt, durationMin);
  const open = confirmed && now >= start && now <= closes;
  const startText = new Date(start).toLocaleString("id-ID", { weekday: "long", day: "numeric", month: "long", hour: "2-digit", minute: "2-digit" });
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [text, setText] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const bottom = useRef<HTMLDivElement>(null);

  async function load() {
    const { data, error } = await supabase.from("chat_messages")
      .select("id, from_counselor, body, created_at").eq("session_id", sessionId).order("created_at").limit(500);
    if (error) setError(error.message);
    else setMsgs((data ?? []) as Msg[]);
  }

  useEffect(() => {
    load();
    const channel = supabase.channel(`chat-${sessionId}`)
      .on("postgres_changes", { event: "INSERT", schema: "public", table: "chat_messages", filter: `session_id=eq.${sessionId}` },
        (p) => setMsgs((prev) => (prev.some((m) => m.id === (p.new as Msg).id) ? prev : [...prev, p.new as Msg])))
      .subscribe();
    const timer = setInterval(load, 8000); // cadangan bila koneksi realtime terputus
    return () => { clearInterval(timer); supabase.removeChannel(channel); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId]);

  useEffect(() => { bottom.current?.scrollIntoView({ behavior: "smooth" }); }, [msgs.length]);

  async function send(e: FormEvent) {
    e.preventDefault();
    const body = text.trim();
    if (!body) return;
    setBusy(true);
    setError(null);
    const { data, error } = await supabase.from("chat_messages")
      .insert({ session_id: sessionId, body, from_counselor: true }).select("id, from_counselor, body, created_at").single();
    setBusy(false);
    if (error) setError(error.message);
    else {
      setText("");
      setMsgs((prev) => (prev.some((m) => m.id === (data as Msg).id) ? prev : [...prev, data as Msg]));
    }
  }

  const time = (iso: string) => new Date(iso).toLocaleString("id-ID", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });

  return (
    <div className="chat">
      <div className="chat-log">
        {msgs.length === 0 && <p className="muted small">Belum ada pesan.</p>}
        {msgs.map((m) => (
          <div key={m.id} className={m.from_counselor ? "bubble mine" : "bubble"}>
            <div>{m.body}</div>
            <div className="bubble-time">{m.from_counselor ? "Konselor" : "Mahasiswa"} · {time(m.created_at)}</div>
          </div>
        ))}
        <div ref={bottom} />
      </div>
      {error && <p className="error">{error}</p>}
      {canSend && open ? (
        <form className="chat-input" onSubmit={send}>
          <textarea rows={2} value={text} maxLength={2000} placeholder="Tulis balasan…"
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); send(e as unknown as FormEvent); } }} />
          <button className="btn" disabled={busy || !text.trim()}>Kirim</button>
        </form>
      ) : (
        <p className="muted small">
          {!confirmed ? "Chat hanya aktif saat sesi berstatus Terkonfirmasi."
            : now < start ? `Ruang chat dibuka pada jam sesi: ${startText}.`
            : now > closes ? "Waktu sesi sudah berakhir. Riwayat chat tetap bisa dibaca."
            : "Hanya konselor sesi ini yang bisa membalas."}
        </p>
      )}
    </div>
  );
}
