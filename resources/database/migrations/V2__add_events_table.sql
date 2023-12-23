create table events
(
    id uuid primary key default gen_random_uuid(),
    type text not null,
    aggregate_id uuid not null,
    aggregate_type text not null,
    payload jsonb not null,
    created_at timestamp not null default current_timestamp
);