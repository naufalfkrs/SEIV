-- =====================================================================
-- SEIV — skema database awal
-- Peran: student (mahasiswa/pengguna), counselor (nurse/psikolog), admin
-- Semua tabel memakai Row Level Security (RLS).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Profil pengguna (1:1 dengan auth.users)
-- ---------------------------------------------------------------------
create table public.profiles (
    id          uuid primary key references auth.users (id) on delete cascade,
    name        text not null default '',
    email       text not null default '',
    role        text not null default 'student' check (role in ('student', 'counselor', 'admin')),
    created_at  timestamptz not null default now()
);

-- Buat profil otomatis saat user mendaftar
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    insert into public.profiles (id, name, email)
    values (
        new.id,
        coalesce(new.raw_user_meta_data ->> 'name', new.raw_user_meta_data ->> 'full_name', split_part(new.email, '@', 1)),
        coalesce(new.email, '')
    );
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- Helper peran (security definer agar tidak memicu rekursi RLS)
create or replace function public.is_admin()
returns boolean
language sql stable security definer set search_path = public
as $$ select exists (select 1 from profiles where id = auth.uid() and role = 'admin'); $$;

-- ---------------------------------------------------------------------
-- Konselor
-- ---------------------------------------------------------------------
create table public.counselors (
    id           text primary key,
    user_id      uuid unique references public.profiles (id) on delete set null,
    name         text not null,
    title        text not null,              -- "Nurse Counselor" / "Psikolog"
    rating       numeric(2, 1) not null default 5.0,
    reviews      integer not null default 0,
    tags         text[] not null default '{}',
    bio          text not null default '',
    price        integer not null check (price >= 0),
    initials     text not null default '',
    color_index  smallint not null default 0,
    active       boolean not null default true,
    created_at   timestamptz not null default now()
);

create or replace function public.my_counselor_id()
returns text
language sql stable security definer set search_path = public
as $$ select id from counselors where user_id = auth.uid(); $$;

-- ---------------------------------------------------------------------
-- Data pribadi mahasiswa
-- ---------------------------------------------------------------------
create table public.check_ins (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    created_at  timestamptz not null default now(),
    mood        smallint not null check (mood between 0 and 4),
    factors     text[] not null default '{}',
    feelings    text[] not null default '{}',
    note        text not null default '' check (char_length(note) <= 500)
);
create index check_ins_user_created_idx on public.check_ins (user_id, created_at desc);

create table public.journals (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    created_at  timestamptz not null default now(),
    prompt      text not null default '',
    body        text not null check (char_length(body) between 1 and 20000)
);
create index journals_user_created_idx on public.journals (user_id, created_at desc);

create table public.screenings (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    created_at  timestamptz not null default now(),
    srq         smallint not null check (srq between 0 and 20),
    gad         smallint not null check (gad between 0 and 21),
    phq         smallint not null check (phq between 0 and 27),
    phq_item9   smallint not null default 0 check (phq_item9 between 0 and 3)
);
create index screenings_user_created_idx on public.screenings (user_id, created_at desc);

-- ---------------------------------------------------------------------
-- Sesi konseling
-- status: waiting_verification -> confirmed -> completed  (atau cancelled)
-- ---------------------------------------------------------------------
create table public.sessions (
    id               text primary key,                        -- contoh: SEIV12345
    user_id          uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    counselor_id     text not null references public.counselors (id),
    starts_at        timestamptz not null,
    duration_min     smallint not null default 45,
    method           text not null check (method in ('Chat', 'Voice Call', 'Video Call')),
    note             text not null default '' check (char_length(note) <= 300),
    share_check_in   boolean not null default false,
    payment_method   text not null,
    total            integer not null check (total >= 0),
    status           text not null default 'waiting_verification'
                     check (status in ('waiting_verification', 'confirmed', 'completed', 'cancelled')),
    counselor_note   text not null default '',
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now()
);
create index sessions_user_idx on public.sessions (user_id, starts_at desc);
create index sessions_counselor_idx on public.sessions (counselor_id, starts_at);
-- Satu konselor tidak bisa dipesan dua kali pada jam yang sama
create unique index sessions_slot_unique on public.sessions (counselor_id, starts_at) where status <> 'cancelled';

-- Apakah konselor yang sedang login boleh melihat data mahasiswa ini?
-- (hanya bila mahasiswa memilih "bagikan ringkasan check-in & skrining")
create or replace function public.counselor_can_view(student uuid)
returns boolean
language sql stable security definer set search_path = public
as $$
    select exists (
        select 1 from sessions s
        join counselors c on c.id = s.counselor_id
        where s.user_id = student and c.user_id = auth.uid()
          and s.share_check_in and s.status <> 'cancelled'
    );
$$;

-- ---------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------
alter table public.profiles   enable row level security;
alter table public.counselors enable row level security;
alter table public.check_ins  enable row level security;
alter table public.journals   enable row level security;
alter table public.screenings enable row level security;
alter table public.sessions   enable row level security;

-- profiles
create policy "profil: baca milik sendiri" on public.profiles
    for select using (id = auth.uid() or public.is_admin()
        or exists (select 1 from sessions s join counselors c on c.id = s.counselor_id
                   where s.user_id = profiles.id and c.user_id = auth.uid()));
create policy "profil: ubah milik sendiri" on public.profiles
    for update using (id = auth.uid()) with check (id = auth.uid());
-- Kolom role/email tidak boleh diubah sendiri oleh pengguna
revoke update on public.profiles from authenticated;
grant update (name) on public.profiles to authenticated;

-- counselors
create policy "konselor: semua user login boleh lihat" on public.counselors
    for select to authenticated using (active or public.is_admin());
