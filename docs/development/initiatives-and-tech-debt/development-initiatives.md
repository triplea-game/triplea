An ongoing list of major goals/initiatives for tripleA

## Efficiency And Ability to Release Code Updates
- general re-organization and re-modelling of the code so it is built in an OO and/or a functional way, and less procedural. This will allow us to be more efficient when working with the code.
- remove java RMI, means a new server to client communication technology
- remove java reflection, involves code re-structure
- remove serialization, involves removing the above java technologies as well as finding a new way to save game files.
- better map version compatibility

Once we have the above accomplished, game saves will be playable with any future versions, and game engine + lobby will be compatible with any version combination. Even games between players will be more flexible and will not always require the same game engine version.

With that done we will then be able to release more frequently, and with the better code structure we will be able to build new features more quickly as well (with less time spent figuring out why we broke something).
## Community
- In game challenge system.  -> #1072
- In game Ladder/ELO -> #1043
- Game internationalization (translations)
## AI
- Improve Performance on Large Maps
- Consider Purchasing 0 Movement Units (Static Defenses)
- Valuing Objectives
- Consider strafing and defending against strafing
- Consider V3 rule around subs not blocking naval movement
- Consider V3 rule where fighters don't defend against subs
- Rewrite bid purchase/place
- Consider victory city points
- Improve AA gun movement
- Defending capital against multi-turn amphib attacks
- Improve unit production consideration to always use TripleAUnit.getProductionPotentialOfTerritory()
- Add per map XML AI configuration
- Add purchase 'value' for resources besides PUs
- Transport mobile factories
- Consider air/naval bases strategic value and purchasing
- Consider value of saving at least 1 land unit when selecting casualties
- Consider capital farming when determining whether to take back an allied capital
- Add support for 'upgrading units' (consuming an existing units)
- Consider interceptions and escorts
- Air/land transport support
- Fuel movement support
## Maps
- Update/fix map content data, update map descriptions and fix up game notes. First focusing on core high quality maps including WaW (in-progress), NWO, TRS, all A&A maps, 270 BC, Napoleonic, etc.
- Better map version management? IE: easier to play save games when the map versions may have changed (this is more to do with the game engine than maps)
## Gameplay
- Efficiency improvements, purchase planning, streamlined UX (such as a 'fight all battles button', 'yes-to-all' buttons), battle calculator copy/paste and other utility features.
- Better info displays, NO's included in the stats, titles next to unit names
- Text zoom option, to increase the size of all text that is displayed

Interested to hear feedback on the list above, and if there is anything important that I left out of the list.
