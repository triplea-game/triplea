package games.strategy.triplea.delegate.battle.steps.fire;

import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import java.io.Serializable;
import lombok.Data;

/** Tracks the state through a Fire round (dice roll, select casualties, remove casualties) */
@Data
public class FireRoundState implements Serializable {
  private static final long serialVersionUID = -3888678807586940147L;

  private DiceRoll dice;

  private CasualtyDetails casualties;
}
