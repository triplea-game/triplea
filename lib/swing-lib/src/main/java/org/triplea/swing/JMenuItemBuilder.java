package org.triplea.swing;

import com.google.common.base.Preconditions;
import java.awt.Toolkit;
import java.util.Optional;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import org.triplea.java.ArgChecker;
import org.triplea.swing.key.binding.KeyCode;

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
  private final int mnemonic;
  private Runnable actionListener;
  private Integer acceleratorKey;
  private boolean selected;

  public JMenuItemBuilder(final String title, final int mnemonic) {
    ArgChecker.checkNotEmpty(title);
    this.title = title;
    this.mnemonic = mnemonic;
  }

  /** Constructs a Swing JMenuItem using current builder values. */
  public JMenuItem build() {
    final JMenuItem menuItem = new JMenuItem(title);
    buildImpl(menuItem);
    return menuItem;
  }

  /** Constructs a Swing JCheckBoxMenuItem using current builder values. */
  public JCheckBoxMenuItem buildCheckbox() {
    final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(title, selected);
    buildImpl(menuItem);
    return menuItem;
  }

  /** Constructs a Swing JRadioButtonMenuItem using current builder values. */
  public JRadioButtonMenuItem buildRadio(final ButtonGroup group) {
    final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(title, selected);
    buildImpl(menuItem);
    group.add(menuItem);
    return menuItem;
  }

  private void buildImpl(final JMenuItem menuItem) {
    Preconditions.checkNotNull(actionListener);

    menuItem.setMnemonic(mnemonic);
    menuItem.addActionListener(e -> actionListener.run());
    Optional.ofNullable(acceleratorKey)
        .ifPresent(
            accelerator ->
                menuItem.setAccelerator(
                    KeyStroke.getKeyStroke(
                        accelerator, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx())));
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
    this.acceleratorKey = acceleratorKey.getInputEventCode();
    return this;
  }

  /**
   * @param selected Whether the menu item should be selected.
   */
  public JMenuItemBuilder selected(final boolean selected) {
    this.selected = selected;
    return this;
  }
}
