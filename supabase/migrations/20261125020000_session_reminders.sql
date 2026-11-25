-- =====================================================================
-- Pengingat sesi lewat push notification.
-- pg_cron menjalankan private.send_session_reminders() tiap menit; untuk
-- sesi terkonfirmasi dikirim (masing-masing sekali):
--   'h60'   ~1 jam sebelum mulai      (jendela: mulai-60m .. mulai-10m)
--   'h10'   ~10 menit sebelum mulai   (jendela: mulai-10m .. mulai)
--   'start' saat ruang sesi dibuka    (jendela: mulai .. mulai+15m)
-- Bila sesi dikonfirmasi mepet, hanya pengingat yang jendelanya sedang berjalan
-- yang terkirim (tidak ada tumpukan pengingat kedaluwarsa).
-- =====================================================================

create table if not exists private.session_reminders (
    session_id  text not null references public.sessions (id) on delete cascade,
    kind        text not null check (kind in ('h60', 'h10', 'start')),
    sent_at     timestamptz not null default now(),
    primary key (session_id, kind)
);

create or replace function private.send_session_reminders()
returns integer
language plpgsql security definer set search_path = public, extensions
as $$
declare
    cfg    private.push_config;
    r      record;
    title  text;
    body   text;
    n      integer := 0;
begin
    select * into cfg from private.push_config where id = 1;
    if not found then return 0; end if;

    for r in
        select s.id, s.user_id, s.method, s.starts_at, c.name as cname,
               case
                   when now() >= s.starts_at then 'start'
                   when now() >= s.starts_at - interval '10 minutes' then 'h10'
                   else 'h60'
               end as kind
        from sessions s
        join counselors c on c.id = s.counselor_id
        where s.status = 'confirmed'
          and now() >= s.starts_at - interval '60 minutes'
          and now() <  s.starts_at + interval '15 minutes'
    loop
        -- catat dulu; bila sudah pernah terkirim, lewati
        insert into private.session_reminders (session_id, kind) values (r.id, r.kind)
        on conflict do nothing;
        if not found then continue; end if;

        case r.kind
            when 'h60' then
                title := 'Sesi konseling sebentar lagi ⏰';
                body  := format('Sesimu dengan %s dimulai pukul %s WIB (sekitar %s menit lagi). Siapkan tempat yang nyaman ya.',
                                r.cname, to_char(r.starts_at at time zone 'Asia/Jakarta', 'HH24:MI'),
                                greatest(1, ceil(extract(epoch from r.starts_at - now()) / 60))::int);
            when 'h10' then
                title := 'Sesi dimulai 10 menit lagi';
                body  := format('Sesimu dengan %s dimulai pukul %s WIB. Ruang sesi terbuka tepat pada jamnya.',
                                r.cname, to_char(r.starts_at at time zone 'Asia/Jakarta', 'HH24:MI'));
            else
                title := 'Ruang sesi sudah dibuka 🟢';
                body  := case when r.method = 'Chat'
                              then format('Sesi dengan %s sudah dimulai. Buka ruang chat sekarang.', r.cname)
                              else format('Sesi dengan %s sudah dimulai. Ketuk untuk masuk ke sesi.', r.cname) end;
        end case;

        perform net.http_post(
            url     := cfg.function_url,
            body    := jsonb_build_object('user_id', r.user_id, 'title', title, 'body', body,
                                          'route', case when r.kind = 'start' and r.method = 'Chat'
                                                        then 'chat/' || r.id else 'session/' || r.id end),
            headers := jsonb_build_object('Content-Type', 'application/json', 'x-push-secret', cfg.secret)
        );
        n := n + 1;
    end loop;
    return n;
end;
$$;

revoke execute on function private.send_session_reminders() from public, anon, authenticated;

-- Jadwal tiap menit (pg_cron)
create extension if not exists pg_cron;
select cron.unschedule(jobid) from cron.job where jobname = 'seiv-session-reminders';
select cron.schedule('seiv-session-reminders', '* * * * *', $$select private.send_session_reminders()$$);
