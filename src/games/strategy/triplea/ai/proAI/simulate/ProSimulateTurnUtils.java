package games.strategy.triplea.ai.proAI.simulate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProBattleResultData;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/**
 * Pro AI attack options utilities.
 * 
 * <ol>
 * <li>Add support for considering carrier landing when calculating air routes</li>
 * </ol>
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProSimulateTurnUtils
{
	private final ProAI ai;
	private final ProUtils utils;
	private final ProBattleUtils battleUtils;
	
	public ProSimulateTurnUtils(final ProAI ai, final ProUtils utils, final ProBattleUtils battleUtils)
	{
		this.ai = ai;
		this.utils = utils;
		this.battleUtils = battleUtils;
	}
	
	public void simulateBattles(final GameData data, final PlayerID player, final IDelegateBridge delegateBridge)
	{
		LogUtils.log(Level.FINE, "Starting battle simulation phase");
		
		final BattleDelegate battleDelegate = DelegateFinder.battleDelegate(data);
		final Map<BattleType, Collection<Territory>> battleTerritories = battleDelegate.getBattles().getBattles();
		for (final Entry<BattleType, Collection<Territory>> entry : battleTerritories.entrySet())
		{
			for (final Territory t : entry.getValue())
			{
				final IBattle battle = battleDelegate.getBattleTracker().getPendingBattle(t, entry.getKey().isBombingRun(), entry.getKey());
				final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
				final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
				LogUtils.log(Level.FINER, "---" + t);
				LogUtils.log(Level.FINER, "attackers=" + attackers);
				LogUtils.log(Level.FINER, "defenders=" + defenders);
				final ProBattleResultData result = battleUtils.calculateBattleResults(player, t, attackers, defenders, true);
				final List<Unit> remainingUnits = result.getAverageUnitsRemaining();
				LogUtils.log(Level.FINER, "remainingUnits=" + remainingUnits);
				
				// Make updates to data
				final List<Unit> attackersToRemove = new ArrayList<Unit>(attackers);
				attackersToRemove.removeAll(remainingUnits);
				LogUtils.log(Level.FINER, "attackersToRemove=" + attackersToRemove);
				LogUtils.log(Level.FINER, "defendersToRemove=" + defenders);
				final Change attackerskilledChange = ChangeFactory.removeUnits(t, attackersToRemove);
				delegateBridge.addChange(attackerskilledChange);
				final Change defenderskilledChange = ChangeFactory.removeUnits(t, defenders);
				delegateBridge.addChange(defenderskilledChange);
				delegateBridge.addChange(ChangeFactory.changeOwner(t, player));
				battleDelegate.getBattleTracker().getConquered().add(t);
				final Territory updatedTerritory = data.getMap().getTerritory(t.getName());
				LogUtils.log(Level.FINER, "after changes owner=" + updatedTerritory.getOwner() + ", units=" + updatedTerritory.getUnits().getUnits());
			}
		}
	}
	
}
