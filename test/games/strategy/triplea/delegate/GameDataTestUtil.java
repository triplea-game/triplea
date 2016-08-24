package games.strategy.triplea.delegate;

import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.List;

import org.junit.Assert;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TestDelegateBridge;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.ui.display.ITripleADisplay;
import junit.framework.AssertionFailedError;

public class GameDataTestUtil {
  public static PlayerID germans(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_GERMANS);
  }

  public static PlayerID italians(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_ITALIANS);
  }

  public static PlayerID russians(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_RUSSIANS);
  }

  public static PlayerID americans(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_AMERICANS);
  }

  public static PlayerID british(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_BRITISH);
  }

  public static PlayerID japanese(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_JAPANESE);
  }

  public static PlayerID chinese(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_CHINESE);
  }

  public static Territory territory(final String name, final GameData data) {
    final Territory t = data.getMap().getTerritory(name);
    if (t == null) {
      throw new IllegalStateException("no territory:" + name);
    }
    return t;
  }

  public static UnitType armour(final GameData data) {
    return unitType(Constants.UNIT_TYPE_ARMOUR, data);
  }

  public static UnitType aaGun(final GameData data) {
    return unitType(Constants.UNIT_TYPE_AAGUN, data);
  }

  public static UnitType transport(final GameData data) {
    return unitType(Constants.UNIT_TYPE_TRANSPORT, data);
  }

  public static UnitType battleship(final GameData data) {
    return unitType(Constants.UNIT_TYPE_BATTLESHIP, data);
  }

  public static UnitType carrier(final GameData data) {
    return unitType(Constants.UNIT_TYPE_CARRIER, data);
  }

  // Some units hard coded here rather than placed in Constants at the insistence of community members Ref: Pull Request 1074
  public static UnitType tacBomber(final GameData data) {
    return unitType("tactical_bomber", data);			
  }

  public static UnitType airbase(final GameData data) {
    return unitType("airbase", data);			
  }

  public static UnitType harbour(final GameData data) {
    return unitType("harbour", data);			
  }

  public static UnitType factoryMajor(final GameData data) {
    return unitType("factory_major", data);			
  }

  public static UnitType factoryMinor(final GameData data) {
    return unitType("factory_minor", data);			
  }

  public static UnitType fighter(final GameData data) {
    return unitType(Constants.UNIT_TYPE_FIGHTER, data);
  }

  public static UnitType destroyer(final GameData data) {
    return unitType(Constants.UNIT_TYPE_DESTROYER, data);
  }

  public static UnitType submarine(final GameData data) {
    return unitType(Constants.UNIT_TYPE_SUBMARINE, data);
  }

  public static UnitType infantry(final GameData data) {
    return unitType(Constants.UNIT_TYPE_INFANTRY, data);
  }

  public static UnitType bomber(final GameData data) {
    return unitType(Constants.UNIT_TYPE_BOMBER, data);
  }

  public static UnitType factory(final GameData data) {
    return unitType(Constants.UNIT_TYPE_FACTORY, data);
  }

  private static UnitType unitType(final String name, final GameData data) {
    return data.getUnitTypeList().getUnitType(name);
  }

  public static void removeFrom(final Territory t, final Collection<Unit> units) {
    t.getData().performChange(ChangeFactory.removeUnits(t, units));
  }

  public static void addTo(final Territory t, final Collection<Unit> units) {
    t.getData().performChange(ChangeFactory.addUnits(t, units));
  }

  public static void addTo(final PlayerID t, final Collection<Unit> units, final GameData data) {
    data.performChange(ChangeFactory.addUnits(t, units));
  }

  public static PlaceDelegate placeDelegate(final GameData data) {
    return (PlaceDelegate) data.getDelegateList().getDelegate("place");
  }

  public static BattleDelegate battleDelegate(final GameData data) {
    return (BattleDelegate) data.getDelegateList().getDelegate("battle");
  }

  public static MoveDelegate moveDelegate(final GameData data) {
    return (MoveDelegate) data.getDelegateList().getDelegate("move");
  }

  public static TechnologyDelegate techDelegate(final GameData data) {
    return (TechnologyDelegate) data.getDelegateList().getDelegate("tech");
  }

  public static PurchaseDelegate purchaseDelegate(final GameData data) {
    return (PurchaseDelegate) data.getDelegateList().getDelegate("purchase");
  }

  public static BidPlaceDelegate bidPlaceDelegate(final GameData data) {
    return (BidPlaceDelegate) data.getDelegateList().getDelegate("placeBid");
  }

  public static ITestDelegateBridge getDelegateBridge(final PlayerID player, final GameData data) {
    return new TestDelegateBridge(data, player, mock(ITripleADisplay.class));
  }

  public static void load(final Collection<Unit> units, final Route route) {
    if (units.isEmpty()) {
      throw new AssertionFailedError("No units");
    }
    final MoveDelegate moveDelegate = moveDelegate(route.getStart().getData());
    final Collection<Unit> transports =
        route.getEnd().getUnits().getMatches(Matches.unitIsOwnedBy(units.iterator().next().getOwner()));
    final String error = moveDelegate.move(units, route, transports);
    if (error != null) {
      throw new AssertionFailedError("Illegal move:" + error);
    }
  }

  public static void move(final Collection<Unit> units, final Route route) {
    if (units.isEmpty()) {
      throw new AssertionFailedError("No units");
    }
    final String error = moveDelegate(route.getStart().getData()).move(units, route);
    if (error != null) {
      throw new AssertionFailedError("Illegal move:" + error);
    }
  }

  public static void assertMoveError(final Collection<Unit> units, final Route route) {
    if (units.isEmpty()) {
      throw new AssertionFailedError("No units");
    }
    final String error = moveDelegate(route.getStart().getData()).move(units, route);
    if (error == null) {
      throw new AssertionFailedError("Should not be Legal move");
    }
  }

  public static int getIndex(final List<IExecutable> steps, final Class<?> type) {
    int rVal = -1;
    int index = 0;
    for (final IExecutable e : steps) {
      if (type.isInstance(e)) {
        if (rVal != -1) {
          throw new AssertionFailedError("More than one instance:" + steps);
        }
        rVal = index;
      }
      index++;
    }
    if (rVal == -1) {
      throw new AssertionFailedError("No instance:" + steps);
    }
    return rVal;
  }

  public static void setSelectAACasualties(final GameData data, final boolean val) {
    for (final IEditableProperty property : data.getProperties().getEditableProperties()) {
      if (property.getName().equals(Constants.CHOOSE_AA)) {
        ((BooleanProperty) property).setValue(val);
        return;
      }
    }
    throw new IllegalStateException();
  }

  public static void makeGameLowLuck(final GameData data) {
    for (final IEditableProperty property : data.getProperties().getEditableProperties()) {
      if (property.getName().equals(Constants.LOW_LUCK)) {
        ((BooleanProperty) property).setValue(true);
        return;
      }
    }
    throw new IllegalStateException();
  }

  public static void givePlayerRadar(final PlayerID player) {
    TechAttachment.get(player).setAARadar(Boolean.TRUE.toString());
  }

  public static void assertValid(final String string) {
    Assert.assertNull(string, string);
  }

  public static void assertError(final String string) {
    Assert.assertNotNull(string, string);
  }
}
