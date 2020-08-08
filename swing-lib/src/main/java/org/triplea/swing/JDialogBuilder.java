package org.triplea.swing;

import com.google.common.base.Preconditions;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.JDialog;
import javax.swing.JFrame;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

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
  private final List<Function<JDialog, Component>> components = new ArrayList<>();
  private boolean escapeKeyCloses;

  public JDialog build() {
    Preconditions.checkNotNull(parent);
    Preconditions.checkNotNull(title);

    final JDialog dialog = new JDialog(parent, title);
    components.stream()
        .map(componentFunction -> componentFunction.apply(dialog))
        .forEach(component -> dialog.getContentPane().add(component));
    if (escapeKeyCloses) {
      SwingKeyBinding.addKeyBinding(dialog, KeyCode.ESCAPE, dialog::dispose);
    }
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
    components.add(dialog -> component);
    return this;
  }

  /**
   * Use this method when adding a component that needs a reference to the JDialog that will be
   * created. Example:
   *
   * <pre>{@code
   * new JDialogBuilder()
   *   .add(dialog -> new JButtonBuilder("Close").actionListener(dialog::dispose).build())
   *   .build();
   * }</pre>
   *
   * WARNING: if the dialog contains any text components, the escape key event may be captured by
   * the text component and might not be sent to dialog window. To overcome this, you need to add a
   * keylistener to the text area that will dispose this dialog when enter key is pressed.
   */
  public JDialogBuilder add(final Function<JDialog, Component> component) {
    components.add(component);
    return this;
  }

  public JDialogBuilder add(final Collection<Component> components) {
    components.forEach(this::add);
    return this;
  }

  public JDialogBuilder size(final int width, final int height) {
    size = new Dimension(width, height);
    return this;
  }

  public JDialogBuilder escapeKeyCloses() {
    escapeKeyCloses = true;
    return this;
  }
}
