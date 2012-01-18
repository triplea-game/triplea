package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.ConnectionLostException;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Fire implements IExecutable
{
	// compatible with 0.9.0.2 saved games
	private static final long serialVersionUID = -3687054738070722403L;
	private final String m_stepName;
	private final Collection<Unit> m_firingUnits;
	private final Collection<Unit> m_attackableUnits;
	private final MustFightBattle.ReturnFire m_canReturnFire;
	private final String m_text;
	private final MustFightBattle m_battle;
	private final PlayerID m_firingPlayer;
	private final PlayerID m_hitPlayer;
	private final boolean m_defending;
	private final Map<Unit, Collection<Unit>> m_dependentUnits;
	private final GUID m_battleID;
	private DiceRoll m_dice;
	private Collection<Unit> m_killed;
	private Collection<Unit> m_damaged;
	private boolean m_confirmOwnCasualties = true;
	private final boolean m_isHeadless;
	
	public Fire(final Collection<Unit> attackableUnits, final MustFightBattle.ReturnFire canReturnFire, final PlayerID firingPlayer, final PlayerID hitPlayer, final Collection<Unit> firingUnits,
				final String stepName, final String text, final MustFightBattle battle, final boolean defending, final Map<Unit, Collection<Unit>> dependentUnits, final ExecutionStack stack,
				final boolean headless)
	{
		/* This is to remove any Factories, AAguns, and Infrastructure from possible targets for the firing.
		 * If, in the future, Infrastructure or other things could be taken casualty, then this will need to be changed back to:
		 * m_attackableUnits = attackableUnits;
		 */
		m_attackableUnits = Match.getMatches(attackableUnits, Matches.UnitIsDestructibleInCombatShort);
		m_canReturnFire = canReturnFire;
		m_firingUnits = firingUnits;
		m_stepName = stepName;
		m_text = text;
		m_battle = battle;
		m_hitPlayer = hitPlayer;
		m_firingPlayer = firingPlayer;
		m_defending = defending;
		m_dependentUnits = dependentUnits;
		m_isHeadless = headless;
		m_battleID = battle.getBattleID();
	}
	
	private void rollDice(final IDelegateBridge bridge)
	{
		if (m_dice != null)
			throw new IllegalStateException("Already rolled");
		final List<Unit> units = new ArrayList<Unit>(m_firingUnits);
		String annotation;
		if (m_isHeadless)
			annotation = "";
		else
			annotation = DiceRoll.getAnnotation(units, m_firingPlayer, m_battle);
		m_dice = DiceRoll.rollDice(units, m_defending, m_firingPlayer, bridge, m_battle, annotation);
	}
	
	private void selectCasualties(final IDelegateBridge bridge)
	{
		final int hitCount = m_dice.getHits();
		MustFightBattle.getDisplay(bridge).notifyDice(m_battle.getBattleID(), m_dice, m_stepName);
		final int countTransports = Match.countMatches(m_attackableUnits, new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.UnitIsSea));
		if (countTransports > 0 && isTransportCasualtiesRestricted(bridge.getData()))
		{
			CasualtyDetails message;
			final Collection<Unit> nonTransports = Match.getMatches(m_attackableUnits, new CompositeMatchOr<Unit>(Matches.UnitIsNotTransportButCouldBeCombatTransport, Matches.UnitIsNotSea));
			final Collection<Unit> transportsOnly = Match.getMatches(m_attackableUnits, new CompositeMatchAnd<Unit>(Matches.UnitIsTransportButNotCombatTransport, Matches.UnitIsSea));
			final int numPossibleHits = MustFightBattle.getMaxHits(nonTransports);
			// more hits than combat units
			if (hitCount > numPossibleHits)
			{
				int extraHits = hitCount - numPossibleHits;
				final Collection<Unit> remainingTargets = new ArrayList<Unit>();
				remainingTargets.addAll(m_attackableUnits);
				remainingTargets.removeAll(nonTransports);
				final Collection<PlayerID> alliedHitPlayer = new ArrayList<PlayerID>();
				// find the players who have transports in the attackable pile
				for (final Unit unit : transportsOnly)
				{
					if (!alliedHitPlayer.contains(unit.getOwner()))
						alliedHitPlayer.add(unit.getOwner());
				}
				final Iterator<PlayerID> playerIter = alliedHitPlayer.iterator();
				// Leave enough transports for each defender for overlfows so they can select who loses them.
				while (playerIter.hasNext())
				{
					final PlayerID player = playerIter.next();
					final CompositeMatch<Unit> match = new CompositeMatchAnd<Unit>();
					match.add(Matches.UnitIsTransportButNotCombatTransport);
					match.add(Matches.unitIsOwnedBy(player));
					final Collection<Unit> playerTransports = Match.getMatches(transportsOnly, match);
					final int transportsToRemove = Math.max(0, playerTransports.size() - extraHits);
					transportsOnly.removeAll(Match.getNMatches(playerTransports, transportsToRemove, Matches.UnitIsTransportButNotCombatTransport));
				}
				m_killed = nonTransports;
				m_damaged = Collections.emptyList();
				// m_confirmOwnCasualties = true;
				if (extraHits > transportsOnly.size())
					extraHits = transportsOnly.size();
				message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer, transportsOnly, bridge, m_text, m_dice, !m_defending, m_battleID, m_isHeadless, extraHits);
				m_killed.addAll(message.getKilled());
				m_confirmOwnCasualties = true;
			}
			// exact number of combat units
			else if (hitCount == numPossibleHits)
			{
				m_killed = nonTransports;
				m_damaged = Collections.emptyList();
				m_confirmOwnCasualties = true;
			}
			// less than possible number
			else
			{
				message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer, nonTransports, bridge, m_text, m_dice, !m_defending, m_battleID, m_isHeadless, m_dice.getHits());
				m_killed = message.getKilled();
				m_damaged = message.getDamaged();
				m_confirmOwnCasualties = message.getAutoCalculated();
			}
		}
		else
		// not isTransportCasualtiesRestricted
		{
			// they all die
			if (hitCount >= MustFightBattle.getMaxHits(m_attackableUnits))
			{
				m_killed = m_attackableUnits;
				m_damaged = Collections.emptyList();
				// everything died, so we need to confirm
				m_confirmOwnCasualties = true;
			}
			// Choose casualties
			else
			{
				CasualtyDetails message;
				message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer, m_attackableUnits, bridge, m_text, m_dice, !m_defending, m_battleID, m_isHeadless, m_dice.getHits());
				m_killed = message.getKilled();
				m_damaged = message.getDamaged();
				m_confirmOwnCasualties = message.getAutoCalculated();
			}
		}
	}
	
	private void notifyCasualties(final IDelegateBridge bridge)
	{
		if (m_isHeadless)
			return;
		MustFightBattle.getDisplay(bridge).casualtyNotification(m_battleID, m_stepName, m_dice, m_hitPlayer, new ArrayList<Unit>(m_killed), new ArrayList<Unit>(m_damaged), m_dependentUnits);
		final Runnable r = new Runnable()
		{
			public void run()
			{
				try
				{
					MustFightBattle.getRemote(m_firingPlayer, bridge).confirmEnemyCasualties(m_battleID, "Press space to continue", m_hitPlayer);
				} catch (final ConnectionLostException cle)
				{
					// somone else will deal with this
					cle.printStackTrace(System.out);
				}
			}
		};
		// execute in a seperate thread to allow either player to click continue first.
		final Thread t = new Thread(r, "Click to continue waiter");
		t.start();
		if (m_confirmOwnCasualties)
			MustFightBattle.getRemote(m_hitPlayer, bridge).confirmOwnCasualties(m_battleID, "Press space to continue");
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
	
	/**
	 * We must execute in atomic steps, push these steps onto the stack, and let them execute
	 */
	@SuppressWarnings("serial")
	public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
	{
		// add to the stack so we will execute,
		// we want to roll dice, select casualties, then notify in that order, so
		// push onto the stack in reverse order
		final IExecutable rollDice = new IExecutable()
		{
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				rollDice(bridge);
			}
		};
		final IExecutable selectCasualties = new IExecutable()
		{
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				selectCasualties(bridge);
			}
		};
		final IExecutable notifyCasualties = new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = -9173385989239225660L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				notifyCasualties(bridge);
				if (m_damaged != null)
					m_battle.markDamaged(m_damaged, bridge);
				m_battle.removeCasualties(m_killed, m_canReturnFire, !m_defending, bridge, false);
			}
		};
		stack.push(notifyCasualties);
		stack.push(selectCasualties);
		stack.push(rollDice);
		return;
	}
	
	/**
	 * @return
	 */
	private boolean isTransportCasualtiesRestricted(final GameData data)
	{
		return games.strategy.triplea.Properties.getTransportCasualtiesRestricted(data);
	}
}
