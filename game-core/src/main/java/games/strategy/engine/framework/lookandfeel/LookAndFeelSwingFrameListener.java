package games.strategy.engine.framework.lookandfeel;

import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingComponents;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Look and feel listener specific for a JFrame.
 * JFrame will be updated when the L&F is updated, listener is automatically removed on teh frame close event.
 *
 * <pre>
 * <code>
 *   LookAndFeelSwingFrameListener.register(yourSwingFrame);
 * </code>
 * </pre>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class LookAndFeelSwingFrameListener implements Consumer<String> {
  private final JFrame component;

  /**
   * Creates a look and feel update listener that will update the passed in JFrame
   * component. Listener removal is also handled and is attached to the window close event.
   */
  public static void register(final JFrame component) {
    final Consumer<String> listener = new LookAndFeelSwingFrameListener(component);
    ClientSetting.lookAndFeel.addSaveListener(listener);
    SwingComponents.addWindowClosingListener(component,
        () -> ClientSetting.lookAndFeel.removeSaveListener(listener));
  }

  @Override
  public void accept(final String s) {
    SwingUtilities.updateComponentTreeUI(component);
  }
}
