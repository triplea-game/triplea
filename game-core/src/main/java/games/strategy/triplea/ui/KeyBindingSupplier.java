package games.strategy.triplea.ui;

import com.google.common.base.Preconditions;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.KeyStroke;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

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
public interface KeyBindingSupplier extends Supplier<Map<KeyStroke, Runnable>> {

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  enum ModifierKey {
    NONE(0),
    SHIFT(InputEvent.SHIFT_DOWN_MASK),
    CONTROL(InputEvent.CTRL_DOWN_MASK);

    private final int modifierMask;
  }

  /**
   * Convenience method to create a {@code KeyStroke} without modifiers.
   *
   * @param code A KeyEvent code, should be a constant from the class {@code KeyEvent}
   */
  static KeyStroke fromKeyEventCode(final int code) {
    return fromKeyEventCode(code, ModifierKey.NONE);
  }

  static KeyStroke fromKeyEventCode(final int code, final ModifierKey modifierKey) {
    Preconditions.checkArgument(
        !KeyEvent.getKeyText(code).toUpperCase().contains("UNKNOWN"),
        "Be sure to use a constant from 'KeyEvent', unknown key constant: " + code);
    return KeyStroke.getKeyStroke(code, modifierKey.modifierMask);
  }
}
