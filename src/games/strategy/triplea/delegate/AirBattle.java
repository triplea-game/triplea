package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.RouteScripted;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
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

/**
 * @author Mark Christopher Duncan (veqryn)
 * 
 */
public class AirBattle extends AbstractBattle
{
	private static final long serialVersionUID = 4686241714027216395L;
	protected final static String AIR_BATTLE = "Air Battle";
	protected final static String INTERCEPTORS_LAUNCH = "Defender Launches Interceptors";
	protected final static String ATTACKERS_FIRE = "Attackers Fire";
	protected final static String DEFENDERS_FIRE = "Defenders Fire";
	protected final static String ATTACKERS_WITHDRAW = "Attackers Withdraw?";
	protected final static String DEFENDERS_WITHDRAW = "Defenders Withdraw?";
	// protected final static String BOMBERS_TO_TARGETS = "Bombers Fly to Their Targets";
	
	protected final ExecutionStack m_stack = new ExecutionStack();
	protected List<String> m_steps;
	protected final Collection<Unit> m_defendingWaitingToDie = new ArrayList<Unit>();
	protected final Collection<Unit> m_attackingWaitingToDie = new ArrayList<Unit>();
	protected boolean m_intercept = false;
	protected final int m_maxRounds; // -1 would mean forever until one side is eliminated
	
	public AirBattle(final Territory battleSite, final boolean bombingRaid, final GameData data, final PlayerID attacker, final BattleTracker battleTracker)
	{
		super(battleSite, attacker, battleTracker, bombingRaid, (bombingRaid ? BattleType.AIR_RAID : BattleType.AIR_BATTLE), data);
		m_isAmphibious = false;
		m_maxRounds = games.strategy.triplea.Properties.getAirBattleRounds(data);
		updateDefendingUnits();
	}
	
	protected void updateDefendingUnits()
	{
		// fill in defenders
		if (m_isBombingRun)
			m_defendingUnits = m_battleSite.getUnits().getMatches(defendingBombingRaidInterceptors(m_attacker, m_data));
		else
			m_defendingUnits = m_battleSite.getUnits().getMatches(defendingGroundSeaBattleInterceptors(m_attacker, m_data));
	}
	
	@Override
	public Change addAttackChange(final Route route, final Collection<Unit> units, final HashMap<Unit, HashSet<Unit>> targets)
	{
		m_attackingUnits.addAll(units);
		return ChangeFactory.EMPTY_CHANGE;
	}
	
	@Override
	public void removeAttack(final Route route, final Collection<Unit> units)
	{
		m_attackingUnits.removeAll(units);
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
		updateDefendingUnits();
		bridge.getHistoryWriter().startEvent("Air Battle in " + m_battleSite, m_battleSite);
		BattleCalculator.sortPreBattle(m_attackingUnits, m_data);
		BattleCalculator.sortPreBattle(m_defendingUnits, m_data);
		m_steps = determineStepStrings(true, bridge);
		showBattle(bridge);
		pushFightLoopOnStack(true, bridge);
		m_stack.execute(bridge);
	}
	
	private void pushFightLoopOnStack(final boolean firstRun, final IDelegateBridge bridge)
	{
		if (m_isOver)
			return;
		final List<IExecutable> steps = getBattleExecutables(firstRun);
		// add in the reverse order we create them
		Collections.reverse(steps);
		for (final IExecutable step : steps)
		{
			m_stack.push(step);
		}
		return;
	}
	
	public static boolean shouldAirBattleUseAirCombatAttDefValues(final boolean isForBombingRun)
	{
		// TODO: do we want to use the airAttack and airDefense values, or the normal attack and defense values?
		// for m_isBombingRun we definitely want to
		// but for a non bombing run, not so sure yet
		if (isForBombingRun)
			return true;
		else
			return true;
	}
	
	public boolean shouldFightAirBattle()
	{
		if (m_isBombingRun)
			return Match.someMatch(m_attackingUnits, Matches.UnitIsStrategicBomber) && !m_defendingUnits.isEmpty();
		else
			return !m_attackingUnits.isEmpty() && !m_defendingUnits.isEmpty();
	}
	
