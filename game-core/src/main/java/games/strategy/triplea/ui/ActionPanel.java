package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.Dimension;
import java.util.concurrent.CountDownLatch;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/** Abstract superclass for all action panels. */
public abstract class ActionPanel extends JPanel {
  private static final long serialVersionUID = -5954576036704958641L;

  @Getter(AccessLevel.PROTECTED)
  protected final MapPanel map;
  /** Refreshes the action panel. */
  protected final Runnable refresh =
      () ->
          SwingUtilities.invokeLater(
              () -> {
                revalidate();
                repaint();
              });

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
    setBorder(new EmptyBorder(5, 5, 0, 0));
    setMinimumSize(new Dimension(240, 0));
  }

  protected final boolean isWW2V2() {
    return Properties.getWW2V2(data);
  }

  protected final boolean isWW2V3TechModel() {
    return Properties.getWW2V3TechModel(data);
  }

  protected final boolean isRestrictedPurchase() {
    return Properties.getPlacementRestrictedByFactory(data);
  }

  protected final boolean isSelectableTechRoll() {
    return Properties.getSelectableTechRoll(data);
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

  /**
   * Executes the appropriate action when a user clicks the 'done' button. Typically this will be to
   * end the current turn phase. If the turn phase ends in some other way than a button click, then
   * this should be a no-op. For example, battle phase ends when all battles have been fought and
   * not when the user clicks done (there is no done button during the battle phase).
   */
  public abstract void performDone();
}
