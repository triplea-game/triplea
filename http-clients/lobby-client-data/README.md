## lobby-client-data

Holds common data structures shared by server/client.

The servers have a dependency on this package.
Modifications to this package must be backward compatible.

This module should have zero dependencies of its own so that we
do not force any transitive dependencies onto the servers.
