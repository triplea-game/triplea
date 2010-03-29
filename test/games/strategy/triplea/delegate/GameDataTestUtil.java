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
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.display.IDisplay;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.ui.display.DummyDisplay;

import java.util.Collection;
import java.util.List;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

public class GameDataTestUtil {

    
    public static PlayerID germans(GameData data) {
        return data.getPlayerList().getPlayerID("Germans");
    }

    public static PlayerID italians(GameData data) {
        return data.getPlayerList().getPlayerID("Italians");
    }
    
    public static PlayerID russians(GameData data) {
        return data.getPlayerList().getPlayerID("Russians");
    }
    
    public static PlayerID americans(GameData data) {
        return data.getPlayerList().getPlayerID("Americans");
    }
    
    public static PlayerID british(GameData data) {
        return data.getPlayerList().getPlayerID("British");
    }
    
    public static PlayerID japanese(GameData data) {
        return data.getPlayerList().getPlayerID("Japanese");
    }
    
    public static PlayerID chinese(GameData data) {
        return data.getPlayerList().getPlayerID("Chinese");
    }
    
    public static Territory territory(String name, GameData data) {
        Territory t = data.getMap().getTerritory(name);
        if(t == null) {
            throw new IllegalStateException("no territory:" + name);
        }
        return t;
    }

    
    public static UnitType armour(GameData data) {
        return unitType("armour", data);
    }
    
    public static UnitType aaGun(GameData data) {
        return unitType("aaGun", data);
    }
    
    public static UnitType transports(GameData data) {
        return unitType("transport", data);
    }
    
    public static UnitType battleship(GameData data) {
        return unitType("battleship", data);
    }
    
    public static UnitType carrier(GameData data) {
        return unitType("carrier", data);
    }
    
    public static UnitType fighter(GameData data) {
        return unitType("fighter", data);
    }
    
    public static UnitType destroyer(GameData data) {
        return unitType("destroyer", data);
    }
    
    public static UnitType submarine(GameData data) {
        return unitType("submarine", data);
    }
    
    public static UnitType infantry(GameData data) {
        return unitType("infantry", data);
    }
    
    public static UnitType bomber(GameData data) {
        return unitType("bomber", data);
    }

    public static UnitType factory(GameData data) {
        return unitType("factory", data);
    }
    
    private static UnitType unitType(String name, GameData data) {
        return data.getUnitTypeList().getUnitType(name);
    }
    
    public static void removeFrom(Territory t, Collection<Unit> units) {
        new ChangePerformer(t.getData()).perform(ChangeFactory.removeUnits(t, units));
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

/*    public static PurchaseDelegate purchaseDelegate(GameData data) {
        return (PurchaseDelegate) data.getDelegateList().getDelegate("purchase");
    }*/
    
    public static BattleDelegate battleDelegate(GameData data) {
        return (BattleDelegate) data.getDelegateList().getDelegate("battle");
    }
    
    public static MoveDelegate moveDelegate(GameData data) {
        return (MoveDelegate) data.getDelegateList().getDelegate("move");
    }
    
    public static TechnologyDelegate techDelegate(GameData data) {
        return (TechnologyDelegate) data.getDelegateList().getDelegate("tech");
    }
    
    public static PurchaseDelegate purchaseDelegate(GameData data) {
        return (PurchaseDelegate) data.getDelegateList().getDelegate("purchase");
    }    
    
    public static BidPlaceDelegate bidPlaceDelegate(GameData data) {
        return (BidPlaceDelegate) data.getDelegateList().getDelegate("placeBid");
    }
    
    public static ITestDelegateBridge getDelegateBridge(PlayerID player)
    {
        return new TestDelegateBridge(player.getData(), player, (IDisplay) new DummyDisplay());                
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
        
    public static int getIndex(List<IExecutable> steps, Class<?> type) 
    {
        int rVal = -1;
        int index = 0;
        for(IExecutable e : steps) 
        {
            if(type.isInstance(e)) 
            {
                if(rVal != -1) {
                    throw new AssertionFailedError("More than one instance:" + steps);
                }
                rVal = index; 
            }
            index++;                
        }
        if(rVal == -1) {
            throw new AssertionFailedError("No instance:" + steps);
        }
        return rVal;
        
    }
    
    public static void setSelectAACasualties(GameData data, boolean val) 
    {
    	for(IEditableProperty property : data.getProperties().getEditableProperties())
        {
            if(property.getName().equals(Constants.CHOOSE_AA))
            {
                 ((BooleanProperty)  property).setValue(val);
                 return;
            }
        }
        throw new IllegalStateException();
    }
    
    public static void makeGameLowLuck(GameData data)
    {
        for(IEditableProperty property : data.getProperties().getEditableProperties())
        {
            if(property.getName().equals(Constants.LOW_LUCK))
            {
                 ((BooleanProperty)  property).setValue(true);
                 return;
            }
        }
        throw new IllegalStateException();
        
    }
    
    public static void givePlayerRadar(PlayerID player) 
    { 
    	TechAttachment.get(player).setAARadar(Boolean.TRUE.toString());
    }
    
    public static void assertValid(String string)
    {
        Assert.assertNull(string,string);
    }
    
    public static void assertError(String string)
    {
        Assert.assertNotNull(string,string);
    }
    
}

