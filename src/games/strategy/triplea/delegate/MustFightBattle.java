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
 * Battle.java
 *
 * Created on November 15, 2001, 12:39 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.delegate.DelegateBridge;

import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.formatter.Formatter;
import games.strategy.triplea.attatchments.*;

/**
 *
 * Handles logic for battles in which fighting actually occurs.
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Represents a battle.
 */
public class MustFightBattle implements Battle, BattleStepStrings
{
  private final Territory m_battleSite;

  //maps Territory-> units
  //stores a collection of who is attacking from where, needed
  //for undoing moves
  private Map m_attackingFromMap = new HashMap();
  private List m_attackingUnits = new LinkedList();
  private Collection m_attackingWaitingToDie = new ArrayList();
  private Set m_attackingFrom = new HashSet();
  private Collection m_amphibiousAttackFrom = new ArrayList();
  private Collection m_amphibiousLandAttackers = new ArrayList();
  private Collection m_defendingUnits = new LinkedList();
  private Collection m_defendingWaitingToDie = new ArrayList();
  private boolean m_amphibious = false;
  private boolean m_over = false;
  private BattleTracker m_tracker;

  private TransportTracker m_transportTracker;

  private PlayerID m_defender;
  private PlayerID m_attacker;

  private GameData m_data;

  //dependent units
  //maps unit -> Collection of units
  //if unit is lost in a battle we are dependent on
  //then we lose the corresponding collection of units
  private Map m_dependentUnits = new HashMap();

  public MustFightBattle(Territory battleSite, PlayerID attacker, GameData data, BattleTracker tracker, TransportTracker transportTracker)
  {
    m_data = data;
    m_tracker = tracker;
    m_battleSite = battleSite;
    m_attacker = attacker;
    m_transportTracker = transportTracker;

    m_defendingUnits.addAll(m_battleSite.getUnits().getMatches(Matches.enemyUnit(attacker, data)));
    m_defender = findDefender(battleSite);
  }

  public void removeAttack(Route route, Collection units)
  {
    //remove all the units from this battle.
    m_attackingUnits.removeAll(units);
    Territory attackingFrom = getAttackFrom(route);

    Collection attackingFromMapUnits = (Collection) m_attackingFromMap.get(attackingFrom);
    attackingFromMapUnits.removeAll(units);
    if (attackingFromMapUnits.isEmpty())
    {
      m_attackingFrom.remove(attackingFrom);
    }

    //deal with amphibious assaults
    if (attackingFrom.isWater())
    {
      //if none of the units is a land unit, the attack from
      //that territory is no longer an amphibious assault
      if (Match.noneMatch(attackingFromMapUnits, Matches.UnitIsLand))
      {
        m_amphibiousAttackFrom.remove(attackingFrom);
        //do we have any amphibious attacks left?
        m_amphibious = !m_amphibiousAttackFrom.isEmpty();
      }
    }

    Iterator transports = m_dependentUnits.keySet().iterator();
    while (transports.hasNext())
    {
      Object transport = transports.next();
      Collection dependent = (Collection) m_dependentUnits.get(transport);
      dependent.removeAll(units);

    }

  }

  public boolean isEmpty()
  {
    return m_attackingUnits.isEmpty();
  }

  public void addAttack(Route route, Collection units)
  {
    Territory attackingFrom = getAttackFrom(route);
    m_attackingFrom.add(attackingFrom);
    m_attackingUnits.addAll(units);

    if (m_attackingFromMap.get(attackingFrom) == null)
    {
      m_attackingFromMap.put(attackingFrom, new ArrayList());
    }
    {
      Collection attackingFromMapUnits = (Collection) m_attackingFromMap.get(attackingFrom);
      attackingFromMapUnits.addAll(units);
    }

    //are we amphibious
    if (route.getStart().isWater() &&
        !route.getEnd().isWater() &&
        Match.someMatch(units, Matches.UnitIsLand))
    {
      m_amphibiousAttackFrom.add(getAttackFrom(route));
      m_amphibiousLandAttackers.addAll(Match.getMatches(units, Matches.UnitIsLand));
      m_amphibious = true;
    }

    //mark units with no movement
    //for all but air
    Collection nonAir = Match.getMatches(units, Matches.UnitIsNotAir);
    DelegateFinder.moveDelegate(m_data).markNoMovement(nonAir);

    //dependencies
    Map dependencies = transporting(units);
    Iterator iter = dependencies.keySet().iterator();
    while (iter.hasNext())
    {
      Unit transport = (Unit) iter.next();
      Collection transporting = (Collection) dependencies.get(transport);
      if (m_dependentUnits.get(transport) != null)
        ( (Collection) m_dependentUnits.get(transport)).addAll(transporting);
      else
        m_dependentUnits.put(transport, transporting);
    }
  }

