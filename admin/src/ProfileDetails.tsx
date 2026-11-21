import { OCCUPATION_LABEL, type Profile } from "./supabase";

/** Data diri pengguna (nama lengkap, domisili, status, instansi, kontak). */
export default function ProfileDetails({ p }: { p: Partial<Profile> }) {
  const dash = <span className="muted">—</span>;
  return (
    <dl className="kv">
      <dt>Nama lengkap</dt><dd>{p.full_name || dash}</dd>
      <dt>Domisili</dt><dd>{p.address || dash}</dd>
      <dt>Status</dt><dd>{OCCUPATION_LABEL[p.occupation ?? ""]}</dd>
      {p.occupation === "mahasiswa" && (
        <>
          <dt>Asal instansi</dt><dd>{p.institution || dash}</dd>
          <dt>Fakultas</dt><dd>{p.faculty || dash}</dd>
        </>
      )}
      <dt>No. HP</dt><dd>{p.phone ? <a href={`https://wa.me/${toWa(p.phone)}`} target="_blank" rel="noreferrer">{p.phone}</a> : dash}</dd>
      <dt>No. HP wali / darurat</dt><dd>{p.guardian_phone ? <a href={`tel:${p.guardian_phone}`}>{p.guardian_phone}</a> : dash}</dd>
    </dl>
  );
}

/** 0812… → 62812… untuk tautan WhatsApp. */
function toWa(phone: string) {
  const d = phone.replace(/[^0-9]/g, "");
  return d.startsWith("0") ? "62" + d.slice(1) : d;
}
