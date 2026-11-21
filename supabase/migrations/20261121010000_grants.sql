-- =====================================================================
-- Hak akses tabel eksplisit.
-- Project Supabase baru tidak lagi otomatis memberi GRANT ke anon/authenticated
-- untuk tabel baru, jadi semua akses didefinisikan di sini (RLS tetap berlaku).
-- anon (belum login) sengaja tidak diberi akses apa pun.
-- =====================================================================

grant usage on schema public to authenticated, service_role;

grant all on all tables in schema public to service_role;
grant execute on all functions in schema public to service_role;

-- Profil: baca (RLS), ubah kolom name saja (sudah di migrasi awal)
grant select on public.profiles to authenticated;
grant update (name) on public.profiles to authenticated;

-- Konselor: semua user login boleh baca; tulis hanya lolos RLS bila admin
grant select, insert, update, delete on public.counselors to authenticated;

-- Data pribadi mahasiswa
grant select, insert, delete on public.check_ins  to authenticated;
grant select, insert, delete on public.journals   to authenticated;
grant select, insert         on public.screenings to authenticated;

-- Sesi: pesan + ubah catatan/berbagi data; status lewat RPC set_session_status
grant select, insert on public.sessions to authenticated;
grant update (note, share_check_in) on public.sessions to authenticated;

-- Fungsi internal tidak perlu bisa dipanggil anon
revoke execute on function public.booked_slots(text, date)              from anon, public;
revoke execute on function public.set_session_status(text, text, text)  from anon, public;
revoke execute on function public.admin_set_role(uuid, text, text)      from anon, public;
revoke execute on function public.admin_stats()                         from anon, public;
grant execute on function public.booked_slots(text, date)               to authenticated;
grant execute on function public.set_session_status(text, text, text)  to authenticated;
grant execute on function public.admin_set_role(uuid, text, text)       to authenticated;
grant execute on function public.admin_stats()                          to authenticated;
