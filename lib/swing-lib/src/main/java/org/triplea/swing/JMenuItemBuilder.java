package org.triplea.swing;

import com.google.common.base.Preconditions;
import java.awt.Toolkit;
import javax.annotation.Nullable;
import javax.swing.Action;
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
  private final KeyCode mnemonic;
  private Runnable actionListener;
  private Integer acceleratorKey;
  private boolean selected;
  private boolean enabled = true;
  @Nullable private String tooltip = null;

  public JMenuItemBuilder(final String title, final KeyCode mnemonic) {
    ArgChecker.checkNotEmpty(title);
    Preconditions.checkNotNull(mnemonic);
    this.title = title;
    this.mnemonic = mnemonic;
  }

  public JMenuItemBuilder(final Action action, final KeyCode mnemonic) {
    this(((String) action.getValue(Action.NAME)), mnemonic);
    actionListener(() -> action.actionPerformed(null));
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
    if (enabled) {
      Preconditions.checkNotNull(actionListener);
      menuItem.addActionListener(e -> actionListener.run());
      menuItem.setMnemonic(mnemonic.getInputEventCode());
      if (acceleratorKey != null) {
        menuItem.setAccelerator(
            KeyStroke.getKeyStroke(
                acceleratorKey, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      }
    } else {
      menuItem.setEnabled(false);
      menuItem.setToolTipText(tooltip);
    }
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

  /**
   * @param tooltip Explaining tooltip why the menu item is disabled.
   */
  public JMenuItemBuilder disabled(final String tooltip) {
    this.enabled = false;
    this.tooltip = tooltip;
    return this;
  }
}
