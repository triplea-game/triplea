-- delete any extra name bans that are duplicates
delete from banned_username bu
where exists (
  select 1
  from banned_username t2
  where bu.date_created < t2.date_created
    and bu.username ilike t2.username
);

-- upper case existing entries
update banned_username set username = upper(username);
