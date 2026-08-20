alter table routines add column color varchar(7) not null default '#60A5FA';

update routines
set color = category
where category ~ '^#[0-9A-Fa-f]{6}$';

update routines
set category = 'OTHER'
where category ~ '^#[0-9A-Fa-f]{6}$';

alter table routines
    add constraint ck_routines_color check (color ~ '^#[0-9A-Fa-f]{6}$');
