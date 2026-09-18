package games.strategy.engine.data;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import org.triplea.io.IoUtils;

class SerializationTest {
  private final GameData gameDataSource = TestMapGameData.TEST.getGameData();
  private final GameState gameDataSink = TestMapGameData.TEST.getGameData();

  private Object serialize(final Object anObject) throws Exception {
    final byte[] bytes =
        IoUtils.writeToMemory(
            os -> {
              try (ObjectOutputStream output = new GameObjectOutputStream(os)) {
                output.writeObject(anObject);
              }
            });
    return IoUtils.readFromMemory(
        bytes,
        is -> {
          try (ObjectInputStream input =
              new GameObjectInputStream(new GameObjectStreamFactory(gameDataSource), is)) {
            return input.readObject();
          } catch (final ClassNotFoundException e) {
            throw new IOException(e);
          }
        });
  }

  @Test
  void testWritePlayerId() throws Exception {
    final GamePlayer gamePlayer = gameDataSource.getPlayerList().getPlayerId("chretian");
    final GamePlayer readId = (GamePlayer) serialize(gamePlayer);
    final GamePlayer localId = gameDataSink.getPlayerList().getPlayerId("chretian");
    assertThat(localId).isNotSameAs(readId);
  }

  @Test
  void testWriteUnitType() throws Exception {
    final Object orig =
        gameDataSource.getUnitTypeList().getUnitTypeOrThrow(Constants.UNIT_TYPE_INF);
    final Object read = serialize(orig);
    final Object local = gameDataSink.getUnitTypeList().getUnitTypeOrThrow(Constants.UNIT_TYPE_INF);
    assertThat(local).isNotSameAs(read);
  }

  @Test
  void testWriteTerritory() throws Exception {
    final Object orig = gameDataSource.getMap().getTerritoryOrNull("canada");
    final Object read = serialize(orig);
    final Object local = gameDataSink.getMap().getTerritoryOrNull("canada");
    assertThat(local).isNotSameAs(read);
  }

  @Test
  void testWriteProductionRule() throws Exception {
    final Object orig = gameDataSource.getProductionRuleList().getProductionRule("infForSilver");
    final Object read = serialize(orig);
    final Object local = gameDataSink.getProductionRuleList().getProductionRule("infForSilver");
    assertThat(local).isNotSameAs(read);
  }
}
