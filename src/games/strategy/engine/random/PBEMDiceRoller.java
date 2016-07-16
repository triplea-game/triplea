package games.strategy.engine.random;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import games.strategy.ui.SwingAction;
import games.strategy.debug.ClientLogger;

/**
 * Its a bit messy, but the threads are a pain to deal with We want to be able
 * to call this from any thread, and have a dialog that doesnt close until the
 * dice roll finishes. If there is an error we wait until we get a good roll
 * before returning.
 */
public class PBEMDiceRoller implements IRandomSource {
  private final String m_gameUUID;
  private final IRemoteDiceServer m_remoteDiceServer;
  private static Frame s_focusWindow;

  /**
   * If the game has multiple frames, allows the ui to
   * set what frame should be the parent of the dice rolling window
   * if set to null, or not set, we try to guess by finding the currently
   * focused window (or a visble window if none are focused).
   */
  public static void setFocusWindow(final Frame w) {
    s_focusWindow = w;
  }

  public PBEMDiceRoller(final IRemoteDiceServer diceServer, final String gameUUID) {
    m_remoteDiceServer = diceServer;
    m_gameUUID = gameUUID;
  }

  /**
   * Do a test roll, leaving the dialog open after the roll is done.
   */
  public void test() {
    // TODO: do a test based on data.getDiceSides()
    final HttpDiceRollerDialog dialog =
        new HttpDiceRollerDialog(getFocusedFrame(), 6, 1, "Test", m_remoteDiceServer, "test-roll");
    dialog.setTest();
    dialog.roll();
  }


  @Override
  public int[] getRandom(final int max, final int count, final String annotation) throws IllegalStateException {
    if (!SwingUtilities.isEventDispatchThread()) {
      final AtomicReference<int[]> result = new AtomicReference<>();
      SwingAction.invokeAndWait(() -> result.set(getRandom(max, count, annotation)));
      return result.get();
    }
    final HttpDiceRollerDialog dialog =
        new HttpDiceRollerDialog(getFocusedFrame(), max, count, annotation, m_remoteDiceServer, m_gameUUID);
    dialog.roll();
    return dialog.getDiceRoll();
  }

  private static Frame getFocusedFrame() {
    if (s_focusWindow != null) {
      return s_focusWindow;
    }
    final Frame[] frames = Frame.getFrames();
    Frame rVal = null;
    for (final Frame frame : frames) {
      // find the window with focus, failing that, get something that is
      // visible
      if (frame.isFocused()) {
        rVal = frame;
      } else if (rVal == null && frame.isVisible()) {
        rVal = frame;
      }
    }
    return rVal;
  }

  @Override
  public int getRandom(final int max, final String annotation) throws IllegalStateException {
    return getRandom(max, 1, annotation)[0];
  }
}


