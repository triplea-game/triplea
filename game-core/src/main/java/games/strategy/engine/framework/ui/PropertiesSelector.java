package games.strategy.engine.framework.ui;

import java.util.List;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUi;
import org.triplea.swing.SwingAction;
import games.strategy.util.Interruptibles;

/**
 * Wrapper for properties selection window.
 */
public final class PropertiesSelector {
  private PropertiesSelector() {}

  /**
   * Displays a property selection window.
   *
   * @param parent parent component
   * @param properties properties that will get displayed
   * @param buttonOptions button options. They will be displayed in a row on the bottom
   * @return pressed button
   */
  public static Object getButton(final JComponent parent, final String title,
      final List<? extends IEditableProperty<?>> properties, final Object... buttonOptions) {
    final Supplier<Object> action = () -> showDialog(parent, title, properties, buttonOptions);
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action)).result
        .orElse(JOptionPane.UNINITIALIZED_VALUE);
  }

  private static Object showDialog(final JComponent parent, final String title,
      final List<? extends IEditableProperty<?>> properties, final Object... buttonOptions) {
    final PropertiesUi panel = new PropertiesUi(properties, true);
    final JScrollPane scroll = new JScrollPane(panel);
    scroll.setBorder(null);
    scroll.getViewport().setBorder(null);
    final JOptionPane pane = new JOptionPane(scroll, JOptionPane.PLAIN_MESSAGE);
    pane.setOptions(buttonOptions);
    final JDialog window = pane.createDialog(JOptionPane.getFrameForComponent(parent), title);
    window.setVisible(true);
    return pane.getValue();
  }
}
