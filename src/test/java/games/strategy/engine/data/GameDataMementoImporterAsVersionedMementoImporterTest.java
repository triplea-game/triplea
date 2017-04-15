package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.data.Matchers.equalToGameData;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableMap;

import games.strategy.util.memento.AbstractVersionedMementoImporterTestCase;
import games.strategy.util.memento.Memento;
import games.strategy.util.memento.MementoImporter;
import games.strategy.util.memento.PropertyBagMemento;

public final class GameDataMementoImporterAsVersionedMementoImporterTest
    extends AbstractVersionedMementoImporterTestCase<GameData> {
  @Override
  protected void assertOriginatorEquals(final GameData expected, final GameData actual) {
    checkNotNull(expected);
    checkNotNull(actual);

    assertThat(actual, is(equalToGameData(expected)));
  }

  @Override
  protected MementoImporter<GameData> createMementoImporter() {
    return GameDataMemento.newImporter();
  }

  @Override
  protected List<SupportedVersion<GameData>> getSupportedVersions() {
    return Arrays.asList(v1());
  }

  private static SupportedVersion<GameData> v1() {
    final GameData expected = TestGameDataFactory.newValidGameData();
    final Memento memento = new PropertyBagMemento(GameDataMemento.SCHEMA_ID, 1L, ImmutableMap.<String, Object>builder()
        .put("name", expected.getGameName())
        .put("version", expected.getGameVersion())
        // TODO: add remaining properties
        .build());
    return new SupportedVersion<>(expected, memento);
  }
}