create policy "konselor: admin kelola" on public.counselors
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- check_ins / journals / screenings: milik sendiri (+ konselor bila dibagikan)
create policy "check-in: milik sendiri" on public.check_ins
    for select using (user_id = auth.uid() or public.counselor_can_view(user_id));
create policy "check-in: tambah sendiri" on public.check_ins
    for insert with check (user_id = auth.uid());
create policy "check-in: hapus sendiri" on public.check_ins
    for delete using (user_id = auth.uid());

create policy "jurnal: milik sendiri" on public.journals
    for select using (user_id = auth.uid());      -- jurnal TIDAK pernah dibagikan
create policy "jurnal: tambah sendiri" on public.journals
    for insert with check (user_id = auth.uid());
create policy "jurnal: hapus sendiri" on public.journals
    for delete using (user_id = auth.uid());

create policy "skrining: milik sendiri" on public.screenings
    for select using (user_id = auth.uid() or public.counselor_can_view(user_id));
create policy "skrining: tambah sendiri" on public.screenings
    for insert with check (user_id = auth.uid());

-- sessions
create policy "sesi: lihat" on public.sessions
    for select using (user_id = auth.uid() or counselor_id = public.my_counselor_id() or public.is_admin());
create policy "sesi: pesan" on public.sessions
    for insert with check (user_id = auth.uid() and status = 'waiting_verification' and counselor_note = '');
create policy "sesi: ubah catatan sendiri" on public.sessions
    for update using (user_id = auth.uid()) with check (user_id = auth.uid());
-- Mahasiswa hanya boleh mengubah catatan & pilihan berbagi data; status lewat RPC
revoke update on public.sessions from authenticated;
grant update (note, share_check_in) on public.sessions to authenticated;

-- ---------------------------------------------------------------------
-- RPC
-- ---------------------------------------------------------------------

-- Jam yang sudah terisi untuk konselor pada tanggal tertentu (zona Asia/Jakarta)
create or replace function public.booked_slots(p_counselor text, p_day date)
returns table (slot text)
language sql stable security definer set search_path = public
as $$
    select to_char(starts_at at time zone 'Asia/Jakarta', 'HH24:MI')
    from sessions
    where counselor_id = p_counselor and status <> 'cancelled'
      and (starts_at at time zone 'Asia/Jakarta')::date = p_day;
$$;
grant execute on function public.booked_slots(text, date) to authenticated;

-- Admin / konselor mengubah status sesi
create or replace function public.set_session_status(p_id text, p_status text, p_counselor_note text default null)
returns public.sessions
language plpgsql security definer set search_path = public
as $$
declare
    s sessions;
begin
    select * into s from sessions where id = p_id;
    if not found then raise exception 'Sesi tidak ditemukan'; end if;

    if public.is_admin() then
        null; -- admin boleh semua transisi
    elsif s.counselor_id = public.my_counselor_id() then
        if p_status not in ('completed', 'cancelled', s.status) then
            raise exception 'Konselor hanya bisa menyelesaikan atau membatalkan sesi';
        end if;
    else
        raise exception 'Tidak diizinkan';
    end if;

    update sessions
       set status = p_status,
           counselor_note = coalesce(p_counselor_note, counselor_note),
           updated_at = now()
     where id = p_id
    returning * into s;
    return s;
end;
$$;
grant execute on function public.set_session_status(text, text, text) to authenticated;

-- Admin mengatur peran pengguna & menautkan akun konselor
create or replace function public.admin_set_role(p_user uuid, p_role text, p_counselor text default null)
returns void
language plpgsql security definer set search_path = public
as $$
begin
    if not public.is_admin() then raise exception 'Hanya admin'; end if;
    update profiles set role = p_role where id = p_user;
    if p_counselor is not null then
        update counselors set user_id = null where user_id = p_user;
        update counselors set user_id = p_user where id = p_counselor;
    end if;
end;
$$;
grant execute on function public.admin_set_role(uuid, text, text) to authenticated;

-- Ringkasan untuk dasbor admin
create or replace function public.admin_stats()
returns json
language sql stable security definer set search_path = public
as $$
    select case when public.is_admin() then json_build_object(
        'students',          (select count(*) from profiles where role = 'student'),
        'check_ins_7d',      (select count(*) from check_ins where created_at > now() - interval '7 days'),
        'screenings_total',  (select count(*) from screenings),
        'high_risk',         (select count(distinct user_id) from screenings where phq_item9 > 0 or phq >= 15 or gad >= 15),
        'waiting_payment',   (select count(*) from sessions where status = 'waiting_verification'),
        'upcoming',          (select count(*) from sessions where status = 'confirmed' and starts_at > now()),
        'revenue',           (select coalesce(sum(total), 0) from sessions where status in ('confirmed', 'completed'))
    ) end;
$$;
grant execute on function public.admin_stats() to authenticated;

-- ---------------------------------------------------------------------
-- Data awal konselor (sesuai desain Figma)
-- ---------------------------------------------------------------------
insert into public.counselors (id, name, title, rating, reviews, tags, bio, price, initials, color_index) values
    ('dinda', 'Ns. Dinda Aulia', 'Nurse Counselor', 4.9, 128, '{"Stres akademik","Kecemasan"}',
     'Spesialis dukungan kesehatan mental remaja dan gaya hidup sehat.', 150000, 'DA', 0),
    ('anisa', 'Dr. Anisa Rahma', 'Psikolog', 4.9, 152, '{"Kecemasan","Self-esteem"}',
     'Berpengalaman dalam terapi kognitif dan manajemen emosi.', 200000, 'AR', 1),
    ('fajar', 'M. Fajar Ramadhan', 'Psikolog', 4.8, 86, '{"Stres akademik","Motivasi"}',
     'Fokus pada produktivitas, motivasi, dan pengembangan diri.', 175000, 'FR', 2);
