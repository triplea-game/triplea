# map.yml file

The map.yml file is used to obtain the name of the map and
the paths and names of the games contained in the map.
The file is used by both the game engine to know which maps
are installed and also by the maps-server to index map repositories
and make those maps available for download.

## map.yml location

The 'map.yml' should be placed at the top-most directory of a map.

```
my-map-folder/
  |- map.yml  <<<
  |+map/
   |- assets/
   |- flags/
   |- reliefTiles/
   |- units/
   |- polygons.txt
```

## YAML file caution

Be careful to not use tabs, SPACES ONLY! The indentation of the file
matters!

## map.yml contents

Here is an example:

```yaml
map_name: Name of My Map
version: 1
games:
  - name: Game Name (1)
    xml_path: games/XmlGameFile.xml
  - name: Game Name (2)
    xml_path: games/XmlGameFile2.xml
```

**map_name**: This is the name of your map, this is the name that will
appear in the 'download maps' window.

**version**: This is a simple number. It will trigger a notification
to users that they can update the map if it is incremented.

**games**: This is a list of each game that is in the map. There should
be one entry per game XML file.

**name**: This is the name of any game that is backed by an XML file. This
name will display when a user clicks 'select game', and this is the game name
that displays in the lobby.

**xml_path**: This is the path from the map.yml file to the game.xml file.
The game engine will find your XML file by starting at the 'map.ym' and then
following this path to find an XML file. This is important to get right!
Use forward slashes: '/', do not start the path with a slash. The path should
end with a '.xml' file. Typically this path will be something like:
`games/my-game.xml`

## Usages

### By Game engine

The game engine scans the `downloadedMaps` folder for 'map.yml' which then
tells it which maps are installed, where the game XML files are, their names
and which version of the map is installed.

### By Maps Server

The maps server will periodically scan map repositories for a 'map.yml' file.
Once found, the maps server will use the information in the 'map.yml' file
to index the map and make it available for download. Whenever a
user clicks 'download maps', that will send a request to the maps server
which will respond with a latest listing of maps and their latest version.

### Map Updates

Game engines know which map version are installed from the 'map.yml' file,
the maps server will scan the 'map.yml' file in repositories for a 'version'
value and store that in database. When the game engine checks for out-of-date
maps it will send a request to the maps server which will return a listing
of maps and their latest version. The game engine will compare this against
all installed maps and notify users to update any maps where there is a
newer version available.
