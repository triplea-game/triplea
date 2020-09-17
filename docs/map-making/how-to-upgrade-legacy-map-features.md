# Map Features Change Log

A list of changes to map files that have occurred over time.
Caution, this list might not be complete, please let us know if there is anything missing.
 

## XML Changes

- `<option name="isMechanized"` >> `<optiona name="isLandTransportable"`
- `<option name="isParatroop"` >> `<option name="isAirTransportable"`
- `="Battleships repair at beginning of round"` >> `="Units Repair Hits Start Turn"`
- `="Battleships repair at end of round"` >> `="Units Repair Hits End Turn"`
- `="conditionType" count="-reset-XOR"` >> `="conditionType" count="-reset-1"`
- `="conditionType" count="XOR"` >> `="conditionType" count="1"`
- `="conditionType" value="XOR"` >> `="conditionType" value="1"`
- `="isImpassible"` >> `="isImpassable"`
- `="isTwoHit" count="-reset-false"` >> `="hitPoints" count="-reset-1"`
- `="isTwoHit" count="-reset-true"` >> `="hitPoints" count="-reset-2"`
- `="isTwoHit" count="false"` >> `="hitPoints" count="1"`
- `="isTwoHit" count="true"` >> `="hitPoints" count="2"`
- `="isTwoHit" value="false"` >> `="hitPoints" count="1"`
- `="isTwoHit" value="true"` >> `="hitPoints" value="2"`
- `="Naval Bombard Casualties Return Fire Restricted"` >> `="Naval Bombard Casualties Return Fire"`
- `="occupiedTerrOf"` >> `="originalOwner"`
- `="SBR Affects Unit Production"` >> `="Damage From Bombing Done To Units Instead Of Territories"`
- `="turns"` >> `="rounds"`
- `="victoryCity" count="-reset-false"` >> `="victoryCity" count="-reset-0"`
- `="victoryCity" count="-reset-true"` >> `="victoryCity" count="-reset-1"`
- `="victoryCity" count="false"` >> `="victoryCity" count="0"`
- `="victoryCity" count="true"` >> `="victoryCity" count="1"`
- `="victoryCity" value="false"` >> `="victoryCity" count="0"`
- `="victoryCity" value="true"` >> `="victoryCity" count="1"`
- `attatchment` >> `attachment`
- `Attatchment` >> `Attachment`
- `attatchTo=` >> `attachTo=`


```
<property name="..." value="...">
   <number min="..." max="..."/>
</property>

>>

<property name="..." value="..." min="..." max="..."/>
```

### XML Deletions

Delete these options entirely:

- `<option name="takeUnitControl"`
- `<option name="giveUnitControl"`


Delete these tags (found under `<property>`)

- `<boolean/>`
- `<string/>`



## map.properties changes

- `color.Impassible=` >> `color.Impassable=`

## sound.properties and sound file changes

- .wav files need to be re-encoded as mp3 (128 or 168Kbps are preferred bit rates)
- all file names in sound.properties need to be updated to match the new '.mp3' suffix
  on sound files.

