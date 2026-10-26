alter table duvida add column datahora datetime(6) not null default current_timestamp(6);
alter table duvida add column tipo_pausa varchar(10) not null default 'NENHUMA';