  private Territory getAttackFrom(Route route)
  {
    int routeSize = route.getLength();
    Territory attackFrom;
    if (routeSize == 1)
      attackFrom = route.getStart();
    else
      attackFrom = route.at(routeSize - 2);
    return attackFrom;
  }

  private String getBattleTitle()
  {
    return m_attacker.getName() + " attacks " + m_defender.getName() + " in " + m_battleSite.getName();
  }

  private PlayerID findDefender(Territory battleSite)
  {
    if (!battleSite.isWater())
      return battleSite.getOwner();
    //if water find the defender based on who has the most units in the territory
    IntegerMap players = battleSite.getUnits().getPlayerUnitCounts();
    int max = -1;
    PlayerID defender = null;
    Iterator iter = players.keySet().iterator();
    while (iter.hasNext())
    {
      PlayerID current = (PlayerID) iter.next();
      if (m_data.getAllianceTracker().isAllied(m_attacker, current) || current.equals(m_attacker))
        continue;
      int count = players.getInt(current);
      if (count > max)
      {
        max = count;
        defender = current;
      }
    }
    if (max == -1)
      throw new IllegalStateException("No defender found");

    return defender;
  }

  public boolean isBombingRun()
  {
    return false;
  }

  public Territory getTerritory()
  {
    return m_battleSite;
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
    if (o == null || ! (o instanceof Battle))
      return false;

    Battle other = (Battle) o;
    return other.getTerritory().equals(this.m_battleSite) &&
      other.isBombingRun() == this.isBombingRun();
  }

  public void fight(DelegateBridge bridge)
  {
    bridge.getHistoryWriter().startEvent("Battle in" + m_battleSite);
    removeAirNoLongerInTerritory();

    //it is possible that no attacking units are present, if so
    //end now
    if (m_attackingUnits.size() == 0)
    {
      endBattle(bridge);
      defenderWins(bridge);
      return;
    }

    //if is possible that no defending units exist
    if (m_defendingUnits.size() == 0)
    {
      endBattle(bridge);
      attackerWins(bridge);
      return;
    }

    //list the steps
    List steps = determineStepStrings(true);

    BattleStartMessage battleStart =
      new BattleStartMessage(
      m_attacker,
      m_defender,
      m_battleSite,
      removeNonCombatants(m_attackingUnits),
      removeNonCombatants(m_defendingUnits),
      m_dependentUnits
      );
    bridge.sendMessage(battleStart);
    bridge.sendMessage(battleStart, m_defender);

    BattleStepMessage battleStepMessage = new BattleStepMessage( (String) steps.get(0), getBattleTitle(), steps, m_battleSite);
    bridge.sendMessage(battleStepMessage);
    bridge.sendMessage(battleStepMessage, m_defender);

    //take the casualties with least movement first
    MoveDelegate moveDelegate = DelegateFinder.moveDelegate(m_data);
    moveDelegate.sortAccordingToMovementLeft(m_attackingUnits, false);

    fightStart(bridge);
    fightLoop(bridge);
  }

  private void removeAirNoLongerInTerritory()
  {
    //remove any air units that were once in this attack, but have now moved out of the territory
    //this is an ilegant way to handle this bug
    CompositeMatch airNotInTerritory = new CompositeMatchAnd();
    airNotInTerritory.add(new InverseMatch(Matches.unitIsInTerritory(m_battleSite)));
   
    m_attackingUnits.removeAll(Match.getMatches(m_attackingUnits, airNotInTerritory));

  }

