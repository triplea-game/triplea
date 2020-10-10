package games.strategy.triplea.delegate.battle.steps.fire;

import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import lombok.Data;

/** Tracks the state through a Fire round (dice roll, select casualties, remove casualties) */
@Data
public class FireRoundState {

  private DiceRoll dice;

  private CasualtyDetails casualties;
}
