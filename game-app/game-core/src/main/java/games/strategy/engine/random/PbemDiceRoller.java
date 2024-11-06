package games.strategy.engine.random;

import com.google.common.base.Preconditions;
import games.strategy.engine.random.IRemoteDiceServer.DiceServerException;
import games.strategy.triplea.UrlConstants;
import games.strategy.ui.Util;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.triplea.java.Interruptibles;
import org.triplea.java.ThreadRunner;
import org.triplea.swing.SwingAction;
import org.triplea.util.ExitStatus;

/**
 * It's a bit messy, but the threads are a pain to deal with. We want to be able to call this from
 * any thread, and have a dialog that doesn't close until the dice roll finishes. If there is an
 * error we wait until we get a good roll before returning.
 */
public class PbemDiceRoller implements IRandomSource {
  private final IRemoteDiceServer remoteDiceServer;

  public PbemDiceRoller(final IRemoteDiceServer diceServer) {
    remoteDiceServer = diceServer;
  }

  /** Do a test roll, leaving the dialog open after the roll is done. */
  public void test() {
    // TODO: do a test based on data.getDiceSides()
    final HttpDiceRollerDialog dialog =
        new HttpDiceRollerDialog(getFocusedFrame(), 6, 1, "Test", remoteDiceServer);
    dialog.setTest();
    dialog.roll();
  }

  @Override
  public int[] getRandom(final int max, final int count, final String annotation) {
    final Supplier<int[]> action =
        () -> {
          final HttpDiceRollerDialog dialog =
              new HttpDiceRollerDialog(getFocusedFrame(), max, count, annotation, remoteDiceServer);
          dialog.roll();
          return dialog.getDiceRoll();
        };
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action))
        .result
        .orElseGet(() -> new int[0]);
  }

  @Override
  public int getRandom(final int max, final String annotation) {
    return getRandom(max, 1, annotation)[0];
  }

  private static Frame getFocusedFrame() {
    final Frame[] frames = Frame.getFrames();
    Frame focusedFrame = null;
    for (final Frame frame : frames) {
      // find the window with focus, failing that, get something that is visible
      if (frame.isFocused() || (focusedFrame == null && frame.isVisible())) {
        focusedFrame = frame;
      }
    }
    return focusedFrame;
  }

  /** The dialog that will show while the dice are rolling. */
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
    private boolean test = false;
    private final JPanel buttons = new JPanel();
    private Window owner;

    /**
     * Initializes a new instance of the HttpDiceRollerDialog class.
     *
     * @param owner owner frame.
     * @param sides the number of sides on the dice
     * @param count the number of dice rolled
     * @param subjectMessage the subject for the email the dice roller will send (if it sends
     *     emails)
     * @param diceServer the dice server implementation
     */
    HttpDiceRollerDialog(
        final Frame owner,
        final int sides,
        final int count,
        final String subjectMessage,
        final IRemoteDiceServer diceServer) {
      super(owner, "Dice roller", true);
      this.owner = owner;
      this.sides = sides;
      this.count = count;
      this.subjectMessage = subjectMessage;
      gameId = diceServer.getGameId() == null ? "" : diceServer.getGameId();
      this.diceServer = diceServer;
      setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      exitButton.addActionListener(e -> ExitStatus.FAILURE.exit());
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
      computeCenter().ifPresent(this::setLocation);
    }

    /** Computes the center of the specified window. */
    private Optional<Point> computeCenter() {
      final Dimension screenSize = Util.getScreenSize(this);
      final int screenWidth = screenSize.width;
      final int screenHeight = screenSize.height;
      final int windowWidth = getWidth();
      final int windowHeight = getHeight();
      if (windowHeight > screenHeight) {
        return Optional.empty();
      }
      if (windowWidth > screenWidth) {
        return Optional.empty();
      }
      final int x = (screenWidth - windowWidth) / 2;
      final int y = (screenHeight - windowHeight) / 2;
      return Optional.of(new Point(x, y));
    }

    /**
     * There are three differences when we are testing, 1 dont close the window when we are done 2
     * remove the exit button 3 add a close button.
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
      SwingUtilities.invokeLater(
          () -> {
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
    void roll() {
      rollInternal();
      setVisible(true);
    }

    private void rollInternal() {
      Util.ensureOnEventDispatchThread();
      reRollButton.setEnabled(false);
      exitButton.setEnabled(false);
      ThreadRunner.runInNewThread(this::rollInSeparateThread);
    }

    private void closeAndReturn() {
      SwingUtilities.invokeLater(
          () -> {
            setVisible(false);
            owner.toFront();
            owner = null;
            dispose();
          });
    }

    /**
     * Should be called from a thread other than the event thread after we are open (or at least in
     * the process of opening) will close the window and notify any waiting threads when completed
     * successfully. Before contacting Irony Dice Server, check if email has a reasonable valid
     * syntax.
     */
    private void rollInSeparateThread() {
      Preconditions.checkState(!SwingUtilities.isEventDispatchThread());

      waitForWindowToBecomeVisible();

      appendText(subjectMessage + "\n");
      appendText("Contacting " + diceServer.getDisplayName() + "\n");
      try {
        final String text = diceServer.postRequest(sides, count, subjectMessage, gameId);
        if (text.isEmpty()) {
          appendText("Nothing could be read from dice server\n");
          appendText("Please check your firewall settings");
          notifyError();
        }
        if (!test) {
          appendText("Contacted: " + text + "\n");
        }
        diceRoll = diceServer.getDice(text, count);
        appendText("Success!");
        if (!test) {
          closeAndReturn();
        }
      } catch (final IOException e) {
        appendText("Connection failure:\n");
        final StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        appendText(writer.toString());
        appendText("\nPlease ensure your Internet connection is working, and try again.");
        appendText("Visit " + UrlConstants.TRIPLEA_FORUM + " for extra help\n");
        notifyError();
      } catch (final DiceServerException e) {
        appendText("The dice server reported an issue with your request!\n");
        appendText("Error Message:\n");
        appendText(e.getMessage());
        appendText("\n");
        notifyError();
      } catch (final RuntimeException e) {
        // Close screen in case an unexpected error occurs
        // and rethrow exception for default error handling.
        closeAndReturn();
        throw e;
      }
    }

    private void waitForWindowToBecomeVisible() {
      final BooleanSupplier isVisible =
          () ->
              Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(this::isVisible))
                  .result
                  .orElse(false);
      while (!isVisible.getAsBoolean()) {
        Interruptibles.sleep(10L);
      }
    }
  }
}