  public List determineStepStrings(boolean showFirstRun)
  {
    List steps = new ArrayList();
    if (showFirstRun)
    {
      if (!m_battleSite.isWater())
      {
        if (canFireAA())
        {
          steps.add(AA_GUNS_FIRE);
          steps.add(SELECT_AA_CASUALTIES);
          steps.add(REMOVE_AA_CASUALTIES);
        }
        if (!getBombardingUnits().isEmpty())
        {
          steps.add(NAVAL_BOMBARDMENT);
          steps.add(SELECT_NAVAL_BOMBARDMENT_CASUALTIES);
        }
      }
    }

    //attacker subs
    if (m_battleSite.isWater())
    {
      if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
      {
        steps.add(ATTACKER_SUBS_FIRE);
        steps.add(DEFENDER_SELECT_SUB_CASUALTIES);
        steps.add(DEFENDER_REMOVE_SUB_CASUALTIES);
      }
    }

    //attacker fire
    if (Match.someMatch(m_attackingUnits, Matches.UnitIsNotSub))
    {
      steps.add(m_attacker.getName() + ATTACKER_FIRES);
      steps.add(m_defender.getName() + DEFENDER_SELECT_CASUALTIES);
    }

    //defender subs
    if (m_battleSite.isWater())
    {
      if (Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
      {
        steps.add(m_defender.getName() + DEFENDER_FIRES_SUBS);
        steps.add(m_attacker.getName() + ATTACKER_SELECT_SUB_CASUALTIES);
      }
    }

    if (Match.someMatch(m_defendingUnits, Matches.UnitIsNotSub))
    {
      //defender fire
      steps.add(m_defender.getName() + DEFENDER_FIRES);
      steps.add(m_attacker.getName() + ATTACKER_SELECT_CASUALTIES);
    }

    //remove casualties
    steps.add(REMOVE_CASUALTIES);

    //retreat subs
    if (m_battleSite.isWater())
    {
      if (canAttackerRetreat())
      {
        if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
        {
          steps.add(m_attacker.getName() + ATTACKER_SUBS_WITHDRAW);
        }
      }
      if (canDefenderRetreatSubs())
      {
        if (Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
        {
          steps.add(m_defender.getName() + DEFENDER_SUBS_WITHDRAW);
        }
      }
    }

    if (canAttackerRetreat())
    {
      steps.add(m_attacker.getName() + ATTACKER_WITHDRAW);
    }

    return steps;

  }

  private void fightStart(DelegateBridge bridge)
  {
    fireAAGuns(bridge);
    fireNavalBombardment(bridge);
    removeNonCombatants();
  }

  private void fightLoop(DelegateBridge bridge)
  {
    if (m_over)
      return;
    attackSubs(bridge);
    attackNonSubs(bridge);
    defendSubs(bridge);
    defendNonSubs(bridge);

    clearWaitingToDie(bridge);

    if (m_attackingUnits.size() == 0)
    {
      endBattle(bridge);
      defenderWins(bridge);
      return;
    }
    else if (m_defendingUnits.size() == 0)
    {
      endBattle(bridge);
      attackerWins(bridge);
      return;
    }

    attackerRetreatSubs(bridge);
    defenderRetreatSubs(bridge);
    attackerRetreat(bridge);

    if (!m_over)
    {
      List steps = determineStepStrings(false);
      BattleStepMessage battleStepMessage = new BattleStepMessage( (String) steps.get(0), getBattleTitle(), steps, m_battleSite);
      bridge.sendMessage(battleStepMessage);
      bridge.sendMessage(battleStepMessage, m_defender);
    }

    fightLoop(bridge);
    return;
  }

  private Collection getAttackerRetreatTerritories()
  {
    //its possible that a sub retreated to a territory we came from,
    //if so we can no longer retreat there
    Collection possible = Match.getMatches(m_attackingFrom, Matches.territoryHasNoEnemyUnits(m_attacker, m_data));

    if (Match.someMatch(m_attackingUnits, Matches.UnitIsLand) && !m_battleSite.isWater())
      possible = Match.getMatches(possible, Matches.TerritoryIsLand);

    if (Match.someMatch(m_attackingUnits, Matches.UnitIsSea))
      possible = Match.getMatches(possible, Matches.TerritoryIsWater);

    return possible;
  }

  private boolean canAttackerRetreat()
  {
    if (m_amphibious)
      return false;

    Collection options = getAttackerRetreatTerritories();
    if (options.size() == 0)
      return false;

    return true;
  }

  private void attackerRetreat(DelegateBridge bridge)
  {
    if (!canAttackerRetreat())
      return;

    Collection possible = getAttackerRetreatTerritories();

    if (!m_over)
      queryRetreat(false, false, bridge, possible, m_attacker.getName() + ATTACKER_WITHDRAW);
  }

  private void attackerRetreatSubs(DelegateBridge bridge)
  {
    if (!canAttackerRetreat())
      return;

    Collection possible = getAttackerRetreatTerritories();

    //retreat subs
    if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
      queryRetreat(false, true, bridge, possible, m_attacker.getName() + ATTACKER_SUBS_WITHDRAW);
  }

  private boolean canDefenderRetreatSubs()
  {
    return getEmptyOrFriendlySeaNeighbors(m_defender).size() != 0;
  }

  private void defenderRetreatSubs(DelegateBridge bridge)
  {
    if (!canDefenderRetreatSubs())
      return;

    if (!m_over)
      queryRetreat(true, true, bridge, getEmptyOrFriendlySeaNeighbors(m_defender), m_defender.getName() + DEFENDER_SUBS_WITHDRAW);
  }

  private Collection getEmptyOrFriendlySeaNeighbors(PlayerID player)
  {
    Collection possible = m_data.getMap().getNeighbors(m_battleSite);
    CompositeMatch match = new CompositeMatchAnd(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, m_data));

    //make sure we can move through the any canals
    Match canalMatch = new Match()
    {
      public boolean match(Object o)
      {
        Route r = new Route();
        r.setStart(m_battleSite);
        r.add((Territory) o);
        return null == MoveValidator.validateCanal(r, m_defender, m_data);
      }
    };
    match.add(canalMatch);

    possible = Match.getMatches(possible, match);
    return possible;
  }

  private void queryRetreat(boolean defender, boolean subs, DelegateBridge bridge, Collection availableTerritories, String stepName)
  {
    if (availableTerritories.isEmpty())
      return;

    //dont allow retreating units to retreat if a naval invasion depends on this
    if (m_tracker.getBlocked(this).size() != 0 && !subs)
      return;

    Collection units = defender ? m_defendingUnits : m_attackingUnits;
    if (subs)
    {
      units = Match.getMatches(units, Matches.UnitIsSub);
    }

    if (Match.someMatch(units, Matches.UnitIsSea))
    {
      availableTerritories = Match.getMatches(availableTerritories, Matches.TerritoryIsWater);
    }

    if (units.size() == 0)
      return;

    PlayerID retreatingPlayer = defender ? m_defender : m_attacker;
    PlayerID nonRetreatingPlayer = defender ? m_attacker : m_defender;
    String text = retreatingPlayer.getName() + " retreat" + (subs ? " subs" : "") + "?";

    String step;
    if (defender)
    {
      step = m_defender.getName() + DEFENDER_SUBS_WITHDRAW;
    }
    else
    {
      if (subs)
        step = m_attacker.getName() + ATTACKER_SUBS_WITHDRAW;
      else
        step = m_attacker.getName() + ATTACKER_WITHDRAW;
    }

    RetreatQueryMessage query = new RetreatQueryMessage(step, availableTerritories, subs, text);
    Message response = bridge.sendMessage(query, retreatingPlayer);
    if (response != null)
    {
      //if attacker retreating non subs then its all over
      if (!defender & !subs)
        m_over = true;
      Territory retreatTo = ( (RetreatMessage) response).getRetreatTo();
      retreatUnits(units, retreatTo, defender, bridge);

      String messageShort = retreatingPlayer.getName() + " retreats";
      String messageLong = retreatingPlayer.getName() + " retreats " + (subs ? "subs" : "all units") + " to " + retreatTo.getName();
      bridge.sendMessage(new BattleInfoMessage(messageLong, messageShort, stepName), nonRetreatingPlayer);
    }
  }

  private void retreatUnits(Collection retreating, Territory to, boolean defender, DelegateBridge bridge)
  {
    retreating.addAll(getTransportedUnits(retreating));
    //air units dont retreat with land units
    retreating = Match.getMatches(retreating, Matches.UnitIsNotAir);

    String transcriptText = Formatter.unitsToTextNoOwner(retreating) + " retreated to " + to.getName();
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList( retreating));

    Change change = ChangeFactory.moveUnits(m_battleSite, to, retreating);
    bridge.addChange(change);

    Collection units = defender ? m_defendingUnits : m_attackingUnits;

    units.removeAll(retreating);
    if (units.isEmpty() || m_over)
    {
      endBattle(bridge);
      if (defender)
        attackerWins(bridge);
      else
        defenderWins(bridge);
    }
  }

