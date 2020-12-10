# Areas of Need

This page is a list of items that are needed from a code/development perspective. Any efforts towards these is really appreciated and encouraged. To help coordinate efforts, feel free to open an issue to explain the projected contribution/effort and to discuss the intended project. If efforts are sustained, we can create a github project to be an umbrella for the intended efforts.

* **Better/More Tests**
  - improve test coverage
  - additions to smoke test
    - launch a game with AI players, run through a number of rounds and verify no errors
    - download a map, verify it is installed
    - load a save game from the latest version, verify still compatible
    - verify a client of the last version can connect to a latest host, and vice versa
* **Incorporation of Debian modifications into the main codebase**
The Debian distribution of TripleA has some key modifications that are made before the TripleA release version can be bundled with Debian. We want for these modifications to instead to be incorporated to the extent possible into the main TripleA codebase to allow the Debian distribution to be a simple copy of the main TripleA code without any modifications.
* **Improved test coverage throughout the code base**
* **Fixing Mac OS specific problems**
* **Fixing any of the bugs in the issues queue**
* **Greater network code compatibility**
We would like a system where network compatibility is easy to identify and where we can have more options for new game versions to be backward compatible with previous game versions.
* **Design and implementation for a new save-game format**
Instead of the save game being a serialization of java objects, we wish to have the save game be done in a plain-text format. The goal is that we could update java code without breaking save-game loading. Today if any serialization aspects of `GameData.java` are modified, then suddenly save games can no longer be loaded. This creates problems in development where we need to avoid such modifications, or sometimes we unexpectedly and undesirably break save-games.
* **Save Game Manager**
Build a UI to present a thumbnail save-game list, with information on who was playing the game, the last round played, which map, the current move phase. Should probably have a vaguely similar feel to the download maps manager, but be for save games.
* **Modularity and testing of game rules**
Most game rules are embedded logic very much directly where-ever they are relevant. We instead want the game rules to be more configuration driven. Essentially when a game starts, you could select "V1" or "V3" rules for example, or you could drill down into specific rules and turn them on/off individually before starting a game. For this we need the rules to become modular, for game code that accesses these rules to be calling out to a game rule strategy, or module, and for the rules to be well tested within themselves.
* **Improvements to map making tools and bringing them in-game**
The map making tools are not ideal in a number ways, a bit clunky. We're looking for these tools to be brought into the game itself with the goal of maps being an upload process from a file menu option. The idea is a player could enter a 'map-edit' mode, change parameters and the map itself from within the game, save it as a new map, then upload the map.

