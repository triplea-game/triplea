package games.strategy.triplea.ui;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.triplea.attachments.UnitAttachment;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.injection.Injections;

class BattleDisplayTest {
  @Mock private GameData gameData;
  private GamePlayer player1 = new GamePlayer("player1", gameData);
  private GamePlayer player2 = new GamePlayer("player2", gameData);

  private UnitType givenUnitType(final String name) {
    final UnitType unitType = new UnitType(name, gameData);
    final UnitAttachment unitAttachment = new UnitAttachment(name, unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    return unitType;
  }

  private static Injections constructInjections() {
    return Injections.builder()
        .engineVersion(new ProductVersionReader().getVersion())
        .playerTypes(new ArrayList<PlayerTypes.Type>())
        .build();
  }

  @Test
  void testPlayerMayChooseToDistributeHitsToUnitsWithDifferentMovement_simplePositiveCase() {
    Injections.init(constructInjections());
    final File file = new File("./src/test/resources/revised_test.xml");
    final Path fPath = file.toPath();
    final Optional<GameData> oGameData = GameParser.parse(fPath);
    gameData = oGameData.get();
    final GamePlayer player = new GamePlayer("player", gameData);
    final UnitType dragon = givenUnitType("Dragon");
    UnitAttachment.get(dragon).setHitPoints(2);
    UnitAttachment.get(dragon).setMovement(4);
    UnitAttachment.get(dragon).setIsAir(true);

    List<Unit> units = new ArrayList<>();
    units.addAll(dragon.createTemp(2, player));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    assertThat(
        "Air units with different movement points should cause that the player can choose",
        BattleDisplay.playerMayChooseToDistributeHitsToUnitsWithDifferentMovement(units));
  }

  @Test
  void testPlayerMayChooseToDistributeHitsToUnitsWithDifferentMovement_noAirUnits() {
    final UnitType mechInfantry = givenUnitType("mech infantry");
    UnitAttachment.get(mechInfantry).setHitPoints(2);
    UnitAttachment.get(mechInfantry).setMovement(4);

    List<Unit> units = new ArrayList<>();
    units.addAll(mechInfantry.createTemp(2, player1));
    units.addAll(mechInfantry.createTemp(1, player2));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));
    units.get(2).setAlreadyMoved(BigDecimal.valueOf(2));

    assertThat(
        "Non-air units should not affect if the player chooses between units"+
        " with different movement points",
        !BattleDisplay.playerMayChooseToDistributeHitsToUnitsWithDifferentMovement(units));
  }
}
