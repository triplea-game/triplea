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
 * StrategicBombingRaidBattle.java
 *
 * Created on November 29, 2001, 2:21 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.DelegateBridge;

import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.formatter.Formatter;
import games.strategy.util.Match;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class StrategicBombingRaidBattle implements Battle
{
  private final static String RAID = "Strategic bombing raid";
  private final static String FIRE_AA = "Fire AA";

  private Territory m_battleSite;
  private List m_units = new ArrayList();
  private PlayerID m_defender;
  private PlayerID m_attacker;
  private GameData m_data;
  private BattleTracker m_tracker;

  /** Creates new StrategicBombingRaidBattle */
    public StrategicBombingRaidBattle(Territory territory, GameData data, PlayerID attacker, PlayerID defender, BattleTracker tracker)
  {
    m_battleSite = territory;
    m_data = data;
    m_attacker = attacker;
    m_defender = defender;
    m_tracker = tracker;
  }

  public boolean isEmpty()
  {
      return m_units.isEmpty();
  }

  public void removeAttack(Route route, Collection units)
  {
      m_units.removeAll(units);
  }

  public void addAttack(Route route, Collection units)
  {
    if(!Match.allMatch(units, Matches.UnitIsStrategicBomber))
      throw new IllegalArgumentException("Non bombers added to strategic bombing raid:" + units);

    m_units.addAll(units);

  }

  public void fight(DelegateBridge bridge)
  {
    bridge.getHistoryWriter().startEvent("Strategic bombing raid in " + m_battleSite);

    //sort according to least movement
    MoveDelegate moveDelegate = DelegateFinder.moveDelegate(m_data);
    moveDelegate.sortAccordingToMovementLeft(m_units, false);

    Collection defendingUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsAAOrFactory);

    BattleStartMessage startBattle = new BattleStartMessage(
                   m_attacker,
                   m_defender,
                   m_battleSite,
                   m_units,
                   defendingUnits,
                   null
                   );
    bridge.sendMessage(startBattle, m_attacker);
    bridge.sendMessage(startBattle, m_defender);

    CompositeMatch hasAAMatch = new CompositeMatchAnd();
    hasAAMatch.add(Matches.UnitIsAA);
    hasAAMatch.add(Matches.enemyUnit(m_attacker, m_data));

    boolean hasAA = m_battleSite.getUnits().someMatch(hasAAMatch);

    List steps = new ArrayList();
    if(hasAA)
      steps.add(FIRE_AA);
    steps.add(RAID);

    String title = "Bombing raid in " + m_battleSite.getName();

    BattleStepMessage listSteps = new BattleStepMessage((String) steps.get(0), title, steps, m_battleSite);
    bridge.sendMessage(listSteps, m_attacker);
    bridge.sendMessage(listSteps, m_defender);


    if(hasAA)
      fireAA(bridge);

    int cost = conductRaid(bridge, m_attacker, m_defender, m_battleSite);

    m_tracker.removeBattle(this);

    bridge.getHistoryWriter().addChildToEvent("AA raid costs + " + cost + Formatter.pluralize("ipc", cost));

    BattleEndMessage battleEnd = new BattleEndMessage("Bombing raid cost " + cost);
    bridge.sendMessage(battleEnd, m_attacker);
    bridge.sendMessage(battleEnd, m_defender);


  }

  private void fireAA(DelegateBridge bridge)
  {
    DiceRoll dice = DiceRoll.rollAA(m_units.size(),bridge, m_battleSite);
    removeAAHits(bridge, dice);
  }

  private void removeAAHits(DelegateBridge bridge, DiceRoll dice)
  {

    Collection casualties = new ArrayList(dice.getHits());
    for(int i = 0; i < dice.getHits() && i < m_units.size(); i++)
    {
      casualties.add(m_units.get(i));
    }

    if(casualties.size() != dice.getHits())
        throw new IllegalStateException("Wrong number of casualties");

    CasualtyNotificationMessage notify = new CasualtyNotificationMessage(FIRE_AA, casualties, null,null, m_attacker,  dice);
    //an awful hack, set all so that the ui will pause and display this roll
    //sorry, but im tired
    notify.setAll(true);
    bridge.sendMessage(notify, m_attacker);
    bridge.sendMessage(notify, m_defender);

    bridge.getHistoryWriter().addChildToEvent(Formatter.unitsToTextNoOwner(casualties) + " killed by aa guns", casualties);



    m_units.removeAll(casualties);
    Change remove = ChangeFactory.removeUnits(m_battleSite, casualties);
    bridge.addChange(remove);

  }

  /**
   *
   * @return how many ipcs the raid cost
   */
  private int conductRaid(DelegateBridge bridge, PlayerID attacker, PlayerID defender, Territory location)
  {
    int rollCount = BattleCalculator.getRolls(m_units, m_attacker, false);
    if(rollCount == 0)
        return 0;
    String annotation = attacker.getName() + " rolling to allocate ipc cost in strategic bombing raid against " + m_defender.getName() + " in " + location.getName();
    int[] dice = bridge.getRandom(Constants.MAX_DICE, rollCount, annotation);
    int cost = 0;

    for(int i = 0; i < dice.length; i++)
    {
      cost += 1 + dice[i];
    }

    BombingResults results = new BombingResults(dice, cost);

    bridge.sendMessage(results);
    bridge.sendMessage(results, m_defender);

    //get resources
    Resource ipcs = m_data.getResourceList().getResource(Constants.IPCS);
    int have = m_defender.getResources().getQuantity(ipcs);
    int toRemove = Math.min(cost, have);



    Change change = ChangeFactory.changeResourcesChange(m_defender, ipcs, -toRemove);
    bridge.addChange(change);

    return cost;
  }

  public boolean isBombingRun()
  {
    return true;
  }

  public void unitsLost(Battle battle, Collection units, DelegateBridge bridge)
  {
    //should never happen
    throw new IllegalStateException("say what, why you telling me that");
  }

  public int hashCode()
  {
    return m_battleSite.hashCode();
  }

  public boolean equals(Object o)
  {
    //2 battles are equal if they are both the same type (boming or not)
    //and occur on the same territory
    //equals in the sense that they should never occupy the same Set
    //if these conditions are met
    if(o == null || ! (o instanceof Battle))
      return false;

    Battle other = (Battle) o;
    return other.getTerritory().equals(this.m_battleSite) &&
      other.isBombingRun() == this.isBombingRun();
  }


  public Territory getTerritory()
  {
    return m_battleSite;
  }
}
