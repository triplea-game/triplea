package games.strategy.engine.data.gameparser;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.xml.TestMapGameDataLoader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.util.Tuple;

final class GameParserTest {

  @Test
  @DisplayName("Verify backward compatibility can parse 1.8 maps")
  void backwardCompatibilityCheck() throws Exception {
    final GameData gameData = TestMapGameDataLoader.loadGameData("v1_8_map__270BC.xml");
    assertNotNullGameData(gameData);

    verifyLegacyPropertiesAreUpdated(gameData);
  }

  /** Asserts that we loaded a relatively complete looking game data. */
  private void assertNotNullGameData(final GameData gameData) {
    assertThat(gameData.getAttachmentOrderAndValues()).isNotNull();
    assertThat(gameData.getAllianceTracker().getAlliances()).isNotNull();
    assertThat(gameData.getBattleRecordsList()).isNotNull();
    assertThat(gameData.getDelegates()).isNotNull();
    assertThat(gameData.getDiceSides()).isNotNull();
    assertThat(gameData.getGameLoader()).isNotNull();
    assertThat(gameData.getGameName()).isNotNull();
    assertThat(gameData.getHistory().getLastNode()).isNotNull();
    assertThat(gameData.getMap().getTerritories()).isNotNull();
    assertThat(gameData.getPlayerList().getPlayers()).isNotNull();
    assertThat(gameData.getProductionFrontierList().getProductionFrontierNames()).isNotNull();
    assertThat(gameData.getProductionRuleList().getProductionRules()).isNotNull();
    assertThat(gameData.getProperties()).isNotNull();
    assertThat(gameData.getRelationshipTracker()).isNotNull();
    assertThat(gameData.getRelationshipTypeList().getAllRelationshipTypes()).isNotNull();
    assertThat(gameData.getRepairFrontierList().getRepairFrontierNames()).isNotNull();
    assertThat(gameData.getResourceList().getResources()).isNotNull();
    assertThat(gameData.getSaveGameFileName()).isNotNull();
    assertThat(gameData.getSequence().getRound()).isNotNull();
    assertThat(gameData.getSequence().getStep()).isNotNull();
    assertThat(gameData.getTechnologyFrontier().getTechs()).isNotNull();
    assertThat(gameData.getTerritoryEffectList()).isNotNull();
    assertThat(gameData.getUnits().getUnits()).isNotNull();
    assertThat(gameData.getUnitTypeList().getAllUnitTypes()).isNotNull();
  }

  /**
   * The test-XML is intentinally loaded with legacy properties and options. Here we assert that
   * those legacy values have been forward-ported to their new, non-legacy values.
   */
  private void verifyLegacyPropertiesAreUpdated(final GameState gameData) {
    assertThat(gameData.getProperties().get(Constants.TWO_HIT_BATTLESHIPS_REPAIR_END_OF_TURN))
        .isEqualTo(true);
    assertThat(gameData.getProperties().get(Constants.TWO_HIT_BATTLESHIPS_REPAIR_BEGINNING_OF_TURN))
        .isEqualTo(true);

    final var spartaTerritoryAttachment =
        (TerritoryAttachment)
            gameData
                .getMap()
                .getTerritoryOrNull("Sparta")
                .getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);

    assertThat(spartaTerritoryAttachment.getVictoryCity()).isEqualTo(1);
    assertThat(spartaTerritoryAttachment.getOriginalOwner().map(GamePlayer::getName).orElse(""))
        .isEqualTo("RomanRepublic");
    assertThat(spartaTerritoryAttachment.getIsImpassable()).isTrue();

    final var romaTerritoryAttachment =
        (TerritoryAttachment)
            gameData
                .getMap()
                .getTerritoryOrNull("Roma")
                .getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);

    assertThat(romaTerritoryAttachment.getVictoryCity()).isEqualTo(0);

    final var archerUnitAttachment =
        (UnitAttachment)
            gameData
                .getUnitTypeList()
                .getUnitTypeOrThrow("archer")
                .getAttachment(Constants.UNIT_ATTACHMENT_NAME);

    assertThat(archerUnitAttachment.getHitPoints())
        .as("Verify isTwoHitPoint=true is converted to hitPoints = 2")
        .isEqualTo(2);
    assertThat(archerUnitAttachment.isAirTransportable())
        .as("Verify is paratroop is converted")
        .isTrue();
    assertThat(archerUnitAttachment.isLandTransportable())
        .as("Verify isMechanized is converted")
        .isTrue();

    final var axemanUnitAttachment =
        ((UnitAttachment)
            gameData
                .getUnitTypeList()
                .getUnitTypeOrThrow("axeman")
                .getAttachment(Constants.UNIT_ATTACHMENT_NAME));

    assertThat(axemanUnitAttachment.isLandTransportable())
        .as("Verify isInfantry is converted")
        .isTrue();

    assertThat(
            ((RulesAttachment)
                    gameData
                        .getPlayerList()
                        .getPlayerId("Carthage")
                        .getAttachment("conditionAttachmentAntiRomanVictory8"))
                .getRounds())
        .isEqualTo(Map.of(1, 1, 2, 2));
  }

  @Nested
  final class DecapitalizeTest {
    @Test
    void shouldReturnValueWithFirstCharacterDecapitalized() {
      List.of(
              Tuple.of("", ""),
              Tuple.of("N", "n"),
              Tuple.of("name", "name"),
              Tuple.of("Name", "name"),
              Tuple.of("NAME", "nAME"))
          .forEach(
              t -> {
                final String value = t.getFirst();
                final String decapitalizedValue = t.getSecond();
                assertThat(GameParser.decapitalize(value))
                    .as(String.format("wrong decapitalization for '%s'", value))
                    .isEqualTo(decapitalizedValue);
              });
    }
  }
}
