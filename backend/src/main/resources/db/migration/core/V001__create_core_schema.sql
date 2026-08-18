create table profiles (
    id uuid primary key,
    nickname varchar(30) not null,
    avatar_object_path varchar(500),
    ai_face_consent boolean not null default false,
    timezone varchar(50) not null default 'Asia/Seoul',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_profiles_nickname check (char_length(nickname) between 1 and 30)
);

create table user_consents (
    id uuid primary key,
    user_id uuid not null,
    terms_version varchar(30) not null,
    privacy_version varchar(30) not null,
    agreed_at timestamptz not null,
    created_at timestamptz not null default now(),
    constraint uq_user_consents_version unique (user_id, terms_version, privacy_version)
);

create table friend_invites (
    id uuid primary key,
    inviter_id uuid not null,
    token varchar(80) not null unique,
    status varchar(20) not null,
    expires_at timestamptz not null,
    accepted_by uuid,
    accepted_at timestamptz,
    created_at timestamptz not null default now(),
    constraint ck_friend_invites_status check (status in ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED'))
);

create table friendships (
    id uuid primary key,
    first_user_id uuid not null,
    second_user_id uuid not null,
    status varchar(20) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_friendships_pair unique (first_user_id, second_user_id),
    constraint ck_friendships_order check (first_user_id::text < second_user_id::text),
    constraint ck_friendships_status check (status in ('ACCEPTED', 'BLOCKED', 'REMOVED'))
);

create table wellness_groups (
    id uuid primary key,
    owner_id uuid not null,
    name varchar(50) not null,
    max_members smallint not null default 8,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_wellness_groups_name check (char_length(name) between 1 and 50),
    constraint ck_wellness_groups_max_members check (max_members between 2 and 20)
);

create table group_members (
    id uuid primary key,
    group_id uuid not null references wellness_groups(id) on delete cascade,
    user_id uuid not null,
    role varchar(20) not null,
    joined_at timestamptz not null default now(),
    constraint uq_group_members_group_user unique (group_id, user_id),
    constraint ck_group_members_role check (role in ('OWNER', 'MEMBER'))
);

create table routines (
    id uuid primary key,
    owner_id uuid not null,
    title varchar(80) not null,
    category varchar(30) not null,
    days_of_week varchar(30) not null,
    reminder_time time not null,
    timezone varchar(50) not null default 'Asia/Seoul',
    start_date date not null,
    end_date date,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_routines_title check (char_length(title) between 1 and 80),
    constraint ck_routines_date_range check (end_date is null or end_date >= start_date)
);

create table routine_completions (
    id uuid primary key,
    routine_id uuid not null references routines(id) on delete cascade,
    user_id uuid not null,
    completion_date date not null,
    completed_at timestamptz not null,
    proof_object_path varchar(500),
    note varchar(500),
    created_at timestamptz not null default now(),
    constraint uq_routine_completions_day unique (routine_id, completion_date)
);

create index idx_friendships_first_status on friendships(first_user_id, status);
create index idx_friendships_second_status on friendships(second_user_id, status);
create index idx_group_members_user_group on group_members(user_id, group_id);
create index idx_routines_owner_active on routines(owner_id, active);
create index idx_routine_completions_user_date on routine_completions(user_id, completion_date desc);
create index idx_routine_completions_routine_date on routine_completions(routine_id, completion_date desc);

alter table profiles enable row level security;
alter table user_consents enable row level security;
alter table friend_invites enable row level security;
alter table friendships enable row level security;
alter table wellness_groups enable row level security;
alter table group_members enable row level security;
alter table routines enable row level security;
alter table routine_completions enable row level security;

do $$
begin
    if exists (select 1 from pg_roles where rolname = 'anon') then
        execute 'revoke all on profiles, user_consents, friend_invites, friendships, wellness_groups, '
            || 'group_members, routines, routine_completions from anon';
    end if;
    if exists (select 1 from pg_roles where rolname = 'authenticated') then
        execute 'revoke all on profiles, user_consents, friend_invites, friendships, wellness_groups, '
            || 'group_members, routines, routine_completions from authenticated';
    end if;
end
$$;
