create table chat_rooms (
    id uuid primary key,
    type varchar(16) not null,
    direct_pair_key varchar(80),
    group_id uuid,
    created_at timestamptz not null,
    constraint ck_chat_rooms_type check (type in ('DIRECT', 'GROUP')),
    constraint ck_chat_rooms_shape check ((type = 'DIRECT' and direct_pair_key is not null and group_id is null) or (type = 'GROUP' and group_id is not null and direct_pair_key is null)),
    constraint uk_chat_rooms_direct_pair unique (direct_pair_key),
    constraint uk_chat_rooms_group unique (group_id)
);

create table chat_room_members (
    id uuid primary key,
    room_id uuid not null references chat_rooms(id) on delete cascade,
    user_id uuid not null,
    joined_at timestamptz not null,
    constraint uk_chat_room_members_room_user unique (room_id, user_id)
);
create index ix_chat_room_members_user_room on chat_room_members(user_id, room_id);

create table chat_messages (
    id uuid primary key,
    room_id uuid not null references chat_rooms(id) on delete cascade,
    sender_id uuid,
    client_message_id varchar(100),
    type varchar(20) not null,
    content text,
    media_url text,
    created_at timestamptz not null,
    constraint ck_chat_messages_type check (type in ('TEXT', 'IMAGE', 'ROUTINE_CARD', 'AI_IMAGE', 'SYSTEM')),
    constraint uk_chat_messages_client_id unique (room_id, sender_id, client_message_id)
);
create index ix_chat_messages_room_created_id on chat_messages(room_id, created_at desc, id desc);

create table message_reactions (
    id uuid primary key,
    message_id uuid not null references chat_messages(id) on delete cascade,
    user_id uuid not null,
    type varchar(30) not null,
    created_at timestamptz not null,
    constraint uk_message_reactions_message_user_type unique (message_id, user_id, type)
);
