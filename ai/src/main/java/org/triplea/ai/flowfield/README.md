# Algorithm Overview

At the start of each turn, certain territories (such as capitals, factories, etc) will be given a value. This value will then be diffused to its neighboring territories. The value and rate of diffusion can be different.

When a unit needs to decide where to go, it will pick the territory with the highest value.

Enemy units, combat, transport, etc will be mapped into the diffusion. A transport will allow values to be diffused across unpassable territories.

# Combat Calculation

Instead of using a combat simulator, a calculation similar to the Lanchester's laws (https://en.wikipedia.org/wiki/Lanchester's_laws) or Salvo Combat Model (https://en.wikipedia.org/wiki/Salvo_combat_model) will be used. The exponent (in Lanchester's laws) and firepower values will be determined through experimentation.

# Decision Making

The AI will simulate not just one turn but several rounds of play. It use a minimax algorithm to find the best action to perform. This will allow the AI to handle political and other user choices, as well as to build units that are non-standard.