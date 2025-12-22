package org.triplea.swing;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
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
  private final Collection<JMenuItem> menuItems = new ArrayList<>();

  public JMenuBuilder(final String title, final KeyCode mnemonic) {
    ArgChecker.checkNotEmpty(title);
    this.title = title;
    this.mnemonic = mnemonic;
  }

  /** Constructs a Swing JMenu using current builder values. */
  public JMenu build() {
    final JMenu menu = new JMenu(title);
    menu.setMnemonic(mnemonic.getInputEventCode());
    menuItems.forEach(menu::add);
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

    final JMenuItem menuItem = new JMenuItem(title);
    menuItem.setMnemonic(mnemonic.getInputEventCode());
    menuItem.addActionListener(e -> menuItemAction.run());

    return addMenuItem(menuItem);
  }

  /** Adds a menu item to the menu. */
  public JMenuBuilder addMenuItem(final JMenuItem menuItem) {
    menuItems.add(menuItem);
    return this;
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
}
