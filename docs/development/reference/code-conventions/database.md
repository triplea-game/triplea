
## Database TimeZone Handling

In short, always use zoned values and use UTC.

<https://justatheory.com/2012/04/postgres-use-timestamptz/>

- Use timestamp with time zone (aka timestamptz)
- Always specify a time zone when inserting into a timestamptz or timetz column.
