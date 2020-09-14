# Upgrading Legacy Map Options & Properties

A list of (case sensitive) changes to upgrade legacy TripleA Game XML properties and options
to the most recent.
 
When changes are required, all occurrences of the code before the `>>` must be substituted
with the code after the `>>`. In any code, the "\*" symbol means whatever (the change or deletion is
to be applied to every given code having whatever character or characters in place of the "\*").

## Upgrade from 1.8.0.9 to 1.9.0.0.3266

Note: The following list covers all basic upgrade cases for maps, but it may not upgrade "Property"
options (playerProperty, unitProperty...) of ineffective items you must delete (such changes have
been omitted from this document for brevity).

Note: Before applying the changes below (assuming you do it with a simple replace), you should make
sure to not have unnecessary spaces inside angle brackets (chevrons).

Note: Some items are not validated. Thus, if the game plays without errors using TripleA 1.9.0.0.3266,
this doesn't necessarily mean that you correctly upgraded it (for more information, see the "warning"
section).

### New game (XML) file settings:

`<triplea minimumVersion="*"/>`:`<triplea minimumVersion="1.9.0.0"/>`

### Changes of not already deprecated game (XML) file items:

_(these are the main changes on game file items that were not deprecated, so you normally need to change
these only, within the game file)_

Within every game (XML) file, inside the "games" folder inside the main folder of the map, change:

- `attatchment` >> `attachment`
- `Attatchment` >> `Attachment`
- `attatchTo=` >> `attachTo=`
- `="isImpassible"`>> `="isImpassable"`
- `="conditionType" value="XOR"` >> `="conditionType" value="1"`
- `="turns"` >> `="rounds"`
- `="Battleships repair at end of round"` >> `="Units Repair Hits End Turn"`
- `="Battleships repair at beginning of round"` >> `="Units Repair Hits Start Turn"`
- `="Naval Bombard Casualties Return Fire Restricted"` >> `="Naval Bombard Casualties Return Fire"`
- `="SBR Affects Unit Production"` >> `="Damage From Bombing Done To Units Instead Of Territories"`

### Changes of game (XML) file special code for the above items:

_(these are cases of advanced coding, so you probably don't have these within the game file)_

Within every game file (XML), inside the "games" folder inside the main folder of the map, change:

- `="conditionType" count="XOR"` >> `="conditionType" count="1"`
- `="conditionType" count="-reset-XOR"` >> `="conditionType" count="-reset-1"`

### Changes of already deprecated game (XML) file items:

_(these items were already deprecated, so you shouldn't have had these within the game file already)_

Within every game (XML) file, inside the "games" folder inside the main folder of the map, change:

- `="isTwoHit" value="true"` >> `="hitPoints" value="2"`
- `="isTwoHit" value="false"` >> `="hitPoints" value="1"`
- `="occupiedTerrOf"` >> `="originalOwner"`
- `="victoryCity" value="true"` >> `="victoryCity" value="1"`
- `="victoryCity" value="false"` >> `="victoryCity" value="0"`

### Changes of game (XML) file special code for the above items:

_(these are cases of advanced coding, so you probably don't have these within the game file)_

Within every game file (XML), inside the "games" folder inside the main folder of the map, change:

- `="isTwoHit" count="true"` >> `="hitPoints" count="2"`
- `="isTwoHit" count="-reset-true"` >> `="hitPoints" count="-reset-2"`
- `="isTwoHit" count="false"` >> `="hitPoints" count="1"`
- `="isTwoHit" count="-reset-false"` >> `="hitPoints" count="-reset-1"`
- `="victoryCity" count="true"` >> `="victoryCity" count="1"`
- `="victoryCity" count="-reset-true"` >> `="victoryCity" count="-reset-1"`
- `="victoryCity" count="false"` >> `="victoryCity" count="0"`
- `="victoryCity" count="-reset-false"` >> `="victoryCity" count="-reset-0"`

### Deletions

_(these items were already deprecated and did nothing)_

- `<option name="isParatroop"*/>`
- `<option name="isMechanized"*/>`
- `<option name="takeUnitControl"*/>`
- `<option name="giveUnitControl" value="true"/>`
- `<option name="giveUnitControl" value="false"/>`

### Changes of map (properties) file items:

Within every "map" properties file, inside the main folder of the map, change:

- `color.Impassible=` >> `color.Impassable=`

### Changes of sounds (properties) file items:

Within every "sounds" properties file, inside the main folder of the map, change:

- `.wav` >> `.mp3`

### Changes of non-code items:

Change all sounds inside the "sounds" main folder of the map from "wav" to "mp3" format.

---

_All that follows is supposed to be unnecessary additional information: If you correctly already made all that is
described above, all that follows is irrelevant and can be ignored._

**Procedural alternatives:**

For a more defined change, instead of the aforementioned:

- `attatchment` >> `attachment`

you can change:

- `attatchmentList` >> `attachmentList`
- `<attatchment` >> `<attachment`
- `attatchment>` >> `attachment>`
- `="games.strategy.triplea.attatchments` >> `="games.strategy.triplea.attachments`

For a more defined change, instead of the aforementioned:

- `Attatchment` >> `Attachment`

you can change:

- `="techAttatchment` >> `="techAttachment`
- `="unitAttatchment` >> `="unitAttachment`
- `="territoryAttatchment` >> `="territoryAttachment`
- `="canalAttatchment` >> `="canalAttachment`
- `="rulesAttatchment` >> `="rulesAttachment`
- `="playerAttatchment` >> `="playerAttachment`

**Warning:**

The following xml codes, as being properties, are not validated (if you leave them, the game will play, but they will
set nothing in the engine) (it is suggested you double check you don't have them within any game files, after
upgrading the map):

- `="Battleships repair at end of round"`
- `="Battleships repair at beginning of round"`
- `="Naval Bombard Casualties Return Fire Restricted"`
- `="SBR Affects Unit Production"`

While all options named "takeUnitControl" must be deleted, any option named "giveUnitControl" must be deleted only if
its value is "true" or "false".
