package games.strategy.engine.framework.startup.ui.pbem;

import java.awt.Color;
import javax.swing.JLabel;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingComponents;

/**
 * Helper Base class for Editors, that provides a basic collection of useful operations.
 *
 * <p>This class used to have a legitimate reason to be a superclass of editors, but making all
 * methods in this class static, and converting it to a utility class is probably a good thing to
 * do.
 */
@UtilityClass
class EditorPanel {

  /**
   * Turns the label red to indicate an error if valid is true.
   *
   * @param valid The parameter that decides if an error should be indicated.
   * @param label The Label whose color should be changed.
   * @return The value of valid
   */
  static boolean setLabelValid(final boolean valid, final JLabel label) {
    label.setForeground(valid ? SwingComponents.getDefaultLabelColor() : Color.RED);
    return valid;
  }
}
