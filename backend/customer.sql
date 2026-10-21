-- Non-destructive: keep any existing customers.
create table if not exists public.customer (
    customerid bigint generated always as identity primary key,
    lastname varchar not null,
    firstname varchar not null,
    mobileno varchar not null,
    address varchar not null
);

alter table public.customer enable row level security;
revoke all on table public.customer from anon, authenticated;
grant usage on schema public to service_role;
grant select, insert on table public.customer to service_role;

-- Grant the actual identity sequence, including when the table already existed.
do $$
declare sequence_name text;
begin
    sequence_name := pg_get_serial_sequence('public.customer', 'customerid');
    if sequence_name is not null then
        execute format('grant usage, select on sequence %s to service_role', sequence_name);
    end if;
end $$;
