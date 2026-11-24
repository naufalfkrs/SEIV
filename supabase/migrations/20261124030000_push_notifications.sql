-- =====================================================================
-- Push notification (Firebase Cloud Messaging)
-- 1) Token perangkat disimpan per akun (hanya lewat RPC; tidak bisa dibaca klien)
-- 2) Saat status sesi berubah / konselor mengirim pesan, trigger memanggil
--    Edge Function push-notify (lewat pg_net) yang mengirim notifikasi via FCM.
-- =====================================================================

create table public.push_tokens (
    token       text primary key,
    user_id     uuid not null references public.profiles (id) on delete cascade,
    platform    text not null default 'android',
    updated_at  timestamptz not null default now()
);
create index push_tokens_user_idx on public.push_tokens (user_id);
alter table public.push_tokens enable row level security;  -- tanpa policy: klien tidak bisa akses langsung
grant all on public.push_tokens to service_role;

-- Kaitkan token dengan akun yang login (token pindah akun bila HP dipakai bergantian)
create or replace function public.register_push_token(p_token text, p_platform text default 'android')
returns void
language plpgsql security definer set search_path = public
as $$
begin
    if auth.uid() is null then raise exception 'Belum login'; end if;
    if p_token is null or char_length(p_token) not between 20 and 4096 then raise exception 'Token tidak valid'; end if;
    insert into push_tokens (token, user_id, platform, updated_at)
    values (p_token, auth.uid(), coalesce(p_platform, 'android'), now())
    on conflict (token) do update set user_id = excluded.user_id, platform = excluded.platform, updated_at = now();
end;
$$;

create or replace function public.unregister_push_token(p_token text)
returns void
language sql security definer set search_path = public
as $$ delete from push_tokens where token = p_token and user_id = auth.uid(); $$;

revoke execute on function public.register_push_token(text, text) from anon, public;
revoke execute on function public.unregister_push_token(text) from anon, public;
grant execute on function public.register_push_token(text, text) to authenticated;
grant execute on function public.unregister_push_token(text) to authenticated;

-- ---------------------------------------------------------------------
-- Konfigurasi webhook (skema private: tidak terekspos ke API)
-- ---------------------------------------------------------------------
create extension if not exists pg_net with schema extensions;
create schema if not exists private;
revoke all on schema private from public, anon, authenticated;

create table if not exists private.push_config (
    id            smallint primary key default 1 check (id = 1),
    function_url  text not null,
    secret        text not null
);

create or replace function private.notify_session_change()
returns trigger
language plpgsql security definer set search_path = public, extensions
as $$
declare
    cfg     private.push_config;
    cname   text;
    whentx  text;
    title   text;
    body    text;
begin
    select * into cfg from private.push_config where id = 1;
    if not found then return new; end if;

    select name into cname from counselors where id = new.counselor_id;
    whentx := to_char(new.starts_at at time zone 'Asia/Jakarta', 'DD/MM HH24:MI') || ' WIB';

    if new.status is distinct from old.status then
        case new.status
            when 'confirmed' then
                title := 'Sesi terkonfirmasi 🎉';
                body  := format('Pembayaranmu sudah diverifikasi. Sesi dengan %s pada %s.', cname, whentx);
            when 'completed' then
                title := 'Sesi selesai';
                body  := format('Terima kasih sudah hadir. Yuk beri ulasan untuk %s.', cname);
            when 'cancelled' then
                title := 'Sesi dibatalkan';
                body  := format('Sesi dengan %s (%s) dibatalkan. Hubungi admin SEIV bila ada pertanyaan.', cname, whentx);
            else
                return new;
        end case;
    elsif new.counselor_note is distinct from old.counselor_note and new.counselor_note <> '' then
        title := 'Pesan dari ' || cname;
        body  := left(new.counselor_note, 180);
    else
        return new;
    end if;

    perform net.http_post(
        url     := cfg.function_url,
        body    := jsonb_build_object('user_id', new.user_id, 'title', title, 'body', body, 'route', 'session/' || new.id),
        headers := jsonb_build_object('Content-Type', 'application/json', 'x-push-secret', cfg.secret)
    );
    return new;
end;
$$;

create trigger sessions_push_notify
    after update of status, counselor_note on public.sessions
    for each row execute function private.notify_session_change();
