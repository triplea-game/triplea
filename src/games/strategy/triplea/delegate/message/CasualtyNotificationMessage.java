package games.strategy.triplea.delegate.message;

import java.util.Collection;
import java.util.Map;
import games.strategy.engine.data.*;
import games.strategy.triplea.delegate.DiceRoll;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class CasualtyNotificationMessage extends BattleMessage
{

  private DiceRoll m_dice;
  private PlayerID m_player;
  private Collection m_units;
  private Map m_dependents;
  private boolean m_all = false;

  public CasualtyNotificationMessage(String step, Collection units, Map dependents, PlayerID player, DiceRoll dice)
  {
    super(step);
    m_units = units;
    m_player = player;
    m_dependents = dependents;
    m_dice = dice;
  }

  public Collection getUnits()
  {
    return m_units;
  }

  /**
   * The player who lost the units
   */
  public PlayerID getPlayer()
  {
    return m_player;
  }

  public Map getDependents()
  {
    return m_dependents;
  }

  public DiceRoll getDice()
  {
   return m_dice;
  }

  /**
   * Flag to indicate that all of the players units have died.
   */
  public boolean getAll()
  {
    return m_all;
  }

  public void setAll(boolean aBool)
  {
    m_all = aBool;
  }
}