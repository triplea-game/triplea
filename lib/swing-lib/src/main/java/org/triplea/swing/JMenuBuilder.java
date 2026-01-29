package org.triplea.swing;

import com.google.common.base.Preconditions;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.triplea.java.ArgChecker;
import org.triplea.swing.key.binding.KeyCode;

/**
 * Builder to creates a menu with title and mnemonic key.
 *
 * <p>Example usage:. <code><pre>
 *   JMenu menu = new JMenuBuilder("Menu Text", 'M')
 *     .addMenuItem("Menu item title", 'M', () -> menuItemAction())
 *     .addMenuItem("Another menu item", 'A', () -> anotherAction())
 *     .build();
 * </pre></code>
 */
public class JMenuBuilder {
  private final String title;
  private final KeyCode mnemonic;
  private final Collection<Component> menuComponents = new ArrayList<>();

  public JMenuBuilder(final String title, final KeyCode mnemonic) {
    ArgChecker.checkNotEmpty(title);
    this.title = title;
    this.mnemonic = mnemonic;
  }

  /** Constructs a Swing JMenu using current builder values. */
  public JMenu build() {
    final JMenu menu = new JMenu(title);
    menu.setMnemonic(mnemonic.getInputEventCode());
    menuComponents.forEach(menu::add);
    return menu;
  }

  /**
   * Adds a menu item.
   *
   * @param title The menu item title, this is the clickable text that will appear in the menu drop
   *     down.
   * @param mnemonic A key that can be pressed to activate the menu item.
   * @param menuItemAction The action that will be fired when user clicks the menu item.
   */
  public JMenuBuilder addMenuItem(
      final String title, final KeyCode mnemonic, final Runnable menuItemAction) {
    ArgChecker.checkNotEmpty(title);
    Preconditions.checkNotNull(menuItemAction);
    return addMenuItem(
        new JMenuItemBuilder(title, mnemonic).actionListener(menuItemAction).build());
  }

  /** Adds a menu item to the menu. */
  public JMenuBuilder addMenuItem(final JMenuItem menuItem) {
    menuComponents.add(menuItem);
    return this;
  }

  /** Adds a menu item to the menu. */
  public JMenuBuilder addMenuItem(final JMenuItemBuilder menuItemBuilder) {
    return addMenuItem(menuItemBuilder.build());
  }

  /**
   * Adds a menu item if a condition is true.
   *
   * @see #addMenuItem(String, KeyCode, Runnable)
   * @param condition The condition to verify, if false the menu item is not added.
   * @param title The menu item title, this is the clickable text that will appear in the menu drop
   *     down.
   * @param mnemonic A key that can be pressed to activate the menu item.
   * @param menuItemAction The action that will be fired when user clicks the menu item.
   */
  public JMenuBuilder addMenuItemIf(
      final boolean condition,
      final String title,
      final KeyCode mnemonic,
      final Runnable menuItemAction) {
    if (condition) {
      addMenuItem(title, mnemonic, menuItemAction);
    }
    return this;
  }

  public JMenuBuilder addMenuItemIf(final boolean condition, final JMenuItem menuItem) {
    if (condition) {
      addMenuItem(menuItem);
    }
    return this;
  }

  public JMenuBuilder addMenuItemIf(
      final boolean condition, final JMenuItemBuilder menuItemBuilder) {
    return addMenuItemIf(condition, menuItemBuilder.build());
  }

  public JMenuBuilder addSeparator() {
    menuComponents.add(new JPopupMenu.Separator());
    return this;
  }
}