  //the maximum number of hits that this collection of units can sustain
  //takes into account units with two hits
  public int getMaxHits(Collection units)
  {
    int count = 0;
    Iterator unitIter = units.iterator();
    while (unitIter.hasNext())
    {
      Unit unit = (Unit) unitIter.next();
      if (UnitAttatchment.get(unit.getUnitType()).isTwoHit())
      {
        count += 2;
        count -= unit.getHits();
      }
      else
      {
        count++;
      }
    }
    return count;
  }

  private void fire(String stepName, Collection firingUnits, Collection attackableUnits, boolean defender, boolean canReturnFire, DelegateBridge bridge, String text)
  {
    PlayerID firingPlayer = defender ? m_defender : m_attacker;
    PlayerID hitPlayer = defender ? m_attacker : m_defender;

    DiceRoll dice = DiceRoll.rollDice(new ArrayList(firingUnits), defender, firingPlayer, bridge);

    int hitCount = dice.getHits();
    Collection killed;
    Collection damaged = null;

    //they all die
    if (hitCount >= getMaxHits(attackableUnits))
    {
      killed = attackableUnits;
    }
    else
    {
      SelectCasualtyMessage message = selectCasualties(stepName, bridge, attackableUnits, !defender, text, dice);
      killed = message.getKilled();
      damaged = message.getDamaged();
    }

    CasualtyNotificationMessage msg = new CasualtyNotificationMessage(stepName, killed, damaged, m_dependentUnits, hitPlayer, dice);
    msg.setAll(killed.size() == attackableUnits.size());

    bridge.sendMessage(msg, hitPlayer);
    bridge.sendMessage(msg, firingPlayer);

    if (damaged != null)
      markDamaged(damaged, bridge);

    removeCasualties(killed, canReturnFire, !defender, bridge);
  }

