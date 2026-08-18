create table routine_completion_reactions (
    id uuid primary key,
    completion_id uuid not null references routine_completions(id) on delete cascade,
    routine_owner_id uuid not null,
    reactor_id uuid not null,
    type varchar(20) not null,
    created_at timestamptz not null,
    constraint uq_routine_completion_reaction unique (completion_id, reactor_id),
    constraint ck_routine_completion_reaction_type
        check (type in ('HEART', 'SAD', 'THUMBS_UP', 'FIRE', 'SMILE'))
);

create index ix_routine_completion_reactions_owner_created
    on routine_completion_reactions(routine_owner_id, created_at desc);

alter table routine_completion_reactions enable row level security;

do $$
begin
    if exists (select 1 from pg_roles where rolname = 'anon') then
        revoke all on routine_completion_reactions from anon;
    end if;
    if exists (select 1 from pg_roles where rolname = 'authenticated') then
        revoke all on routine_completion_reactions from authenticated;
    end if;
end
$$;
