package games.strategy.engine.framework.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
          when(entry1.compareTo(entry2)).thenReturn(-1);
          when(entry2.compareTo(entry3)).thenReturn(-1);
          when(entry3.compareTo(entry2)).thenReturn(1);
          when(entry2.compareTo(entry1)).thenReturn(1);
          when(entry1.compareTo(entry3)).thenReturn(-1);
          when(entry3.compareTo(entry1)).thenReturn(1);
          final GameChooserModel model = new GameChooserModel(Set.of(entry1, entry2, entry3));
          assertEquals(entry1, model.get(0));
          assertEquals(entry2, model.get(1));
          assertEquals(entry3, model.get(2));
        });
  }
}
