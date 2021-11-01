package games.strategy.triplea.ui;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.ui.CasualtySelection.playerMayChooseToDistributeHitsToUnitsWithDifferentMovement;
import static org.hamcrest.MatcherAssert.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CasualtySelectionTest {
  CasualtySelectionTest() {
    System.out.println("costrictiong CasualtySelectionTest");
  }

  private static final GameData gameData = TestMapGameData.BIG_WORLD_1942_V3.getGameData();
  private static final GamePlayer player1 = new GamePlayer("player1", gameData);
  private static final GamePlayer player2 = new GamePlayer("player2", gameData);
  private static boolean windowClosed = false;


  private static UnitType givenUnitType(final String name) {
    final UnitType unitType = new UnitType(name, gameData);
    final UnitAttachment unitAttachment = new UnitAttachment(name, unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    return unitType;
  }

  @Test
  void testPlayerMayChooseToDistributeHitsToUnitsWithDifferentMovement_simplePositiveCase() {
    final GamePlayer player = new GamePlayer("player", gameData);
    final UnitType dragon = givenUnitType("Dragon");
    UnitAttachment.get(dragon).setHitPoints(2);
    UnitAttachment.get(dragon).setMovement(4);
    UnitAttachment.get(dragon).setIsAir(true);

    final List<Unit> units = new ArrayList<>(dragon.createTemp(2, player));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    assertThat(
        "Air units with different movement points should cause that the player can choose",
        playerMayChooseToDistributeHitsToUnitsWithDifferentMovement(units));
  }

  @Test
  void testPlayerMay_Movement_onlyUnitsWithMoreThanOneHitpointLeftCount() {
    final GamePlayer player = new GamePlayer("player", gameData);
    final UnitType dragon = givenUnitType("Dragon");
    UnitAttachment.get(dragon).setHitPoints(2);
    UnitAttachment.get(dragon).setMovement(4);
    UnitAttachment.get(dragon).setIsAir(true);

    final List<Unit> units = new ArrayList<>(dragon.createTemp(2, player));

    units.get(0).setHits(1);
    units.get(1).setHits(1);

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    assertThat(
        "Air units with only one hitpoint left should not cause that the player can choose",
        !playerMayChooseToDistributeHitsToUnitsWithDifferentMovement(units));
  }

  @Test
  void testPlayerMayChooseToDistributeHitsToUnitsWithDifferentMovement_noAirUnits() {
    final UnitType mechInfantry = givenUnitType("mech infantry");
    UnitAttachment.get(mechInfantry).setHitPoints(2);
    UnitAttachment.get(mechInfantry).setMovement(4);

    final List<Unit> units = new ArrayList<>();
    units.addAll(mechInfantry.createTemp(2, player1));
    units.addAll(mechInfantry.createTemp(1, player2));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));
    units.get(2).setAlreadyMoved(BigDecimal.valueOf(2));

    assertThat(
        "Non-air units should not affect if the player chooses between units"
            +" with different movement points",
        !playerMayChooseToDistributeHitsToUnitsWithDifferentMovement(units));
  }
}