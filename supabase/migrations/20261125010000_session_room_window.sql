-- =====================================================================
-- Ruang sesi hanya bisa dipakai saat jam sesi: mulai tepat pada starts_at
-- sampai akhir durasi + 15 menit kelonggaran. Di luar itu chat hanya-baca.
-- =====================================================================

create or replace function public.session_room_open(p_session text)
returns boolean
language sql stable security definer set search_path = public
as $$
    select exists (
        select 1 from sessions s
        where s.id = p_session
          and s.status = 'confirmed'
          and now() >= s.starts_at
          and now() <= s.starts_at + make_interval(mins => s.duration_min + 15)
    );
$$;
grant execute on function public.session_room_open(text) to authenticated;

drop policy if exists "chat: peserta kirim saat sesi aktif" on public.chat_messages;

create policy "chat: peserta kirim saat jam sesi" on public.chat_messages
    for insert to authenticated with check (
        sender_id = auth.uid()
        and public.is_session_participant(session_id)
        and public.session_room_open(session_id)
        and from_counselor = exists (
            select 1 from sessions s where s.id = session_id and s.counselor_id = public.my_counselor_id()
        )
    );
