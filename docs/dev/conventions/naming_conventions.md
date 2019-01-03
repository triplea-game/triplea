## Java

- Follow the naming in: [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html#s5-naming)
- Use a `new` prefix for factory methods (e.g. `newFoo()` instead of `createFoo()`)

## URLs

- [kebab-case](https://en.wikipedia.org/wiki/Kebab_case)


## Database

- [snake_case](https://en.wikipedia.org/wiki/Snake_case) 
- Use singular names, eg: `user` instead of `users`
- Use `id` for primary key column name
- Foreign key columns are named after the table the refer to, eg: `<table_name>.id`
- Use spaces, no tabs
- line continuations are 4 space indented
- Line break on SQL keywords "select, join, where, having, group by, order by"

EG:

```
select * from user_ban ub
join users u on u.id = ub.user_id
where ub.expires_on > now()
   and ub.some_column = 'example';
```

