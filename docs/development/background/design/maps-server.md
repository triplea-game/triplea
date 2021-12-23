# Maps Server Design

Describes the design behind how maps are uploaded and downloaded.

## Map Upload

Upload is done directly in github, triplea-maps organization where a new repository
is created.

The maps server scans github for new repositories on a periodic basis.
When found, it will search for a 'map.yml' descriptor file to load in data such
as the: {map name, map description, map version, game names}. That data will then
be stored in the maps-server database

## Map Download System

Requests for a map listing are sent to the maps-server which then returns a payload
that describes the available maps and includes a download URL.

