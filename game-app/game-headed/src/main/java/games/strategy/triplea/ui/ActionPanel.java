package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.Dimension;
import java.util.concurrent.CountDownLatch;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.SwingComponents;

/** Abstract superclass for all action panels. */
public abstract class ActionPanel extends JPanel {
  private static final long serialVersionUID = -5954576036704958641L;
  protected final JLabel actionLabel = createIndentedLabel();

  @Getter(AccessLevel.PROTECTED)
  protected final MapPanel map;

  /** Refreshes the action panel. */
  protected final Runnable refresh =
      () -> SwingUtilities.invokeLater(() -> SwingComponents.redraw(this));

  @Getter(AccessLevel.PROTECTED)
  private final GameData data;

  @Getter(AccessLevel.PROTECTED)
  private GamePlayer currentPlayer;

  /** Called when the history panel shows used to disable the panel temporarily. */
  @Setter @Getter private boolean active;

  private CountDownLatch latch;
  private final Object latchLock = new Object();

  public ActionPanel(final GameData data, final MapPanel map) {
    this.data = data;
    this.map = map;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
    setMinimumSize(new Dimension(240, 0));
  }

  /**
   * Creates a label suitable for showing text directly in the action panel. This allows for
   * consistent indentation between different panels without requiring other non-label component to
   * be indented.
   */
  public static JLabel createIndentedLabel() {
    return new JLabelBuilder().border(BorderFactory.createEmptyBorder(0, 5, 0, 0)).build();
  }

  protected static JPanel createButtonsPanel(JButton... buttons) {
    JPanel buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
    for (JButton button : buttons) {
      buttonsPanel.add(button);
    }
    return buttonsPanel;
  }

  /**
   * Wait for another thread to call release. If the thread is interrupted, we will return silently.
   *
   * <p>A memory barrier will be crossed both on entering and before exiting this method.
   *
   * <p>This method will return in the event of the game shutting down.
   */
  protected void waitForRelease() {
    if (Thread.currentThread().isInterrupted()) {
      release();
      return;
    }
    synchronized (latchLock) {
      if (latch != null) {
        throw new IllegalStateException("Latch not null");
      }
      latch = new CountDownLatch(1);
      map.getUiContext().addShutdownLatch(latch);
    }
    try {
      latch.await();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      release();
    }
    // cross a memory barrier
    synchronized (latchLock) {
    }
  }

  /**
   * Release the latch acquired by waitOnNewLatch().
   *
   * <p>This method will crossed on entering this method.
   */
  protected void release() {
    synchronized (latchLock) {
      // not set up yet
      // this is ok as we set up in one thread and wait in another
      // if the release happens too early, the user will be able to press done again
      if (latch == null) {
        return;
      }
      map.getUiContext().removeShutdownLatch(latch);
      latch.countDown();
      latch = null;
    }
  }

  public void display(final GamePlayer player) {
    currentPlayer = player;
    setActive(true);
  }

  protected JButton createDoneButton() {
    return new JButtonBuilder()
        .title("Done")
        .actionListener(this::performDone)
        .toolTip(ActionButtonsPanel.DONE_BUTTON_TOOLTIP)
        .build();
  }

  /**
   * Executes the appropriate action when a user clicks the 'done' button. Typically this will be to
   * end the current turn phase. If the turn phase ends in some other way than a button click, then
   * this should be a no-op. For example, battle phase ends when all battles have been fought and
   * not when the user clicks done (there is no done button during the battle phase).
   */
  public abstract void performDone();
}
