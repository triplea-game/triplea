package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.RouteScripted;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.weakAI.WeakAI;
import games.strategy.util.CompositeMatchAnd;
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
			steps.add(new AttackersFire());
			steps.add(new DefendersFire());
			steps.add(new IExecutable()
			{
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					getDisplay(bridge).gotoBattleStep(m_battleID, BOMBERS_TO_TARGETS);
					String text;
					if (Match.someMatch(m_attackingUnits, Matches.UnitIsStrategicBomber))
						text = "Air Battle is over, the remaining Bombers go on to their targets";
					else
						text = "Air Battle is over, the bombers have all died";
					bridge.getHistoryWriter().addChildToEvent(text);
					
					if (!m_intercept)
						return;
					
					// kill any suicide attackers (veqryn)
					if (Match.someMatch(m_attackingUnits, new CompositeMatchAnd<Unit>(Matches.UnitIsSuicide, Matches.UnitIsNotStrategicBomber)))
					{
						final List<Unit> suicideUnits = Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide);
						m_attackingUnits.removeAll(suicideUnits);
						final Change removeSuicide = ChangeFactory.removeUnits(m_battleSite, suicideUnits);
						final String transcriptText = MyFormatter.unitsToText(suicideUnits) + " lost in " + m_battleSite.getName();
						bridge.getHistoryWriter().addChildToEvent(transcriptText, suicideUnits);
						bridge.addChange(removeSuicide);
					}
					if (Match.someMatch(m_defendingUnits, Matches.UnitIsSuicide))
					{
						final List<Unit> suicideUnits = Match.getMatches(m_defendingUnits, Matches.UnitIsSuicide);
						m_defendingUnits.removeAll(suicideUnits);
						final Change removeSuicide = ChangeFactory.removeUnits(m_battleSite, suicideUnits);
						final String transcriptText = MyFormatter.unitsToText(suicideUnits) + " lost in " + m_battleSite.getName();
						bridge.getHistoryWriter().addChildToEvent(transcriptText, suicideUnits);
						bridge.addChange(removeSuicide);
					}
				}
			});
		}
		
		steps.add(new IExecutable()
		{
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
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
		getDisplay(bridge)
					.showBattle(m_battleID, m_battleSite, title, m_attackingUnits, getDefendingUnits(), null, null, null, Collections.<Unit, Collection<Unit>> emptyMap(), m_attacker, m_defender);
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
			return new WeakAI(player.getName(), "E.Z. Fodder (AI)");
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
					m_intercept = true;
				}
			};
			// push in reverse order of execution
			m_stack.push(getInterceptors);
		}
		
		private void getInterceptors(final IDelegateBridge bridge)
		{
			// stuff
		}
	}
	

	class AttackersFire implements IExecutable
	{
		private int[] m_dice;
		
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			final IExecutable attackersFire = new IExecutable()
			{
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					rollDice(bridge);
				}
			};
			// push in reverse order of execution
			m_stack.push(attackersFire);
		}
		
		private void rollDice(final IDelegateBridge bridge)
		{
			getDisplay(bridge).gotoBattleStep(m_battleID, ATTACKERS_FIRE);
			final Collection<Unit> firingUnits = Match.getMatches(m_attackingUnits, unitHasAirAttackGreaterThanZero());
			/*final int rollCount = firingUnits.size();
			if (rollCount == 0)
			{
				m_dice = null;
				return;
			}
			m_dice = new int[rollCount];*/

		}
	}
	

	class DefendersFire implements IExecutable
	{
		private int[] m_dice;
		
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			final IExecutable defendersFire = new IExecutable()
			{
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					rollDice(bridge);
				}
			};
			// push in reverse order of execution
			m_stack.push(defendersFire);
		}
		
		private void rollDice(final IDelegateBridge bridge)
		{
			getDisplay(bridge).gotoBattleStep(m_battleID, DEFENDERS_FIRE);
			final Collection<Unit> firingUnits = Match.getMatches(m_defendingUnits, unitHasAirDefenseGreaterThanZero());
			/*final int rollCount = firingUnits.size();
			if (rollCount == 0)
			{
				m_dice = null;
				return;
			}
			m_dice = new int[rollCount];*/

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
							Matches.UnitWasScrambled.invert(), Matches.UnitIsDisabled().invert());
				return canIntercept.match(u);
			}
		};
	}
}
