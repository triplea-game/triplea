package games.strategy.engine.framework.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.framework.map.file.system.loader.AvailableGamesList;
import java.net.URI;
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
          when(entry1.getUri()).thenReturn(URI.create("b"));
          when(entry2.getGameName()).thenReturn("a");
          when(entry2.getUri()).thenReturn(URI.create("a"));
          when(entry3.getGameName()).thenReturn("c");
          when(entry3.getUri()).thenReturn(URI.create("c"));
          // use the real compareTo methods so it is sorted correctly
          when(entry1.compareTo(any())).thenCallRealMethod();
          when(entry2.compareTo(any())).thenCallRealMethod();
          when(entry3.compareTo(any())).thenCallRealMethod();

          final GameChooserModel model =
              new GameChooserModel(new AvailableGamesList(Set.of(entry1, entry2, entry3)));
          assertThat(model.get(0), is(entry2));
          assertThat(model.get(1), is(entry1));
          assertThat(model.get(2), is(entry3));
        });
  }
}
