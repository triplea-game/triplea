package org.triplea.swing.key.binding;

import java.awt.KeyboardFocusManager;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.text.JTextComponent;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;

/**
 * Supports adding key bindings. Reference:
 * https://docs.oracle.com/javase/tutorial/uiswing/misc/keybinding.html
 *
 * <p>Key bindings added through this class will "always" be active regardless if the component is
 * currently focused or not. Single key key bindings will not be active when focus is inside of a
 * text component. This is to avoid, for example, a user typing a chat message and unintentionally
 * firing key bindings.
 */
@UtilityClass
public class SwingKeyBinding {
  public static void addKeyBinding(
      final JDialog frame, final KeyCombination keyCombination, final Runnable action) {
    addKeyMapping(frame.getRootPane(), keyCombination, action);
  }

  public static void addKeyBinding(
      final JFrame frame, final KeyCombination keyCombination, final Runnable action) {
    addKeyMapping(frame.getRootPane(), keyCombination, action);
  }

  public static void addKeyBinding(
      final JDialog frame, final KeyCode keyCode, final Runnable action) {
    addKeyMapping(frame.getRootPane(), keyCode, action);
  }

  public static void addKeyBinding(
      final JFrame frame, final KeyCode keyCode, final Runnable action) {
    addKeyMapping(frame.getRootPane(), keyCode, action);
  }

  public static void addKeyBinding(
      final JComponent component, final KeyCode keyCode, final Runnable action) {
    addKeyMapping(component, keyCode, action);
  }

  /**
   * Adds two keybindings to a given key, both active when "ctrl" and "meta" are held down.
   *
   * @param key The key to be bind in combination with "ctrl" being held down, or "meta" being held
   *     down.
   * @param action The action to execute when the keybinding is activated.
   */
  public static void addKeyListenerWithMetaAndCtrlMasks(
      final JFrame component, final KeyCode key, final Runnable action) {

    addKeyMapping(
        (JComponent) component.getContentPane(),
        KeyCombination.of(key, ButtonDownMask.CTRL),
        action);
    addKeyMapping(
        (JComponent) component.getContentPane(),
        KeyCombination.of(key, ButtonDownMask.META),
        action);
  }

  private static void addKeyMapping(
      final JComponent component, final KeyCode keyCode, final Runnable action) {
    addKeyMapping(component, KeyCombination.of(keyCode, ButtonDownMask.NONE), action);
  }

  private static void addKeyMapping(
      final JComponent component, final KeyCombination keyCombination, final Runnable action) {
    final String keyBindingIdentifier = UUID.randomUUID().toString();

    final AtomicBoolean enabled = new AtomicBoolean(true);

    if (keyCombination.getButtonDownMask() == ButtonDownMask.NONE
        || keyCombination.getButtonDownMask() == ButtonDownMask.SHIFT) {
      // Disable single-key keybindings or 'shift+key" keybindings if focus is on a text component.
      // We do not want to fire keybindings while user is typing (chatting).

      KeyboardFocusManager.getCurrentKeyboardFocusManager()
          .addPropertyChangeListener(
              "focusOwner",
              evt ->
                  enabled.set(
                      evt.getNewValue() == null
                          || !JTextComponent.class.isAssignableFrom(evt.getNewValue().getClass())));
    }

    component
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(keyCombination.toKeyStroke(), keyBindingIdentifier);
    component
        .getActionMap()
        .put(
            keyBindingIdentifier,
            SwingAction.of(
                e -> {
                  if (enabled.get()) {
                    action.run();
                  }
                }));
  }
}
