# Regenerating the map

The geometry, base tiles, unit counters and game XML are generated, not hand-authored. Edit the site
list in `MapGen.java`, the graph tables in `genxml.py`, or the counter drawing in `UnitGen.java`, then
run the steps in order:

```sh
java -Djava.awt.headless=true tools/MapGen.java <workdir>                                  # 1. geometry + adjacency
python tools/genxml.py <workdir>/adjacency.txt <workdir>/Small_Front_Meuse.xml             # 2. game XML
java -Djava.awt.headless=true tools/MapGen.java <workdir> <workdir>/Small_Front_Meuse.xml  # 3. tiles, now with roads
java -Djava.awt.headless=true tools/UnitGen.java <workdir>                                 # 4. NATO-style unit counters
python tools/manifest.py .                                                                  # 5. refresh manifest byte counts
python tools/validate_map.py
```

Then copy `<workdir>/map/*` and `<workdir>/preview.png` into place.

`MapGen` runs twice on purpose. The first pass has no XML to read, so it emits the Voronoi cells and
the candidate adjacency that `genxml.py` needs. The second pass reads the finished XML back and
draws the roads, objective markers and terrain from it, so none of those are duplicated between the
renderer and the game rules: a road only appears on the map if the XML declares it.

`genxml.py` drops the edges in its `DROP` set to create the forest wall and the river crossings, and
asserts every declared road is a real movement edge, so a typo in `ROADS` fails the build rather than
silently orphaning supply. The three supply-only cuts are declared by omitting Vielsalm-Durbuy,
Hotton-Marche and Libramont-Neufchateau from `ROADS`; those borders remain movement edges.
La Roche-Marche remains a supply road, giving the central plateau an indirect second axis without
restoring the removed Hotton-Marche shortcut.

`UnitGen` writes 48x48 transparent PNG counters for both players. Every counter uses a rectangular
unit frame and a battalion `II` echelon marker. Armour uses a horizontal oval, mechanized infantry
combines the infantry cross with tracked mobility, self-propelled artillery uses a horizontal track
oval, fighters use an infinity-shaped fixed-wing mark, and airfields use a marked runway. The XML
connects those airfields to TripleA's native radius-2 scramble and return-to-base rules.

## What draws what

TripleA fills every land polygon with the owner's colour at `POLYGONS_LEVEL`, opaquely, directly on
top of `BASE_MAP_LEVEL`. Anything a base tile draws inside a polygon is invisible in play. So:

| Layer | Holds | Why |
| --- | --- | --- |
| `baseTiles` | terrain fill, title | covered in play; still feeds the small map and the margin |
| `reliefTiles` | terrain texture, roads | `RELIEF_LEVEL` draws above the owner fill; must be translucent |
| `units/<player>` | generated NATO-style counters | engine draws the player-specific PNG for each unit type |
| `name_place.txt` | name positions | engine draws names at `TERRITORY_TEXT_LEVEL` |
| `vc.txt` + `misc/vc.png` | objective markers | engine draws at `VC_MARKER_LEVEL`; it ships no default marker |

`MapGen` writes `name_place.txt` because the engine otherwise centres each name in the territory's
bounding box, which lands under the unit stack. Names go 30px above the site and markers go left of
it, both clearing the `place.txt` unit box. Name x-offsets are measured in Arial Bold 12, the font
`MapImage.getPropertyMapFont` uses; changing the client's map font size shifts the centring slightly.

`preview.png` is documentation only. It composites the layers the way a reader wants to see them,
not the way the engine draws them, so it is not a check that the map renders correctly.
