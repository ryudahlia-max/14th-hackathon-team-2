do $$
declare
    table_name text;
begin
    foreach table_name in array array[
        'profiles',
        'user_consents',
        'friend_invites',
        'friendships',
        'wellness_groups',
        'group_members',
        'routines',
        'routine_completions',
        'routine_completion_reactions',
        'chat_rooms',
        'chat_room_members',
        'chat_messages',
        'message_reactions',
        'ai_generation_jobs',
        'notifications',
        'monthly_recaps'
    ]
    loop
        execute format('alter table public.%I enable row level security', table_name);
        if exists (select 1 from pg_roles where rolname = 'anon') then
            execute format('revoke all on table public.%I from anon', table_name);
        end if;
        if exists (select 1 from pg_roles where rolname = 'authenticated') then
            execute format('revoke all on table public.%I from authenticated', table_name);
        end if;
    end loop;
end
$$;

create index if not exists ix_ai_generation_jobs_requester_created
    on public.ai_generation_jobs(requester_id, created_at desc);

create index if not exists ix_notifications_unread_user_created
    on public.notifications(user_id, created_at desc, id desc)
    where read_at is null;

do $$
begin
    if to_regclass('realtime.messages') is not null
            and to_regprocedure('auth.uid()') is not null then
        create schema if not exists private;
        revoke all on schema private from public;

        execute $function$
            create or replace function private.can_receive_engagement_topic(requested_topic text)
            returns boolean
            language sql
            stable
            security definer
            set search_path = ''
            as $body$
                select (select auth.uid()) is not null and (
                    (
                        requested_topic ~ '^chat-room:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
                        and exists (
                            select 1
                            from public.chat_room_members member
                            join public.chat_rooms room on room.id = member.room_id
                            where member.room_id = substring(requested_topic from 11)::uuid
                              and member.user_id = (select auth.uid())
                              and (
                                (
                                    room.type = 'GROUP'
                                    and exists (
                                        select 1 from public.group_members current_member
                                        where current_member.group_id = room.group_id
                                          and current_member.user_id = (select auth.uid())
                                    )
                                )
                                or
                                (
                                    room.type = 'DIRECT'
                                    and exists (
                                        select 1
                                        from public.chat_room_members peer
                                        join public.friendships friendship
                                          on friendship.status = 'ACCEPTED'
                                         and (
                                              (friendship.first_user_id = (select auth.uid())
                                               and friendship.second_user_id = peer.user_id)
                                           or (friendship.second_user_id = (select auth.uid())
                                               and friendship.first_user_id = peer.user_id)
                                         )
                                        where peer.room_id = room.id
                                          and peer.user_id <> (select auth.uid())
                                    )
                                )
                              )
                        )
                    )
                    or
                    (
                        requested_topic ~ '^ai-generation:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
                        and exists (
                            select 1
                            from public.ai_generation_jobs job
                            where job.id = substring(requested_topic from 15)::uuid
                              and (job.requester_id = (select auth.uid())
                                   or job.target_user_id = (select auth.uid()))
                        )
                    )
                );
            $body$
        $function$;

        revoke execute on function private.can_receive_engagement_topic(text) from public;
        if exists (select 1 from pg_roles where rolname = 'anon') then
            revoke all on schema private from anon;
            revoke execute on function private.can_receive_engagement_topic(text) from anon;
        end if;
        if exists (select 1 from pg_roles where rolname = 'authenticated') then
            grant usage on schema private to authenticated;
            grant execute on function private.can_receive_engagement_topic(text) to authenticated;
        end if;

        execute 'drop policy if exists "engagement members can receive broadcasts" on realtime.messages';
        execute $policy$
            create policy "engagement members can receive broadcasts"
            on realtime.messages
            for select
            to authenticated
            using (
                realtime.messages.extension = 'broadcast'
                and (select private.can_receive_engagement_topic((select realtime.topic())))
            )
        $policy$;
    end if;
end
$$;
