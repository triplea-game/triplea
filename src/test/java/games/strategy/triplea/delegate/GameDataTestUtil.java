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

/**
 * A utility class for GameData test classes.
 */
public class GameDataTestUtil {
  /**
   * Get the german PlayerID for the given GameData object.
   * @return A german PlayerID.
   */
  public static PlayerID germans(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_GERMANS);
  }

  /**
   * Get the italian PlayerID for the given GameData object.
   * @return A italian PlayerID.
   */
  public static PlayerID italians(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_ITALIANS);
  }

  /**
   * Get the russian PlayerID for the given GameData object.
   * @return A russian PlayerID.
   */
  public static PlayerID russians(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_RUSSIANS);
  }

  /**
   * Get the american PlayerID for the given GameData object.
   * @return A american PlayerID.
   */
  public static PlayerID americans(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_AMERICANS);
  }

  /**
   * Get the british PlayerID for the given GameData object.
   * @return A british PlayerID.
   */
  public static PlayerID british(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_BRITISH);
  }

  /**
   * Get the japanese PlayerID for the given GameData object.
   * @return A japanese PlayerID.
   */
  public static PlayerID japanese(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_JAPANESE);
  }

  /**
   * Get the chinese PlayerID for the given GameData object.
   * @return A chinese PlayerID.
   */
  public static PlayerID chinese(final GameData data) {
    return data.getPlayerList().getPlayerID(Constants.PLAYER_NAME_CHINESE);
  }

  /**
   * Returns a territory object for the given name in the specified GameData object.
   * @return A Territory matching the given name if present, otherwise throwing an Exception.
   */
  public static Territory territory(final String name, final GameData data) {
    final Territory t = data.getMap().getTerritory(name);
    if (t == null) {
      throw new IllegalStateException("no territory:" + name);
    }
    return t;
  }

  /**
   * Returns an armor UnitType object for the specified GameData object.
   */
  public static UnitType armour(final GameData data) {
    return unitType(Constants.UNIT_TYPE_ARMOUR, data);
  }

  /**
   * Returns an aaGun UnitType object for the specified GameData object.
   */
  public static UnitType aaGun(final GameData data) {
    return unitType(Constants.UNIT_TYPE_AAGUN, data);
  }

  /**
   * Returns a transport UnitType object for the specified GameData object.
   */
  public static UnitType transport(final GameData data) {
    return unitType(Constants.UNIT_TYPE_TRANSPORT, data);
  }

  /**
   * Returns a battleship UnitType object for the specified GameData object.
   */
  public static UnitType battleship(final GameData data) {
    return unitType(Constants.UNIT_TYPE_BATTLESHIP, data);
  }

  /**
   * Returns a carrier UnitType object for the specified GameData object.
   */
  public static UnitType carrier(final GameData data) {
    return unitType(Constants.UNIT_TYPE_CARRIER, data);
  }

  // Some units hard coded here rather than placed in Constants at the request of community members Ref: Pull Request
  // 1074
  /**
   * Returns a tacBomber UnitType object for the specified GameData object.
   */
  public static UnitType tacBomber(final GameData data) {
    return unitType("tactical_bomber", data);
  }

  /**
   * Returns an airbase UnitType object for the specified GameData object.
   */
  public static UnitType airbase(final GameData data) {
    return unitType("airbase", data);
  }

  /**
   * Returns a harbour UnitType object for the specified GameData object.
   */
  public static UnitType harbour(final GameData data) {
    return unitType("harbour", data);
  }

  /**
   * Returns a factoryMajor UnitType object for the specified GameData object.
   */
  public static UnitType factoryMajor(final GameData data) {
    return unitType("factory_major", data);
  }

  /**
   * Returns a factoryMinor UnitType object for the specified GameData object.
   */
  public static UnitType factoryMinor(final GameData data) {
    return unitType("factory_minor", data);
  }

  /**
   * Returns a fighter UnitType object for the specified GameData object.
   */
  public static UnitType fighter(final GameData data) {
    return unitType(Constants.UNIT_TYPE_FIGHTER, data);
  }

  /**
   * Returns a destroyer UnitType object for the specified GameData object.
   */
  public static UnitType destroyer(final GameData data) {
    return unitType(Constants.UNIT_TYPE_DESTROYER, data);
  }

  /**
   * Returns a submarine UnitType object for the specified GameData object.
   */
  public static UnitType submarine(final GameData data) {
    return unitType(Constants.UNIT_TYPE_SUBMARINE, data);
  }

  /**
   * Returns an infantry UnitType object for the specified GameData object.
   */
  public static UnitType infantry(final GameData data) {
    return unitType(Constants.UNIT_TYPE_INFANTRY, data);
  }

  /**
   * Returns a bomber UnitType object for the specified GameData object.
   */
  public static UnitType bomber(final GameData data) {
    return unitType(Constants.UNIT_TYPE_BOMBER, data);
  }

  /**
   * Returns a factory UnitType object for the specified GameData object.
   */
  public static UnitType factory(final GameData data) {
    return unitType(Constants.UNIT_TYPE_FACTORY, data);
  }

  /**
   * Returns a UnitType object matching the given name for the specified GameData object if present.
   */
  private static UnitType unitType(final String name, final GameData data) {
    return data.getUnitTypeList().getUnitType(name);
  }

  /**
   * Removes all units from the given Collection from the given Territory.
   */
  public static void removeFrom(final Territory t, final Collection<Unit> units) {
    t.getData().performChange(ChangeFactory.removeUnits(t, units));
  }

  /**
   * Adds all units from the given Collection to the given Territory.
   */
  public static void addTo(final Territory t, final Collection<Unit> units) {
    t.getData().performChange(ChangeFactory.addUnits(t, units));
  }

  /**
   * Adds all units from the given Collection to the given PlayerID.
   */
  public static void addTo(final PlayerID t, final Collection<Unit> units, final GameData data) {
    data.performChange(ChangeFactory.addUnits(t, units));
  }

  /**
   * Returns a PlaceDelegate from the given GameData object.
   */
  public static PlaceDelegate placeDelegate(final GameData data) {
    return (PlaceDelegate) data.getDelegateList().getDelegate("place");
  }

  /**
   * Returns a BattleDelegate from the given GameData object.
   */
  public static BattleDelegate battleDelegate(final GameData data) {
    return (BattleDelegate) data.getDelegateList().getDelegate("battle");
  }

  /**
   * Returns a MoveDelegate from the given GameData object.
   */
  public static MoveDelegate moveDelegate(final GameData data) {
    return (MoveDelegate) data.getDelegateList().getDelegate("move");
  }

  /**
   * Returns a TechnologyDelegate from the given GameData object.
   */
  public static TechnologyDelegate techDelegate(final GameData data) {
    return (TechnologyDelegate) data.getDelegateList().getDelegate("tech");
  }

  /**
   * Returns a PurchaseDelegate from the given GameData object.
   */
  public static PurchaseDelegate purchaseDelegate(final GameData data) {
    return (PurchaseDelegate) data.getDelegateList().getDelegate("purchase");
  }

  /**
   * Returns a BidPlaceDelegate from the given GameData object.
   */
  public static BidPlaceDelegate bidPlaceDelegate(final GameData data) {
    return (BidPlaceDelegate) data.getDelegateList().getDelegate("placeBid");
  }
  
  /**
   * Returns a TestDelegateBridge for the given GameData + PlayerID objects.
   */
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
    int indexOfType = -1;
    int index = 0;
    for (final IExecutable e : steps) {
      if (type.isInstance(e)) {
        if (indexOfType != -1) {
          throw new AssertionFailedError("More than one instance:" + steps);
        }
        indexOfType = index;
      }
      index++;
    }
    if (indexOfType == -1) {
      throw new AssertionFailedError("No instance:" + steps);
    }
    return indexOfType;
  }

  public static void setSelectAaCasualties(final GameData data, final boolean val) {
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

  /**
   * Helper method to check if a String is null and otherwise print the String.
   */
  public static void assertValid(final String string) {
    Assert.assertNull(string, string);
  }
  
  /**
   * Helper method to check if a String is null and otherwise print the String
   * which doesn't make any sense because the String will then always be null.
   */
  public static void assertError(final String string) {
    Assert.assertNotNull(string, string);
  }
}
