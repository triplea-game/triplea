package games.strategy.engine.framework.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.framework.map.file.system.loader.AvailableGamesList;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameChooserModelTest {
  @Test
  void testOrdering() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final DefaultGameChooserEntry entry1 = mock(DefaultGameChooserEntry.class);
          final DefaultGameChooserEntry entry2 = mock(DefaultGameChooserEntry.class);
          final DefaultGameChooserEntry entry3 = mock(DefaultGameChooserEntry.class);

          when(entry1.getGameName()).thenReturn("b");
          when(entry2.getGameName()).thenReturn("a");
          when(entry3.getGameName()).thenReturn("c");

          final GameChooserModel model =
              new GameChooserModel(new AvailableGamesList(Set.of(entry1, entry2, entry3)));
          assertEquals("a", model.get(0));
          assertEquals("b", model.get(1));
          assertEquals("c", model.get(2));
        });
  }
}
