package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.util.IntegerMap;

/**
 * Used to describe a tech roll.
 * advance may be null if the game does not support rolling for
 * specific techs
 */
public class TechRoll {
  private final TechnologyFrontier tech;
  private final int rolls;
  private int newTokens;
  private final IntegerMap<PlayerID> whoPaysHowMuch;

  public TechRoll(final TechnologyFrontier advance, final int rolls) {
    this(advance, rolls, 0);
  }

  public TechRoll(final TechnologyFrontier advance, final int rolls, final int newTokens) {
    this(advance, rolls, newTokens, null);
  }

  public TechRoll(final TechnologyFrontier advance, final int rolls, final int newTokens,
      final IntegerMap<PlayerID> whoPaysHowMuch) {
    this.rolls = rolls;
    tech = advance;
    this.newTokens = newTokens;
    this.whoPaysHowMuch = whoPaysHowMuch;
  }

  public int getRolls() {
    return rolls;
  }

  public TechnologyFrontier getTech() {
    return tech;
  }

  public int getNewTokens() {
    return newTokens;
  }

  public void setNewTokens(final int tokens) {
    this.newTokens = tokens;
  }

  public IntegerMap<PlayerID> getWhoPaysHowMuch() {
    return whoPaysHowMuch;
  }
}
