create table todo
(
    todo_id    uuid primary key default gen_random_uuid(),
    created_at timestamp not null default current_timestamp,
    title      text      not null
);

create table todo_item
(
    todo_item_id uuid primary key default gen_random_uuid(),
    todo_id      uuid references todo (todo_id),
    created_at   timestamp not null default current_timestamp,
    title        text      not null
);