	public boolean shouldEndAirBattle()
	{
		return m_maxRounds > 0 && m_maxRounds <= m_round;
	}
	
	protected boolean canAttackerRetreat()
	{
		return !shouldEndAirBattle() && shouldFightAirBattle() && games.strategy.triplea.Properties.getAirBattleAttackersCanRetreat(m_data);
	}
	
	protected boolean canDefenderRetreat()
	{
		return !shouldEndAirBattle() && shouldFightAirBattle() && games.strategy.triplea.Properties.getAirBattleDefendersCanRetreat(m_data);
	}
	
	List<IExecutable> getBattleExecutables(final boolean firstRun)
	{
		final List<IExecutable> steps = new ArrayList<IExecutable>();
		if (shouldFightAirBattle())
		{
			if (firstRun)
			{
				steps.add(new InterceptorsLaunch());
			}
			steps.add(new AttackersFire());
			steps.add(new DefendersFire());
			steps.add(new IExecutable() // just calculates lost TUV and kills off any suicide units
			{
				private static final long serialVersionUID = -5575569705493214941L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					// getDisplay(bridge).gotoBattleStep(m_battleID, BOMBERS_TO_TARGETS);
					if (!m_intercept)
						return;
					
					final IntegerMap<UnitType> defenderCosts = BattleCalculator.getCostsForTUV(m_defender, m_data);
					final IntegerMap<UnitType> attackerCosts = BattleCalculator.getCostsForTUV(m_attacker, m_data);
					m_attackingUnits.removeAll(m_attackingWaitingToDie);
					remove(m_attackingWaitingToDie, bridge, m_battleSite);
					m_defendingUnits.removeAll(m_defendingWaitingToDie);
					remove(m_defendingWaitingToDie, bridge, m_battleSite);
					int tuvLostAttacker = BattleCalculator.getTUV(m_attackingWaitingToDie, m_attacker, attackerCosts, m_data);
					m_attackerLostTUV += tuvLostAttacker;
					int tuvLostDefender = BattleCalculator.getTUV(m_defendingWaitingToDie, m_defender, defenderCosts, m_data);
					m_defenderLostTUV += tuvLostDefender;
					m_attackingWaitingToDie.clear();
					m_defendingWaitingToDie.clear();
					
					// kill any suicide attackers (veqryn)
					final CompositeMatch<Unit> attackerSuicide = new CompositeMatchAnd<Unit>(Matches.UnitIsSuicide);
					if (m_isBombingRun)
						attackerSuicide.add(Matches.UnitIsNotStrategicBomber);
					if (Match.someMatch(m_attackingUnits, attackerSuicide))
					{
						final List<Unit> suicideUnits = Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide);
						m_attackingUnits.removeAll(suicideUnits);
						remove(suicideUnits, bridge, m_battleSite);
						tuvLostAttacker = BattleCalculator.getTUV(suicideUnits, m_attacker, attackerCosts, m_data);
						m_attackerLostTUV += tuvLostAttacker;
					}
					if (Match.someMatch(m_defendingUnits, Matches.UnitIsSuicide))
					{
						final List<Unit> suicideUnits = Match.getMatches(m_defendingUnits, Matches.UnitIsSuicide);
						m_defendingUnits.removeAll(suicideUnits);
						remove(suicideUnits, bridge, m_battleSite);
						tuvLostDefender = BattleCalculator.getTUV(suicideUnits, m_defender, defenderCosts, m_data);
						m_defenderLostTUV += tuvLostDefender;
					}
				}
			});
		}
		steps.add(new IExecutable()
		{
			private static final long serialVersionUID = 3148193405425861565L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				if (shouldFightAirBattle() && !shouldEndAirBattle())
					return;
				makeBattle(bridge);
			}
		});
		
		steps.add(new IExecutable()
		{
			private static final long serialVersionUID = 3148193405425861565L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				if (shouldFightAirBattle() && !shouldEndAirBattle())
					return;
				end(bridge);
			}
		});
		steps.add(new IExecutable()
		{
			private static final long serialVersionUID = -5408702756335356985L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				if (!m_isOver && canAttackerRetreat())
				{
					attackerRetreat(bridge);
				}
			}
		});
		steps.add(new IExecutable()
		{
			private static final long serialVersionUID = -7819137222487595113L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				if (!m_isOver && canDefenderRetreat())
				{
					defenderRetreat(bridge);
				}
			}
		});
		final IExecutable loop = new IExecutable()
		{
			private static final long serialVersionUID = -5408702756335356985L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				pushFightLoopOnStack(false, bridge);
			}
		};
		steps.add(new IExecutable()
		{
			private static final long serialVersionUID = -4136481765101946944L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				if (!m_isOver)
				{
					m_steps = determineStepStrings(false, bridge);
					final ITripleaDisplay display = getDisplay(bridge);
					display.listBattleSteps(m_battleID, m_steps);
					m_round++;
					// continue fighting
					// the recursive step
					// this should always be the base of the stack
					// when we execute the loop, it will populate the stack with the battle steps
					if (!m_stack.isEmpty())
						throw new IllegalStateException("Stack not empty:" + m_stack);
					m_stack.push(loop);
				}
			}
		});
		return steps;
	}
	
	public List<String> determineStepStrings(final boolean showFirstRun, final IDelegateBridge bridge)
	{
		final List<String> steps = new ArrayList<String>();
		if (showFirstRun)
		{
			steps.add(AIR_BATTLE);
			steps.add(INTERCEPTORS_LAUNCH);
		}
		steps.add(ATTACKERS_FIRE);
		steps.add(DEFENDERS_FIRE);
		if (canAttackerRetreat())
		{
			steps.add(ATTACKERS_WITHDRAW);
		}
		if (canDefenderRetreat())
		{
			steps.add(DEFENDERS_WITHDRAW);
		}
		// steps.add(BOMBERS_TO_TARGETS);
		return steps;
	}
	
	private void recordUnitsWereInAirBattle(final Collection<Unit> units, final IDelegateBridge bridge)
	{
		final CompositeChange wasInAirBattleChange = new CompositeChange();
		for (final Unit u : units)
		{
			wasInAirBattleChange.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.WAS_IN_AIR_BATTLE));
		}
		if (!wasInAirBattleChange.isEmpty())
		{
			bridge.addChange(wasInAirBattleChange);
		}
	}
	
	private void makeBattle(final IDelegateBridge bridge)
	{
		// record who was in this battle first, so that they do not take part in any ground battles
		if (m_isBombingRun)
		{
			recordUnitsWereInAirBattle(m_attackingUnits, bridge);
			recordUnitsWereInAirBattle(m_defendingUnits, bridge);
		}
		// so as of right now, Air Battles are created before both normal battles and strategic bombing raids
		// once completed, the air battle will create a strategic bombing raid, if that is the purpose of those aircraft
		// however, if the purpose is a normal battle, it will have already been created by the battle tracker / combat move
		// so we do not have to create normal battles, only bombing raids
		
		// setup new battle here
		if (m_isBombingRun)
		{
			final Collection<Unit> bombers = Match.getMatches(m_attackingUnits, Matches.UnitIsStrategicBomber);
			if (!bombers.isEmpty())
			{
				HashMap<Unit, HashSet<Unit>> targets = null;
				final Collection<Unit> enemyTargetsTotal = m_battleSite.getUnits().getMatches(new CompositeMatchAnd<Unit>(
										Matches.enemyUnit(bridge.getPlayerID(), m_data),
										Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(m_battleSite).invert(),
										Matches.unitIsBeingTransported().invert()));
				for (final Unit unit : bombers)
				{
					final Collection<Unit> enemyTargets = Match.getMatches(enemyTargetsTotal, Matches.UnitIsLegalBombingTargetBy(unit));
					if (!enemyTargets.isEmpty())
					{
						Unit target = null;
						if (enemyTargets.size() > 1 && games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(m_data))
						{
							while (target == null)
							{
								target = getRemote(bridge).whatShouldBomberBomb(m_battleSite, enemyTargets, Collections.singletonList(unit));
							}
						}
						else if (!enemyTargets.isEmpty())
							target = enemyTargets.iterator().next();
						if (target != null)
						{
							targets = new HashMap<Unit, HashSet<Unit>>();
							targets.put(target, new HashSet<Unit>(Collections.singleton(unit)));
						}
						m_battleTracker.addBattle(new RouteScripted(m_battleSite), Collections.singleton(unit), true, m_attacker, bridge, null, null, targets, true);
					}
				}
				final IBattle battle = m_battleTracker.getPendingBattle(m_battleSite, true, null);
				final IBattle dependent = m_battleTracker.getPendingBattle(m_battleSite, false, BattleType.NORMAL);
				if (dependent != null)
					m_battleTracker.addDependency(dependent, battle);
				final IBattle dependentAirBattle = m_battleTracker.getPendingBattle(m_battleSite, false, BattleType.AIR_BATTLE);
				if (dependentAirBattle != null)
					m_battleTracker.addDependency(dependentAirBattle, battle);
			}
		}
		else
		{
			if (!m_attackingUnits.isEmpty())
			{
				// currently we already have the battle setup
				// m_battleTracker.addBattle(new RouteScripted(m_battleSite), new ArrayList<Unit>(m_attackingUnits), false, m_attacker, bridge, null, null, null, true);
			}
		}
	}
	
	private void end(final IDelegateBridge bridge)
	{
		// record it
		String text;
		if (!m_attackingUnits.isEmpty())
		{
			if (m_isBombingRun)
			{
				if (Match.someMatch(m_attackingUnits, Matches.UnitIsStrategicBomber))
				{
					m_whoWon = WhoWon.ATTACKER;
					if (m_defendingUnits.isEmpty())
						m_battleResultDescription = BattleRecord.BattleResultDescription.WON_WITHOUT_CONQUERING;
					else
						m_battleResultDescription = BattleRecord.BattleResultDescription.WON_WITH_ENEMY_LEFT;
					text = "Air Battle is over, the remaining bombers go on to their targets";
					ClipPlayer.play(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, m_attacker.getName());
				}
				else
				{
					m_whoWon = WhoWon.DRAW;
					m_battleResultDescription = BattleRecord.BattleResultDescription.STALEMATE;
					text = "Air Battle is over, the bombers have all died";
					ClipPlayer.play(SoundPath.CLIP_BATTLE_FAILURE, m_attacker.getName());
				}
			}
			else
			{
				if (m_defendingUnits.isEmpty())
				{
					m_whoWon = WhoWon.ATTACKER;
					m_battleResultDescription = BattleRecord.BattleResultDescription.WON_WITHOUT_CONQUERING;
					text = "Air Battle is over, the defenders have all died";
					ClipPlayer.play(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, m_attacker.getName());
				}
				else
				{
					m_whoWon = WhoWon.DRAW;
					m_battleResultDescription = BattleRecord.BattleResultDescription.STALEMATE;
					text = "Air Battle is over, neither side is eliminated";
					ClipPlayer.play(SoundPath.CLIP_BATTLE_STALEMATE, m_attacker.getName());
				}
			}
		}
		else
		{
			m_whoWon = WhoWon.DEFENDER;
			m_battleResultDescription = BattleRecord.BattleResultDescription.LOST;
			text = "Air Battle is over, the attackers have all died";
			ClipPlayer.play(SoundPath.CLIP_BATTLE_FAILURE, m_attacker.getName());
		}
		bridge.getHistoryWriter().addChildToEvent(text);
		
		m_battleTracker.getBattleRecords(m_data).addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV, m_defenderLostTUV, m_battleResultDescription,
					new BattleResults(this, m_data), 0);
		getDisplay(bridge).battleEnd(m_battleID, "Air Battle over");
		m_isOver = true;
		m_battleTracker.removeBattle(AirBattle.this);
	}
	
	public void finishBattleAndRemoveFromTrackerHeadless(final IDelegateBridge bridge)
	{
		makeBattle(bridge);
		m_whoWon = WhoWon.ATTACKER;
		m_battleResultDescription = BattleRecord.BattleResultDescription.NO_BATTLE;
		m_battleTracker.getBattleRecords(m_data).removeBattle(m_attacker, m_battleID);
		m_isOver = true;
		m_battleTracker.removeBattle(AirBattle.this);
	}
	
	private void attackerRetreat(final IDelegateBridge bridge)
	{
		// planes retreat to the same square the battle is in, and then should
		// move during non combat to their landing site, or be scrapped if they can't find one.
		final Collection<Territory> possible = new ArrayList<Territory>(2);
		possible.add(m_battleSite);
		// retreat planes
		if (!m_attackingUnits.isEmpty())
			queryRetreat(false, bridge, possible);
	}
	
	private void defenderRetreat(final IDelegateBridge bridge)
	{
		// planes retreat to the same square the battle is in, and then should
		// move during non combat to their landing site, or be scrapped if they can't find one.
		final Collection<Territory> possible = new ArrayList<Territory>(2);
		possible.add(m_battleSite);
		// retreat planes
		if (!m_defendingUnits.isEmpty())
			queryRetreat(true, bridge, possible);
	}
	
	private void queryRetreat(final boolean defender, final IDelegateBridge bridge, final Collection<Territory> availableTerritories)
	{
		if (availableTerritories.isEmpty())
			return;
		final Collection<Unit> units = defender ? new ArrayList<Unit>(m_defendingUnits) : new ArrayList<Unit>(m_attackingUnits);
		if (units.isEmpty())
			return;
		final PlayerID retreatingPlayer = defender ? m_defender : m_attacker;
		final String text = retreatingPlayer.getName() + " retreat?";
		final String step = defender ? DEFENDERS_WITHDRAW : ATTACKERS_WITHDRAW;
		getDisplay(bridge).gotoBattleStep(m_battleID, step);
		final Territory retreatTo = getRemote(retreatingPlayer, bridge).retreatQuery(m_battleID, false, m_battleSite, availableTerritories, text);
		if (retreatTo != null && !availableTerritories.contains(retreatTo))
		{
			System.err.println("Invalid retreat selection :" + retreatTo + " not in " + MyFormatter.defaultNamedToTextList(availableTerritories));
			Thread.dumpStack();
			return;
		}
		if (retreatTo != null)
		{
			if (!m_headless)
				ClipPlayer.play(SoundPath.CLIP_BATTLE_RETREAT_AIR, m_attacker.getName());
			retreat(units, defender, bridge);
			final String messageShort = retreatingPlayer.getName() + " retreats";
			final String messageLong = retreatingPlayer.getName() + " retreats all units to " + retreatTo.getName();
			getDisplay(bridge).notifyRetreat(messageShort, messageLong, step, retreatingPlayer);
		}
	}
	
	private void retreat(final Collection<Unit> retreating, final boolean defender, final IDelegateBridge bridge)
	{
		if (!defender)
		{
			// we must remove any of these units from the land battle that follows (this comes before we remove them from this battle, because after we remove from this battle we are no longer blocking any battles)
			final Collection<IBattle> dependentBattles = m_battleTracker.getBlocked(AirBattle.this);
			removeFromDependents(retreating, bridge, dependentBattles, true);
		}
		final String transcriptText = MyFormatter.unitsToText(retreating) + " retreated";
		final Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
		units.removeAll(retreating);
		bridge.getHistoryWriter().addChildToEvent(transcriptText, retreating);
		recordUnitsWereInAirBattle(retreating, bridge);
	}
	
	private void showBattle(final IDelegateBridge bridge)
	{
		final String title = "Air Battle in " + m_battleSite.getName();
		getDisplay(bridge).showBattle(m_battleID, m_battleSite, title, m_attackingUnits, m_defendingUnits,
					null, null, null, Collections.<Unit, Collection<Unit>> emptyMap(), m_attacker, m_defender, isAmphibious(), getBattleType());
		getDisplay(bridge).listBattleSteps(m_battleID, m_steps);
	}
	
	
	class InterceptorsLaunch implements IExecutable
	{
		private static final long serialVersionUID = 4300406315014471768L;
		
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			getInterceptors(bridge);
			if (!m_defendingUnits.isEmpty())
			{
				m_intercept = true;
				// play a sound
				ClipPlayer.play(SoundPath.CLIP_BATTLE_AIR, m_attacker.getName());
			}
		}
		
		private void getInterceptors(final IDelegateBridge bridge)
		{
			final Collection<Unit> interceptors;
			if (m_isBombingRun)
			{
				interceptors = getRemote(m_defender, bridge).selectUnitsQuery(m_battleSite, new ArrayList<Unit>(m_defendingUnits), "Select Air to Intercept");
			}
			else
			{
				interceptors = new ArrayList<Unit>(m_defendingUnits);
			}
			if (interceptors != null && !m_defendingUnits.containsAll(interceptors))
				throw new IllegalStateException("Interceptors choose from outside of available units");
			final Collection<Unit> beingRemoved = new ArrayList<Unit>(m_defendingUnits);
			m_defendingUnits.clear();
			if (interceptors != null)
			{
				beingRemoved.removeAll(interceptors);
				m_defendingUnits.addAll(interceptors);
			}
			getDisplay(bridge).changedUnitsNotification(m_battleID, m_defender, beingRemoved, null, null);
			
			if (!m_attackingUnits.isEmpty())
				bridge.getHistoryWriter().addChildToEvent(m_attacker.getName() + " attacks with " + m_attackingUnits.size() + " units heading to " + m_battleSite.getName(), m_attackingUnits);
			if (!m_defendingUnits.isEmpty())
				bridge.getHistoryWriter().addChildToEvent(m_defender.getName() + " launches " + m_defendingUnits.size() + " interceptors out of " + m_battleSite.getName(), m_defendingUnits);
		}
	}
	

	class AttackersFire implements IExecutable
	{
		private static final long serialVersionUID = -5289634214875797408L;
		DiceRoll m_dice;
		CasualtyDetails m_details;
		
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			if (!m_intercept)
				return;
			final IExecutable roll = new IExecutable()
			{
				private static final long serialVersionUID = 6579019987019614374L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					if (shouldAirBattleUseAirCombatAttDefValues(m_isBombingRun))
						m_dice = DiceRoll.airBattle(m_attackingUnits, false, m_attacker, bridge, AirBattle.this, "Attackers Fire, ");
					else
						m_dice = DiceRoll.rollDice(m_attackingUnits, false, m_attacker, bridge, AirBattle.this, "Attackers Fire, ", m_territoryEffects);
				}
			};
			final IExecutable calculateCasualties = new IExecutable()
			{
				private static final long serialVersionUID = 4556409970663527142L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					m_details = BattleCalculator.selectCasualties(m_defender, m_defendingUnits, bridge, ATTACKERS_FIRE, m_dice, true, m_battleID, true);
					m_defendingWaitingToDie.addAll(m_details.getKilled());
					markDamaged(m_details.getDamaged(), bridge);
				}
			};
			final IExecutable notifyCasualties = new IExecutable()
			{
				private static final long serialVersionUID = 4224354422817922451L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					notifyCasualties(m_battleID, bridge, ATTACKERS_FIRE, m_dice, m_defender, m_attacker, m_details);
				}
			};
			// push in reverse order of execution
			stack.push(notifyCasualties);
			stack.push(calculateCasualties);
			stack.push(roll);
		}
	}
	

	class DefendersFire implements IExecutable
	{
		private static final long serialVersionUID = -7277182945495744003L;
		DiceRoll m_dice;
		CasualtyDetails m_details;
		
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			if (!m_intercept)
				return;
			final IExecutable roll = new IExecutable()
			{
				private static final long serialVersionUID = 5953506121350176595L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					if (shouldAirBattleUseAirCombatAttDefValues(m_isBombingRun))
						m_dice = DiceRoll.airBattle(m_defendingUnits, true, m_defender, bridge, AirBattle.this, "Defenders Fire, ");
					else
						m_dice = DiceRoll.rollDice(m_defendingUnits, true, m_defender, bridge, AirBattle.this, "Defenders Fire, ", m_territoryEffects);
				}
			};
			final IExecutable calculateCasualties = new IExecutable()
			{
				private static final long serialVersionUID = 6658309931909306564L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					m_details = BattleCalculator.selectCasualties(m_attacker, m_attackingUnits, bridge, DEFENDERS_FIRE, m_dice, false, m_battleID, true);
					m_attackingWaitingToDie.addAll(m_details.getKilled());
					markDamaged(m_details.getDamaged(), bridge);
				}
			};
			final IExecutable notifyCasualties = new IExecutable()
			{
				private static final long serialVersionUID = 4461950841000674515L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					notifyCasualties(m_battleID, bridge, DEFENDERS_FIRE, m_dice, m_attacker, m_defender, m_details);
				}
			};
			// push in reverse order of execution
			stack.push(notifyCasualties);
			stack.push(calculateCasualties);
			stack.push(roll);
		}
	}
	
	private static Match<Unit> unitHasAirDefenseGreaterThanZero()
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return UnitAttachment.get(u.getType()).getAirDefense(u.getOwner()) > 0;
			}
		};
	}
	
	private static Match<Unit> unitHasAirAttackGreaterThanZero()
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return UnitAttachment.get(u.getType()).getAirAttack(u.getOwner()) > 0;
			}
		};
	}
	
	public static Match<Unit> attackingGroundSeaBattleEscorts(final PlayerID attacker, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				final CompositeMatch<Unit> canIntercept = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.UnitIsDisabled().invert());
				if (shouldAirBattleUseAirCombatAttDefValues(false))
					canIntercept.add(new CompositeMatchOr<Unit>(unitHasAirAttackGreaterThanZero(), Matches.unitCanEscort));
				return canIntercept.match(u);
			}
		};
	}
	
	public static Match<Unit> defendingGroundSeaBattleInterceptors(final PlayerID attacker, final GameData data)
	{
		final boolean canScrambleIntoAirBattles = games.strategy.triplea.Properties.getCanScrambleIntoAirBattles(data);
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				final CompositeMatch<Unit> canIntercept = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitIsEnemyOf(data, attacker), Matches.UnitIsDisabled().invert());
				canIntercept.add(Matches.UnitWasInAirBattle.invert());
				if (shouldAirBattleUseAirCombatAttDefValues(false))
					canIntercept.add(Matches.unitCanIntercept);
				if (!canScrambleIntoAirBattles)
					canIntercept.add(Matches.UnitWasScrambled.invert());
				return canIntercept.match(u);
			}
		};
	}
	
	public static Match<Unit> defendingBombingRaidInterceptors(final PlayerID attacker, final GameData data)
	{
		final boolean canScrambleIntoAirBattles = games.strategy.triplea.Properties.getCanScrambleIntoAirBattles(data);
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				final CompositeMatch<Unit> canIntercept = new CompositeMatchAnd<Unit>(Matches.unitCanIntercept, Matches.unitIsEnemyOf(data, attacker), Matches.UnitIsDisabled().invert());
				canIntercept.add(Matches.UnitWasInAirBattle.invert());
				if (!canScrambleIntoAirBattles)
					canIntercept.add(Matches.UnitWasScrambled.invert());
				return canIntercept.match(u);
			}
		};
	}
	
	public static boolean territoryCouldPossiblyHaveAirBattleDefenders(final Territory territory, final PlayerID attacker, final GameData data, final boolean bombing)
	{
		final boolean canScrambleToAirBattle = games.strategy.triplea.Properties.getCanScrambleIntoAirBattles(data);
		final Match<Unit> defendingAirMatch = bombing ? defendingBombingRaidInterceptors(attacker, data) : defendingGroundSeaBattleInterceptors(attacker, data);
		int maxScrambleDistance = 0;
		if (canScrambleToAirBattle)
		{
			final Iterator<UnitType> utIter = data.getUnitTypeList().iterator();
			while (utIter.hasNext())
			{
				final UnitAttachment ua = UnitAttachment.get(utIter.next());
				if (ua.getCanScramble() && maxScrambleDistance < ua.getMaxScrambleDistance())
					maxScrambleDistance = ua.getMaxScrambleDistance();
			}
		}
		else
			return territory.getUnits().someMatch(defendingAirMatch);
		// should we check if the territory also has an air base?
		return Match.someMatch(data.getMap().getNeighbors(territory, maxScrambleDistance), Matches.territoryHasUnitsThatMatch(defendingAirMatch));
	}
	
	public static int getAirBattleRolls(final Collection<Unit> units, final boolean defending)
	{
		int rolls = 0;
		for (final Unit u : units)
		{
			rolls += getAirBattleRolls(u, defending);
		}
		return rolls;
	}
	
	public static int getAirBattleRolls(final Unit unit, final boolean defending)
	{
		if (defending)
		{
			if (!unitHasAirDefenseGreaterThanZero().match(unit))
				return 0;
		}
		else
		{
			if (!unitHasAirAttackGreaterThanZero().match(unit))
				return 0;
		}
		return Math.max(0, (defending ? UnitAttachment.get(unit.getType()).getDefenseRolls(unit.getOwner()) : UnitAttachment.get(unit.getType()).getAttackRolls(unit.getOwner())));
	}
	
	private void remove(final Collection<Unit> killed, final IDelegateBridge bridge, final Territory battleSite)
	{
		if (killed.size() == 0)
			return;
		final Collection<Unit> dependent = getDependentUnits(killed);
		killed.addAll(dependent);
		final Change killedChange = ChangeFactory.removeUnits(battleSite, killed);
		// m_killed.addAll(killed);
		final String transcriptText = MyFormatter.unitsToText(killed) + " lost in " + battleSite.getName();
		bridge.getHistoryWriter().addChildToEvent(transcriptText, killed);
		bridge.addChange(killedChange);
		final Collection<IBattle> dependentBattles = m_battleTracker.getBlocked(AirBattle.this);
		removeFromDependents(killed, bridge, dependentBattles, false);
	}
	
	private void notifyCasualties(final GUID battleID, final IDelegateBridge bridge, final String stepName, final DiceRoll dice, final PlayerID hitPlayer,
				final PlayerID firingPlayer, final CasualtyDetails details)
	{
		getDisplay(bridge).casualtyNotification(battleID, stepName, dice, hitPlayer, details.getKilled(), details.getDamaged(), Collections.<Unit, Collection<Unit>> emptyMap());
		final Runnable r = new Runnable()
		{
			public void run()
			{
				try
				{
					getRemote(firingPlayer, bridge).confirmEnemyCasualties(battleID, "Press space to continue", hitPlayer);
				} catch (final Exception e)
				{
				}
			}
		};
		// execute in a seperate thread to allow either player to click continue first.
		final Thread t = new Thread(r, "Click to continue waiter");
		t.start();
		if (true)
			getRemote(hitPlayer, bridge).confirmOwnCasualties(battleID, "Press space to continue");
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
	
	private void removeFromDependents(final Collection<Unit> units, final IDelegateBridge bridge, final Collection<IBattle> dependents, final boolean withdrawn)
	{
		for (final IBattle dependent : dependents)
		{
			dependent.unitsLostInPrecedingBattle(this, units, bridge, withdrawn);
		}
	}
	
	@Override
	public boolean isEmpty()
	{
		return m_attackingUnits.isEmpty();
	}
	
	@Override
	public void unitsLostInPrecedingBattle(final IBattle battle, final Collection<Unit> units, final IDelegateBridge bridge, final boolean withdrawn)
	{
		// should never happen
		// throw new IllegalStateException("AirBattle should not have any preceding battle with which to possibly remove dependents from");
	}
}
