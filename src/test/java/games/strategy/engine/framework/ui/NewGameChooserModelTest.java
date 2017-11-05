package games.strategy.engine.framework.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import javax.swing.SwingUtilities;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class NewGameChooserModelTest {

  /**
   * Simply create the object to see that we can do that without exception.
   */
  @Test
  public void testCreate() throws Throwable {
    assertThrows(IllegalStateException.class, () -> new NewGameChooserModel(() -> {
    }));

    try {
      SwingUtilities.invokeAndWait(() -> {
        final Runnable doneAction = mock(Runnable.class);
        final NewGameChooserEntry entry = mock(NewGameChooserEntry.class);
        final NewGameChooserModel model = new NewGameChooserModel(doneAction, () -> Collections.singleton(entry));
        assertEquals(entry, model.get(0));
        verify(doneAction).run();
      });
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }
}
