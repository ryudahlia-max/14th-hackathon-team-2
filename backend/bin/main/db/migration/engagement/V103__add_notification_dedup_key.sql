alter table notifications add column dedup_key varchar(180);

alter table notifications
    add constraint uk_notifications_dedup_key unique (dedup_key);
