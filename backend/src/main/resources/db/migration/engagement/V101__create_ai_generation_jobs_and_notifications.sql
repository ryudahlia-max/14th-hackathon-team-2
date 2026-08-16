create table ai_generation_jobs (
 id uuid primary key, requester_id uuid not null, target_user_id uuid not null, occurrence_id uuid not null, client_request_id varchar(100) not null, status varchar(16) not null, attempt_count integer not null default 0, next_attempt_at timestamptz not null, output_object_key text, failure_code varchar(64), created_at timestamptz not null, updated_at timestamptz not null,
 constraint uk_ai_jobs_requester_client unique(requester_id,client_request_id), constraint ck_ai_jobs_status check(status in ('QUEUED','RUNNING','SUCCEEDED','FAILED','BLOCKED'))
);
create index ix_ai_generation_jobs_status_next_attempt on ai_generation_jobs(status,next_attempt_at);
create table notifications (id uuid primary key,user_id uuid not null,type varchar(32) not null,content text not null,created_at timestamptz not null);
create index ix_notifications_user_created_id on notifications(user_id,created_at desc,id desc);
