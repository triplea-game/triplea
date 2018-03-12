package tools.util;

import static com.google.common.base.Preconditions.checkState;

import javax.swing.SwingUtilities;

import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.triplea.settings.ClientSetting;

/**
 * Provides methods for support tools when run as standalone applications.
 */
public final class ToolApplication {
  private ToolApplication() {}

  /**
   * Performs initialization required by all map making tool applications.
   *
   * @throws IllegalStateException If not invoked on the EDT.
   */
  public static void initialize() {
    checkState(SwingUtilities.isEventDispatchThread());

    ClientSetting.initialize();
    LookAndFeel.setupLookAndFeel();
  }
}
