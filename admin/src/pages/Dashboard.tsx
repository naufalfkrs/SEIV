import { useEffect, useState } from "react";
import { rupiah, supabase } from "../supabase";

interface Stats {
  students: number;
  check_ins_7d: number;
  screenings_total: number;
  high_risk: number;
  waiting_payment: number;
  upcoming: number;
  revenue: number;
}

export default function Dashboard() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    supabase.rpc("admin_stats").then(({ data, error }) => {
      if (error) setError(error.message);
      else setStats(data as Stats);
    });
  }, []);

  if (error) return <p className="error">{error}</p>;
  if (!stats) return <p className="muted">Memuat…</p>;

  const tiles: { label: string; value: string; warn?: boolean }[] = [
    { label: "Mahasiswa terdaftar", value: String(stats.students) },
    { label: "Check-in 7 hari terakhir", value: String(stats.check_ins_7d) },
    { label: "Total skrining", value: String(stats.screenings_total) },
    { label: "Pengguna berisiko tinggi", value: String(stats.high_risk), warn: stats.high_risk > 0 },
    { label: "Pembayaran menunggu verifikasi", value: String(stats.waiting_payment), warn: stats.waiting_payment > 0 },
    { label: "Sesi terjadwal", value: String(stats.upcoming) },
    { label: "Pendapatan (terkonfirmasi)", value: rupiah(stats.revenue) },
  ];

  return (
    <>
      <h1>Dasbor</h1>
      <div className="grid">
        {tiles.map((t) => (
          <div key={t.label} className={t.warn ? "card stat warn" : "card stat"}>
            <div className="stat-value">{t.value}</div>
            <div className="muted">{t.label}</div>
          </div>
        ))}
      </div>
      <p className="muted small">
        “Berisiko tinggi” = pernah menjawab PHQ-9 butir 9 lebih dari 0, atau skor PHQ-9/GAD-7 ≥ 15. Demi privasi, data
        individual tidak ditampilkan ke admin; konselor hanya bisa melihat data klien yang memilih untuk membagikannya.
      </p>
    </>
  );
}
