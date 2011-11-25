package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

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
	public StrategicBombingRaidPreBattle(final Territory battleSite, final GameData data, final PlayerID attacker, final PlayerID defender, final BattleTracker battleTracker)
	{
		super(battleSite, data, attacker, defender, battleTracker);
		m_battleType = "StrategicBombingRaidPreBattle";
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
		// TODO Auto-generated method stub
		super.removeAttack(route, units);
	}
	
	@Override
	public void fight(final IDelegateBridge bridge)
	{
		// TODO Auto-generated method stub
		super.fight(bridge);
	}
	
	private void showBattle(final IDelegateBridge bridge)
	{
		final String title = "Bombing raid in " + m_battleSite.getName();
		getDisplay(bridge)
					.showBattle(m_battleID, m_battleSite, title, m_attackingUnits, getDefendingUnits(), null, null, null, Collections.<Unit, Collection<Unit>> emptyMap(), m_attacker, m_defender);
		getDisplay(bridge).listBattleSteps(m_battleID, m_steps);
	}
	
	@Override
	public List<Unit> getDefendingUnits()
	{
		final Match<Unit> defenders = new CompositeMatchOr<Unit>(Matches.UnitIsAAforBombing, Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(m_battleSite).invert());
		if (m_targets.isEmpty())
			return Match.getMatches(m_battleSite.getUnits().getUnits(), defenders);
		final List<Unit> targets = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsAAforBombing);
		targets.addAll(m_targets.keySet());
		return targets;
	}
	
	private ITripleaPlayer getRemote(final IDelegateBridge bridge)
	{
		return (ITripleaPlayer) bridge.getRemote();
	}
}
