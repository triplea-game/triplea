package games.strategy.triplea.util;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.xml.TestMapGameData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnitSeparatorTest {
  private final GameData gameData = givenGameData().build();
  private final GamePlayer player1 = new GamePlayer("player1", gameData);

  @Mock private MapData mockMapData;

  private UnitType givenUnitType(final String name) {
    final UnitType unitType = new UnitType(name, gameData);
    final UnitAttachment unitAttachment = new UnitAttachment(name, unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    return unitType;
  }

  @Test
  void testGetSortedUnitCategories() {
    final GameData data = TestMapGameData.TWW.getGameData();
    final Territory northernGermany = territory("Northern Germany", data);
    northernGermany.getUnitCollection().clear();
    final List<Unit> units = new ArrayList<>();
    final GamePlayer italians = GameDataTestUtil.italy(data);
    units.addAll(GameDataTestUtil.italianInfantry(data).create(1, italians));
    units.addAll(GameDataTestUtil.italianFactory(data).create(1, italians));
    units.addAll(GameDataTestUtil.truck(data).create(1, italians));
    final GamePlayer british = GameDataTestUtil.britain(data);
    units.addAll(GameDataTestUtil.britishInfantry(data).create(1, british));
    units.addAll(GameDataTestUtil.britishFactory(data).create(1, british));
    units.addAll(GameDataTestUtil.truck(data).create(1, british));
    final GamePlayer germans = GameDataTestUtil.germany(data);
    units.addAll(GameDataTestUtil.germanInfantry(data).create(1, germans));
    units.addAll(GameDataTestUtil.germanFactory(data).create(1, germans));
    units.addAll(GameDataTestUtil.truck(data).create(1, germans));
    GameDataTestUtil.addTo(northernGermany, units);
    when(mockMapData.shouldDrawUnit(ArgumentMatchers.anyString())).thenReturn(true);
    final List<UnitCategory> categories =
        UnitSeparator.getSortedUnitCategories(northernGermany, mockMapData);
    final List<UnitCategory> expected = new ArrayList<>();
    expected.add(newUnitCategory("germanFactory", germans, data));
    expected.add(newUnitCategory("Truck", germans, data));
    expected.add(newUnitCategory("germanInfantry", germans, data));
    expected.add(newUnitCategory("italianFactory", italians, data));
    expected.add(newUnitCategory("Truck", italians, data));
    expected.add(newUnitCategory("italianInfantry", italians, data));
    expected.add(newUnitCategory("britishFactory", british, data));
    expected.add(newUnitCategory("Truck", british, data));
    expected.add(newUnitCategory("britishInfantry", british, data));
    assertEquals(expected, categories);
  }

  @Test
  void testGetSortedUnitCategoriesDontDrawUnit() {
    final GameState data = TestMapGameData.TWW.getGameData();
    final Territory northernGermany = territory("Northern Germany", data);
    northernGermany.getUnitCollection().clear();
    final GamePlayer italians = GameDataTestUtil.italy(data);
    final List<Unit> units =
        new ArrayList<>(GameDataTestUtil.italianInfantry(data).create(1, italians));
    GameDataTestUtil.addTo(northernGermany, units);
    when(mockMapData.shouldDrawUnit(ArgumentMatchers.anyString())).thenReturn(false);
    final List<UnitCategory> categories =
        UnitSeparator.getSortedUnitCategories(northernGermany, mockMapData);
    final List<UnitCategory> expected = new ArrayList<>();
    assertEquals(expected, categories);
  }

  private static UnitCategory newUnitCategory(
      final String unitName, final GamePlayer player, final GameData data) {
    return new UnitCategory(new UnitType(unitName, data), player);
  }

  @Test
  void testCategorizeWithAirUnitsWithDifferentMovement_simplePositiveCase() {

    final UnitType dragon = givenUnitType("Dragon");
    dragon.getUnitAttachment().setHitPoints(2);
    dragon.getUnitAttachment().setMovement(4);
    dragon.getUnitAttachment().setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(dragon.createTemp(2, player1));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    final UnitSeparator.SeparatorCategories separatorCategories =
        UnitSeparator.SeparatorCategories.builder().movementForAirUnitsOnly(true).build();

    final Set<UnitCategory> categories = UnitSeparator.categorize(units, separatorCategories);

    assertThat(
        "Air units with different movement points left should be in different categories",
        categories.size() == 2);
  }

  @Test
  void testCategorizeWithAirUnitsWithDifferentMovement_insensitiveToAirUnitsOneHitPoint() {
    final UnitType drake = givenUnitType("Drake");
    drake.getUnitAttachment().setHitPoints(1);
    drake.getUnitAttachment().setMovement(4);
    drake.getUnitAttachment().setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(drake.createTemp(2, player1));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    final UnitSeparator.SeparatorCategories separatorCategories =
        UnitSeparator.SeparatorCategories.builder().movementForAirUnitsOnly(true).build();

    final Set<UnitCategory> categories = UnitSeparator.categorize(units, separatorCategories);

    assertThat(
        "Air units with type with one hit point should be in the same category",
        categories.size() == 1);
  }

  @Test
  void
      testCategorizeWithAirUnitsWithDifferentMovement_regardingAirUnitsInsensitiveToMovementFlag() {
    final UnitType dragon = givenUnitType("Dragon");
    dragon.getUnitAttachment().setHitPoints(2);
    dragon.getUnitAttachment().setMovement(4);
    dragon.getUnitAttachment().setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(dragon.createTemp(2, player1));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    final UnitSeparator.SeparatorCategories separatorCategories =
        UnitSeparator.SeparatorCategories.builder().movementForAirUnitsOnly(true).build();

    final Set<UnitCategory> categories = UnitSeparator.categorize(units, separatorCategories);

    final UnitSeparator.SeparatorCategories separatorCategoriesIncludingMovement =
        UnitSeparator.SeparatorCategories.builder()
            .movement(true)
            .movementForAirUnitsOnly(true)
            .build();

    final Set<UnitCategory> categoriesWithMovementFlag =
        UnitSeparator.categorize(units, separatorCategoriesIncludingMovement);

    assertThat(
        "Categorization of air units should be the same regardless of the pure movement flag",
        categories.equals(categoriesWithMovementFlag));
  }

  @Test
  void testCategorizeWithAirUnitsWithDifferentMovement_sensitiveToAirUnitsWithOneHitPointLeft() {
    final UnitType dragon = givenUnitType("Dragon");
    dragon.getUnitAttachment().setHitPoints(2);
    dragon.getUnitAttachment().setMovement(4);
    dragon.getUnitAttachment().setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(dragon.createTemp(2, player1));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    units.get(0).setHits(1);
    units.get(1).setHits(1);

    final UnitSeparator.SeparatorCategories separatorCategories =
        UnitSeparator.SeparatorCategories.builder().movementForAirUnitsOnly(true).build();

    final Set<UnitCategory> categories = UnitSeparator.categorize(units, separatorCategories);

    assertThat(
        "Categorization of air units should take into account hit points by type "
            + "but not hit points the unit has already taken",
        categories.size() == 2);
  }

  @Test
  void testCategorizeWithAirUnitsWithDifferentMovement_unitsWithDifferentHitsInDifferentCategory() {
    final UnitType dragon = givenUnitType("Dragon");
    dragon.getUnitAttachment().setHitPoints(2);
    dragon.getUnitAttachment().setMovement(4);
    dragon.getUnitAttachment().setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(dragon.createTemp(2, player1));

    units.get(0).setHits(1);

    final UnitSeparator.SeparatorCategories separatorCategories =
        UnitSeparator.SeparatorCategories.builder().movementForAirUnitsOnly(true).build();

    final Set<UnitCategory> categories = UnitSeparator.categorize(units, separatorCategories);

    assertThat(
        "units with different hits should be in different categories", categories.size() == 2);
  }

  @Test
  void testCategorizeWithAirUnitsWithDifferentMovement_insensitiveToNonAirUnits() {
    final UnitType tank = givenUnitType("Tank");
    tank.getUnitAttachment().setHitPoints(2);
    tank.getUnitAttachment().setMovement(4);

    final UnitType battleship = givenUnitType("Battleship");
    battleship.getUnitAttachment().setHitPoints(2);
    battleship.getUnitAttachment().setMovement(4);
    battleship.getUnitAttachment().setIsSea(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(tank.createTemp(2, player1));
    units.addAll(battleship.createTemp(2, player1));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));
    units.get(2).setAlreadyMoved(BigDecimal.ONE);
    units.get(3).setAlreadyMoved(BigDecimal.valueOf(2));

    final UnitSeparator.SeparatorCategories separatorCategories =
        UnitSeparator.SeparatorCategories.builder().movementForAirUnitsOnly(true).build();

    final Set<UnitCategory> categories = UnitSeparator.categorize(units, separatorCategories);

    assertThat(
        "Non-Air units with different movement points left should be in the same category",
        categories.size() == 2);
  }
}
