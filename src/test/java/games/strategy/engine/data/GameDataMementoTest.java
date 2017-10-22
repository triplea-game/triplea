package games.strategy.engine.data;

import static games.strategy.engine.data.Matchers.equalToGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import games.strategy.util.memento.Memento;
import games.strategy.util.memento.MementoExporter;
import games.strategy.util.memento.MementoImportException;
import games.strategy.util.memento.MementoImporter;

public final class GameDataMementoTest {
  private final MementoExporter<GameData> mementoExporter = GameDataMemento.newExporter();

  private final MementoImporter<GameData> mementoImporter = GameDataMemento.newImporter();

  @Test
  public void shouldBeAbleToRoundTripGameData() throws Exception {
    final GameData expected = TestGameDataFactory.newValidGameData();

    final Memento memento = mementoExporter.exportMemento(expected);
    final GameData actual = mementoImporter.importMemento(memento);

    assertThat(actual, is(equalToGameData(expected)));
  }

  @Test
  public void mementoImporter_ShouldThrowExceptionWhenRequiredPropertyIsAbsent() throws Exception {
    final Memento memento = TestGameDataMementoFactory.newMementoWithoutProperty(GameDataMemento.PropertyNames.VERSION);

    assertThat(assertThrows(MementoImportException.class,
        () -> mementoImporter.importMemento(memento)).getMessage(),
        containsString(String.format(
            "missing required property '%s'",
            GameDataMemento.PropertyNames.VERSION)));
  }

  @Test
  public void mementoImporter_ShouldThrowExceptionWhenPropertyValueHasWrongType() throws Exception {
    final Memento memento =
        TestGameDataMementoFactory.newMementoWithProperty(GameDataMemento.PropertyNames.VERSION, "1.2.3.4");

    assertThat(assertThrows(MementoImportException.class, () -> mementoImporter.importMemento(memento)).getMessage(),
        containsString(String.format(
            "property '%s' has wrong type",
            GameDataMemento.PropertyNames.VERSION)));
  }
}
