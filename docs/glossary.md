# Glossary

## Game (core) Code:
- **Node**: Playername plus IP address and port of a (human) player
- **PlayerName**: the display name of a human player
- **PlayerId**: (AKA Map XML Player) a playable side, a faction
- **PlayerChatId**: An identifier assigned to players when they join chat

## Map XML:
- **Player**: (AKA PlayerId) a playable side, a faction
- **Game**: (AKA Game XML) refers to a specific XML within a map
- **Map**: collection of files, eg: images, polygons text, properties, and at least one game XML

## Battle Terms:
- **Roll**: How many dice can the unit roll.
- **Strength**: The number on the dice that determines whether a hit was made. Any number less than or equal to the number is a hit.
- **Power**: A calculation using the roll and strength that is used for low luck battles and AI calculations. It is usually Strength multiplied by Roll.
- **Battle Round**: A battle round includes both sides firing at each other. A battle can consist of multiple rounds. During and at the end of each round, casualties are removed from the game and can not participate anymore.
- **Battle Phase**: A battle phase is subpart of a battle round where specific rules are in play that determine what the units can do. Some phases allow the units to retreat. Other phases allow the units to fire at each other.
- **General Combat Phase**: This phase is the default phase for units to fire. The attacker fires first and then the defender retaliates. Any casualties that the attacker hits can retaliate. At the end of the phase, all casualties are removed from the game or transformed into other units which do not participate in the battle.
- **First Strike Combat Phase**: This phase is for units to fire and remove casualties before the General Phase. Historically, this was used for submarines to sink sea based units as a "sneak attack". Now, any unit can participate during this phase if they have the `isFirstStrike` attribute. Units that fire during this phase will not participate during the General Phase. At the end of the phase, all casualties are removed from the game or transformed into other units which do not participate in the battle.
  - If an opposing side has an `isDestroyer` unit, this phase is skipped.
  - WW2V2 always has this phase. The casualties will not be removed at the end of the phase if an `isDestroyer` unit is present on the opposing side.
  - `Defending Subs Sneak Attack` game property controls whether the defender gets to fire in the phase.
- **Submarine Combat Phase**: Another name for **First Strike Phase** and generally used when only submarines have first strike capability.
- **AA Combat Phase**: This phase is for units with AA attributes (such as `attackAA` and `offensiveAttackAA`) to fire. The `typeAA` and `targetsAA` attributes determine what units are targeted during this attack. At the end of the phase, all casualties are removed from the game or transformed into other units which do not participate in the battle. Units that fire during this phase can fire during later phases if they have main battle strength and roll attributes.
- **Targeted Attack Combat Phase**: Another name for **AA Combat Phase** and generally used when the units are not anti-aircraft.
- **Retreat Phase**: This phase allows the attacker to retreat from battle. All units retreat together.
  - Amphibious assaults only allow retreating of planes if `Attacker Retreat Planes` property is set or land based attackers and airplanes if `Partial Amphibious Retreat` is set.
- **Evasion Phase**: This phase allows units with `canEvade` to retreat or submerge (determined by `Submersible Subs` property).
  - Defending units can only participate in this phase if `Submarines Defending May Submerge Or Retreat` property is set.
  - `Sub Retreat Before Battle` defines if this phase occurs before the First Strike Phase or at the end of the Battle Round.
- **Submerge Subs Phase**: Another name for **Evasion Phase** and generally used when only submarines have `canEvade` and `Submersible Subs` is set.
