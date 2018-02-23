package games.strategy.engine.random;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Window;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import games.strategy.triplea.UrlConstants;
import games.strategy.ui.SwingAction;
import games.strategy.ui.Util;
import games.strategy.util.Interruptibles;

/**
 * Its a bit messy, but the threads are a pain to deal with We want to be able
 * to call this from any thread, and have a dialog that doesnt close until the
 * dice roll finishes. If there is an error we wait until we get a good roll
 * before returning.
 */
public class PbemDiceRoller implements IRandomSource {
  private final String gameUuid;
  private final IRemoteDiceServer remoteDiceServer;
  private static Frame focusWindow;

  /**
   * If the game has multiple frames, allows the ui to
   * set what frame should be the parent of the dice rolling window
   * if set to null, or not set, we try to guess by finding the currently
   * focused window (or a visble window if none are focused).
   */
  public static void setFocusWindow(final Frame w) {
    focusWindow = w;
  }

  public PbemDiceRoller(final IRemoteDiceServer diceServer, final String gameUuid) {
    remoteDiceServer = diceServer;
    this.gameUuid = gameUuid;
  }

  /**
   * Do a test roll, leaving the dialog open after the roll is done.
   */
  public void test() {
    // TODO: do a test based on data.getDiceSides()
    final HttpDiceRollerDialog dialog =
        new HttpDiceRollerDialog(getFocusedFrame(), 6, 1, "Test", remoteDiceServer, "test-roll");
    dialog.setTest();
    dialog.roll();
  }

