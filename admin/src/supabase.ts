import { createClient } from "@supabase/supabase-js";

const url = import.meta.env.VITE_SUPABASE_URL as string | undefined;
const key = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined;

export const configured = Boolean(url && key);
export const supabase = createClient(url ?? "http://localhost", key ?? "missing");

export type Role = "student" | "counselor" | "admin";

export interface Profile {
  id: string;
  name: string;
  email: string;
  role: Role;
  created_at: string;
  full_name: string;
  address: string;
  occupation: "" | "mahasiswa" | "bekerja" | "lainnya";
  institution: string;
  faculty: string;
  phone: string;
  guardian_phone: string;
}

export const OCCUPATION_LABEL: Record<string, string> = {
  mahasiswa: "Mahasiswa", bekerja: "Bekerja", lainnya: "Lainnya", "": "Belum diisi",
};

export interface ScreeningRow {
  created_at: string;
  srq: number;
  gad: number;
  phq: number;
  phq_item9: number;
  asq: number | null;
  asq_acute: boolean | null;
  asq_attempt: number | null;
  cape: number | null;
  cape_frequent: number | null;
  cdrisc: number | null;
  srq_items: number | null;
}

/** Subskala SRQ-20 (pedoman Riskesdas). */
const SRQ_SUBSCALES: [string, number[]][] = [
  ["depresi", [6, 9, 10, 14, 15, 16, 17]], ["cemas", [3, 4, 5]], ["somatik", [1, 2, 7, 19]],
  ["kognitif", [8, 12, 13]], ["energi", [8, 11, 12, 13, 18, 20]],
];

/** Ringkasan skor skrining + tanda risiko (sama dengan logika aplikasi). */
export function screeningSummary(r: ScreeningRow): { text: string; risk: string | null } {
  const parts = [`SRQ-20 ${r.srq}${r.srq >= 6 ? " (GME)" : ""}`, `PHQ-9 ${r.phq}`, `GAD-7 ${r.gad}`];
  if (r.srq_items !== null) {
    const sub = SRQ_SUBSCALES.map(([n, items]) => `${n} ${items.filter((i) => (r.srq_items! >> (i - 1)) & 1).length}/${items.length}`);
    parts.push(`SRQ subskala: ${sub.join(", ")}`);
  }
  if (r.asq !== null) parts.push(`ASQ ${r.asq_acute ? "positif akut" : (r.asq ?? 0) > 0 ? "positif non-akut" : "negatif"}`);
  // CAPE-P15: butir 1–5, rata-rata; positif bila ≥ 1,47 (Núñez et al., 2021)
  if (r.cape !== null) {
    const mean = r.cape / 15;
    parts.push(`CAPE-15 rata-rata ${mean.toFixed(2)} (${mean >= 1.47 ? "positif" : "negatif"})`);
  }
  if (r.cdrisc !== null) parts.push(`CD-RISC-10 ${r.cdrisc}/40`);
  const risks: string[] = [];
  if (r.asq_acute) risks.push("ASQ positif AKUT — perlu evaluasi keselamatan segera");
  else if ((r.asq ?? 0) > 0) risks.push("ASQ positif non-akut — perlu penilaian keselamatan singkat");
  if (r.asq_attempt === 0) risks.push("percobaan ≤12 bulan terakhir");
  if (r.phq_item9 > 0) risks.push(`PHQ-9 butir 9 = ${r.phq_item9} — perlu penilaian risiko bunuh diri`);
  return { text: parts.join(" · "), risk: risks.length ? risks.join(" · ") : null };
}

export interface Counselor {
  id: string;
  user_id: string | null;
  name: string;
  title: string;
  rating: number;
  reviews: number;
  tags: string[];
  bio: string;
  price: number;
  initials: string;
  color_index: number;
  active: boolean;
}

export type SessionStatus = "waiting_verification" | "confirmed" | "completed" | "cancelled";

export interface SessionRow {
  id: string;
  user_id: string;
  counselor_id: string;
  starts_at: string;
  duration_min: number;
  method: string;
  note: string;
  share_check_in: boolean;
  payment_method: string;
  total: number;
  status: SessionStatus;
  counselor_note: string;
  created_at: string;
  profiles: { name: string; email: string } | null;
  counselors: { name: string; title: string } | null;
}

export const STATUS_LABEL: Record<SessionStatus, string> = {
  waiting_verification: "Menunggu verifikasi",
  confirmed: "Terkonfirmasi",
  completed: "Selesai",
  cancelled: "Dibatalkan",
};

export const rupiah = (n: number) => "Rp " + n.toLocaleString("id-ID");

export const fmtDateTime = (iso: string) =>
  new Date(iso).toLocaleString("id-ID", {
    weekday: "short", day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit",
  });
