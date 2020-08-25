# DB Rider for Unit Testing

We use a framework called [DB Rider](https://github.com/database-rider/database-rider).
It sits on top of [DBUnit](http://dbunit.sourceforge.net/).

The tests are driven by annotations. By convention we always use "cleanBefore = true" on the initial data set. A typical test execution flow follows these steps:
- deletes data from all DB tables
For each test:
  - runs initial `@Dataset` defined at top of class, or defined at the top of the unit test
  - runs the test code
  - runs any '@ExpectedDataSet` tags at end of test

A typical test could be to set up an initial dataset, run an insert, then use the expected data set to confirm the database has the expected end-state.

## Conventions
- tests are typically per DAO, DAO is a single class, usually organized around a single table
- dataset files are stored in a folder per test file
- dataset folder is typically named after the table/DAO being tested, lower snake case
- dataset files are YML, lower snake case

## Notes

### Importance of Clean Before

Avoids foreign key integrity violations failures when inserting initial data.

EG:
```
@DataSet(cleanBefore =  true, value = "user_role/initial.yml")
```

For example, table A has an FK into table B. If we define a dataset that defines table B, then table B is cleaned before inserting data. When cleaning table B, because table A still has FKs into B, the clean of table B can cause FK violations and the insertion will fail. What is devious about this is there is an ordering dependency, if we do not run the test that places data in table A first, then there are no FKs and so the test would seemingly pass. The cleanbefore instructs DB unit to clean all tables before inserting any initial data sets, so we avoid this problem.