  @Override
  public int[] getRandom(final int max, final int count, final String annotation) throws IllegalStateException {
    final Supplier<int[]> action = () -> {
      final HttpDiceRollerDialog dialog =
          new HttpDiceRollerDialog(getFocusedFrame(), max, count, annotation, remoteDiceServer, gameUuid);
      dialog.roll();
      return dialog.getDiceRoll();
    };
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWait(action)).result
        .orElseGet(() -> new int[0]);
  }

  @Override
  public int getRandom(final int max, final String annotation) throws IllegalStateException {
    return getRandom(max, 1, annotation)[0];
  }

  private static Frame getFocusedFrame() {
    if (focusWindow != null) {
      return focusWindow;
    }
    final Frame[] frames = Frame.getFrames();
    Frame focusedFrame = null;
    for (final Frame frame : frames) {
      // find the window with focus, failing that, get something that is
      // visible
      if (frame.isFocused() || ((focusedFrame == null) && frame.isVisible())) {
        focusedFrame = frame;
      }
    }
    return focusedFrame;
  }

  /**
   * The dialog that will show while the dice are rolling.
   */
  private static final class HttpDiceRollerDialog extends JDialog {
    private static final long serialVersionUID = -4802403913826489223L;
    private final JButton exitButton = new JButton("Exit");
    private final JButton reRollButton = new JButton("Roll Again");
    private final JButton okButton = new JButton("OK");
    private final JTextArea textArea = new JTextArea();
    private int[] diceRoll;
    private final int count;
    private final int sides;
    private final String subjectMessage;
    private final String gameId;
    private final IRemoteDiceServer diceServer;
    private final String gameUuid;
    private boolean test = false;
    private final JPanel buttons = new JPanel();
    private Window owner;

    /**
     * @param owner
     *        owner frame.
     * @param sides
     *        the number of sides on the dice
     * @param count
     *        the number of dice rolled
     * @param subjectMessage
     *        the subject for the email the dice roller will send (if it sends emails)
     * @param diceServer
     *        the dice server implementation
     * @param gameUuid
     *        the TripleA game UUID or null
     */
    HttpDiceRollerDialog(final Frame owner, final int sides, final int count, final String subjectMessage,
        final IRemoteDiceServer diceServer, final String gameUuid) {
      super(owner, "Dice roller", true);
      this.owner = owner;
      this.sides = sides;
      this.count = count;
      this.subjectMessage = subjectMessage;
      gameId = (diceServer.getGameId() == null) ? "" : diceServer.getGameId();
      this.diceServer = diceServer;
      this.gameUuid = gameUuid;
      setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      exitButton.addActionListener(e -> System.exit(-1));
      exitButton.setEnabled(false);
      reRollButton.addActionListener(e -> rollInternal());
      okButton.addActionListener(e -> closeAndReturn());
      reRollButton.setEnabled(false);
      getContentPane().setLayout(new BorderLayout());
      buttons.add(exitButton);
      buttons.add(reRollButton);
      getContentPane().add(buttons, BorderLayout.SOUTH);
      getContentPane().add(new JScrollPane(textArea));
      textArea.setEditable(false);
      setSize(400, 300);
      Util.center(this);
    }

    /**
     * There are three differences when we are testing, 1 dont close the window
     * when we are done 2 remove the exit button 3 add a close button.
     */
    void setTest() {
      test = true;
      buttons.removeAll();
      buttons.add(okButton);
      buttons.add(reRollButton);
    }

    void appendText(final String text) {
      textArea.setText(textArea.getText() + text);
    }

    void notifyError() {
      SwingUtilities.invokeLater(() -> {
        exitButton.setEnabled(true);
        reRollButton.setEnabled(true);
      });
    }

    int[] getDiceRoll() {
      return diceRoll;
    }

    // should only be called if we are not visible
    // should be called from the event thread
    // wont return until the roll is done.
    void roll() throws IllegalStateException {
      rollInternal();
      setVisible(true);
    }

    private void rollInternal() throws IllegalStateException {
      if (!SwingUtilities.isEventDispatchThread()) {
        throw new IllegalStateException("Wrong thread");
      }
      reRollButton.setEnabled(false);
      exitButton.setEnabled(false);
      new Thread(this::rollInSeperateThread, "Triplea, roll in seperate thread").start();
    }

    private void closeAndReturn() {
      SwingUtilities.invokeLater(() -> {
        setVisible(false);
        owner.toFront();
        owner = null;
        dispose();
      });
    }

    /**
     * should be called from a thread other than the event thread after we are
     * open (or at least in the process of opening) will close the window and
     * notify any waiting threads when completed successfully.
     * Before contacting Irony Dice Server, check if email has a reasonable
     * valid syntax.
     */
    private void rollInSeperateThread() throws IllegalStateException {
      if (SwingUtilities.isEventDispatchThread()) {
        throw new IllegalStateException("Wrong thread");
      }
      while (!isVisible()) {
        Thread.yield();
      }
      appendText(subjectMessage + "\n");
      appendText("Contacting  " + diceServer.getDisplayName() + "\n");
      String text = null;
      try {
        text = diceServer.postRequest(sides, count, subjectMessage, gameId, gameUuid);
        if (text.length() == 0) {
          appendText("Nothing could be read from dice server\n");
          appendText("Please check your firewall settings");
          notifyError();
        }
        if (!test) {
          appendText("Contacted :" + text + "\n");
        }
        diceRoll = diceServer.getDice(text, count);
        appendText("Success!");
        if (!test) {
          closeAndReturn();
        }
      } catch (final SocketException ex) { // an error in networking
        appendText("Connection failure:" + ex.getMessage() + "\n"
            + "Please ensure your Internet connection is working, and try again.");
        notifyError();
      } catch (final InvocationTargetException e) {
        appendText("\nError:" + e.getMessage() + "\n\n");
        appendText("Text from dice server:\n" + text + "\n");
        notifyError();
      } catch (final IOException ex) {
        appendText("An error has occured!\n");
        appendText("Possible reasons the error could have happened:\n");
        appendText("  1: An invalid e-mail address\n");
        appendText("  2: Firewall could be blocking TripleA from connecting to the Dice Server\n");
        appendText("  3: The e-mail address does not exist\n");
        appendText("  4: An unknown error, please see the error console and consult the forums for help\n");
        appendText("     Visit " + UrlConstants.TRIPLEA_FORUM + "  for extra help\n");
        if (text != null) {
          appendText("Text from dice server:\n" + text + "\n");
        }
        final StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        appendText(writer.toString());
        notifyError();
      }
    }
  }
}
