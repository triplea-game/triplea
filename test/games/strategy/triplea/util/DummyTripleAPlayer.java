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

package games.strategy.triplea.util;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.Match;

import java.util.Collection;
import java.util.Map;

public class DummyTripleAPlayer implements ITripleaPlayer
{
	
	@Override
	public void confirmEnemyCasualties(GUID battleId, String message, PlayerID hitPlayer)
	{
		
	}
	
	@Override
	public boolean confirmMoveHariKari()
	{
		
		return false;
	}
	
	@Override
	public boolean confirmMoveInFaceOfAA(Collection<Territory> aaFiringTerritories)
	{
		
		return false;
	}
	
	@Override
	public boolean confirmMoveKamikaze()
	{
		
		return false;
	}
	
	@Override
	public void confirmOwnCasualties(GUID battleId, String message)
	{
		
	}
	
	@Override
	public PlayerID getID()
	{
		
		return null;
	}
	
	@Override
	public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(
				Collection<Unit> fightersThatCanBeMoved, Territory from)
	{
		
		return null;
	}
	
	@Override
	public void reportError(String error)
	{
		
	}
	
	@Override
	public void reportMessage(String message)
	{
		
	}
	
	@Override
	public void reportPoliticalMessage(String message) {
		
	}
	
	@Override
	public boolean acceptPoliticalAction(String message) {
		return true;
	}
	
	@Override
	public Territory retreatQuery(GUID battleID, boolean submerge,
				Collection<Territory> possibleTerritories, String message)
	{
		
		return null;
	}
	
	@Override
	public Collection<Unit> scrambleQuery(GUID battleID,
				Collection<Territory> possibleTerritories, String message)
	{
		
		return null;
	}
	
	@Override
	public boolean selectAttackSubs(Territory unitTerritory)
	{
		
		return false;
	}
	
	@Override
	public boolean selectAttackTransports(Territory unitTerritory)
	{
		
		return false;
	}
	
	@Override
	public boolean selectAttackUnits(Territory unitTerritory)
	{
		
		return false;
	}
	
	@Override
	public Territory selectBombardingTerritory(Unit unit, Territory unitTerritory,
				Collection<Territory> territories, boolean noneAvailable)
	{
		
		return null;
	}
	
	@Override
	public CasualtyDetails selectCasualties(Collection<Unit> selectFrom,
				Map<Unit, Collection<Unit>> dependents, int count, String message, DiceRoll dice,
				PlayerID hit, CasualtyList defaultCasualties, GUID battleID)
	{
		
		return new CasualtyDetails(defaultCasualties.getKilled(), defaultCasualties.getDamaged(), true);
	}
	
	@Override
	public int[] selectFixedDice(int numDice, int hitAt, boolean hitOnlyIfEquals, String title, int diceSides)
	{
		
		return null;
	}
	
	@Override
	public boolean selectShoreBombard(Territory unitTerritory)
	{
		
		return false;
	}
	
	@Override
	public Territory selectTerritoryForAirToLand(Collection<Territory> candidates)
	{
		
		return null;
	}
	
	@Override
	public boolean shouldBomberBomb(Territory territory)
	{
		return false;
	}
	
	@Override
	public Unit whatShouldBomberBomb(Territory territory, Collection<Unit> units)
	{
		return (Unit) Match.getNMatches(units, 1, Matches.UnitIsFactory);
	}
	
	@Override
	public Territory whereShouldRocketsAttack(Collection<Territory> candidates, Territory from)
	{
		
		return null;
	}
	
}
