package games.strategy.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class SwingActionTest {

  @Test
  public void testActionOf(@Mock final ActionEvent event, @Mock final ActionListener listener) {
    final Action action = SwingAction.of("Name1234", listener);
    assertEquals("Name1234", action.getValue(Action.NAME));
    action.actionPerformed(event);
    verify(listener).actionPerformed(event);
  }

  @Test
  public void testKeyReleaseListener(@Mock final Consumer<KeyEvent> listener) {
    final KeyEvent event = mock(KeyEvent.class);
    final KeyListener action = SwingAction.keyReleaseListener(listener);
    action.keyReleased(event);
    verify(listener).accept(event);
  }

  @Test
  public void testInvokeAndWait(@Mock final Runnable action) throws Exception {
    SwingAction.invokeAndWait(action);
    verify(action).run();
    SwingUtilities.invokeAndWait(() -> {
      SwingAction.invokeAndWait(action);
      verify(action, times(2)).run();
    });
  }
}
