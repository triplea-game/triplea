package org.triplea.swing;

import com.google.common.base.Preconditions;
import java.util.function.Consumer;
import javax.swing.JCheckBoxMenuItem;
import org.triplea.java.ArgChecker;

/**
 * Builds a JMenuCheckBox. This is a menu item with a checkbox next to it. By default the checkbox
 * will be unselected.
 *
 * <p>Example usage:. <code><pre>
 *   JMenu menu = new JMenuBuilder("Menu Text")
 *     .addMenuItem(new JMenuItemCheckboxBuilder("checkbox", 'C')
 *         .selected(true)
 *         .actionListener(selected -> doAction(selected))
 *         .build())
 *     .build();
 * </pre></code>
 */
public class JMenuItemCheckBoxBuilder {
  private String title;
  private Character mnemonic;
  private Consumer<Boolean> action;
  private boolean selected;

  /** Sets the title that appears next to the checkbox. */
  public JMenuItemCheckBoxBuilder(final String title, final char mnemonic) {
    ArgChecker.checkNotEmpty(title);
    this.title = title;
    this.mnemonic = mnemonic;
  }

  /** Constructs a Swing JMenu using current builder values. */
  public JCheckBoxMenuItem build() {
    ArgChecker.checkNotEmpty(title);
    Preconditions.checkNotNull(mnemonic);
    Preconditions.checkNotNull(action);

    final var checkBox = new JCheckBoxMenuItem(title);
    checkBox.setMnemonic(mnemonic);
    checkBox.setSelected(selected);
    checkBox.addActionListener(e -> action.accept(checkBox.isSelected()));
    return checkBox;
  }

  public JMenuItemCheckBoxBuilder selected(final boolean selected) {
    this.selected = selected;
    return this;
  }

  public JMenuItemCheckBoxBuilder actionListener(final Consumer<Boolean> actionListener) {
    this.action = actionListener;
    return this;
  }
}
