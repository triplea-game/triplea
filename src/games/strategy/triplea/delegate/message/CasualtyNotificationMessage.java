package games.strategy.triplea.delegate.message;

import java.util.Collection;
import java.util.Map;
import games.strategy.engine.data.*;
import games.strategy.triplea.delegate.DiceRoll;
import java.util.*;

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
  private Collection m_killed;
  private Collection m_damaged;
  private Map m_dependents;
  private boolean m_autoCalculated = false;

  public CasualtyNotificationMessage(String step, Collection killed, Collection damaged, Map dependents, PlayerID player, DiceRoll dice)
  {
    super(step);
    m_killed = killed;
    m_damaged = damaged;
    if(m_damaged == null)
        m_damaged = Collections.EMPTY_LIST;
    m_player = player;
    m_dependents = dependents;
    m_dice = dice;
  }

  public Collection getKilled()
  {
    return m_killed;
  }

  public Collection getDamaged()
  {
      return m_damaged;
  }

  public boolean isEmpty()
  {
      return m_killed.isEmpty() && m_damaged.isEmpty();
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
   * Flag to indicate that all casualties were calculated by the computer
   */
  public boolean getAutoCalculated()
  {
    return m_autoCalculated;
  }

  public void setAutoCalculated(boolean aBool)
  {
    m_autoCalculated = aBool;
  }
}
