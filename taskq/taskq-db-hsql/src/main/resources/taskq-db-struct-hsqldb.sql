--[]
create table taskq_props
(
taskq_key varchar(256) primary key not null,
taskq_value varchar(4096)
);

--[]
create table taskq_servers
(
id integer generated always as identity(start with 1) primary key,
created timestamp default now,
last_active timestamp not null,
abandoned boolean default false,
name varchar(256) not null,
taskq_group varchar(128),
);

--[]
create table taskq_items 
(
id integer generated always as identity(start with 1) primary key,
created timestamp default now,
item_key varchar(256) default null,
item blob not null
);

--[]
create index idx_taskq_item_key on taskq_items(item_key);

--[]
create table taskq_tasks 
( 
id integer generated always as identity(start with 1) primary key,
created timestamp default now,
task_id binary(36) not null,
server_id integer not null,
abandoned boolean default false,
item_id integer not null,
expire_date timestamp not null,
before_task_id binary(36) default null,
after_task_id binary(36) default null,
qname varchar(128) not null,
qos_key varchar(256) default null,
constraint idx_taskq_tasks_task_id
	unique (task_id),
constraint fk_item_task_id
	foreign key (item_id)
	references taskq_items (id)
	on delete cascade
);
