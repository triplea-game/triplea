package games.strategy.engine.data.gameparser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

import games.strategy.engine.data.GameData;
import java.net.URI;
import java.util.List;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.util.Tuple;

final class GameParserTest {

  @Test
  @DisplayName("Verify backward compatibility can parse 1.8 maps")
  void backwardCompatibilityCheck() throws Exception {
    final URI mapUri =
        GameParserTest.class.getClassLoader().getResource("v1_8_map__270BC.xml").toURI();

    final GameData gameData = GameParser.parse(mapUri, new XmlGameElementMapper()).orElseThrow();

    assertThat(gameData.getAttachmentOrderAndValues(), Is.is(notNullValue()));
    assertThat(gameData.getAllianceTracker().getAlliances(), Is.is(notNullValue()));
    assertThat(gameData.getBattleRecordsList(), Is.is(notNullValue()));
    assertThat(gameData.getDelegates(), Is.is(notNullValue()));
    assertThat(gameData.getDiceSides(), Is.is(notNullValue()));
    assertThat(gameData.getGameLoader(), Is.is(notNullValue()));
    assertThat(gameData.getGameName(), Is.is(notNullValue()));
    assertThat(gameData.getGameVersion(), Is.is(notNullValue()));
    assertThat(gameData.getHistory().getActivePlayer(), Is.is(notNullValue()));
    assertThat(gameData.getHistory().getLastNode(), Is.is(notNullValue()));
    assertThat(gameData.getMap().getTerritories(), Is.is(notNullValue()));
    assertThat(gameData.getPlayerList().getPlayers(), Is.is(notNullValue()));
    assertThat(
        gameData.getProductionFrontierList().getProductionFrontierNames(), Is.is(notNullValue()));
    assertThat(gameData.getProductionRuleList().getProductionRules(), Is.is(notNullValue()));
    assertThat(gameData.getProperties(), Is.is(notNullValue()));
    assertThat(gameData.getRelationshipTracker(), Is.is(notNullValue()));
    assertThat(gameData.getRelationshipTypeList().getAllRelationshipTypes(), Is.is(notNullValue()));
    assertThat(gameData.getRepairFrontierList().getRepairFrontierNames(), Is.is(notNullValue()));
    assertThat(gameData.getResourceList().getResources(), Is.is(notNullValue()));
    assertThat(gameData.getSaveGameFileName(), Is.is(notNullValue()));
    assertThat(gameData.getSequence().getRound(), Is.is(notNullValue()));
    assertThat(gameData.getSequence().getStep(), Is.is(notNullValue()));
    assertThat(gameData.getTechnologyFrontier().getTechs(), Is.is(notNullValue()));
    assertThat(gameData.getTerritoryEffectList(), Is.is(notNullValue()));
    assertThat(gameData.getUnits().getUnits(), Is.is(notNullValue()));
    assertThat(gameData.getUnitTypeList().getAllUnitTypes(), Is.is(notNullValue()));
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
}
