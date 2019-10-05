package org.triplea.swing;

import com.google.common.base.Preconditions;
import java.awt.Toolkit;
import java.util.Optional;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import org.triplea.java.ArgChecker;

/**
 * Builder to creates a menu item with title and mnemonic key.
 *
 * <p>Example usage:. <code><pre>
 *   JMenuItem menuItem = new JMenuItemBuilder("Menu Item Text", 'M')
 *     .actionListener(() -> menuItemAction())
 *     .build();
 * </pre></code>
 */
public class JMenuItemBuilder {
  private final String title;
  private final KeyCode mnemonic;
  private Runnable actionListener;
  private KeyCode acceleratorKey;

  public JMenuItemBuilder(final String title, final KeyCode mnemonic) {
    ArgChecker.checkNotEmpty(title);
    this.title = title;
    this.mnemonic = mnemonic;
  }

  /** Constructs a Swing JMenuItem using current builder values. */
  public JMenuItem build() {
    Preconditions.checkNotNull(actionListener);

    final JMenuItem menuItem = new JMenuItem(title);
    menuItem.setMnemonic(mnemonic.getKeyEvent());
    menuItem.addActionListener(e -> actionListener.run());
    Optional.ofNullable(acceleratorKey)
        .ifPresent(
            accelerator ->
                menuItem.setAccelerator(
                    KeyStroke.getKeyStroke(
                        accelerator.getKeyEvent(),
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx())));
    return menuItem;
  }

  /**
   * Sets the action that will be executed when this menu item is clicked (menu items may only have
   * one action).
   *
   * @param actionListener The action to be executed when the menu item is clicked.
   */
  public JMenuItemBuilder actionListener(final Runnable actionListener) {
    this.actionListener = actionListener;
    return this;
  }

  /**
   * Accelerator key is essentially a hotkey that invokes the menu item action directly without
   * needing to open up the menus. Most systems use 'ctrl' to bind to the accelerator, for example
   * with an accelerator key of 's', 'ctrl+s' would activate this menu's action.
   *
   * @param acceleratorKey The accelerator key to bind, typically meaning ctrl and this accelerator
   *     key will fire the action bound to this menu item.
   */
  public JMenuItemBuilder accelerator(final KeyCode acceleratorKey) {
    this.acceleratorKey = acceleratorKey;
    return this;
  }
}
