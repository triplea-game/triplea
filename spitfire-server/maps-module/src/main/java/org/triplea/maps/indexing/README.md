Map indexing refers to the process where the map server fetches
metadata from all maps available to download. As part of this
fetch we are gathering enough information to list the map as
available to download.

To index we gather data from two key locations:

- **Github API**: This tells us which repositories exist,
each repository represents a map

- **map.yml file**: Each repository is expected to contain
a map.yml file that in turn tells us the name and the
version of the map.
