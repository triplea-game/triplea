package games.strategy.engine.data.gameparser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.util.Tuple;
import org.triplea.util.Version;

final class GameParserTest {

  @Test
  @DisplayName("Verify backward compatibility can parse 1.8 maps")
  void backwardCompatibilityCheck() throws Exception {
    final Path mapFile = getTestMap("v1_8_map__270BC.xml");
    final GameData gameData =
        GameParser.parse(mapFile, new XmlGameElementMapper(), new Version("2.0.0"), false)
            .orElseThrow();
    assertNotNullGameData(gameData);

    verifyLegacyPropertiesAreUpdated(gameData);
  }

  /** Asserts that we loaded a relatively complete looking game data. */
  private void assertNotNullGameData(final GameData gameData) {
    assertThat(gameData.getAttachmentOrderAndValues(), is(notNullValue()));
    assertThat(gameData.getAllianceTracker().getAlliances(), is(notNullValue()));
    assertThat(gameData.getBattleRecordsList(), is(notNullValue()));
    assertThat(gameData.getDelegates(), is(notNullValue()));
    assertThat(gameData.getDiceSides(), is(notNullValue()));
    assertThat(gameData.getGameLoader(), is(notNullValue()));
    assertThat(gameData.getGameName(), is(notNullValue()));
    assertThat(gameData.getHistory().getLastNode(), is(notNullValue()));
    assertThat(gameData.getMap().getTerritories(), is(notNullValue()));
    assertThat(gameData.getPlayerList().getPlayers(), is(notNullValue()));
    assertThat(
        gameData.getProductionFrontierList().getProductionFrontierNames(), is(notNullValue()));
    assertThat(gameData.getProductionRuleList().getProductionRules(), is(notNullValue()));
    assertThat(gameData.getProperties(), is(notNullValue()));
    assertThat(gameData.getRelationshipTracker(), is(notNullValue()));
    assertThat(gameData.getRelationshipTypeList().getAllRelationshipTypes(), is(notNullValue()));
    assertThat(gameData.getRepairFrontierList().getRepairFrontierNames(), is(notNullValue()));
    assertThat(gameData.getResourceList().getResources(), is(notNullValue()));
    assertThat(gameData.getSaveGameFileName(), is(notNullValue()));
    assertThat(gameData.getSequence().getRound(), is(notNullValue()));
    assertThat(gameData.getSequence().getStep(), is(notNullValue()));
    assertThat(gameData.getTechnologyFrontier().getTechs(), is(notNullValue()));
    assertThat(gameData.getTerritoryEffectList(), is(notNullValue()));
    assertThat(gameData.getUnits().getUnits(), is(notNullValue()));
    assertThat(gameData.getUnitTypeList().getAllUnitTypes(), is(notNullValue()));
  }

  /**
   * The test-XML is intentinally loaded with legacy properties and options. Here we assert that
   * those legacy values have been forward-ported to their new, non-legacy values.
   */
  private void verifyLegacyPropertiesAreUpdated(final GameState gameData) {
    assertThat(
        gameData.getProperties().get(Constants.TWO_HIT_BATTLESHIPS_REPAIR_END_OF_TURN), is(true));
    assertThat(
        gameData.getProperties().get(Constants.TWO_HIT_BATTLESHIPS_REPAIR_BEGINNING_OF_TURN),
        is(true));

    final var spartaTerritoryAttachment =
        (TerritoryAttachment)
            gameData
                .getMap()
                .getTerritory("Sparta")
                .getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);

    assertThat(spartaTerritoryAttachment.getVictoryCity(), is(1));
    assertThat(
        spartaTerritoryAttachment.getOriginalOwner().map(GamePlayer::getName).orElse(""),
        is("RomanRepublic"));
    assertThat(spartaTerritoryAttachment.getIsImpassable(), is(true));

    final var romaTerritoryAttachment =
        (TerritoryAttachment)
            gameData
                .getMap()
                .getTerritory("Roma")
                .getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);

    assertThat(romaTerritoryAttachment.getVictoryCity(), is(0));

    final var archerUnitAttachment =
        (UnitAttachment)
            gameData
                .getUnitTypeList()
                .getUnitTypeOrThrow("archer")
                .getAttachment(Constants.UNIT_ATTACHMENT_NAME);

    assertThat(
        "Verify isTwoHitPoint=true is converted to hitPoints = 2",
        archerUnitAttachment.getHitPoints(),
        is(2));
    assertThat(
        "Verify is paratroop is converted", archerUnitAttachment.isAirTransportable(), is(true));
    assertThat(
        "Verify isMechanized is converted", archerUnitAttachment.isLandTransportable(), is(true));

    final var axemanUnitAttachment =
        ((UnitAttachment)
            gameData
                .getUnitTypeList()
                .getUnitTypeOrThrow("axeman")
                .getAttachment(Constants.UNIT_ATTACHMENT_NAME));

    assertThat(
        "Verify isInfantry is converted", axemanUnitAttachment.isLandTransportable(), is(true));

    assertThat(
        ((RulesAttachment)
                gameData
                    .getPlayerList()
                    .getPlayerId("Carthage")
                    .getAttachment("conditionAttachmentAntiRomanVictory8"))
            .getRounds(),
        is(Map.of(1, 1, 2, 2)));
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
                assertThat(
                    String.format("wrong decapitalization for '%s'", value),
                    GameParser.decapitalize(value),
                    is(decapitalizedValue));
              });
    }
  }

  private Path getTestMap(String name) throws Exception {
    return Path.of(GameParserTest.class.getClassLoader().getResource(name).toURI());
  }
}
