## Java

- Follow the naming in: [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html#s5-naming)
- Use a `new` prefix for factory methods (e.g. `newFoo()` instead of `createFoo()`)

## URLs

- [kebab-case](https://en.wikipedia.org/wiki/Kebab_case)


## Database

- [snake_case](https://en.wikipedia.org/wiki/Snake_case) 
- Use singular names, eg: `user` instead of `users`
- Favor using a PK column that is a number
- Use `id` for the name of the primary key column
   - Good: `user.id`
   - Bad: `user.user_id`
- Foreign key columns are named after the table and column they refer to, eg: `<table_name>_<column_name>`.
  EG: `user_id` would reference the `id` column of the `user` table.
- Spaces, no tabs
- line continuations are 4 space indented
- Line break on SQL keywords "select, join, where, having, group by, order by"

EG:

```
select * from user_ban ub
join users u on u.id = ub.user_id
where ub.expires_on > now()
   and ub.some_column = 'example';
```

