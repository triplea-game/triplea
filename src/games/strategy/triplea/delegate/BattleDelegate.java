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
    if(DelegateFinder.techDelegate(gameData).getTechTracker().hasRocket(aBridge.getPlayerID()))
      fireRockets(aBridge, gameData, aBridge.getPlayerID());
  }

  public String getName()
  {
    return m_name;
  }

  public String getDisplayName()
  {
    return m_displayName;
  }


  private void fireRockets(DelegateBridge bridge, GameData data, PlayerID player)
  {
    Collection targets = getTargetsWithinRange(data, player);
    if(targets.isEmpty())
    {
      bridge.sendMessage(new StringMessage("No targets to attack with rockets"));
      return;
    }

    PlayerID attacked = getTarget(targets, player, bridge);
    if(attacked != null)
      fireRocket(player, attacked, bridge, data);
  }

  private Collection getTargetsWithinRange(GameData data, PlayerID player)
  {
    Set targets = new HashSet();

    CompositeMatch ownedAA = new CompositeMatchAnd();
    ownedAA.add(Matches.UnitIsAA);
    ownedAA.add(Matches.unitIsOwnedBy(player));

    Iterator iter = data.getMap().iterator();
    while(iter.hasNext())
    {
      Territory current = (Territory) iter.next();
      if(current.isWater())
        continue;

      if(current.getUnits().someMatch(ownedAA))
        targets.addAll(getTargetsWithinRange(current, data, player));
    }
    return targets;
  }

  private Collection getTargetsWithinRange(Territory territory, GameData data, PlayerID player)
  {
    Collection possible = data.getMap().getNeighbors(territory, 3);

    CompositeMatch enemyFactory = new CompositeMatchAnd();
    enemyFactory.add(Matches.UnitIsFactory);
    enemyFactory.add(Matches.enemyUnit(player, data));

    Collection hasFactory = new ArrayList();

    Iterator iter = possible.iterator();
    while(iter.hasNext())
    {
      Territory current = (Territory) iter.next();
      if(current.getUnits().someMatch(enemyFactory))
        hasFactory.add(current);
    }
    return hasFactory;
  }

  private PlayerID getTarget(Collection targets, PlayerID player, DelegateBridge bridge)
  {
    Message response = bridge.sendMessage(new RocketAttackQuery(targets), player);
    if(!(response instanceof TerritoryMessage))
      throw new IllegalStateException("Message of wrong type:" + response);
    TerritoryMessage territoryMessage = (TerritoryMessage) response;
    if(territoryMessage.getTerritory() == null)
      return null;
    return territoryMessage.getTerritory().getOwner();
  }

  private void fireRocket(PlayerID player, PlayerID attacked,  DelegateBridge bridge, GameData data)
  {
    Resource ipcs = data.getResourceList().getResource(Constants.IPCS);
    //int cost = bridge.getRandom(Constants.MAX_DICE);

    int cost = bridge.getRandom(Constants.MAX_DICE, "Rocket fired by " + player.getName() + " at " + attacked.getName());

    //account for 0 base
    cost++;

    // Trying to remove more IPCs than the victim has is A Bad Thing[tm]
    int availForRemoval = attacked.getResources().getQuantity(ipcs);
    if (cost > availForRemoval)
      cost = availForRemoval;

    bridge.sendMessage(new StringMessage("Rocket attack costs:" + cost));
    Change rocketCharge = ChangeFactory.changeResourcesChange(attacked, ipcs, -cost);
    bridge.addChange(rocketCharge);

    String transcriptText = attacked.getName() + " lost " + cost + " ipcs to rocket attack by " + player.getName() ;
    bridge.getTranscript().write(transcriptText);
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
