-- =====================================================================
-- Scoring CAPE-P15 mengikuti Núñez et al. (2021, PLoS One, PMC8389461):
-- butir dinilai 1 (tidak pernah) – 5 (sangat sering), total 15–75,
-- skor = rata-rata semua butir; cut-off rata-rata 1,47 (Bukenaite et al.).
-- Domain: ide paranoid (butir 1–5), pengalaman aneh (6–12), anomali persepsi (13–15).
-- =====================================================================

alter table public.screenings drop constraint if exists screenings_cape_check;

-- Data lama memakai skala 0–4 per butir (total 0–60): geser ke skala 1–5
update public.screenings set cape = cape + 15 where cape is not null;

alter table public.screenings
    add constraint screenings_cape_check check (cape between 15 and 75),
    add column cape_pi smallint check (cape_pi between 5 and 25),   -- total domain ide paranoid
    add column cape_be smallint check (cape_be between 7 and 35),   -- total domain pengalaman aneh
    add column cape_pa smallint check (cape_pa between 3 and 15);   -- total domain anomali persepsi

comment on column public.screenings.cape is 'CAPE-P15 total (15–75, butir 1–5). Positif bila rata-rata (cape/15) ≥ 1,47';
comment on column public.screenings.cape_frequent is 'Jumlah butir CAPE-P15 yang dijawab Sering/Sangat sering';
