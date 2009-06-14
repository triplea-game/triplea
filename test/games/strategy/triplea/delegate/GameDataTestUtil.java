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

package games.strategy.triplea.delegate;

import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TestDelegateBridge;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.display.IDisplay;
import games.strategy.kingstable.ui.display.DummyDisplay;

import java.util.Collection;

import junit.framework.AssertionFailedError;

public class GameDataTestUtil {

    
    public static PlayerID germans(GameData data) {
        return data.getPlayerList().getPlayerID("Germans");
    }
    
    public static PlayerID british(GameData data) {
        return data.getPlayerList().getPlayerID("British");
    }
    
    public static Territory territory(String name, GameData data) {
        Territory t = data.getMap().getTerritory(name);
        if(t == null) {
            throw new IllegalStateException("no territory:" + name);
        }
        return t;
    }
    
    public static UnitType transports(GameData data) {
        return unitType("transport", data);
    }
    
    public static UnitType infantry(GameData data) {
        return unitType("infantry", data);
    }
    
    public static UnitType bomber(GameData data) {
        return unitType("bomber", data);
    }
    
    private static UnitType unitType(String name, GameData data) {
        return data.getUnitTypeList().getUnitType(name);
    }
    
    public static void addTo(Territory t, Collection<Unit> units) {
        new ChangePerformer(t.getData()).perform(ChangeFactory.addUnits(t, units));
    }
    
    public static void addTo(PlayerID t, Collection<Unit> units) {
        new ChangePerformer(t.getData()).perform(ChangeFactory.addUnits(t, units));
    }

    public static PlaceDelegate placeDelegate(GameData data) {
        return (PlaceDelegate) data.getDelegateList().getDelegate("place");
    }
    
    public static BattleDelegate battleDelegate(GameData data) {
        return (BattleDelegate) data.getDelegateList().getDelegate("battle");
    }
    
    public static MoveDelegate moveDelegate(GameData data) {
        return (MoveDelegate) data.getDelegateList().getDelegate("move");
    }
    
    public static ITestDelegateBridge getDelegateBridge(GameData data, PlayerID player)
    {
        ITestDelegateBridge bridge1 = new TestDelegateBridge(data, player, (IDisplay) new DummyDisplay());
        TestTripleADelegateBridge bridge2 = new TestTripleADelegateBridge(bridge1, data);
        return bridge2;
    }
    
    public static void load(Collection<Unit> units, Route route) {
        if(units.isEmpty()) {
            throw new AssertionFailedError("No units");
        }
        MoveDelegate moveDelegate = moveDelegate(route.getStart().getData());
        Collection<Unit> transports = route.getEnd().getUnits().getMatches(Matches.unitIsOwnedBy(units.iterator().next().getOwner()));
        String error = moveDelegate.move(units, route, transports);
        if(error != null) {
            throw new AssertionFailedError("Illegal move:" + error);
        }
    }
    
    public static void move(Collection<Unit> units, Route route) {
        if(units.isEmpty()) {
            throw new AssertionFailedError("No units");
        }
        String error = moveDelegate(route.getStart().getData()).move(units, route);
        if(error != null) {
            throw new AssertionFailedError("Illegal move:" + error);
        }
    }
        
    
}

