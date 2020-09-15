# How To Upgrade Legacy Map Features

A list of changes to upgrade legacy TripleA Game XML properties and options to the most recent.
 
Note: Before applying the changes below (assuming you do it with a simple replace), you should make
sure to not have unnecessary spaces inside angle brackets (chevrons).

Note: Some items are not validated. Thus, if the game plays without errors using a latest,
this doesn't necessarily mean that you correctly upgraded it (for more information, see the "warning"
section).


## XML Changes

These changes need to happen in your game XML files.

### New game (XML) file settings:

`<triplea minimumVersion="*"/>`:`<triplea minimumVersion="1.9.0.0"/>`

### Changes:

All occurrences of the code before the `>>` must be substituted with the code after the `>>`.

- `attatchment` >> `attachment`
- `Attatchment` >> `Attachment`
- `attatchTo=` >> `attachTo=`
- `="isImpassible"` >> `="isImpassable"`
- `="conditionType" value="XOR"` >> `="conditionType" value="1"`
- `="turns"` >> `="rounds"`
- `="Battleships repair at end of round"` >> `="Units Repair Hits End Turn"`
- `="Battleships repair at beginning of round"` >> `="Units Repair Hits Start Turn"`
- `="Naval Bombard Casualties Return Fire Restricted"` >> `="Naval Bombard Casualties Return Fire"`
- `="SBR Affects Unit Production"` >> `="Damage From Bombing Done To Units Instead Of Territories"`
- `="conditionType" count="XOR"` >> `="conditionType" count="1"`
- `="conditionType" count="-reset-XOR"` >> `="conditionType" count="-reset-1"`
- `="isTwoHit" count="true"` >> `="hitPoints" count="2"`
- `="isTwoHit" value="false"` >> `="hitPoints" value="1"`
- `="isTwoHit" count="-reset-true"` >> `="hitPoints" count="-reset-2"`
- `="isTwoHit" count="false"` >> `="hitPoints" count="1"`
- `="isTwoHit" count="-reset-false"` >> `="hitPoints" count="-reset-1"`
- `="occupiedTerrOf"` >> `="originalOwner"`
- `="victoryCity" count="true"` >> `="victoryCity" count="1"`
- `="victoryCity" count="-reset-true"` >> `="victoryCity" count="-reset-1"`
- `="victoryCity" count="false"` >> `="victoryCity" count="0"`
- `="victoryCity" count="-reset-false"` >> `="victoryCity" count="-reset-0"`


### **Procedural alternatives:**

For a more defined change, instead of the aforementioned:

- `attatchment` >> `attachment`

you can change:

- `attatchmentList` >> `attachmentList`
- `<attatchment` >> `<attachment`
- `attatchment>` >> `attachment>`
- `="games.strategy.triplea.attatchments` >> `="games.strategy.triplea.attachments`

- `="techAttatchment` >> `="techAttachment`
- `="unitAttatchment` >> `="unitAttachment`
- `="territoryAttatchment` >> `="territoryAttachment`
- `="canalAttatchment` >> `="canalAttachment`
- `="rulesAttatchment` >> `="rulesAttachment`
- `="playerAttatchment` >> `="playerAttachment`


### XML Deletions

_(these items were already deprecated and did nothing)_

- `<option name="isParatroop"*/>`
- `<option name="isMechanized"*/>`
- `<option name="takeUnitControl"*/>`
- `<option name="giveUnitControl" value="true"/>`
- `<option name="giveUnitControl" value="false"/>`

## map.properties Changes

Within every "map" properties file, inside the main folder of the map, change:

- `color.Impassible=` >> `color.Impassable=`

## Sound file changes

Change all sounds inside the "sounds" main folder of the map from "wav" to "mp3" format.
128Kbps and 168Kbps are the preferred bit rates.


## Warnings

The following xml codes, as being properties, are not validated (if you leave them, the game will play,
but they will set nothing in the engine) (it is suggested you double check you don't have them within
any game files, after upgrading the map):

- `="Battleships repair at end of round"`
- `="Battleships repair at beginning of round"`
- `="Naval Bombard Casualties Return Fire Restricted"`
- `="SBR Affects Unit Production"`

While all options named "takeUnitControl" must be deleted, any option named "giveUnitControl" must be
deleted only if its value is "true" or "false".