  private void defendNonSubs(DelegateBridge bridge)
  {
    if (m_attackingUnits.size() == 0)
      return;
    Collection units = new ArrayList(m_defendingUnits.size() + m_defendingWaitingToDie.size());
    units.addAll(m_defendingUnits);
    units.addAll(m_defendingWaitingToDie);
    units = Match.getMatches(units, Matches.UnitIsNotSub);

    if (units.isEmpty())
      return;

    fire(m_attacker.getName() + ATTACKER_SELECT_CASUALTIES, units, m_attackingUnits, true, true, bridge, "Defenders fire, ");
  }

  private void attackNonSubs(DelegateBridge bridge)
  {
    if (m_defendingUnits.size() == 0)
      return;
    Collection units = Match.getMatches(m_attackingUnits, Matches.UnitIsNotSub);

    if (units.isEmpty())
      return;

    fire(m_defender.getName() + DEFENDER_SELECT_CASUALTIES, units, m_defendingUnits, false, true, bridge, "Attackers fire,");
  }

  private void attackSubs(DelegateBridge bridge)
  {
    Collection firing = Match.getMatches(m_attackingUnits, Matches.UnitIsSub);
    if (firing.isEmpty())
      return;
    Collection attacked = Match.getMatches(m_defendingUnits, Matches.UnitIsNotAir);
    fire(DEFENDER_SELECT_SUB_CASUALTIES, firing, attacked, false, false, bridge, "Subs fire,");
  }

