-- :name insert-todo-query :<! :1
insert into todo (title)
values (:title)
returning *;

-- :name todo-by-id-query :? :1
select * from todo
where todo_id = :todo_id;

-- :name find-all-todos-query :? :*
select * from todo;

-- :name find-todos-count-query :? :1
select count(*) as c from todo;
