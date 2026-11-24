-- =====================================================================
-- Ruang sesi
--  • Sesi "Chat": ruang chat di dalam aplikasi (mahasiswa ↔ konselor)
--  • Sesi "Voice Call"/"Video Call": tautan rapat (mis. Google Meet) dari konselor/admin
-- =====================================================================

-- ---------------------------------------------------------------------
-- Tautan rapat
-- ---------------------------------------------------------------------
alter table public.sessions
    add column meeting_url text not null default ''
        check (meeting_url = '' or (meeting_url ~ '^https://\S+\.\S+$' and char_length(meeting_url) <= 500));

-- Hanya admin / konselor sesi itu yang boleh mengisi tautan
create or replace function public.set_session_meeting(p_id text, p_url text)
returns void
language plpgsql security definer set search_path = public
as $$
declare
    s sessions;
begin
    select * into s from sessions where id = p_id;
    if not found then raise exception 'Sesi tidak ditemukan'; end if;
    -- coalesce: my_counselor_id() bernilai NULL untuk non-konselor; NULL tidak boleh lolos pemeriksaan
    if not (public.is_admin() or coalesce(s.counselor_id = public.my_counselor_id(), false)) then
        raise exception 'Tidak diizinkan';
    end if;
    update sessions set meeting_url = coalesce(trim(p_url), ''), updated_at = now() where id = p_id;
end;
$$;
revoke execute on function public.set_session_meeting(text, text) from anon, public;
grant execute on function public.set_session_meeting(text, text) to authenticated;

-- ---------------------------------------------------------------------
-- Pesan chat
-- ---------------------------------------------------------------------
create table public.chat_messages (
    id          uuid primary key default gen_random_uuid(),
    session_id  text not null references public.sessions (id) on delete cascade,
    sender_id   uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    from_counselor boolean not null default false,
    body        text not null check (char_length(trim(body)) between 1 and 2000),
    created_at  timestamptz not null default now()
);
create index chat_messages_session_idx on public.chat_messages (session_id, created_at);
alter table public.chat_messages enable row level security;

-- Peserta sesi: pemilik sesi (mahasiswa) atau konselor yang tertaut ke sesi
create or replace function public.is_session_participant(p_session text)
returns boolean
language sql stable security definer set search_path = public
as $$
    select exists (
        select 1 from sessions s
        where s.id = p_session
          and (s.user_id = auth.uid() or s.counselor_id = public.my_counselor_id())
    );
$$;

create policy "chat: peserta & admin boleh baca" on public.chat_messages
    for select to authenticated using (public.is_session_participant(session_id) or public.is_admin());

-- Kirim pesan: hanya peserta, hanya saat sesi terkonfirmasi; penanda konselor harus jujur
create policy "chat: peserta kirim saat sesi aktif" on public.chat_messages
    for insert to authenticated with check (
        sender_id = auth.uid()
        and public.is_session_participant(session_id)
        and exists (select 1 from sessions s where s.id = session_id and s.status = 'confirmed')
        and from_counselor = exists (
            select 1 from sessions s where s.id = session_id and s.counselor_id = public.my_counselor_id()
        )
    );

grant select, insert on public.chat_messages to authenticated;
grant all on public.chat_messages to service_role;

-- Panel web memakai Realtime agar pesan mahasiswa langsung muncul
alter publication supabase_realtime add table public.chat_messages;

-- ---------------------------------------------------------------------
-- Push notification: pesan konselor & tautan sesi baru → ke mahasiswa
-- ---------------------------------------------------------------------
create or replace function private.notify_chat_message()
returns trigger
language plpgsql security definer set search_path = public, extensions
as $$
declare
    cfg   private.push_config;
    s     sessions;
    cname text;
begin
    if not new.from_counselor then return new; end if;  -- konselor memakai panel web (tanpa push)
    select * into cfg from private.push_config where id = 1;
    if not found then return new; end if;
    select * into s from sessions where id = new.session_id;
    select name into cname from counselors where id = s.counselor_id;
    perform net.http_post(
        url     := cfg.function_url,
        body    := jsonb_build_object('user_id', s.user_id, 'title', 'Pesan baru dari ' || cname,
                                      'body', left(new.body, 180), 'route', 'chat/' || s.id),
        headers := jsonb_build_object('Content-Type', 'application/json', 'x-push-secret', cfg.secret)
    );
    return new;
end;
$$;

create trigger chat_messages_push
    after insert on public.chat_messages
    for each row execute function private.notify_chat_message();

create or replace function private.notify_meeting_link()
returns trigger
language plpgsql security definer set search_path = public, extensions
as $$
declare
    cfg   private.push_config;
    cname text;
begin
    if new.meeting_url = '' or new.meeting_url is not distinct from old.meeting_url then return new; end if;
    select * into cfg from private.push_config where id = 1;
    if not found then return new; end if;
    select name into cname from counselors where id = new.counselor_id;
    perform net.http_post(
        url     := cfg.function_url,
        body    := jsonb_build_object('user_id', new.user_id, 'title', 'Tautan sesi tersedia',
                                      'body', format('%s sudah mengirim tautan %s. Ketuk "Masuk ke Sesi" saat jadwal tiba.', cname, lower(new.method)),
                                      'route', 'session/' || new.id),
        headers := jsonb_build_object('Content-Type', 'application/json', 'x-push-secret', cfg.secret)
    );
    return new;
end;
$$;

create trigger sessions_meeting_push
    after update of meeting_url on public.sessions
    for each row execute function private.notify_meeting_link();
