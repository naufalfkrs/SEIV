import { useEffect, useState, type FormEvent } from "react";
import { rupiah, supabase, type Counselor } from "../supabase";

const EMPTY: Counselor = {
  id: "", user_id: null, name: "", title: "Psikolog", rating: 5, reviews: 0, tags: [], bio: "",
  price: 150000, initials: "", color_index: 0, active: true,
};

export default function Counselors() {
  const [rows, setRows] = useState<Counselor[]>([]);
  const [editing, setEditing] = useState<Counselor | null>(null);
  const [isNew, setIsNew] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    const { data, error } = await supabase.from("counselors").select("*").order("created_at");
    if (error) setError(error.message);
    else setRows((data ?? []) as Counselor[]);
  }

  useEffect(() => {
    load();
  }, []);

  async function save(c: Counselor) {
    const row = {
      ...c,
      id: c.id.trim().toLowerCase().replace(/[^a-z0-9-]/g, "-"),
      initials: c.initials || c.name.split(" ").filter(Boolean).slice(-2).map((w) => w[0]).join("").toUpperCase(),
    };
    const { error } = isNew
      ? await supabase.from("counselors").insert(row)
      : await supabase.from("counselors").update(row).eq("id", c.id);
    if (error) alert(error.message);
    else {
      setEditing(null);
      load();
    }
  }

  return (
    <>
      <div className="row-between">
        <h1>Konselor</h1>
        <button className="btn" onClick={() => { setEditing({ ...EMPTY }); setIsNew(true); }}>+ Tambah konselor</button>
      </div>
      {error && <p className="error">{error}</p>}
      <div className="grid">
        {rows.map((c) => (
          <div key={c.id} className={c.active ? "card" : "card inactive"}>
            <div className="row-between">
              <div>
                <b>{c.name}</b>
                <div className="muted small">{c.title} · ⭐ {c.rating} ({c.reviews})</div>
              </div>
              <span className={c.user_id ? "badge confirmed" : "badge waiting_verification"}>
                {c.user_id ? "Akun tertaut" : "Belum ada akun"}
              </span>
            </div>
            <p className="small">{c.bio}</p>
            <div className="muted small">{c.tags.join(" · ")}</div>
            <div className="row-between">
              <b>{rupiah(c.price)}</b>
              <button className="btn secondary small" onClick={() => { setEditing({ ...c }); setIsNew(false); }}>Ubah</button>
            </div>
          </div>
        ))}
      </div>
      {editing && <CounselorForm value={editing} isNew={isNew} onCancel={() => setEditing(null)} onSave={save} />}
    </>
  );
}

function CounselorForm({
  value, isNew, onCancel, onSave,
}: { value: Counselor; isNew: boolean; onCancel: () => void; onSave: (c: Counselor) => void }) {
  const [c, setC] = useState(value);
  const set = <K extends keyof Counselor>(k: K, v: Counselor[K]) => setC((prev) => ({ ...prev, [k]: v }));

  function submit(e: FormEvent) {
    e.preventDefault();
    onSave(c);
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="card modal" onClick={(e) => e.stopPropagation()} onSubmit={submit}>
        <h2>{isNew ? "Tambah konselor" : `Ubah ${value.name}`}</h2>
        {isNew && (
          <label>ID singkat (huruf kecil, tanpa spasi)
            <input value={c.id} onChange={(e) => set("id", e.target.value)} required placeholder="mis. budi" />
          </label>
        )}
        <label>Nama lengkap<input value={c.name} onChange={(e) => set("name", e.target.value)} required /></label>
        <label>Jabatan
          <select value={c.title} onChange={(e) => set("title", e.target.value)}>
            <option>Psikolog</option>
            <option>Nurse Counselor</option>
            <option>Psikiater</option>
            <option>Konselor Sebaya</option>
          </select>
        </label>
        <div className="two">
          <label>Tarif per sesi (Rp)
            <input type="number" min={0} step={5000} value={c.price} onChange={(e) => set("price", Number(e.target.value))} required />
          </label>
          <label>Warna avatar
            <select value={c.color_index} onChange={(e) => set("color_index", Number(e.target.value))}>
              <option value={0}>Biru</option>
              <option value={1}>Oranye</option>
              <option value={2}>Ungu</option>
            </select>
          </label>
        </div>
        <label>Keahlian (pisahkan dengan koma)
          <input value={c.tags.join(", ")} onChange={(e) => set("tags", e.target.value.split(",").map((t) => t.trim()).filter(Boolean))} />
        </label>
        <label>Bio<textarea rows={3} value={c.bio} onChange={(e) => set("bio", e.target.value)} /></label>
        <label className="check">
          <input type="checkbox" checked={c.active} onChange={(e) => set("active", e.target.checked)} /> Tampil di aplikasi
        </label>
        <div className="actions">
          <button type="button" className="btn ghost" onClick={onCancel}>Batal</button>
          <button className="btn">Simpan</button>
        </div>
      </form>
    </div>
  );
}
