/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * BattleDelegate.java
 *
 * Created on November 2, 2001, 12:26 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;
import java.io.Serializable;

import games.strategy.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;

import games.strategy.triplea.Constants;

import games.strategy.triplea.delegate.message.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class BattleDelegate implements SaveableDelegate
{

  private String m_name;
  private String m_displayName;
  private DelegateBridge m_bridge;
  private BattleTracker m_battleTracker = new BattleTracker();
  private OriginalOwnerTracker m_originalOwnerTracker = new OriginalOwnerTracker();

  //dont allow saving while handling a message
  private boolean m_inBattle;

  public void initialize(String name, String displayName)
  {
    m_name = name;
    m_displayName = displayName;
  }

  /**
   * Called before the delegate will run.
   */
  public void start(DelegateBridge aBridge, GameData gameData)
  {
    m_bridge = aBridge;
  }

  public String getName()
  {
    return m_name;
  }

  public String getDisplayName()
  {
    return m_displayName;
  }










  /**
   * A message from the given player.
   */
  public Message sendMessage(Message message)
  {
    if(message instanceof GetBattles)
      return getBattles();
    if(message instanceof FightBattleMessage)
    {

      m_inBattle = true;

      FightBattleMessage fightMessage = (FightBattleMessage) message;

      Territory territory = fightMessage.getTerritory();
      boolean bombing = fightMessage.getStrategicBombingRaid();
      Battle battle = m_battleTracker.getPendingBattle(territory, bombing);

      //does the battle exist
      if(battle == null)
        return new StringMessage("No battle in given territory", true);

      //are there battles that must occur first
      Collection allMustPrecede = m_battleTracker.getDependentOn(battle);
      if(!allMustPrecede.isEmpty())
      {
        Battle firstPrecede = (Battle) allMustPrecede.iterator().next();
        String name = firstPrecede.getTerritory().getName();
        String fightingWord = firstPrecede.isBombingRun() ? "Bombing Run" : "Battle";
        return new StringMessage("Must complete " +  fightingWord + " in " + name + " first", true);
      }

      //fight the battle
      battle.fight(m_bridge);

      m_inBattle = false;

      //and were done
      return new StringMessage("Battle fought");
    }
    else
      throw new IllegalArgumentException("Battle delegate received message of wrong type:" + message);
  }

  private BattleListingMessage getBattles()
  {
    Collection battles = m_battleTracker.getPendingBattleSites(false);
    Collection bombing = m_battleTracker.getPendingBattleSites(true);
    return new BattleListingMessage(battles, bombing);
  }

  /**
   * Called before the delegate will stop running.
   */
  public void end()
  {}

  public BattleTracker getBattleTracker()
  {
    return m_battleTracker;
  }

  public OriginalOwnerTracker getOriginalOwnerTracker()
  {
    return m_originalOwnerTracker;
  }


  /**
   * Can the delegate be saved at the current time.
   * @arg message, a String[] of size 1, hack to pass an error message back.
   */
  public boolean canSave(String[] message)
  {
    if(m_inBattle)
      message[0] = "Cant save while fighting a battle";
    return !m_inBattle;
  }

  /**
   * Returns the state of the Delegate.
   */
  public Serializable saveState()
  {
    BattleState state = new BattleState();
    state.m_battleTracker = m_battleTracker;
    state.m_originalOwnerTracker = m_originalOwnerTracker;
    return state;
  }


  /**
   * Loads the delegates state
   */
  public void loadState(Serializable aState)
  {
    BattleState state = (BattleState) aState;
    m_battleTracker = state.m_battleTracker;
    m_originalOwnerTracker = state.m_originalOwnerTracker;
  }
}

class BattleState implements Serializable
{
  public BattleTracker m_battleTracker = new BattleTracker();
  public OriginalOwnerTracker m_originalOwnerTracker = new OriginalOwnerTracker();
}
