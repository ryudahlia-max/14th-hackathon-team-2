alter table routines
    add column if not exists completion_deadline time;

update routines
set completion_deadline = reminder_time
where completion_deadline is null;

alter table routines
    alter column completion_deadline set not null;
