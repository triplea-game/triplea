package games.strategy.triplea.delegate.message;

import java.util.List;
import games.strategy.engine.data.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class TechResultsMessage implements games.strategy.engine.message.Message
{

  private int[] m_rolls;
  private int m_hits;
  //a list of Strings
  private List m_advances;
  private PlayerID m_playerID;

  /**
   *
   * @param advances - a List of Strings
   */
  public TechResultsMessage(int[] rolls, int hits, List advances, PlayerID id)
  {
    m_rolls = rolls;
    m_hits = hits;
    m_advances = advances;
    m_playerID = id;
  }

  public int getHits()
  {
    return m_hits;
  }

  public PlayerID getPlayer()
  {
      return m_playerID;
  }

  public int[] getRolls()
  {
    return m_rolls;
  }

  /**
   *
   * @return a List of Strings
   */
  public List getAdvances()
  {
    return m_advances;
  }
}