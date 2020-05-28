package games.strategy.triplea.delegate;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitHolder;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.util.TransportUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import junit.framework.AssertionFailedError;
import lombok.experimental.UtilityClass;
import org.junit.jupiter.api.Assertions;
import org.triplea.java.collections.IntegerMap;

@UtilityClass
public final class GameDataTestUtil {

  /**
   * Get the german PlayerId for the given GameData object.
   *
   * @return A german PlayerId.
   */
  public static GamePlayer germans(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_GERMANS);
  }

  /**
   * Get the germany PlayerId for the given GameData object.
   *
   * @return A germany PlayerId.
   */
  public static GamePlayer germany(final GameData data) {
    return data.getPlayerList().getPlayerId("Germany");
  }

  /**
   * Get the italian PlayerId for the given GameData object.
   *
   * @return A italian PlayerId.
   */
  static GamePlayer italians(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_ITALIANS);
  }

  public static GamePlayer italy(final GameData data) {
    return data.getPlayerList().getPlayerId("Italy");
  }

  /**
   * Get the russian PlayerId for the given GameData object.
   *
   * @return A russian PlayerId.
   */
  public static GamePlayer russians(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_RUSSIANS);
  }

  /**
   * Get the american PlayerId for the given GameData object.
   *
   * @return A american PlayerId.
   */
  public static GamePlayer americans(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_AMERICANS);
  }

  /**
   * Get the USA PlayerId for the given GameData object.
   *
   * @return A USA PlayerId.
   */
  public static GamePlayer usa(final GameData data) {
    return data.getPlayerList().getPlayerId("Usa");
  }

  /**
   * Get the british PlayerId for the given GameData object.
   *
   * @return A british PlayerId.
   */
  public static GamePlayer british(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_BRITISH);
  }

  public static GamePlayer britain(final GameData data) {
    return data.getPlayerList().getPlayerId("Britain");
  }

  public static GamePlayer french(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_FRENCH);
  }

  /**
   * Get the japanese PlayerId for the given GameData object.
   *
   * @return A japanese PlayerId.
   */
  public static GamePlayer japanese(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_JAPANESE);
  }

  /**
   * Get the Japan PlayerId for the given GameData object.
   *
   * @return A Japan PlayerId.
   */
  public static GamePlayer japan(final GameData data) {
    return data.getPlayerList().getPlayerId("Japan");
  }

  /**
   * Get the chinese PlayerId for the given GameData object.
   *
   * @return A chinese PlayerId.
   */
  public static GamePlayer chinese(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_CHINESE);
  }

  /**
   * Returns a territory object for the given name in the specified GameData object.
   *
   * @return A Territory matching the given name if present, otherwise throwing an Exception.
   */
  public static Territory territory(final String name, final GameData data) {
    return checkNotNull(data.getMap().getTerritory(name), "No territory:" + name);
  }

  /** Returns an armor UnitType object for the specified GameData object. */
  public static UnitType armour(final GameData data) {
    return unitType(Constants.UNIT_TYPE_ARMOUR, data);
  }

  /** Returns an aaGun UnitType object for the specified GameData object. */
  public static UnitType aaGun(final GameData data) {
    return unitType(Constants.UNIT_TYPE_AAGUN, data);
  }

  /** Returns a transport UnitType object for the specified GameData object. */
  public static UnitType transport(final GameData data) {
    return unitType(Constants.UNIT_TYPE_TRANSPORT, data);
  }

  /** Returns a battleship UnitType object for the specified GameData object. */
  public static UnitType battleship(final GameData data) {
    return unitType(Constants.UNIT_TYPE_BATTLESHIP, data);
  }

  /** Returns a germanBattleship UnitType object for the specified GameData object. */
  public static UnitType germanBattleship(final GameData data) {
    return unitType("germanBattleship", data);
  }

  public static UnitType britishBattleship(final GameData data) {
    return unitType("britishBattleship", data);
  }

  public static UnitType britishAlpineInfantry(final GameData data) {
    return unitType("britishAlpineInfantry", data);
  }

  /** Returns a carrier UnitType object for the specified GameData object. */
  public static UnitType carrier(final GameData data) {
    return unitType(Constants.UNIT_TYPE_CARRIER, data);
  }

  /** Returns a fighter UnitType object for the specified GameData object. */
  public static UnitType fighter(final GameData data) {
    return unitType(Constants.UNIT_TYPE_FIGHTER, data);
  }

  public static UnitType airTransport(final GameData data) {
    return unitType("air_transport", data);
  }

  /** Returns a germanFighter UnitType object for the specified GameData object. */
  public static UnitType germanFighter(final GameData data) {
    return unitType("germanFighter", data);
  }

  /** Returns a britishFighter UnitType object for the specified GameData object. */
  public static UnitType britishFighter(final GameData data) {
    return unitType("britishFighter", data);
  }

  /** Returns a destroyer UnitType object for the specified GameData object. */
  public static UnitType destroyer(final GameData data) {
    return unitType(Constants.UNIT_TYPE_DESTROYER, data);
  }

  /** Returns a submarine UnitType object for the specified GameData object. */
  public static UnitType submarine(final GameData data) {
    return unitType(Constants.UNIT_TYPE_SUBMARINE, data);
  }

  /** Returns a germanSubmarine UnitType object for the specified GameData object. */
  public static UnitType germanSubmarine(final GameData data) {
    return unitType("germanSubmarine", data);
  }

  /** Returns a britishSubmarine UnitType object for the specified GameData object. */
  public static UnitType britishSubmarine(final GameData data) {
    return unitType("britishSubmarine", data);
  }

  /** Returns an infantry UnitType object for the specified GameData object. */
  public static UnitType infantry(final GameData data) {
    return unitType(Constants.UNIT_TYPE_INFANTRY, data);
  }

  /** Returns an artillery UnitType object for the specified GameData object. */
  public static UnitType artillery(final GameData data) {
    return unitType(Constants.UNIT_TYPE_ARTILLERY, data);
  }

  /** Returns a germanInfantry UnitType object for the specified GameData object. */
  public static UnitType germanInfantry(final GameData data) {
    return unitType("germanInfantry", data);
  }

  public static UnitType italianInfantry(final GameData data) {
    return unitType("italianInfantry", data);
  }

  public static UnitType britishInfantry(final GameData data) {
    return unitType("britishInfantry", data);
  }

  /** Returns a bomber UnitType object for the specified GameData object. */
  public static UnitType bomber(final GameData data) {
    return unitType(Constants.UNIT_TYPE_BOMBER, data);
  }

  /** Returns a factory UnitType object for the specified GameData object. */
  public static UnitType factory(final GameData data) {
    return unitType(Constants.UNIT_TYPE_FACTORY, data);
  }

  /** Returns a germanFactory UnitType object for the specified GameData object. */
  public static UnitType germanFactory(final GameData data) {
    return unitType("germanFactory", data);
  }

  public static UnitType italianFactory(final GameData data) {
    return unitType("italianFactory", data);
  }

  public static UnitType britishFactory(final GameData data) {
    return unitType("britishFactory", data);
  }

  /** Returns a germanFortification UnitType object for the specified GameData object. */
  public static UnitType germanFortification(final GameData data) {
    return unitType("germanFortification", data);
  }

  /** Returns a truck UnitType object for the specified GameData object. */
  public static UnitType truck(final GameData data) {
    return unitType("Truck", data);
  }

  /** Returns a large truck UnitType object for the specified GameData object. */
  public static UnitType largeTruck(final GameData data) {
    return unitType("LargeTruck", data);
  }

  /** Returns a germanTrain UnitType object for the specified GameData object. */
  public static UnitType germanTrain(final GameData data) {
    return unitType("germanTrain", data);
  }

  /** Returns a germanRail UnitType object for the specified GameData object. */
  public static UnitType germanRail(final GameData data) {
    return unitType("germanRail", data);
  }

  /** Returns a germanMine UnitType object for the specified GameData object. */
  public static UnitType germanMine(final GameData data) {
    return unitType("germanMine", data);
  }

  /** Returns a germanArtillery UnitType object for the specified GameData object. */
  public static UnitType germanArtillery(final GameData data) {
    return unitType("germanArtillery", data);
  }

  /** Returns a britishArtillery UnitType object for the specified GameData object. */
  public static UnitType britishArtillery(final GameData data) {
    return unitType("britishArtillery", data);
  }

  /** Returns a germanAntiTankGun UnitType object for the specified GameData object. */
  public static UnitType germanAntiTankGun(final GameData data) {
    return unitType("germanAntiTankGun", data);
  }

  public static UnitType germanAntiAirGun(final GameData data) {
    return unitType("germanAntiAirGun", data);
  }

  /** Returns a germanATSupport UnitType object for the specified GameData object. */
  public static UnitType germanAtSupport(final GameData data) {
    return unitType("germanATSupport", data);
  }

  /** Returns a americanTank UnitType object for the specified GameData object. */
  public static UnitType americanAtCounter(final GameData data) {
    return unitType("americanATCounter", data);
  }

  /** Returns a americanTank UnitType object for the specified GameData object. */
  public static UnitType americanTank(final GameData data) {
    return unitType("americanTank", data);
  }

  public static UnitType britishTank(final GameData data) {
    return unitType("britishTank", data);
  }

  public static UnitType germanTank(final GameData data) {
    return unitType("germanTank", data);
  }

  /** Returns a americanCruiser UnitType object for the specified GameData object. */
  public static UnitType americanCruiser(final GameData data) {
    return unitType("americanCruiser", data);
  }

  /** Returns a americanStrategicBomber UnitType object for the specified GameData object. */
  public static UnitType americanStrategicBomber(final GameData data) {
    return unitType("americanStrategicBomber", data);
  }

  public static UnitType antiTankGun(final GameData data) {
    return unitType("antiTankGun", data);
  }

  public static UnitType antiAirGun(final GameData data) {
    return unitType("antiAirGun", data);
  }

  public static UnitType suicideAntiAirGun(final GameData data) {
    return unitType("suicideAntiAirGun", data);
  }

  public static UnitType suicideAttacker(final GameData data) {
    return unitType("suicideAttacker", data);
  }

  public static UnitType suicideDefender(final GameData data) {
    return unitType("suicideDefender", data);
  }

  public static UnitType missile(final GameData data) {
    return unitType("missile", data);
  }

  public static UnitType spySuicider(final GameData data) {
    return unitType("spySuicider", data);
  }

  public static UnitType spyKiller(final GameData data) {
    return unitType("spyKiller", data);
  }

  public static UnitType spy(final GameData data) {
    return unitType("spy", data);
  }

  public static UnitType cruiser(final GameData data) {
    return unitType("cruiser", data);
  }

  public static UnitType tank(final GameData data) {
    return unitType("tank", data);
  }

  /**
   * Returns a UnitType object matching the given name for the specified GameData object if present.
   */
  static UnitType unitType(final String name, final GameData data) {
    return data.getUnitTypeList().getUnitType(name);
  }

  /** Removes all units from the given Collection from the given Territory. */
  static void removeFrom(final Territory t, final Collection<Unit> units) {
    t.getData().performChange(ChangeFactory.removeUnits(t, units));
  }

  /** Adds all units from the given Collection to the given Territory. */
  public static void addTo(final Territory t, final Collection<Unit> units) {
    t.getData().performChange(ChangeFactory.addUnits(t, units));
  }

  /** Adds all units from the given Collection to the given PlayerId. */
  static void addTo(final GamePlayer t, final Collection<Unit> units, final GameData data) {
    data.performChange(ChangeFactory.addUnits(t, units));
  }

  /** Returns a PlaceDelegate from the given GameData object. */
  static PlaceDelegate placeDelegate(final GameData data) {
    return (PlaceDelegate) data.getDelegate("place");
  }

  /** Returns a BattleDelegate from the given GameData object. */
  public static BattleDelegate battleDelegate(final GameData data) {
    return (BattleDelegate) data.getDelegate("battle");
  }

  /** Returns a MoveDelegate from the given GameData object. */
  public static MoveDelegate moveDelegate(final GameData data) {
    return (MoveDelegate) data.getDelegate("move");
  }

  /** Returns a TechnologyDelegate from the given GameData object. */
  static TechnologyDelegate techDelegate(final GameData data) {
    return (TechnologyDelegate) data.getDelegate("tech");
  }

  /** Returns a PurchaseDelegate from the given GameData object. */
  static PurchaseDelegate purchaseDelegate(final GameData data) {
    return (PurchaseDelegate) data.getDelegate("purchase");
  }

  /** Returns a BidPlaceDelegate from the given GameData object. */
  static BidPlaceDelegate bidPlaceDelegate(final GameData data) {
    return (BidPlaceDelegate) data.getDelegate("placeBid");
  }

  static void load(final Collection<Unit> units, final Route route) {
    if (units.isEmpty()) {
      throw new AssertionFailedError("No units");
    }
    final MoveDelegate moveDelegate = moveDelegate(route.getStart().getData());
    final Collection<Unit> transports =
        route
            .getEnd()
            .getUnitCollection()
            .getMatches(Matches.unitIsOwnedBy(units.iterator().next().getOwner()));
    final Map<Unit, Unit> unitsToTransports =
        TransportUtils.mapTransports(route, units, transports);
    if (unitsToTransports.size() != units.size()) {
      throw new AssertionFailedError("Not all units mapped to transports");
    }
    final String error =
        moveDelegate.performMove(new MoveDescription(units, route, unitsToTransports));
    if (error != null) {
      throw new AssertionFailedError("Illegal move:" + error);
    }
  }

  public static void move(final Collection<Unit> units, final Route route) {
    if (units.isEmpty()) {
      throw new AssertionFailedError("No units");
    }
    final String error =
        moveDelegate(route.getStart().getData()).performMove(new MoveDescription(units, route));
    if (error != null) {
      throw new AssertionFailedError("Illegal move:" + error);
    }
  }

  static void assertMoveError(final Collection<Unit> units, final Route route) {
    if (units.isEmpty()) {
      throw new AssertionFailedError("No units");
    }
    final String error = moveDelegate(route.getStart().getData()).move(units, route);
    if (error == null) {
      throw new AssertionFailedError("Should not be Legal move");
    }
  }

  static int getIndex(final List<IExecutable> steps, final Class<?> type) {
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
    for (final IEditableProperty<?> property : data.getProperties().getEditableProperties()) {
      if (property.getName().equals(Constants.CHOOSE_AA)) {
        ((BooleanProperty) property).setValue(val);
        return;
      }
    }
    throw new IllegalStateException();
  }

  public static void makeGameLowLuck(final GameData data) {
    for (final IEditableProperty<?> property : data.getProperties().getEditableProperties()) {
      if (property.getName().equals(Constants.LOW_LUCK)) {
        ((BooleanProperty) property).setValue(true);
        return;
      }
    }
    throw new IllegalStateException();
  }

  static void givePlayerRadar(final GamePlayer player) {
    TechAttachment.get(player).setAaRadar(Boolean.TRUE.toString());
  }

  /** Helper method to check if a String is null and otherwise print the String. */
  static void assertValid(final String string) {
    Assertions.assertNull(string, string);
  }

  /**
   * Helper method to check if a String is not null. In this scenario used to verify an error
   * message exists.
   */
  static void assertError(final String string) {
    Assertions.assertNotNull(string);
  }

  /**
   * Gets a collection of units from the specified unit holder (e.g. territory, player, etc.)
   * consisting of up to the specified maximum count of each specified unit type.
   *
   * @param maxUnitCountsByType The maximum count of each type of unit to include in the returned
   *     collection. The key is the unit type. The value is the maximum unit count.
   * @param from The territory from which the units are to be collected.
   * @return A collection of units from the specified unit holder.
   */
  public static Collection<Unit> getUnits(
      final IntegerMap<UnitType> maxUnitCountsByType, final UnitHolder from) {
    checkNotNull(maxUnitCountsByType);
    checkNotNull(from);

    return maxUnitCountsByType.entrySet().stream()
        .flatMap(
            entry -> from.getUnitCollection().getUnits(entry.getKey(), entry.getValue()).stream())
        .collect(Collectors.toList());
  }
}
