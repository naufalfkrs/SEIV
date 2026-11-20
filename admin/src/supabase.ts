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
