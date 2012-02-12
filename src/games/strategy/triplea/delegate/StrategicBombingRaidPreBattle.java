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
import games.strategy.triplea.TripleA;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.BattleRecords;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.weakAI.WeakAI;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * @author Mark Christopher Duncan (veqryn)
 * 
 */
public class StrategicBombingRaidPreBattle extends StrategicBombingRaidBattle
{
	private final static String AIR_BATTLE = "Air Battle";
	private final static String INTERCEPTORS_LAUNCH = "Defender Launches Interceptors";
	private final static String ATTACKERS_FIRE = "Attacking Escorts and Bombers Fire";
	private final static String DEFENDERS_FIRE = "Defending Interceptors Fire";
	private final static String WITHDRAW = "Escorts and Interceptors Withdraw";
	private final static String BOMBERS_TO_TARGETS = "Bombers Fly to Their Targets";
	
	// protected final List<Unit> m_attackingUnits = new ArrayList<Unit>();
	// protected final HashMap<Unit, HashSet<Unit>> m_targets = new HashMap<Unit, HashSet<Unit>>(); // these would be the factories or other targets. does not include aa.
	// protected final PlayerID m_defender;
	// protected final GUID m_battleID = new GUID();
	// protected final ExecutionStack m_stack = new ExecutionStack();
	// protected List<String> m_steps;
	protected final List<Unit> m_defendingUnits = new ArrayList<Unit>();
	private final Collection<Unit> m_defendingWaitingToDie = new ArrayList<Unit>();
	private final Collection<Unit> m_attackingWaitingToDie = new ArrayList<Unit>();
	protected boolean m_intercept = false;
	
	public StrategicBombingRaidPreBattle(final Territory battleSite, final GameData data, final PlayerID attacker, final PlayerID defender, final BattleTracker battleTracker)
	{
		super(battleSite, data, attacker, defender, battleTracker);
		m_battleType = BattleTracker.BATTLE_TYPE_AIR_BATTLE;
		m_defendingUnits.addAll(battleSite.getUnits().getMatches(defendingInterceptors(m_attacker, m_data)));
	}
	
