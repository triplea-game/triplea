package org.triplea.swing;

import com.google.common.base.Preconditions;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 * Builder to construct a JDialog. A JDialog is essentially a pop-up window, it generally should be
 * used in preference to using a JFrame.
 *
 * <p>Example usage: <code>
 *   new JDialogBuilder()
 *     .parent(parentFrame)
 *     .title("Title of the dialog")
 *     .add(new JLabel("A label"))
 *     .add(new JButton("A Button"))
 *     .buildAndShow();
 * </code>
 */
public class JDialogBuilder {

  private JFrame parent;
  private String title;
  private Dimension size;
  private final List<Component> components = new ArrayList<>();

  public JDialog build() {
    Preconditions.checkNotNull(parent);
    Preconditions.checkNotNull(title);

    final JDialog dialog = new JDialog(parent, title);
    components.forEach(component -> dialog.getContentPane().add(component));
    dialog.setLocationRelativeTo(parent);
    dialog.pack();
    Optional.ofNullable(size).ifPresent(dialog::setSize);
    return dialog;
  }

  public JDialog buildAndShow() {
    final JDialog dialog = build();
    dialog.setVisible(true);
    return dialog;
  }

  public JDialogBuilder parent(final JFrame parent) {
    this.parent = parent;
    return this;
  }

  public JDialogBuilder title(final String title) {
    this.title = title;
    return this;
  }

  public JDialogBuilder add(final Component component) {
    components.add(component);
    return this;
  }

  public JDialogBuilder add(final Collection<Component> components) {
    this.components.addAll(components);
    return this;
  }

  public JDialogBuilder size(final int width, final int height) {
    size = new Dimension(width, height);
    return this;
  }
}
