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

package games.strategy.triplea.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.player.ITripleaPlayer;

public class DummyTripleAPlayer implements ITripleaPlayer {

    public void confirmEnemyCasualties(GUID battleId, String message, PlayerID hitPlayer) {
        
        
    }

    public boolean confirmMoveHariKari() {
        
        return false;
    }

    public boolean confirmMoveInFaceOfAA(Collection<Territory> aaFiringTerritories) {
        
        return false;
    }

    public boolean confirmMoveKamikaze() {
        
        return false;
    }

    public void confirmOwnCasualties(GUID battleId, String message) {
        
        
    }

    public PlayerID getID() {
        
        return null;
    }

    public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(
        Collection<Unit> fightersThatCanBeMoved, Territory from) {
        
        return null;
    }

    public void reportError(String error) {
        
        
    }

    public void reportMessage(String message) {
        
        
    }

    public Territory retreatQuery(GUID battleID, boolean submerge,
        Collection<Territory> possibleTerritories, String message) {
        
        return null;
    }

    public boolean selectAttackSubs(Territory unitTerritory) {
        
        return false;
    }

    public boolean selectAttackTransports(Territory unitTerritory) {
        
        return false;
    }

    public boolean selectAttackUnits(Territory unitTerritory) {
        
        return false;
    }

    public Territory selectBombardingTerritory(Unit unit, Territory unitTerritory,
        Collection<Territory> territories, boolean noneAvailable) {
        
        return null;
    }

    public CasualtyDetails selectCasualties(Collection<Unit> selectFrom,
        Map<Unit, Collection<Unit>> dependents, int count, String message, DiceRoll dice,
        PlayerID hit, List<Unit> defaultCasualties, GUID battleID) {
        
        return null;
    }

    public int[] selectFixedDice(int numDice, int hitAt, boolean hitOnlyIfEquals, String title) {
        
        return null;
    }

    public boolean selectShoreBombard(Territory unitTerritory) {
        
        return false;
    }

    public Territory selectTerritoryForAirToLand(Collection<Territory> candidates) {
        
        return null;
    }

    public boolean shouldBomberBomb(Territory territory) {
        
        return false;
    }

    public Territory whereShouldRocketsAttack(Collection<Territory> candidates, Territory from) {
        
        return null;
    }

}
