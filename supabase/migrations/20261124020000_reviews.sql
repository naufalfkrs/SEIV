-- =====================================================================
-- Ulasan konselor: mahasiswa memberi rating 1–5 (+ komentar) untuk sesi
-- miliknya yang sudah SELESAI. Satu ulasan per sesi. Rating & jumlah ulasan
-- di tabel counselors dihitung ulang otomatis dari ulasan sungguhan.
-- =====================================================================

create table public.reviews (
    id            uuid primary key default gen_random_uuid(),
    session_id    text not null unique references public.sessions (id) on delete cascade,
    user_id       uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    counselor_id  text not null references public.counselors (id) on delete cascade,
    rating        smallint not null check (rating between 1 and 5),
    comment       text not null default '' check (char_length(comment) <= 500),
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);
create index reviews_counselor_idx on public.reviews (counselor_id, created_at desc);

alter table public.reviews enable row level security;

-- Ulasan boleh dibaca semua pengguna login (tanpa identitas: tabel profil tetap dilindungi RLS)
create policy "ulasan: semua user login boleh baca" on public.reviews
    for select to authenticated using (true);

-- Hanya untuk sesi milik sendiri yang sudah selesai, dan konselornya harus sesuai sesi
create policy "ulasan: tulis untuk sesi selesai milik sendiri" on public.reviews
    for insert to authenticated with check (
        user_id = auth.uid() and exists (
            select 1 from public.sessions s
            where s.id = session_id and s.user_id = auth.uid()
              and s.status = 'completed' and s.counselor_id = reviews.counselor_id
        )
    );

create policy "ulasan: ubah milik sendiri" on public.reviews
    for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

grant select, insert on public.reviews to authenticated;
grant update (rating, comment, updated_at) on public.reviews to authenticated;
grant all on public.reviews to service_role;

-- Hitung ulang rating konselor dari ulasan (security definer: mahasiswa tidak bisa ubah tabel konselor)
create or replace function public.refresh_counselor_rating()
returns trigger
language plpgsql security definer set search_path = public
as $$
declare
    cid text := coalesce(new.counselor_id, old.counselor_id);
begin
    update counselors c
       set rating  = coalesce((select round(avg(r.rating)::numeric, 1) from reviews r where r.counselor_id = cid), 0),
           reviews = (select count(*) from reviews r where r.counselor_id = cid)
     where c.id = cid;
    return null;
end;
$$;

create trigger reviews_refresh_rating
    after insert or update or delete on public.reviews
    for each row execute function public.refresh_counselor_rating();

-- Angka rating awal dari desain bukan ulasan sungguhan: mulai dari nol
alter table public.counselors alter column rating set default 0;
alter table public.counselors drop constraint if exists counselors_rating_check;
alter table public.counselors add constraint counselors_rating_check check (rating between 0 and 5);
update public.counselors set rating = 0, reviews = 0;