	@Override
	public Change addAttackChange(final Route route, final Collection<Unit> units, final HashMap<Unit, HashSet<Unit>> targets)
	{
		// TODO Auto-generated method stub
		return super.addAttackChange(route, units, targets);
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
		bridge.getHistoryWriter().startEvent("Air Battle in " + m_battleSite);
		bridge.getHistoryWriter().setRenderingData(m_battleSite);
		BattleCalculator.sortPreBattle(m_attackingUnits, m_data);
		BattleCalculator.sortPreBattle(m_defendingUnits, m_data);
		m_steps = new ArrayList<String>();
		m_steps.add(AIR_BATTLE);
		m_steps.add(INTERCEPTORS_LAUNCH);
		m_steps.add(DEFENDERS_FIRE);
		m_steps.add(ATTACKERS_FIRE);
		m_steps.add(WITHDRAW);
		m_steps.add(BOMBERS_TO_TARGETS);
		showBattle(bridge);
		final List<IExecutable> steps = new ArrayList<IExecutable>();
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsStrategicBomber) && Match.someMatch(m_battleSite.getUnits().getUnits(), defendingInterceptors(m_attacker, m_data)))
		{
			steps.add(new InterceptorsLaunch());
			steps.add(new DefendersFire());
			steps.add(new AttackersFire());
			steps.add(new IExecutable()
			{
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					getDisplay(bridge).gotoBattleStep(m_battleID, BOMBERS_TO_TARGETS);
					String text;
					if (Match.someMatch(m_attackingUnits, Matches.UnitIsStrategicBomber))
					{
						if (m_defendingUnits.isEmpty())
							m_battleResult = BattleRecords.BattleResult.WON_WITHOUT_CONQUERING;
						else
							m_battleResult = BattleRecords.BattleResult.WON_WITH_ENEMY_LEFT;
						text = "Air Battle is over, the remaining Bombers go on to their targets";
					}
					else if (!m_attackingUnits.isEmpty())
					{
						m_battleResult = BattleRecords.BattleResult.STALEMATE;
						text = "Air Battle is over, the bombers have all died";
					}
					else
					{
						m_battleResult = BattleRecords.BattleResult.LOST;
						text = "Air Battle is over, the bombers have all died";
					}
					bridge.getHistoryWriter().addChildToEvent(text);
					
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
					
					// kill any suicide attackers (veqryn)
					if (Match.someMatch(m_attackingUnits, new CompositeMatchAnd<Unit>(Matches.UnitIsSuicide, Matches.UnitIsNotStrategicBomber)))
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
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV, m_defenderLostTUV, m_battleResult, 0);
				m_battleTracker.removeBattle(StrategicBombingRaidPreBattle.this);
				getDisplay(bridge).battleEnd(m_battleID, "Air Battle over");
				m_isOver = true;
				// setup new battle here
				final Collection<Unit> bombers = Match.getMatches(m_attackingUnits, Matches.UnitIsStrategicBomber);
				if (!bombers.isEmpty())
				{
					HashMap<Unit, HashSet<Unit>> targets = null;
					Unit target = null;
					final Collection<Unit> enemyTargets = m_battleSite.getUnits().getMatches(
								new CompositeMatchAnd<Unit>(Matches.enemyUnit(bridge.getPlayerID(), m_data), Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(m_battleSite).invert()));
					for (final Unit unit : bombers)
					{
						if (enemyTargets.size() > 1 && games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(m_data))
							target = getRemote(bridge).whatShouldBomberBomb(m_battleSite, enemyTargets);
						else if (!enemyTargets.isEmpty())
							target = enemyTargets.iterator().next();
						if (target != null)
						{
							targets = new HashMap<Unit, HashSet<Unit>>();
							targets.put(target, new HashSet<Unit>(Collections.singleton(unit)));
						}
						m_battleTracker.addBattle(new RouteScripted(m_battleSite), Collections.singleton(unit), true, m_attacker, bridge, null, targets, true);
					}
				}
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
		final String title = "Air Battle in " + m_battleSite.getName();
		getDisplay(bridge).showBattle(m_battleID, m_battleSite, title, m_attackingUnits, getDefendingUnits(),
					null, null, null, Collections.<Unit, Collection<Unit>> emptyMap(), m_attacker, m_defender, getBattleType());
		getDisplay(bridge).listBattleSteps(m_battleID, m_steps);
	}
	
	@Override
	public List<Unit> getDefendingUnits()
	{
		return m_defendingUnits;
	}
	
	private ITripleaPlayer getRemote(final IDelegateBridge bridge)
	{
		return (ITripleaPlayer) bridge.getRemote();
	}
	
	private static ITripleaPlayer getRemote(final PlayerID player, final IDelegateBridge bridge)
	{
		// if its the null player, return a do nothing proxy
		if (player.isNull())
			return new WeakAI(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE);
		return (ITripleaPlayer) bridge.getRemote(player);
	}
	
	
	class InterceptorsLaunch implements IExecutable
	{
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			final IExecutable getInterceptors = new IExecutable()
			{
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					getInterceptors(bridge);
					if (!m_defendingUnits.isEmpty())
						m_intercept = true;
				}
			};
			// push in reverse order of execution
			m_stack.push(getInterceptors);
		}
		
		private void getInterceptors(final IDelegateBridge bridge)
		{
			final Collection<Unit> interceptors = getRemote(m_defender, bridge).selectUnitsQuery(m_battleSite, m_defendingUnits, "Select Air to Intercept");
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
			
			final CompositeChange changeAttacker = new CompositeChange();
			for (final Unit u : m_attackingUnits)
			{
				changeAttacker.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.WAS_IN_AIR_BATTLE));
			}
			if (!changeAttacker.isEmpty())
			{
				bridge.getHistoryWriter().startEvent(m_attacker.getName() + " attacks with " + m_attackingUnits.size() + " units heading to " + m_battleSite.getName());
				bridge.getHistoryWriter().setRenderingData(m_attackingUnits);
				bridge.addChange(changeAttacker);
			}
			final CompositeChange changeDefender = new CompositeChange();
			for (final Unit u : m_defendingUnits)
			{
				changeDefender.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.WAS_IN_AIR_BATTLE));
			}
			if (!changeDefender.isEmpty())
			{
				bridge.getHistoryWriter().startEvent(m_defender.getName() + " launches " + m_defendingUnits.size() + " interceptors out of " + m_battleSite.getName());
				bridge.getHistoryWriter().setRenderingData(m_defendingUnits);
				bridge.addChange(changeDefender);
			}
		}
	}
	

	class AttackersFire implements IExecutable
	{
		DiceRoll m_dice;
		CasualtyDetails m_details;
		
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			if (!m_intercept)
				return;
			final IExecutable roll = new IExecutable()
			{
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					m_dice = DiceRoll.airBattle(m_attackingUnits, false, m_attacker, bridge, StrategicBombingRaidPreBattle.this, "Attackers Fire, ");
				}
			};
			final IExecutable calculateCasualties = new IExecutable()
			{
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					final List<Unit> defendingUnits = getDefendingUnits();
					m_details = BattleCalculator.selectCasualties(m_defender, defendingUnits, bridge, ATTACKERS_FIRE, m_dice, true, m_battleID);
					m_defendingWaitingToDie.addAll(m_details.getKilled());
					markDamaged(m_details.getDamaged(), bridge);
				}
			};
			final IExecutable notifyCasualties = new IExecutable()
			{
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
		DiceRoll m_dice;
		CasualtyDetails m_details;
		
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			if (!m_intercept)
				return;
			final IExecutable roll = new IExecutable()
			{
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					m_dice = DiceRoll.airBattle(getDefendingUnits(), true, m_defender, bridge, StrategicBombingRaidPreBattle.this, "Defenders Fire, ");
				}
			};
			final IExecutable calculateCasualties = new IExecutable()
			{
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					m_details = BattleCalculator.selectCasualties(m_attacker, m_attackingUnits, bridge, DEFENDERS_FIRE, m_dice, false, m_battleID);
					m_attackingWaitingToDie.addAll(m_details.getKilled());
					markDamaged(m_details.getDamaged(), bridge);
				}
			};
			final IExecutable notifyCasualties = new IExecutable()
			{
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
	
	public static Match<Unit> defendingInterceptors(final PlayerID attacker, final GameData data)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				final Match<Unit> canIntercept = new CompositeMatchAnd<Unit>(Matches.unitCanIntercept, Matches.unitIsEnemyOf(data, attacker),
							Matches.UnitWasScrambled.invert(), Matches.UnitIsDisabled().invert(), Matches.UnitWasInAirBattle.invert());
				return canIntercept.match(u);
			}
		};
	}
	
	public static int getAirBattleRolls(final Collection<Unit> units, final boolean defending)
	{
		final Collection<Unit> firingUnits = Match.getMatches(units, (defending ? unitHasAirDefenseGreaterThanZero() : unitHasAirAttackGreaterThanZero()));
		return firingUnits.size();
	}
	
	private void markDamaged(final Collection<Unit> damaged, final IDelegateBridge bridge)
	{
		if (damaged.size() == 0)
			return;
		Change damagedChange = null;
		final IntegerMap<Unit> damagedMap = new IntegerMap<Unit>();
		damagedMap.putAll(damaged, 1);
		damagedChange = ChangeFactory.unitsHit(damagedMap);
		bridge.getHistoryWriter().addChildToEvent("Units damaged: " + MyFormatter.unitsToText(damaged), damaged);
		bridge.addChange(damagedChange);
	}
	
	private void remove(final Collection<Unit> killed, final IDelegateBridge bridge, final Territory battleSite)
	{
		if (killed.size() == 0)
			return;
		final Change killedChange = ChangeFactory.removeUnits(battleSite, killed);
		final String transcriptText = MyFormatter.unitsToText(killed) + " lost in " + battleSite.getName();
		bridge.getHistoryWriter().addChildToEvent(transcriptText, killed);
		bridge.addChange(killedChange);
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
}
