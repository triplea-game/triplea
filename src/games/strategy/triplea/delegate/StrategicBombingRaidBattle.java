/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * StrategicBombingRaidBattle.java
 * 
 * Created on November 29, 2001, 2:21 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.BattleRecords;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Sean Bridges
 * @version 1.0
 */
public class StrategicBombingRaidBattle extends AbstractBattle
{
	private static final long serialVersionUID = 8490171037606078890L;
	private final static String RAID = "Strategic bombing raid";
	private final static String FIRE_AA = "Fire AA";
	
	protected final HashMap<Unit, HashSet<Unit>> m_targets = new HashMap<Unit, HashSet<Unit>>(); // these would be the factories or other targets. does not include aa.
	protected final PlayerID m_defender;
	protected final ExecutionStack m_stack = new ExecutionStack();
	protected List<String> m_steps;
	protected List<Unit> m_defendingAA;
	protected Set<String> m_AAtypes;
	
	private int m_bombingRaidTotal;
	private final IntegerMap<Unit> m_bombingRaidDamage = new IntegerMap<Unit>();
	
	/**
	 * Creates new StrategicBombingRaidBattle
	 * 
	 * @param battleSite
	 *            - battle territory
	 * @param data
	 *            - game data
	 * @param attacker
	 *            - attacker PlayerID
	 * @param defender
	 *            - defender PlayerID
	 * @param battleTracker
	 *            - BattleTracker
	 **/
	public StrategicBombingRaidBattle(final Territory battleSite, final GameData data, final PlayerID attacker, final PlayerID defender, final BattleTracker battleTracker)
	{
		super(battleSite, attacker, battleTracker, true, BattleType.BOMBING_RAID, data);
		m_defender = defender;
		m_isAmphibious = false;
		
		// fill in defenders
		final Match<Unit> defenders = new CompositeMatchOr<Unit>(Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(m_battleSite).invert(), Matches.UnitIsAAthatCanFire(m_attackingUnits, m_attacker,
					Matches.UnitIsAAforBombingThisUnitOnly, m_data));
		if (m_targets.isEmpty())
		{
			m_defendingUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), defenders);
		}
		else
		{
			final List<Unit> targets = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsAAthatCanFire(m_attackingUnits, m_attacker, Matches.UnitIsAAforBombingThisUnitOnly, m_data));
			targets.addAll(m_targets.keySet());
			m_defendingUnits = targets;
		}
	}
	
	/*@Override
	public List<Unit> getDefendingUnits()
	{
		final Match<Unit> defenders = new CompositeMatchOr<Unit>(Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(m_battleSite).invert(), Matches.UnitIsAAthatCanFire(m_attackingUnits, m_attacker,
					Matches.UnitIsAAforBombingThisUnitOnly, m_data));
		if (m_targets.isEmpty())
			return Match.getMatches(m_battleSite.getUnits().getUnits(), defenders);
		final List<Unit> targets = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsAAthatCanFire(m_attackingUnits, m_attacker, Matches.UnitIsAAforBombingThisUnitOnly, m_data));
		targets.addAll(m_targets.keySet());
		return targets;
	}*/

	/**
	 * @param bridge
	 * @return
	 */
	protected ITripleaDisplay getDisplay(final IDelegateBridge bridge)
	{
		return (ITripleaDisplay) bridge.getDisplayChannelBroadcaster();
	}
	
	@Override
	public boolean isEmpty()
	{
		return m_attackingUnits.isEmpty();
	}
	
	@Override
	public void removeAttack(final Route route, final Collection<Unit> units)
	{
		removeAttackers(units, true);
	}
	
	private void removeAttackers(final Collection<Unit> units, final boolean removeTarget)
	{
		m_attackingUnits.removeAll(units);
		final Iterator<Unit> targetIter = m_targets.keySet().iterator();
		while (targetIter.hasNext())
		{
			final HashSet<Unit> currentAttackers = m_targets.get(targetIter.next());
			currentAttackers.removeAll(units);
			if (currentAttackers.isEmpty() && removeTarget)
				targetIter.remove();
		}
	}
	
	private Unit getTarget(final Unit attacker)
	{
		for (final Unit target : m_targets.keySet())
		{
			if (m_targets.get(target).contains(attacker))
				return target;
		}
		throw new IllegalStateException("Unit " + attacker.getType().getName() + " has no target");
	}
	
	@Override
	public Change addAttackChange(final Route route, final Collection<Unit> units, final HashMap<Unit, HashSet<Unit>> targets)
	{
		m_attackingUnits.addAll(units);
		if (targets == null)
			return ChangeFactory.EMPTY_CHANGE;
		for (final Unit target : targets.keySet())
		{
			HashSet<Unit> currentAttackers = m_targets.get(target);
			if (currentAttackers == null)
				currentAttackers = new HashSet<Unit>();
			currentAttackers.addAll(targets.get(target));
			m_targets.put(target, currentAttackers);
		}
		return ChangeFactory.EMPTY_CHANGE;
	}
	
	@Override
	public void fight(final IDelegateBridge bridge)
	{
		// we were interrupted
		if (m_stack.isExecuting())
		{
			showBattle(bridge);
			m_stack.execute(bridge);
			return;
		}
		bridge.getHistoryWriter().startEvent("Strategic bombing raid in " + m_battleSite);
		bridge.getHistoryWriter().setRenderingData(m_battleSite);
		BattleCalculator.sortPreBattle(m_attackingUnits, m_data);
		// TODO: determine if the target has the property, not just any unit with the property isAAforBombingThisUnitOnly
		m_defendingAA = m_battleSite.getUnits().getMatches(Matches.UnitIsAAthatCanFire(m_attackingUnits, m_attacker, Matches.UnitIsAAforBombingThisUnitOnly, m_data));
		m_AAtypes = UnitAttachment.getAllOfTypeAAs(m_defendingAA); // TODO: order this list in some way
		final boolean hasAA = m_defendingAA.size() > 0;
		m_steps = new ArrayList<String>();
		if (hasAA)
		{
			for (int i = 0; i < UnitAttachment.getAllOfTypeAAs(m_defendingAA).size(); ++i)
			{
				m_steps.add(FIRE_AA);
			}
		}
		m_steps.add(RAID);
		showBattle(bridge);
		final List<IExecutable> steps = new ArrayList<IExecutable>();
		if (hasAA)
		{
			steps.add(new FireAA());
		}
		steps.add(new ConductBombing());
		steps.add(new IExecutable()
		{
			private static final long serialVersionUID = 4299575008166316488L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				getDisplay(bridge).gotoBattleStep(m_battleID, RAID);
				m_battleTracker.removeBattle(StrategicBombingRaidBattle.this);
				if (isSBRAffectsUnitProduction())
					bridge.getHistoryWriter().addChildToEvent("AA raid costs " + m_bombingRaidTotal + " " + " production in " + m_battleSite.getName());
				else if (isDamageFromBombingDoneToUnitsInsteadOfTerritories())
					bridge.getHistoryWriter().addChildToEvent("Bombing raid in " + m_battleSite.getName() + " causes " + m_bombingRaidTotal + " damage total. " +
								(m_bombingRaidDamage.size() > 1 ? ("\r\n Damaged units is as follows: " + MyFormatter.integerUnitMapToString(m_bombingRaidDamage)) : ""));
				else
					bridge.getHistoryWriter().addChildToEvent("AA raid costs " + m_bombingRaidTotal + " " + MyFormatter.pluralize("PU", m_bombingRaidTotal));
				// TODO remove the reference to the constant.japanese- replace with a rule
				if (isPacificTheater() || isSBRVictoryPoints())
				{
					if (m_defender.getName().equals(Constants.JAPANESE))
					{
						Change changeVP;
						final PlayerAttachment pa = PlayerAttachment.get(m_defender);
						if (pa != null)
						{
							changeVP = ChangeFactory.attachmentPropertyChange(pa, ((-(m_bombingRaidTotal / 10)) + pa.getVps()), "vps");
							bridge.addChange(changeVP);
							bridge.getHistoryWriter().addChildToEvent("AA raid costs " + (m_bombingRaidTotal / 10) + " " + MyFormatter.pluralize("vp", (m_bombingRaidTotal / 10)));
						}
					}
				}
				// kill any suicide attackers (veqryn)
				if (Match.someMatch(m_attackingUnits, Matches.UnitIsSuicide))
				{
					final List<Unit> suicideUnits = Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide);
					m_attackingUnits.removeAll(suicideUnits);
					final Change removeSuicide = ChangeFactory.removeUnits(m_battleSite, suicideUnits);
					final String transcriptText = MyFormatter.unitsToText(suicideUnits) + " lost in " + m_battleSite.getName();
					final IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(m_attacker, m_data);
					final int tuvLostAttacker = BattleCalculator.getTUV(suicideUnits, m_attacker, costs, m_data);
					m_attackerLostTUV += tuvLostAttacker;
					bridge.getHistoryWriter().addChildToEvent(transcriptText, suicideUnits);
					bridge.addChange(removeSuicide);
				}
				// kill any units that can die if they have reached max damage (veqryn)
				if (Match.someMatch(m_targets.keySet(), Matches.UnitCanDieFromReachingMaxDamage))
				{
					final List<Unit> unitsCanDie = Match.getMatches(m_targets.keySet(), Matches.UnitCanDieFromReachingMaxDamage);
					unitsCanDie.retainAll(Match.getMatches(unitsCanDie, Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(m_battleSite)));
					if (!unitsCanDie.isEmpty())
					{
						// m_targets.removeAll(unitsCanDie);
						final Change removeDead = ChangeFactory.removeUnits(m_battleSite, unitsCanDie);
						final String transcriptText = MyFormatter.unitsToText(unitsCanDie) + " lost in " + m_battleSite.getName();
						final IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(m_defender, m_data);
						final int tuvLostDefender = BattleCalculator.getTUV(unitsCanDie, m_defender, costs, m_data);
						m_defenderLostTUV += tuvLostDefender;
						bridge.getHistoryWriter().addChildToEvent(transcriptText, unitsCanDie);
						bridge.addChange(removeDead);
					}
				}
			}
		});
		steps.add(new IExecutable()
		{
			private static final long serialVersionUID = -7649516174883172328L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				if (isSBRAffectsUnitProduction())
					getDisplay(bridge).battleEnd(m_battleID, "Bombing raid cost " + m_bombingRaidTotal + " production.");
				else if (isDamageFromBombingDoneToUnitsInsteadOfTerritories())
					getDisplay(bridge).battleEnd(m_battleID, "Raid causes " + m_bombingRaidTotal + " damage total." +
								(m_bombingRaidDamage.size() > 1 ? (" To units: " + MyFormatter.integerUnitMapToString(m_bombingRaidDamage)) : ""));
				else
					getDisplay(bridge).battleEnd(m_battleID, "Bombing raid cost " + m_bombingRaidTotal + " " + MyFormatter.pluralize("PU", m_bombingRaidTotal));
				if (m_bombingRaidTotal > 0)
					m_battleResult = BattleRecords.BattleResultDescription.BOMBED;
				else
					m_battleResult = BattleRecords.BattleResultDescription.LOST;
				m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV, m_defenderLostTUV, m_battleResult, m_bombingRaidTotal);
				m_isOver = true;
			}
		});
		Collections.reverse(steps);
		for (final IExecutable executable : steps)
		{
			m_stack.push(executable);
		}
		m_stack.execute(bridge);
	}
	
	private void showBattle(final IDelegateBridge bridge)
	{
		final String title = "Bombing raid in " + m_battleSite.getName();
		getDisplay(bridge).showBattle(m_battleID, m_battleSite, title, m_attackingUnits, getDefendingUnits(),
					null, null, null, Collections.<Unit, Collection<Unit>> emptyMap(), m_attacker, m_defender, getBattleType());
		getDisplay(bridge).listBattleSteps(m_battleID, m_steps);
	}
	
	
	class FireAA implements IExecutable
	{
		private static final long serialVersionUID = -4667856856747597406L;
		DiceRoll m_dice;
		Collection<Unit> m_casualties;
		
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			final boolean isEditMode = EditDelegate.getEditMode(bridge.getData());
			for (final String currentTypeAA : m_AAtypes)
			{
				final Collection<Unit> currentPossibleAA = Match.getMatches(m_defendingAA, Matches.UnitIsAAofTypeAA(currentTypeAA));
				final Set<UnitType> targetUnitTypesForThisTypeAA = UnitAttachment.get(currentPossibleAA.iterator().next().getType()).getTargetsAA(m_data);
				
				final IExecutable roll = new IExecutable()
				{
					private static final long serialVersionUID = 379538344036513009L;
					
					public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
					{
						m_dice = DiceRoll.rollAA(m_attackingUnits, currentPossibleAA, targetUnitTypesForThisTypeAA, bridge, m_battleSite);
					}
				};
				final IExecutable calculateCasualties = new IExecutable()
				{
					private static final long serialVersionUID = -4658133491636765763L;
					
					public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
					{
						m_casualties = calculateCasualties(m_attackingUnits, currentPossibleAA, targetUnitTypesForThisTypeAA, bridge, m_dice);
					}
				};
				final IExecutable notifyCasualties = new IExecutable()
				{
					private static final long serialVersionUID = -4989154196975570919L;
					
					public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
					{
						notifyAAHits(bridge, m_dice, m_casualties);
					}
				};
				final IExecutable removeHits = new IExecutable()
				{
					private static final long serialVersionUID = -3673833177336068509L;
					
					public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
					{
						removeAAHits(bridge, m_dice, m_casualties);
					}
				};
				// push in reverse order of execution
				stack.push(removeHits);
				stack.push(notifyCasualties);
				stack.push(calculateCasualties);
				if (!isEditMode)
					stack.push(roll);
			}
		}
	}
	
	private boolean isSBRAffectsUnitProduction()
	{
		return games.strategy.triplea.Properties.getSBRAffectsUnitProduction(m_data);
	}
	
	private boolean isDamageFromBombingDoneToUnitsInsteadOfTerritories()
	{
		return games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(m_data);
	}
	
	private boolean isWW2V2()
	{
		return games.strategy.triplea.Properties.getWW2V2(m_data);
	}
	
	private boolean isLimitSBRDamageToProduction()
	{
		return games.strategy.triplea.Properties.getLimitSBRDamageToProduction(m_data);
	}
	
	private boolean isLimitSBRDamagePerTurn(final GameData data)
	{
		return games.strategy.triplea.Properties.getLimitSBRDamagePerTurn(data);
	}
	
	private ITripleaPlayer getRemote(final IDelegateBridge bridge)
	{
		return (ITripleaPlayer) bridge.getRemote();
	}
	
	private boolean isPUCap(final GameData data)
	{
		return games.strategy.triplea.Properties.getPUCap(data);
	}
	
	private boolean isSBRVictoryPoints()
	{
		return games.strategy.triplea.Properties.getSBRVictoryPoint(m_data);
	}
	
	private boolean isPacificTheater()
	{
		return games.strategy.triplea.Properties.getPacificTheater(m_data);
	}
	
	private Collection<Unit> calculateCasualties(final Collection<Unit> attackingUnitsAll, final Collection<Unit> defendingAA, final Set<UnitType> targetUnitTypesForThisTypeAA,
				final IDelegateBridge bridge, final DiceRoll dice)
	{
		final Collection<Unit> validAttackingUnitsForThisRoll = Match.getMatches(attackingUnitsAll, Matches.unitIsOfTypes(targetUnitTypesForThisTypeAA));
		final boolean isEditMode = EditDelegate.getEditMode(m_data);
		if (isEditMode)
		{
			final String text = "AA guns fire";
			final CasualtyDetails casualtySelection = BattleCalculator.selectCasualties(RAID, m_attacker, validAttackingUnitsForThisRoll, bridge, text, /* dice */null,/* defending */false,
						m_battleID, /* head-less */
						false, 0);
			return casualtySelection.getKilled();
		}
		final Collection<Unit> casualties = BattleCalculator.getAACasualties(validAttackingUnitsForThisRoll, defendingAA, dice, bridge, m_defender, m_attacker, m_battleID, m_battleSite);
		// AA guns, at this point, kill units outright. so hits should equal casualties unless we have extra hits.
		final int totalExpectingHits = dice.getHits() > validAttackingUnitsForThisRoll.size() ? validAttackingUnitsForThisRoll.size() : dice.getHits();
		// TODO: we should allow aa guns to cause hits instead of kills, that way 2-hit air units could survive aa guns if they map maker wants them to.
		if (casualties.size() != totalExpectingHits)
			throw new IllegalStateException("Wrong number of casualties, expecting:" + totalExpectingHits + " but got:" + casualties.size());
		return casualties;
	}
	
	private void notifyAAHits(final IDelegateBridge bridge, final DiceRoll dice, final Collection<Unit> casualties)
	{
		getDisplay(bridge).casualtyNotification(m_battleID, FIRE_AA, dice, m_attacker, casualties, Collections.<Unit> emptyList(), Collections.<Unit, Collection<Unit>> emptyMap());
		final Runnable r = new Runnable()
		{
			public void run()
			{
				final ITripleaPlayer defender = (ITripleaPlayer) bridge.getRemote(m_defender);
				defender.confirmEnemyCasualties(m_battleID, "Press space to continue", m_attacker);
			}
		};
		final Thread t = new Thread(r, "click to continue waiter");
		t.start();
		final ITripleaPlayer attacker = (ITripleaPlayer) bridge.getRemote(m_attacker);
		attacker.confirmOwnCasualties(m_battleID, "Press space to continue");
		try
		{
			bridge.leaveDelegateExecution();
			t.join();
		} catch (final InterruptedException e)
		{
			// ignore
		} finally
		{
			bridge.enterDelegateExecution();
		}
	}
	
	private void removeAAHits(final IDelegateBridge bridge, final DiceRoll dice, final Collection<Unit> casualties)
	{
		if (!casualties.isEmpty())
			bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToTextNoOwner(casualties) + " killed by AA guns", casualties);
		
		final IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(m_attacker, m_data);
		final int tuvLostAttacker = BattleCalculator.getTUV(casualties, m_attacker, costs, m_data);
		m_attackerLostTUV += tuvLostAttacker;
		// m_attackingUnits.removeAll(casualties);
		removeAttackers(casualties, false);
		final Change remove = ChangeFactory.removeUnits(m_battleSite, casualties);
		bridge.addChange(remove);
	}
	
	
	class ConductBombing implements IExecutable
	{
		private static final long serialVersionUID = 5579796391988452213L;
		private int[] m_dice;
		
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			final IExecutable rollDice = new IExecutable()
			{
				private static final long serialVersionUID = -4097858758514452368L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					rollDice(bridge);
				}
			};
			final IExecutable findCost = new IExecutable()
			{
				private static final long serialVersionUID = 8573539936364094095L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					findCost(bridge);
				}
			};
			// push in reverse order of execution
			m_stack.push(findCost);
			m_stack.push(rollDice);
		}
		
		private void rollDice(final IDelegateBridge bridge)
		{
			final int rollCount = BattleCalculator.getRolls(m_attackingUnits, m_battleSite, m_attacker, false);
			if (rollCount == 0)
			{
				m_dice = null;
				return;
			}
			m_dice = new int[rollCount];
			final boolean isEditMode = EditDelegate.getEditMode(m_data);
			if (isEditMode)
			{
				final String annotation = m_attacker.getName() + " fixing dice to allocate cost of strategic bombing raid against " + m_defender.getName() + " in " + m_battleSite.getName();
				final ITripleaPlayer attacker = (ITripleaPlayer) bridge.getRemote(m_attacker);
				m_dice = attacker.selectFixedDice(rollCount, 0, true, annotation, m_data.getDiceSides()); // does not take into account bombers with dice sides higher than getDiceSides
			}
			else
			{
				final String annotation = m_attacker.getName() + " rolling to allocate cost of strategic bombing raid against " + m_defender.getName() + " in " + m_battleSite.getName();
				if (!games.strategy.triplea.Properties.getLL_DAMAGE_ONLY(m_data))
					m_dice = bridge.getRandom(m_data.getDiceSides(), rollCount, annotation);
				else
				{
					int i = 0;
					final int diceSides = m_data.getDiceSides();
					for (final Unit u : m_attackingUnits)
					{
						final UnitAttachment ua = UnitAttachment.get(u.getType());
						int maxDice = ua.getBombingMaxDieSides();
						int bonus = ua.getBombingBonus();
						if (maxDice < 0 && bonus < 0 && diceSides >= 5)
						{
							maxDice = (diceSides + 1) / 3;
							bonus = (diceSides + 1) / 3;
						}
						if (bonus < 0)
							bonus = 0;
						if (maxDice < 0)
							maxDice = diceSides;
						if (maxDice > 0)
							m_dice[i] = bridge.getRandom(maxDice, annotation) + bonus;
						else
							m_dice[i] = bonus;
						i++;
					}
				}
			}
		}
		
		private void findCost(final IDelegateBridge bridge)
		{
			// if no planes left after aa fires, this is possible
			if (m_attackingUnits.isEmpty())
			{
				return;
			}
			final TerritoryAttachment ta = TerritoryAttachment.get(m_battleSite);
			int cost = 0;
			final boolean lhtrHeavyBombers = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(m_data);
			// boolean lhtrHeavyBombers = m_data.getProperties().get(Constants.LHTR_HEAVY_BOMBERS, false);
			int damageLimit = ta.getProduction();
			final Iterator<Unit> iter = m_attackingUnits.iterator();
			int index = 0;
			final Boolean limitDamage = isWW2V2() || isLimitSBRDamageToProduction();
			// limit to maxDamage
			while (iter.hasNext())
			{
				final Unit attacker = iter.next();
				int rolls;
				rolls = BattleCalculator.getRolls(attacker, m_battleSite, m_attacker, false);
				int costThisUnit = 0;
				if (lhtrHeavyBombers && rolls > 1)
				{
					int max = 0;
					for (int i = 0; i < rolls; i++)
					{
						// +2 since 0 based (LHTR adds +1 to base roll)
						max = Math.max(max, m_dice[index] + 2);
						index++;
					}
					costThisUnit = max;
				}
				else
				{
					for (int i = 0; i < rolls; i++)
					{
						costThisUnit += m_dice[index] + 1;
						index++;
					}
				}
				if (limitDamage)
					costThisUnit = Math.min(costThisUnit, damageLimit);
				cost += costThisUnit;
				if (!m_targets.isEmpty())
					m_bombingRaidDamage.add(getTarget(attacker), costThisUnit);
			}
			// Limit PUs lost if we would like to cap PUs lost at territory value
			if (isPUCap(m_data) || isLimitSBRDamagePerTurn(m_data))
			{
				final int alreadyLost = DelegateFinder.moveDelegate(m_data).PUsAlreadyLost(m_battleSite);
				final int limit = Math.max(0, damageLimit - alreadyLost);
				cost = Math.min(cost, limit);
				if (!m_targets.isEmpty())
				{
					for (final Unit u : m_bombingRaidDamage.keySet())
					{
						if (m_bombingRaidDamage.getInt(u) > limit)
							m_bombingRaidDamage.put(u, limit);
					}
				}
			}
			// If we damage units instead of territories
			if (isDamageFromBombingDoneToUnitsInsteadOfTerritories() && !isSBRAffectsUnitProduction())
			{
				// at this point, m_bombingRaidDamage should contain all units that m_targets contains
				if (!m_targets.keySet().containsAll(m_bombingRaidDamage.keySet()))
					throw new IllegalStateException("targets should contain all damaged units");
				for (final Unit current : m_bombingRaidDamage.keySet())
				{
					int currentUnitCost = m_bombingRaidDamage.getInt(current);
					// determine the max allowed damage
					// UnitAttachment ua = UnitAttachment.get(current.getType());
					final TripleAUnit taUnit = (TripleAUnit) current;
					damageLimit = taUnit.getHowMuchMoreDamageCanThisUnitTake(current, m_battleSite);
					if (m_bombingRaidDamage.getInt(current) > damageLimit)
					{
						m_bombingRaidDamage.put(current, damageLimit);
						cost = (cost - currentUnitCost) + damageLimit;
						currentUnitCost = m_bombingRaidDamage.getInt(current);
					}
					final int totalDamage = taUnit.getUnitDamage() + currentUnitCost;
					// display the results
					getDisplay(bridge).bombingResults(m_battleID, m_dice, currentUnitCost);
					// Record production lost
					DelegateFinder.moveDelegate(m_data).PUsLost(m_battleSite, currentUnitCost);
					// apply the hits to the targets
					final IntegerMap<Unit> hits = new IntegerMap<Unit>();
					final CompositeChange change = new CompositeChange();
					hits.put(current, 1);
					change.add(ChangeFactory.unitPropertyChange(current, totalDamage, TripleAUnit.UNIT_DAMAGE));
					// taUnit.setUnitDamage(totalDamage);
					bridge.addChange(ChangeFactory.unitsHit(hits));
					bridge.addChange(change);
					bridge.getHistoryWriter().startEvent("Bombing raid in " + m_battleSite.getName() + " causes: " + currentUnitCost + " damage to unit: " + current.getType().getName());
					getRemote(bridge).reportMessage("Bombing raid in " + m_battleSite.getName() + " causes: " + currentUnitCost + " damage to unit: " + current.getType().getName(),
								"Bombing raid causes " + currentUnitCost + " damage to " + current.getType().getName());
				}
			}
			else if (isSBRAffectsUnitProduction())
			{
				// the old way of doing it, based on doing damage to the territory rather than the unit
				// get current production
				final int unitProduction = ta.getUnitProduction();
				// Determine the max that can be taken as losses
				final int alreadyLost = damageLimit - unitProduction;
				final int limit = 2 * damageLimit - alreadyLost;
				cost = Math.min(cost, limit);
				getDisplay(bridge).bombingResults(m_battleID, m_dice, cost);
				// Record production lost
				DelegateFinder.moveDelegate(m_data).PUsLost(m_battleSite, cost);
				final Collection<Unit> damagedFactory = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsFactoryOrCanBeDamaged);
				final IntegerMap<Unit> hits = new IntegerMap<Unit>();
				for (final Unit factory : damagedFactory)
				{
					hits.put(factory, 1);
				}
				// add a hit to the factory
				bridge.addChange(ChangeFactory.unitsHit(hits));
				final Integer newProduction = unitProduction - cost;
				// decrease the unitProduction capacity of the territory
				final Change change = ChangeFactory.attachmentPropertyChange(ta, newProduction.toString(), "unitProduction");
				bridge.addChange(change);
				bridge.getHistoryWriter().startEvent("Bombing raid in " + m_battleSite.getName() + " costs: " + cost + " production.");
				getRemote(bridge).reportMessage("Bombing raid in " + m_battleSite.getName() + " costs: " + cost + " production.",
							"Bombing raid in " + m_battleSite.getName() + " costs: " + cost + " production.");
			}
			else
			{
				// Record PUs lost
				DelegateFinder.moveDelegate(m_data).PUsLost(m_battleSite, cost);
				cost *= Properties.getPU_Multiplier(m_data);
				getDisplay(bridge).bombingResults(m_battleID, m_dice, cost);
				// get resources
				final Resource PUs = m_data.getResourceList().getResource(Constants.PUS);
				final int have = m_defender.getResources().getQuantity(PUs);
				final int toRemove = Math.min(cost, have);
				final Change change = ChangeFactory.changeResourcesChange(m_defender, PUs, -toRemove);
				bridge.addChange(change);
			}
			m_bombingRaidTotal = cost;
		}
	}
	
	@Override
	public void unitsLostInPrecedingBattle(final IBattle battle, final Collection<Unit> units, final IDelegateBridge bridge)
	{
		// should never happen
		throw new IllegalStateException("StrategicBombingRaidBattle should not have any preceding battle with which to possibly remove dependents from");
	}
}
