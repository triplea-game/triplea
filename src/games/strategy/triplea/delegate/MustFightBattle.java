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

import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttatchment;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.formatter.Formatter;


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
	private final Territory m_territory;
	private List m_attackingUnits = new LinkedList();
	private Collection m_attackingWaitingToDie = new ArrayList();
	private Collection m_attackingNonCombatants;
	private Collection m_attackingFrom = new ArrayList();
	private Collection m_amphibiousAttackFrom = new ArrayList();
	private Collection m_defendingUnits = new LinkedList();
	private Collection m_defendingWaitingToDie = new ArrayList();
	private Collection m_defendingNonCombatants;
	private boolean m_amphibious = false;
	private boolean m_over = false;
	private BattleTracker m_tracker;
	
	private PlayerID m_defender;
	private PlayerID m_attacker;

	private GameData m_data;
	
	//dependent units
	//maps unit -> Collection of units
	//if unit is lost in a battle we are dependent on
	//then we lose the corresponding collection of units
	private Map m_dependentUnits;
	
	public MustFightBattle(Territory battleSite, PlayerID attacker, GameData data, BattleTracker tracker, Map dependentUnits)
	{
		m_tracker = tracker;
		m_territory = battleSite;
		m_defendingUnits.addAll(m_territory.getUnits().getMatches(Matches.enemyUnit(attacker, data)));
		m_defender = findDefender(battleSite);
		m_attacker = attacker;
		m_data = data;
		m_dependentUnits = dependentUnits;
	}

	public void addAttack(Route route, Collection units)
	{
		int routeSize = route.getLength();
		Territory attackFrom;
		if(routeSize == 1)
			attackFrom = route.getStart();
		else
			attackFrom = route.at(routeSize - 2);
		
		m_attackingFrom.add(attackFrom);
		
		m_attackingUnits.addAll(units);
		
		//are we amphibious
		if(route.getStart().isWater() && 
		   !route.getEnd().isWater() &&
		   Match.someMatch(units, Matches.UnitIsLand))
		{
			m_amphibiousAttackFrom.add(attackFrom);
			m_amphibious = true;
		}
		
		//mark units with no movement
		//for all but air
		Collection nonAir = Match.getMatches(units, Matches.UnitIsNotAir);
		DelegateFinder.moveDelegate(m_data).markNoMovement(nonAir);
	}

	private String getBattleTitle()
	{
		return m_attacker.getName() + " attacks " + findDefender(m_territory).getName() + " in " + m_territory.getName();
	}
	
	private PlayerID findDefender(Territory battleSite)
	{
		if(!battleSite.isWater())
			return battleSite.getOwner();
		//if water find the defender based on who has the most units in the territory
		IntegerMap players =  battleSite.getUnits().getPlayerUnitCounts();
		int max = -1;
		PlayerID defender = null;
		Iterator iter = players.keySet().iterator();
		while(iter.hasNext())
		{
			PlayerID current = (PlayerID) iter.next();
			int count = players.getInt(current);
			if(count > max)
			{
				max = count;
				defender = current;
			}
		}
		if(max == -1)
			throw new IllegalStateException("No defender found");
		
		return defender;
	}
	
	public boolean isBombingRun()
	{
		return false;
	}
		
	
	public Territory getTerritory()
	{
		return m_territory;
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
		return other.getTerritory().equals(this.m_territory) && 
			other.isBombingRun() == this.isBombingRun();
	}
		
	public void fight(DelegateBridge bridge)
	{
		//it is possible that no attacking units are present, if so
		//end now
		if(m_attackingUnits.size() == 0)
		{
			endBattle(bridge);
			defenderWins(bridge);
			return;
		}
				
		//if is possible that no defending units exist
		if(m_defendingUnits.size() == 0)
		{
			endBattle(bridge);
			attackerWins(bridge);
			return;
		}
		//list the steps
		List steps = determineStepStrings(true);
		bridge.sendMessage( new BattleStepMessage((String) steps.get(0),getBattleTitle(), steps));
		
		
		//take the casualties with least movement first
		MoveDelegate moveDelegate = DelegateFinder.moveDelegate(m_data);
		moveDelegate.sortAccordingToMovementLeft(m_attackingUnits, false);
		
		fightStart(bridge);
		fightLoop(bridge);
	}
	
	public List determineStepStrings(boolean showFirstRun)
	{
		List steps = new ArrayList();
		if(showFirstRun)
		{
			if(!m_territory.isWater())
			{
				if(canFireAA())
				{
					steps.add(AA_GUNS_FIRE);
					steps.add(SELECT_AA_CASUALTIES);
					steps.add(REMOVE_AA_CASUALTIES);
				}
				if(!getBombardingUnits().isEmpty())
				{
					steps.add(NAVAL_BOMBARDMENT);
					steps.add(SELECT_NAVAL_BOMBARDMENT_CASUALTIES);
				}
			}
		}
		
		//attacker subs
		if(m_territory.isWater())
		{
			if(Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
			{
				steps.add(ATTACKER_SUBS_FIRE);
				steps.add(DEFENDER_SELECT_SUB_CASUALTIES);
				steps.add(DEFENDER_REMOVE_SUB_CASUALTIES);
			}
		}
	
		//attacker fire
		steps.add(ATTACKER_FIRES);
		steps.add(DEFENDER_SELECT_CASUALTIES);
		
		//defender subs
		if(m_territory.isWater())
		{
			if(Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
			{
				steps.add(DEFENDER_FIRES_SUBS);
				steps.add(ATTACKER_SELECT_SUB_CASUALTIES);
			}
		}
		//defender fire
		steps.add(DEFENDER_FIRES);
		steps.add(ATTACKER_SELECT_CASUALTIES);
		
		//remove casualties
		steps.add(REMOVE_CASUALTIES);
		
		//retreat subs
		if(m_territory.isWater())
		{
			if(canAttackerRetreat())
			{
				if(Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
				{
					steps.add(ATTACKER_SUBS_WITHDRAW);
				}
			}
			if(canDefenderRetreatSubs())
			{
				if(Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
				{
					steps.add(DEFENDER_SUBS_WITHDRAW);
				}
			}
		}
		
		if(canAttackerRetreat())
		{
			steps.add(ATTACKER_WITHDRAW);
		}
		
		return steps;
		
	}
	
	public void fightStart(DelegateBridge bridge)
	{	
		fireAAGuns(bridge);
		fireNavalBombardment(bridge);
		removeNonCombatants();
	}
	
	private void fightLoop(DelegateBridge bridge)
	{	
		if(m_over)
			return;
		attackSubs(bridge);
		attackNonSubs(bridge);
		defendSubs(bridge);
		defendNonSubs(bridge);
		
		clearWaitingToDie(bridge);
		
		if(m_attackingUnits.size() == 0)
		{	
			endBattle(bridge);
			defenderWins(bridge);
			return;
		}
		else if(m_defendingUnits.size() == 0)
		{
			endBattle(bridge);
			attackerWins(bridge);
			return;
		}
		
		attackerRetreat(bridge);
		defenderRetreat(bridge);
		
		List steps = determineStepStrings(false);
		bridge.sendMessage( new BattleStepMessage((String) steps.get(0), getBattleTitle(),  steps));
		
		fightLoop(bridge);
		return;
	}
	
	private Collection getAttackerRetreatTerritories()
	{
		//its possible that a sub retreated to a territory we came from,
		//if so we can no longer retreat there
		Collection possible = Match.getMatches(m_attackingFrom, Matches.territoryHasNoEnemyUnits(m_attacker, m_data));
		
		return possible;
	}
	
	private boolean canAttackerRetreat()
	{
		if(m_amphibious)
			return false;
		
		Collection options = getAttackerRetreatTerritories();
		if(options.size() == 0)
			return false;
		
		return true;
	}
	
	private void attackerRetreat(DelegateBridge bridge)
	{
		if(!canAttackerRetreat())
			return;
		
		Collection possible = getAttackerRetreatTerritories();
		
		//retreat subs
		if( Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
			queryRetreat(false,true,bridge, m_attackingFrom);
		//retreat all
		if(!m_over)
			queryRetreat(false,false,bridge, m_attackingFrom);
	}
		
	private boolean canDefenderRetreatSubs()
	{
		return  getEmptyOrFriendlySeaNeighbors(m_defender).size() != 0;
	}
	
	private void defenderRetreat(DelegateBridge bridge)
	{
		if(!canDefenderRetreatSubs())
			return;
		
		if(!m_over)
			queryRetreat(true,true,bridge, getEmptyOrFriendlySeaNeighbors(m_defender));
	}

	private Collection getEmptyOrFriendlySeaNeighbors(PlayerID player)
	{
		Collection possible = m_data.getMap().getNeighbors(m_territory);
		Match match = new CompositeMatchAnd( Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, m_data));
		possible = Match.getMatches(possible, match);
		return possible;
	}
	
	private void queryRetreat(boolean defender, boolean subs, DelegateBridge bridge, Collection availableTerritories)
	{
		if(availableTerritories.isEmpty() )
			return;
		
		if(m_tracker.getBlocked(this).size() != 0)
			return;
		
		Collection units = defender ? m_defendingUnits : m_attackingUnits;
		if(subs)
		{
			units = Match.getMatches( units, Matches.UnitIsSub );
		}
		
		if(Match.someMatch(units, Matches.UnitIsSea))
		{
			availableTerritories = Match.getMatches(availableTerritories, Matches.TerritoryIsWater);
		}
		
		if(units.size() == 0)
			return;
				
		PlayerID player = defender ? m_defender : m_attacker;
		String text = player.getName() + " retreat" + (subs ? " subs" : "") + "?";
		
		String step;
		if(defender)
		{
			step = DEFENDER_SUBS_WITHDRAW;
		}
		else
		{
			if(subs)
				step = ATTACKER_SUBS_WITHDRAW;
			else
				step = ATTACKER_WITHDRAW;
				
		}
		
		RetreatQueryMessage query = new RetreatQueryMessage(step, availableTerritories, subs, text);
		Message response = bridge.sendMessage(query, player);
		if(response != null)
		{
			//if attacker retreating non subs then its all over
			if(!defender & !subs)
				m_over = true;
			Territory retreatTo = ( (RetreatMessage) response).getRetreatTo();
			retreatUnits(units, retreatTo, defender, bridge);
		}
	}
	
	private void retreatUnits(Collection retreating, Territory to, boolean defender, DelegateBridge bridge)
	{
		retreating.addAll( getTransportedUnits(retreating));
		Change change = ChangeFactory.moveUnits(m_territory, to, retreating);
		bridge.addChange(change);
		
		String transcriptText = Formatter.unitsToText(retreating) + " retreated to " + to.getName();
		bridge.getTranscript().write(transcriptText);
		
		Collection units = defender ? m_defendingUnits : m_attackingUnits;
		
		units.removeAll(retreating);
		if(units.isEmpty())
		{
			if(defender)
				defenderWins(bridge);
			else
				attackerWins(bridge);
			endBattle(bridge);
		}		
	}
	
	private void fire(String stepName, Collection firingUnits, Collection attackableUnits,  boolean defender, boolean canReturnFire,  DelegateBridge bridge, String text)
	{
		PlayerID firingPlayer = defender ? m_defender : m_attacker;
		int hitCount = getCasualties(firingUnits, defender, bridge);	
		if(hitCount >= attackableUnits.size())
		{
			BattleStringMessage msg = new BattleStringMessage(stepName, text + " " + hitCount + " hits for " + firingPlayer.getName());
			bridge.sendMessage(msg);
			removeCasualties(attackableUnits, canReturnFire, !defender, bridge);
		} else if(hitCount == 0)
		{
			BattleStringMessage msg = new BattleStringMessage(stepName, text + " no hits for " + firingPlayer.getName() + ".");
			bridge.sendMessage(msg);
		} else
		{
			Collection casualties = selectCasualties(stepName, bridge,attackableUnits, hitCount, !defender, text);
			removeCasualties(casualties, canReturnFire, !defender, bridge);
		}		
	}
	
	private void defendNonSubs(DelegateBridge bridge)
	{
		Collection units = new ArrayList(m_defendingUnits.size() + m_defendingWaitingToDie.size());
		units.addAll(m_defendingUnits);
		units.addAll(m_defendingWaitingToDie);
		units = Match.getMatches(units, Matches.UnitIsNotSub);
		fire(ATTACKER_SELECT_CASUALTIES, units, m_attackingUnits, true, true, bridge, "Defenders fire, ");
	}
	
	private void attackNonSubs(DelegateBridge bridge)
	{
		if(m_defendingUnits.size() == 0)
			return;
		Collection units = Match.getMatches(m_attackingUnits, Matches.UnitIsNotSub);
		fire(DEFENDER_SELECT_CASUALTIES,units, m_defendingUnits, false, true, bridge, "Attackers fire,");
	}
	
	private void attackSubs(DelegateBridge bridge)
	{
		Collection attacker = Match.getMatches(m_attackingUnits, Matches.UnitIsSub);
		if(attacker.isEmpty())
			return;
		Collection attacked = Match.getMatches(m_defendingUnits, Matches.UnitIsNotAir);
		fire(DEFENDER_SELECT_SUB_CASUALTIES, attacker, attacked, false, false, bridge, "Subs fire,");
	}

	private void defendSubs(DelegateBridge bridge)
	{
		Collection attacker = Match.getMatches(m_defendingUnits, Matches.UnitIsSub);
		if(attacker.isEmpty())
			return;
		Collection attacked = Match.getMatches(m_attackingUnits, Matches.UnitIsNotAir);
		fire(ATTACKER_SELECT_SUB_CASUALTIES, attacker, attacked, true, true, bridge, "Subs defend, ");
	}

	
	private Collection selectCasualties(String step, DelegateBridge bridge, Collection attackableUnits, int hitCount, boolean defender, String text)
	{
		if(hitCount == 0)
			return Collections.EMPTY_LIST;
		
		PlayerID player = defender ?  m_defender : m_attacker;
		
		return BattleCalculator.selectCasualties(step, player, attackableUnits, hitCount, bridge, text, m_data);
	}

	private int getCasualties(Collection units, boolean defending, DelegateBridge bridge)
	{
		Iterator iter = units.iterator();
		int count = 0;
		PlayerID player = defending ? m_defender : m_attacker;
		int rollCount = BattleCalculator.getRolls(units, player, defending);
		int[] dice = bridge.getRandom(Constants.MAX_DICE, rollCount);
		int index = 0;
		while(iter.hasNext())
		{
			Unit current = (Unit) iter.next();
			UnitAttatchment ua = UnitAttatchment.get(current.getType());
			int rolls = defending ? 1 : ua.getAttackRolls(player);
			for(int i = 0; i < rolls; i++)
			{
				int strength;
				if(defending)
					strength = ua.getDefense(current.getOwner());
				else
					strength = ua.getAttack(current.getOwner());
		
				//dice is [0-MAX_DICE)
				if( strength > dice[index])
					count++;
				index++;
			}
		}
		return count;
	}

	private void removeCasualties(Collection casualties, boolean canReturnFire, boolean defender, DelegateBridge bridge)
	{
	
		if(canReturnFire)
		{
			//move to waiting to die
			if(defender)
				m_defendingWaitingToDie.addAll(casualties);
			else
				m_attackingWaitingToDie.addAll(casualties);
		} else
			//remove immediately
			remove(casualties, bridge);
		
		//remove from the active fighting
		if(defender)
			m_defendingUnits.removeAll(casualties);
		else 
			m_attackingUnits.removeAll(casualties);
		
	}
	
	private void fireNavalBombardment(DelegateBridge bridge)
	{
		Collection bombard = getBombardingUnits();
		if(bombard.size() > 0 && m_defendingUnits.size() > 0)
			fire(SELECT_NAVAL_BOMBARDMENT_CASUALTIES, bombard, m_defendingUnits, false, true, bridge, "Bombard");
		
	}
	
	private Collection getBombardingUnits()
	{
		Iterator territories = m_amphibiousAttackFrom.iterator();
		Collection bombard = new ArrayList();
		while(territories.hasNext())
		{
			Territory possible = (Territory) territories.next();
			if(m_tracker.hasPendingBattle(possible, false))
				throw new IllegalStateException("Navel battle pending where amphibious assault originated");
			if(! m_tracker.wasBattleFought(possible))
			{
				bombard.addAll( possible.getUnits().getMatches(Matches.UnitCanBombard));
			}
		}
		return bombard;
	}
	
	private void fireAAGuns(DelegateBridge bridge)
	{
		String step = AA_GUNS_FIRE;
		if(!canFireAA())
			return;
		
		int attackingAirCount = Match.countMatches(m_attackingUnits, Matches.UnitIsAir);
		
		int hitCount = BattleCalculator.getAAHits(m_attackingUnits, bridge);
		if(hitCount == 0)
			bridge.sendMessage(new BattleStringMessage(step, "No AA hits"));
		else
		{
			Collection attackable = Match.getMatches(m_attackingUnits, Matches.UnitIsAir);
			Collection casualties = selectCasualties(step, bridge,attackable, hitCount, false,"AA guns fire,");
			removeCasualties(casualties, false, false, bridge);
		}
	}
	
	private boolean canFireAA()
	{
		return 
			Match.someMatch(m_defendingUnits, Matches.UnitIsAA) &&
			Match.someMatch(m_attackingUnits, Matches.UnitIsAir);
	}
		
	private void removeNonCombatants()
	{
		m_defendingNonCombatants = Match.getMatches(m_defendingUnits, Matches.UnitIsAAOrFactory);
		if(m_territory.isWater())
			m_defendingNonCombatants.addAll( Match.getMatches(m_defendingUnits, Matches.UnitIsLand));

		m_defendingUnits.removeAll(m_defendingNonCombatants);
		
		if(m_territory.isWater() )
			m_attackingNonCombatants = Match.getMatches(m_attackingUnits, Matches.UnitIsLand);
		else
			m_attackingNonCombatants = new ArrayList();
		
		m_attackingUnits.removeAll(m_attackingNonCombatants);
	}
	
	private Collection getTransportedUnits(Collection transports)
	{
		Iterator iter = transports.iterator();
		Collection transported = new ArrayList();
		while(iter.hasNext())
		{
			Collection transporting = DelegateFinder.moveDelegate(m_data).getTransportTracker().transporting((Unit) iter.next());
			if(transporting != null)
				transported.addAll(transporting);
		}
		return transported;
	}
	
	private void remove(Collection units, DelegateBridge bridge)
	{		
		if(units.size() == 0)
			return;
		
		//get the transported units
		if(m_territory.isWater())
		{
			Collection transported = getTransportedUnits(units);
			units.addAll(transported);
		}
		bridge.addChange(ChangeFactory.removeUnits(m_territory,units));
		removeFromDependents(units, bridge);
		
		String transcriptText = Formatter.unitsToText(units) + " lost in " + m_territory.getName();
		bridge.getTranscript().write(transcriptText);
		
	}
	
	private void removeFromDependents(Collection units, DelegateBridge bridge)
	{
		Collection dependents = m_tracker.getBlocked(this);
		Iterator iter = dependents.iterator();
		while(iter.hasNext())
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
		//for symmetry
	}
	
	private void attackerWins(DelegateBridge bridge)
	{
		//do we need to change ownership
		if(!m_territory.isWater())
		{
			
			if(Match.someMatch(m_attackingUnits, Matches.UnitIsNotAir))
			{
				m_tracker.addToConquered(m_territory);
				m_tracker.takeOver(m_territory, m_attacker, bridge, m_data);	
			}
		}
	}
	
	/**
	 * Returns a map of UnitType
	 */
	private IntegerMap toIntegerMap(Collection units)
	{
		IntegerMap rVal = new IntegerMap();
		Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			rVal.add(unit.getType(), 1);
		}
		return rVal;
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
		"Battle in:" + m_territory +
		" attacked by:" + m_attackingUnits + 
		" from:" + m_attackingFrom + 
		" defender:" + m_defender +
		" bombing:" + isBombingRun();
	}
	
	public void unitsLost(Battle battle, Collection units, DelegateBridge bridge) 
	{
		Iterator iter = units.iterator();
		Collection lost = new ArrayList();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			Collection dependent = (Collection) m_dependentUnits.get(unit);
			if(dependent != null)
				lost.addAll(dependent);
		}
		if(lost.size() != 0)
			remove(lost, bridge);
	}		
}