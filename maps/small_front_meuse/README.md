# Small Front: Meuse Corridor

A 33-territory prototype for the custom Small Front TripleA engine. It is the successor to
`small_front_ardennes`, which was a 4x4 grid: every centre in that map sits at x = 230, 608, 988 or
1372, so every route across it was the same route, and there was nothing to choose. This map is
built to make the choice of *where* the load-bearing decision.

## Scope

- 33 land territories, 67 movement edges, degree 2 to 7 (the old map was 2 to 4)
- 2 players: Germans and Americans
- road-based supply and two-turn isolation attrition
- terrain stack capacity and terrain-specific battle round limits
- fixed reinforcements through round 7; no purchase or placement phase
- separate air and ground combat, independent air control
- radius-1 fog of war
- scored at the end of round 8

This is an operational prototype, not a geographically exact simulation.

## Shape

Territory shapes are Voronoi cells around hand-placed sites, so the map is irregular by
construction rather than a lattice with jitter. Six bands run east (the German staging area) to west
(the Meuse):

| Band | Count | Character |
| --- | --- | --- |
| A: German staging | 4 | supply sources, German capital at Prum |
| B: the forest wall | 3 | Forest terrain, the waist |
| C: road hubs | 5 | Town terrain |
| D: the plateau | 9 | open, the highest degrees on the map |
| E: approaches | 7 | open |
| F: Meuse crossings | 5 | Town terrain, American capital at Namur |

The narrow-wide-narrow rhythm is the point. Traffic from four staging territories funnels into three
forest territories, opens out across the nine-territory plateau where armour can manoeuvre, then
narrows again onto five river crossings.

### Two deliberate movement cuts

The Voronoi adjacency is not used as-is. Five candidate edges are dropped:

- **The forest wall has no lateral movement links.** Losheim Gap, Clervaux and Vianden do not connect
  to each other, so the three breakthroughs are three independent axes. Shifting between them means
  going back through the German rear, which costs turns. Under radius-1 fog the German has to commit
  to an axis without knowing where the American reserve is.
- **Each Meuse crossing keeps one approach.** The river is a barrier, not a field: Durbuy-Huy,
  Havelange-Andenne, Ciney-Namur, Wellin-Dinant, Beauraing-Givet. The west bank is connected along
  itself, so once across you can roll up the river, but getting across is a single door each.

### Supply road cuts

Roads are a strict subset of movement edges: 44 of 67. The borders Vielsalm-Durbuy, Hotton-Marche
and Libramont-Neufchateau remain legal movement edges, but no longer carry supply. La Roche-Marche
provides an indirect second route across the central plateau, so losing Hotton, Nassogne or
Neufchateau alone cannot divide the entire road network. A spearhead can still manoeuvre off-road,
and coordinated control of several road hubs can isolate a meaningful pocket without making one
central territory an automatic half-map cutoff.

## Air operations and armour

Fighters use TripleA's native airbase and scramble system rather than a scenario-specific interceptor
search. Prum, Bitburg, Ciney and Namur each contain a two-aircraft airfield; fighters may scramble up
to two movement edges into a pending battle and return to their originating base after combat. This
lets aircraft remain behind the front while still contesting nearby battles. Air control persists
until a later battle changes it, and surviving numerical superiority establishes control while equal
survivors leave it contested.

Armour rolls two attack dice at attack 2, retains one defense die at defense 3, costs two stack
capacity, and has TUV 7. The change concentrates its value in offensive shock without increasing its
defensive output.

## Victory scoring

Scored at the end of round 8, by `SmallFrontScoringAttachment` on each player.

- 1 point per objective held: St. Vith, Bastogne, Marche, Neufchateau, Namur, Dinant
- 2 additional German points for a supplied German land unit on any Meuse crossing
- 1 additional German point for supplied German occupation of Bastogne or Marche
- 2 additional American points if no supplied German land unit holds any Meuse crossing
- ties favour the Americans

The opening position scores Americans 8, Germans 0. Reaching the river is therefore decisive rather
than incremental: taking objectives alone does not get the Germans there, because every objective
taken is worth 2 of swing while the Meuse bonus pair is worth 4.

Set `Auto Termination` to false to play past round 8 with the rubric still tallied.

## Balance is unvalidated

The reinforcement schedule and the starting order of battle are first guesses. They have not been
played. The Pro AI cannot test them: this map, like its predecessor, has no economy, so
`TuvCostsCalculator` returns an empty cost map, every unit is worth 0 to the AI, no attack ever
scores as profitable, and the AI stands still for eight rounds. An all-AI game therefore always
reports Americans 8, Germans 0 and proves only that the map runs.

## Installation

Unzip the top-level `small_front_meuse` folder into the TripleA downloaded-maps directory. Requires
the custom Small Front engine build; see `../small_front_ardennes/XML_FEATURE_GUIDE.md` for the XML
features both maps use.
