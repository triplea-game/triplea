package swinglib;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.awt.Component;

import javax.swing.JScrollPane;

/**
 * A builder for incrementally creating instances of {@link JScrollPane}.
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * <code>
 * JScrollPaneBuilder scrollPane = JScrollPaneBuilder.builder()
 *     .view(view)
 *     .build();
 * </code>
 * </pre>
 */
public final class JScrollPaneBuilder {
  private Component view;

  private JScrollPaneBuilder() {}

  public static JScrollPaneBuilder builder() {
    return new JScrollPaneBuilder();
  }

  /**
   * Sets the component to display in the scroll pane's viewport.
   *
   * @param view The component to display in the scroll pane's viewport.
   *
   * @throws NullPointerException If {@code view} is {@code null}.
   */
  public JScrollPaneBuilder view(final Component view) {
    checkNotNull(view, "view must not be null");

    this.view = view;
    return this;
  }

  /**
   * Creates a new scroll pane using the builder's current state.
   *
   * @return A new scroll pane.
   *
   * @throws IllegalStateException If {@code view} is unspecified.
   */
  public JScrollPane build() {
    checkState(view != null, "view must be specified");

    return new JScrollPane(view);
  }
}
