package games.strategy.engine.framework.startup.ui.editors;

import java.awt.Color;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Helper Base class for Editors, that provides a basic collection of useful operations.
 *
 * <p>This class used to have a legitimate reason to be a superclass of editors, but making all
 * methods in this class static, and converting it to a utility class is probably a good thing to
 * do.
 */
abstract class EditorPanel extends JPanel {
  private static final long serialVersionUID = 8156959717037201321L;
  private final Color labelColor = new JLabel().getForeground();

  EditorPanel() {
    super(new GridBagLayout());
  }

  /**
   * Turns the label red to indicate an error if valid is true.
   *
   * @param valid The parameter that decides if an error should be indicated.
   * @param label The Label whose color should be changed.
   * @return The value of valid
   */
  boolean setLabelValid(final boolean valid, final JLabel label) {
    label.setForeground(valid ? labelColor : Color.RED);
    return valid;
  }
}