  private void defendSubs(DelegateBridge bridge)
  {
    if (m_attackingUnits.size() == 0)
      return;
    Collection units = new ArrayList(m_defendingUnits.size() + m_defendingWaitingToDie.size());
    units.addAll(m_defendingUnits);
    units.addAll(m_defendingWaitingToDie);
    units = Match.getMatches(units, Matches.UnitIsSub);

    if (units.isEmpty())
      return;

    Collection attacked = Match.getMatches(m_attackingUnits, Matches.UnitIsNotAir);
    if (attacked.isEmpty())
      return;

    fire(m_attacker.getName() + ATTACKER_SELECT_SUB_CASUALTIES, units, attacked, true, true, bridge, "Subs defend, ");
  }

  private SelectCasualtyMessage selectCasualties(String step, DelegateBridge bridge, Collection attackableUnits, boolean defender, String text, DiceRoll dice)
  {
    PlayerID hit = defender ? m_defender : m_attacker;
    return BattleCalculator.selectCasualties(step, hit, attackableUnits, bridge, text, m_data, dice);
  }

  private void removeCasualties(Collection killed, boolean canReturnFire, boolean defender, DelegateBridge bridge)
  {
    if (canReturnFire)
    {
      //move to waiting to die
      if (defender)
        m_defendingWaitingToDie.addAll(killed);
      else
        m_attackingWaitingToDie.addAll(killed);
    }
    else

      //remove immediately
      remove(killed, bridge);

      //remove from the active fighting
    if (defender)
      m_defendingUnits.removeAll(killed);
    else
      m_attackingUnits.removeAll(killed);
  }

  private void fireNavalBombardment(DelegateBridge bridge)
  {
    Collection bombard = getBombardingUnits();
    Collection attacked = Match.getMatches(m_defendingUnits, Matches.UnitIsDestructible);

    if (bombard.size() > 0 && attacked.size() > 0)
      fire(SELECT_NAVAL_BOMBARDMENT_CASUALTIES, bombard, attacked, false, true, bridge, "Bombard");
    markBombardingSources();
  }

  /**
   * Marks all the naval origins as having been the source for a bombardment
   */
  private void markBombardingSources()
  {
    m_tracker.addPreviouslyNavalBombardmentSource(m_amphibiousAttackFrom);
  }

  private Collection getBombardingUnits()
  {
    Iterator territories = m_amphibiousAttackFrom.iterator();
    Collection bombard = new HashSet();
    while (territories.hasNext())
    {
      Territory possible = (Territory) territories.next();
      if (m_tracker.hasPendingBattle(possible, false))
        throw new IllegalStateException("Navel battle pending where amphibious assault originated");
      if (!m_tracker.wasBattleFought(possible) && !m_tracker.wasNavalBombardmentSource(possible))
      {
        bombard.addAll(possible.getUnits().getMatches(Matches.UnitCanBombard));
      }
    }
    return bombard;
  }

  private void fireAAGuns(DelegateBridge bridge)
  {
    String step = SELECT_AA_CASUALTIES;
    if (!canFireAA())
      return;

    int attackingAirCount = Match.countMatches(m_attackingUnits, Matches.UnitIsAir);
    //DiceRoll dice = DiceRoll.rollAA(attackingAirCount, bridge);
    // NEW VERSION
    DiceRoll dice = DiceRoll.rollAA(attackingAirCount, bridge, m_battleSite);

    if (dice.getHits() == 0)
    {
      bridge.sendMessage(new BattleStringMessage(step, "No AA hits"));
      bridge.sendMessage(new BattleInfoMessage("No AA hits", "No AA hits", step), m_defender);
    }
    else
    {
      Collection attackable = Match.getMatches(m_attackingUnits, Matches.UnitIsAir);
      Collection casualties = selectCasualties(step, bridge, attackable, false, "AA guns fire,", dice).getKilled();

      CasualtyNotificationMessage msg = new CasualtyNotificationMessage(REMOVE_AA_CASUALTIES, casualties, Collections.EMPTY_LIST, m_dependentUnits, m_attacker, dice);
      msg.setAll(false);

      bridge.sendMessage(msg, m_attacker);
      bridge.sendMessage(msg, m_defender);

      removeCasualties(casualties, false, false, bridge);
    }
  }

