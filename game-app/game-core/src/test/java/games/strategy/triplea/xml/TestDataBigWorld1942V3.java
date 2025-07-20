package games.strategy.triplea.xml;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.delegate.TechAdvance;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.triplea.java.collections.IntegerMap;

public class TestDataBigWorld1942V3 {

  public final GameData gameData;
  public final GamePlayer british;
  public final Territory france;

  public final UnitType tank;
  public final UnitType infantry;
  public final UnitType marine;
  public final UnitType artillery;
  public final UnitType fighter;
  public final UnitType bomber;
  public final IntegerMap<UnitType> costMap;

  public TestDataBigWorld1942V3() {
    gameData = TestMapGameData.BIG_WORLD_1942_V3.getGameData();
    british = checkNotNull(gameData.getPlayerList().getPlayerId("British"));
    france = checkNotNull(gameData.getMap().getTerritoryOrThrow("France"));
    tank = checkNotNull(gameData.getUnitTypeList().getUnitTypeOrThrow("armour"));
    infantry = checkNotNull(gameData.getUnitTypeList().getUnitTypeOrThrow("infantry"));
    marine = checkNotNull(gameData.getUnitTypeList().getUnitTypeOrThrow("marine"));
    artillery = checkNotNull(gameData.getUnitTypeList().getUnitTypeOrThrow("artillery"));
    fighter = checkNotNull(gameData.getUnitTypeList().getUnitTypeOrThrow("fighter"));
    bomber = checkNotNull(gameData.getUnitTypeList().getUnitTypeOrThrow("bomber"));

    costMap =
        IntegerMap.of(
            Map.of(
                marine, 4,
                artillery, 4,
                tank, 5,
                fighter, 10));
  }

  public Collection<Unit> tank(final int count) {
    return createUnit(tank, count);
  }

  public Collection<Unit> infantry(final int count) {
    return createUnit(infantry, count);
  }

  public Collection<Unit> marine(final int count) {
    return createUnit(marine, count);
  }

  public Collection<Unit> artillery(final int count) {
    return createUnit(artillery, count);
  }

  public Collection<Unit> fighter(final int count) {
    return createUnit(fighter, count);
  }

  public Collection<Unit> bomber(final int count) {
    return createUnit(bomber, count);
  }

  private Collection<Unit> createUnit(final UnitType unitType, final int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> new Unit(unitType, british, gameData))
        .collect(Collectors.toSet());
  }

  public void addTech(final TechAdvance techAdvance) {
    final var change =
        ChangeFactory.attachmentPropertyChange(
            british.getTechAttachment(), true, techAdvance.getProperty());
    gameData.performChange(change);
  }
}
