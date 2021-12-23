package games.strategy.engine.framework.lookandfeel;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.swing.SwingComponents;

/**
 * Look and feel listener specific for a JFrame. JFrame will be updated when the L&F is updated,
 * listener is automatically removed on teh frame close event.
 *
 * <pre>
 * <code>
 *   LookAndFeelSwingFrameListener.register(yourSwingFrame);
 * </code>
 * </pre>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class LookAndFeelSwingFrameListener implements Consumer<GameSetting<String>> {
  private final JFrame component;

  /**
   * Creates a look and feel update listener that will update the passed in JFrame component.
   * Listener removal is also handled and is attached to the window closed event.
   */
  public static void register(final JFrame component) {
    final Consumer<GameSetting<String>> listener = new LookAndFeelSwingFrameListener(component);
    ClientSetting.lookAndFeel.addListener(listener);
    SwingComponents.addWindowClosedListener(
        component, () -> ClientSetting.lookAndFeel.removeListener(listener));
  }

  @Override
  public void accept(final GameSetting<String> gameSetting) {
    SwingUtilities.updateComponentTreeUI(component);
  }
}
