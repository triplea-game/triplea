# Feature Backlog
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
- Improve USA purchases on Great War
- Improve unit production consideration to always use TripleAUnit.getProductionPotentialOfTerritory()
- Add per map XML AI configuration
- Add purchase 'value' for resources besides PUs
- Transport mobile factories
- Consider air/naval bases strategic value and purchasing
- Consider value of saving at least 1 land unit when selecting casualties
- Consider capital farming when determining whether to take back an allied capital
- Add support for 'upgrading units' (consuming an existing units)
- Consider interceptions and escorts

# Overview

## Summary
- Mostly based on trying to optimize TUV gains and minimize TUV loses based on the battle calculator.
- It does some strategic consideration in regards to where to move units towards and whether to be defensive if threatened.
- It performs pretty well on maps based on revised/V3/V1 rules in that order
- Should not crash on any map that is considered stable

## Combat Move
- Check if capital is threatened and if so enable defensive stance (tends to attack less)
- Find max number of my units that can attack each territory
- Prioritize attack territories based on value
- Loop through prioritized attack territories and see how many I can actually attack
- Find max enemy counter attackers for each attack territory
- Determine which attack territories can possibly be held
- Remove any attack territories that aren't worth attacking (neutrals, low value, etc)
- Determine which units to attack each attack territory with
- Determine final sea territories for transports making amphibious assaults
- Determine if I can defend those transports and if not then possibly abort that attack
- Determine if I can hold my capital after attacks and if not then slowly remove attack territories
- Actually send all combat moves to delegate to update game data

## Non-Combat Move
- Find list of all allied territories that I can move units to and how many I can move to each one
- Find units in move territories that can't move (allied, no movement, etc)
- Separate out non-combat infra units (mobile factories, AA guns, etc)
- Try to move at least 1 defender to each territory to block blitz attacks
- Find max enemy attackers for each move territory
- Determine which move territories can possibly be held
- Prioritize territories to try to defend
- Determine which units to defend each prioritized territory or if it can't be held
- Determine strategic value for each move territory based mostly on distance to valuable enemy territories
- Determine where to move remaining units to safe territory with highest strategic value
- Determine were to move non-combat infra units
- Actually send all non-combat moves to delegate to update game data

## Purchase
- ***Simulate all phases until place is reached***
- Find all purchase options
- Find all territories that units can be placed and how many
- Find max enemy attackers for each place territory
- Prioritize place territories that need defended
- Determine best defender and purchase necessary amount for each prioritized territory
- Determine strategic value for each place territory based mostly on distance to valuable enemy territories
- Prioritize land place territories by strategic value
- Determine whether to purchase AA for any land place territories
- Purchase best land units combinations for prioritized land territories (based on available purchase options and enemy distance)
- Purchase land factory if any 'good' candidates
- Prioritize sea place territories based on strategic value
- Purchase naval defenders, transports, and amphib units for prioritized sea territories
- Purchase high attack and high movement (preferrably air) with any remaining production
- Replace purchased land units with more expensive units with any remaining PUs
- Purchase factory if any 'decent' candidates with any remaining PUs
- Actually send purchase map to delegate to update game data and save map of where to place them

## Place
- Place units based on purchase phase result or if no purchase map then try to place in best possible territories

## Code Organization
- games.strategy.triplea.ai.proAI.ProAI - main AI class which extends AbstractAI and overrides many of the methods, orchestrates the 'Hard AI'
- games.strategy.triplea.ai.proAI - has core AI phases and data containers
- games.strategy.triplea.ai.proAI.logging - has logging utils and UI
- games.strategy.triplea.ai.proAI.simulate - has simulate utils used to simulate combat move, battle, and non-combat move for purchases
- games.strategy.triplea.ai.proAI.util - lots of utilities, matches, etc

