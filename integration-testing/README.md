Sub-project to isolate system and integration tests.

These are tests that require live resources, such as a running http
server or a running database.

A major motivation for this sub-project is to isolate tests that interact
with database to a single sub-project. This allows us to run tests for
all sub-projects in parallel without conflicts in database updates. For
example, if one sub-project removes and adds data to database, another
sub-project running tests in parallel could depend on that data and
fail (race condition). 