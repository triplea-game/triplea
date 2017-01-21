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
  private final TechnologyFrontier m_tech;
  private final int m_rolls;
  private int m_newTokens;
  private final IntegerMap<PlayerID> m_whoPaysHowMuch;

  public TechRoll(final TechnologyFrontier advance, final int rolls) {
    this(advance, rolls, 0);
  }

  public TechRoll(final TechnologyFrontier advance, final int rolls, final int newTokens) {
    this(advance, rolls, newTokens, null);
  }

  public TechRoll(final TechnologyFrontier advance, final int rolls, final int newTokens,
      final IntegerMap<PlayerID> whoPaysHowMuch) {
    m_rolls = rolls;
    m_tech = advance;
    m_newTokens = newTokens;
    m_whoPaysHowMuch = whoPaysHowMuch;
  }

  public int getRolls() {
    return m_rolls;
  }

  public TechnologyFrontier getTech() {
    return m_tech;
  }

  public int getNewTokens() {
    return m_newTokens;
  }

  public void setNewTokens(final int tokens) {
    this.m_newTokens = tokens;
  }

  public IntegerMap<PlayerID> getWhoPaysHowMuch() {
    return m_whoPaysHowMuch;
  }
}
