# Maps Server - Overview

The maps-index server indexes all of the maps and provides an API
for game-clients to get a listing of maps and to interact with
the maps list.

## Historical Context

The maps index was historically first created as an XML file, and
each map was an entry in the XML file. The game-client would download
this file from a static location and then parse it to know the list
of maps.

Eventually this was evolved to be a YML file. The maps-index server
is a further step on this evolution to where maps are dynamically
read from the triplea-maps github organization and then the list
fo maps is maintained in a database.