  private boolean canFireAA()
  {
    return
      Match.someMatch(m_defendingUnits, Matches.UnitIsAA) &&
      Match.someMatch(m_attackingUnits, Matches.UnitIsAir);
  }

  /**
   * @return a collection containing all the combatants in units
   * non combatants include such things as factories, aaguns, land units in a water battle.
   */
  private List removeNonCombatants(Collection units)
  {
    CompositeMatch combat = new CompositeMatchAnd();
    combat.add(new InverseMatch(Matches.UnitIsAAOrFactory));

    if (m_battleSite.isWater())
      combat.add(new InverseMatch(Matches.UnitIsLand));

    return Match.getMatches(units, combat);

  }

  private void removeNonCombatants()
  {
    m_defendingUnits = removeNonCombatants(m_defendingUnits);
    m_attackingUnits = removeNonCombatants(m_attackingUnits);
  }

  private Collection getTransportedUnits(Collection transports)
  {
    Iterator iter = transports.iterator();
    Collection transported = new ArrayList();
    while (iter.hasNext())
    {
      Collection transporting = DelegateFinder.moveDelegate(m_data).getTransportTracker().transporting( (Unit) iter.next());
      if (transporting != null)
        transported.addAll(transporting);
    }
    return transported;
  }

  private void markDamaged(Collection damaged, DelegateBridge bridge)
  {
    if (damaged.size() == 0)
      return;
    Change damagedChange = null;
    IntegerMap damagedMap = new IntegerMap();
    damagedMap.putAll(damaged, 1);
    damagedChange = ChangeFactory.unitsHit(damagedMap);
    bridge.getHistoryWriter().addChildToEvent("Units damaged:" + Formatter.unitsToTextNoOwner(damaged), damaged);
    bridge.addChange(damagedChange);

  }

  private void remove(Collection killed, DelegateBridge bridge)
  {
    if (killed.size() == 0)
      return;

    //get the transported units
    if (m_battleSite.isWater())
    {
      Collection transported = getTransportedUnits(killed);
      killed.addAll(transported);
    }
    Change killedChange = ChangeFactory.removeUnits(m_battleSite, killed);

    String transcriptText = Formatter.unitsToText(killed) + " lost in " + m_battleSite.getName();
    bridge.getHistoryWriter().addChildToEvent(transcriptText, killed);

    bridge.addChange(killedChange);
    removeFromDependents(killed, bridge);

  }

  private void removeFromDependents(Collection units, DelegateBridge bridge)
  {
    Collection dependents = m_tracker.getBlocked(this);
    Iterator iter = dependents.iterator();
    while (iter.hasNext())
    {
      Battle dependent = (Battle) iter.next();
      dependent.unitsLost(this, units, bridge);
    }
  }

  private void clearWaitingToDie(DelegateBridge bridge)
  {
    Collection units = new ArrayList();
    units.addAll(m_attackingWaitingToDie);
    units.addAll(m_defendingWaitingToDie);
    remove(units, bridge);
    m_defendingWaitingToDie.clear();
    m_attackingWaitingToDie.clear();
  }

  private void defenderWins(DelegateBridge bridge)
  {

    BattleEndMessage msg = new BattleEndMessage(m_defender.getName() + " wins");
    bridge.sendMessage(msg, m_attacker);
    bridge.sendMessage(msg, m_defender);
    bridge.getHistoryWriter().addChildToEvent(m_defender.getName() + " wins");

    checkDefendingPlanesCanLand(bridge);

  }

