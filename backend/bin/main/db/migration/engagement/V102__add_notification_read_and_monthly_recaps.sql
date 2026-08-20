alter table notifications add column read_at timestamptz;
create table monthly_recaps (id uuid primary key,group_id uuid not null,recap_month varchar(7) not null,summary text not null,image_object_key text,created_at timestamptz not null,constraint uk_monthly_recaps_group_month unique(group_id,recap_month));
