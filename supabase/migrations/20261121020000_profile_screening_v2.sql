-- =====================================================================
-- v2: data diri pengguna + skrining 6 instrumen
-- (SRQ-20, PHQ-9, GAD-7, ASQ, CAPE-15, CD-RISC-10)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Data diri pengguna
-- ---------------------------------------------------------------------
alter table public.profiles
    add column full_name      text not null default '' check (char_length(full_name) <= 100),
    add column address        text not null default '' check (char_length(address) <= 300),
    add column occupation     text not null default '' check (occupation in ('', 'mahasiswa', 'bekerja', 'lainnya')),
    add column institution    text not null default '' check (char_length(institution) <= 150),
    add column faculty        text not null default '' check (char_length(faculty) <= 150),
    add column phone          text not null default '' check (phone ~ '^(\+?[0-9]{8,15})?$'),
    add column guardian_phone text not null default '' check (guardian_phone ~ '^(\+?[0-9]{8,15})?$');

-- Pengguna hanya boleh mengubah kolom data dirinya (bukan role/email)
grant update (name, full_name, address, occupation, institution, faculty, phone, guardian_phone)
    on public.profiles to authenticated;

-- ---------------------------------------------------------------------
-- Skrining: instrumen tambahan (null = skrining versi lama, belum diukur)
-- ---------------------------------------------------------------------
alter table public.screenings
    add column phq_difficulty smallint check (phq_difficulty between 0 and 3),
    add column gad_difficulty smallint check (gad_difficulty between 0 and 3),
    add column asq            smallint check (asq between 0 and 4),          -- jumlah "Ya" pada ASQ 1–4
    add column asq_attempt    smallint check (asq_attempt between 0 and 1),  -- 0 = ≤12 bulan, 1 = >1 tahun
    add column asq_acute      boolean,                                       -- ASQ 6: pikiran bunuh diri saat ini
    add column cape           smallint check (cape between 0 and 60),        -- total CAPE-15 (0–4 per butir)
    add column cape_frequent  smallint check (cape_frequent between 0 and 15), -- butir "Sering"/"Sangat sering"
    add column cdrisc         smallint check (cdrisc between 0 and 40);      -- total CD-RISC-10

-- Dasbor admin: risiko tinggi kini juga menghitung ASQ positif
create or replace function public.admin_stats()
returns json
language sql stable security definer set search_path = public
as $$
    select case when public.is_admin() then json_build_object(
        'students',          (select count(*) from profiles where role = 'student'),
        'check_ins_7d',      (select count(*) from check_ins where created_at > now() - interval '7 days'),
        'screenings_total',  (select count(*) from screenings),
        'high_risk',         (select count(distinct user_id) from screenings
                              where phq_item9 > 0 or phq >= 15 or gad >= 15
                                 or coalesce(asq, 0) > 0 or coalesce(asq_acute, false)),
        'waiting_payment',   (select count(*) from sessions where status = 'waiting_verification'),
        'upcoming',          (select count(*) from sessions where status = 'confirmed' and starts_at > now()),
        'revenue',           (select coalesce(sum(total), 0) from sessions where status in ('confirmed', 'completed'))
    ) end;
$$;