  /**
   * The defender has won, but there may be defending fighters that cant stay in the sea zone
   * due to insufficient carriers.
   */
  private void checkDefendingPlanesCanLand(DelegateBridge bridge)
  {
    //not water, not relevant.
    if(!m_battleSite.isWater())
      return;

    CompositeMatch alliedDefendingAir = new CompositeMatchAnd( Matches.UnitIsAir, Matches.isUnitAllied(m_defender, m_data));
    Collection defendingAir = Match.getMatches(m_defendingUnits, alliedDefendingAir);
    //ne defending air
    if(defendingAir.isEmpty())
      return;

    int carrierCost = MoveValidator.carrierCost(defendingAir);
    int carrierCapacity = MoveValidator.carrierCapacity(m_defendingUnits);

    if(carrierCapacity >= carrierCost)
      return;

    //find out what we must remove
    //remove all the air that can land on carriers from defendingAir
    carrierCost = 0;
    Iterator defendingAirIter = new ArrayList(defendingAir).iterator();
    while (defendingAirIter.hasNext() && carrierCapacity >= carrierCost)
    {
      Unit currentUnit = (Unit)defendingAirIter.next();
      carrierCost += UnitAttatchment.get(currentUnit.getType()).getCarrierCost();
      if(carrierCapacity >= carrierCost)
      {
        defendingAir.remove(currentUnit);
      }
    }

    //now defending air has what cant stay, is there a place we can go?
    //check for an island in this sea zone
    Set neighbors = m_data.getMap().getNeighbors(m_battleSite);
    Iterator neighborsIter = neighbors.iterator();
    while (neighborsIter.hasNext())
    {
      Territory currentTerritory = (Territory)neighborsIter.next();
      //we cant land them on water
      if(currentTerritory.isWater())
        continue;
      //is it safe to move the planes there?
      if(!m_data.getAllianceTracker().isAllied(m_defender, currentTerritory.getOwner()))
        continue;

      //only one neighbor, its an island.
      if(m_data.getMap().getNeighbors(currentTerritory).size() == 1)
      {
        bridge.getHistoryWriter().addChildToEvent(Formatter.unitsToText(defendingAir) + " forced to land in " + currentTerritory.getName(), defendingAir);
        Change change = ChangeFactory.moveUnits(m_battleSite, currentTerritory, defendingAir);
        bridge.addChange(change);
        return;
      }
    }

    //no were to go, they must die
    bridge.getHistoryWriter().addChildToEvent(Formatter.unitsToText(defendingAir) + " could not land and were killed", defendingAir);
    Change change = ChangeFactory.removeUnits(m_battleSite, defendingAir);
    bridge.addChange(change);
  }


  private void attackerWins(DelegateBridge bridge)
  {
    BattleEndMessage msg = new BattleEndMessage(m_attacker.getName() + " wins");
    bridge.sendMessage(msg, m_attacker);
    bridge.sendMessage(msg, m_defender);

    //do we need to change ownership
    if (!m_battleSite.isWater())
    {

      if (Match.someMatch(m_attackingUnits, Matches.UnitIsNotAir))
      {
        m_tracker.addToConquered(m_battleSite);
        m_tracker.takeOver(m_battleSite, m_attacker, bridge, m_data, null);
      }
    }

    bridge.getHistoryWriter().addChildToEvent(m_attacker.getName() + " wins");
  }

  private void endBattle(DelegateBridge bridge)
  {
    clearWaitingToDie(bridge);
    m_over = true;
    m_tracker.removeBattle(this);
  }

  public String toString()
  {
    return
      "Battle in:" + m_battleSite +
      " attacked by:" + m_attackingUnits +
      " from:" + m_attackingFrom +
      " defender:" + m_defender.getName() +
      " bombing:" + isBombingRun();
  }

  public void unitsLost(Battle battle, Collection units, DelegateBridge bridge)
  {
    Iterator iter = units.iterator();
    Collection lost = new ArrayList();
    while (iter.hasNext())
    {
      Unit unit = (Unit) iter.next();
      Collection dependent = (Collection) m_dependentUnits.get(unit);
      if (dependent != null)
        lost.addAll(dependent);
    }

    //if all the amphibious attacking land units are lost, then we are
    //no longer a naval invasion
    m_amphibiousLandAttackers.removeAll(lost);
    if (m_amphibiousLandAttackers.isEmpty())
      m_amphibious = false;

    if (lost.size() != 0)
    {
      m_attackingUnits.removeAll(lost);
      remove(lost, bridge);
    }

    if (m_attackingUnits.isEmpty())
      m_tracker.removeBattle(this);
  }

  /**
   * Returns a map of transport -> collection of transported units.
   */
  private Map transporting(Collection units)
  {
    return m_transportTracker.transporting(units);
  }
}
