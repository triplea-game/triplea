A list of (case sensitive) changes, to upgrade a map from a TripleA version to another one.

## Upgrade from 1.8.0.9 to 1.9.0.0.3266
(old:new)
Note: The following list covers all basic upgrade cases for maps, but it may not upgrade "Property" options (playerProperty, unitProperty...) of already deprecated items.
Note: Before applying the changes below (assuming you do it with a simple replace), you should make sure to not have unnecessary spaces inside angle brackets (chevrons).
Note: Some items are not validated; thus, if your game plays on 1.9.0.0.3266, this doesn't necessarily mean that you correctly upgraded it.

### Set:

`<triplea minimumVersion="1.9.0.0"/>`

### Not already deprecated xml changes:
_(you normally need to change these only)_

- `attatchment`:`attachment`
- `Attatchment`:`Attachment`
- `attatchTo=`:`attachTo=`
- `="isImpassible"`:`="isImpassable"`
- `="conditionType" value="XOR"`:`="conditionType" value="1"`
- `="turns":="rounds"`
- `="Battleships repair at end of round"`:`="Units Repair Hits End Turn"`
- `="Battleships repair at beginning of round"`:`="Units Repair Hits Start Turn"`
- `="Naval Bombard Casualties Return Fire Restricted"`:`="Naval Bombard Casualties Return Fire"`

### Special code xml changes for the above changes:
_(you probably don't have these)_

- `value="conditionType" count="XOR"`:`value="conditionType" count="1"`
- `value="conditionType" count="-reset-XOR"`:`value="conditionType" count="-reset-1"`

### Already deprecated but effective xml changes:
_(you shouldn't have had these in your games already)_

- `="isTwoHit" value="true"`:`="hitPoints" value="2"`
- `="isTwoHit" value="false"`:`="hitPoints" value="1"`
- `="occupiedTerrOf"`:`="originalOwner"`
- `="victoryCity" value="true"`:`="victoryCity" value="1"`
- `="victoryCity" value="false"`:`="victoryCity" value="0"`

### map.properties changes:

- `color.Impassible=`:`color.Impassable=`

### sounds.properties changes:

- `.wav`:`.mp3`

### Non-code changes:

Change all sounds inside "/sounds" from "wav" to "mp3" format.

**Procedural alternatives:**

For a more defined change, instead of the aforementioned:
- `attatchment`:`attachment`
you can change:
- `attatchmentList`:`attachmentList`
- `<attatchment`:`<attachment`
- `attatchment>`:`attachment>`
- `="games.strategy.triplea.attatchments`:`="games.strategy.triplea.attachments`

For a more defined change, instead of the aforementioned:
- `Attatchment`:`Attachment`
you can change:
- `="techAttatchment`:`="techAttachment`
- `="unitAttatchment`:`="unitAttachment`
- `="territoryAttatchment`:`="territoryAttachment`
- `="canalAttatchment`:`="canalAttachment`
- `="rulesAttatchment`:`="rulesAttachment`
- `="playerAttatchment`:`="playerAttachment`

**Note:**

The following xml codes, as being properties, are not validated (if you leave them, the game will play, but they will set nothing in the engine) (it is suggested you double check you don't have them anymore):
- `="Battleships repair at end of round"`
- `="Battleships repair at beginning of round"`
- `="Naval Bombard Casualties Return Fire Restricted"`
- `="isParatroop"`:*
- `="isMechanized"`:*
- `="takeUnitControl"`:*
- `="giveUnitControl" value="true"`:*
- `="giveUnitControl" value="false"`:*
*Delete the whole `<option/>`
