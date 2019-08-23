package games.strategy.triplea.ui;

import com.google.common.base.Preconditions;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.KeyStroke;

/**
 * Interface for classes that have key listeners. Key bindings are more 'global' than classic swing
 * key listeners. Key bindings will fire even if the source component does not have focus. Key
 * bindings need to be registered to the JFrame ContentPane to function.
 *
 * <p>To register a {@code KeyStroke} with no modifiers, use:
 *
 * <pre>
 *   KeyStroke.getKeyStroke(KeyEvent.VK_S, 0);
 * </pre>
 *
 * To register a {@code KeyStroke} with modifier, for example "Ctrl+S", use:
 *
 * <pre>
 *   KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
 * </pre>
 */
public interface KeyBindings extends Supplier<Map<KeyStroke, Runnable>> {

  /**
   * Convenience method to create a {@code KeyStroke} without modifiers.
   *
   * @param code A KeyEvent code, should be a constant from the class {@code KeyEvent}
   */
  default KeyStroke fromKeyEventCode(int code) {
    Preconditions.checkArgument(
        !KeyEvent.getKeyText(code).toUpperCase().contains("UNKNOWN"),
        "Be sure to use a constant from 'KeyEvent', unknown key constant: " + code);
    return KeyStroke.getKeyStroke(code, 0);
  }
}
