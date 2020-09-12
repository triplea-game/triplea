A list of (case sensitive) changes, to upgrade a map from a TripleA version to another one.

## Upgrade from 1.8.0.9 to 1.9.0.0.3266

Note: The following list covers all basic upgrade cases for maps, but it may not upgrade "Property" options (playerProperty, unitProperty...) of already deprecated items.

Note: Before applying the changes below (assuming you do it with a simple replace), you should make sure to not have unnecessary spaces inside angle brackets (chevrons).

Note: Some items are not validated; thus, if your game plays on 1.9.0.0.3266, this doesn't necessarily mean that you correctly upgraded it (for more information, see the "warning" section).

### New XML settings:

Within every game file (XML), inside the "games" folder inside the main folder of the map, change all occurrences of the code before the colon with the code after the colon.

`<triplea minimumVersion="*"/>`:`<triplea minimumVersion="1.9.0.0"/>`

(The "*" means whatever)

### Not already deprecated XML changes:

_(these are the main changes on game file items that were not deprecated, so you normally need to change these only, within the game file)_

Within every game file (XML), inside the "games" folder inside the main folder of the map, change all occurrences of the code before the colon with the code after the colon.

`attatchment`:`attachment`

`Attatchment`:`Attachment`

`attatchTo=`:`attachTo=`

`="isImpassible"`:`="isImpassable"`

`="conditionType" value="XOR"`:`="conditionType" value="1"`

`="turns"`:`="rounds"`

`="Battleships repair at end of round"`:`="Units Repair Hits End Turn"`

`="Battleships repair at beginning of round"`:`="Units Repair Hits Start Turn"`

`="Naval Bombard Casualties Return Fire Restricted"`:`="Naval Bombard Casualties Return Fire"`

### Special code XML changes for the above changes:

_(these are cases of advanced coding, so you probably don't have these)_

Within every game file (XML), inside the "games" folder inside the main folder of the map, change all occurrences of the code before the colon with the code after the colon.

`value="conditionType" count="XOR"`:`value="conditionType" count="1"`

`value="conditionType" count="-reset-XOR"`:`value="conditionType" count="-reset-1"`

### Already deprecated but effective XML changes:

_(these items were already deprecated, so you shouldn't have had these in your games already)_

Within every game file (XML), inside the "games" folder inside the main folder of the map, change all occurrences of the code before the colon with the code after the colon.

`="isTwoHit" value="true"`:`="hitPoints" value="2"`

`="isTwoHit" value="false"`:`="hitPoints" value="1"`

`="occupiedTerrOf"`:`="originalOwner"`

`="victoryCity" value="true"`:`="victoryCity" value="1"`

`="victoryCity" value="false"`:`="victoryCity" value="0"`

### Already deprecated and ineffective XML deletions, for all the respective attachments:

_(these items were already deprecated and were also ineffective, so you shouldn't have had these in your games already and, if you did, they were doing nothing)_

Within every game file (XML), inside the "games" folder inside the main folder of the map, delete the whole `<option/>` containing the code.

`="isParatroop"`

`="isMechanized"`

`="takeUnitControl"`

`="giveUnitControl" value="true"`

`="giveUnitControl" value="false"`

### map.properties changes:

Within every "map" properties file, inside the main folder of the map, change all occurrences of the code before the colon with the code after the colon.

`color.Impassible=`:`color.Impassable=`

### sounds.properties changes:

Within every "sounds" properties file, inside the main folder of the map, change all occurrences of the code before the colon with the code after the colon.

`.wav`:`.mp3`

### Non-code changes:

Change all sounds inside the "sounds" main folder of the map from "wav" to "mp3" format.

---

_All that follows is supposed to be unnecessary additional information: If you correctly already made all that is described above, all that follows is irrelevant and can be ignored._

**Procedural alternatives:**

For a more defined change, instead of the aforementioned:

`attatchment`:`attachment`

you can change:

`attatchmentList`:`attachmentList`

`<attatchment`:`<attachment`

`attatchment>`:`attachment>`

`="games.strategy.triplea.attatchments`:`="games.strategy.triplea.attachments`

For a more defined change, instead of the aforementioned:

`Attatchment`:`Attachment`

you can change:

`="techAttatchment`:`="techAttachment`

`="unitAttatchment`:`="unitAttachment`

`="territoryAttatchment`:`="territoryAttachment`

`="canalAttatchment`:`="canalAttachment`

`="rulesAttatchment`:`="rulesAttachment`

`="playerAttatchment`:`="playerAttachment`

**Warning:**

The following xml codes, as being properties, are not validated (if you leave them, the game will play, but they will set nothing in the engine) (it is suggested you double check you don't have them anymore):

`="Battleships repair at end of round"`

`="Battleships repair at beginning of round"`

`="Naval Bombard Casualties Return Fire Restricted"`
