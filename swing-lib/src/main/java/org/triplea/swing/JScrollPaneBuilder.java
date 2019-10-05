package org.triplea.swing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.awt.Component;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

/**
 * A builder for incrementally creating instances of {@link JScrollPane}.
 *
 * <p>Example usage:
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
  private @Nullable Border border;
  private Component view;

  private JScrollPaneBuilder() {}

  public static JScrollPaneBuilder builder() {
    return new JScrollPaneBuilder();
  }

  /**
   * Sets the scroll pane border.
   *
   * @param border The border.
   */
  public JScrollPaneBuilder border(final Border border) {
    checkNotNull(border);

    this.border = border;
    return this;
  }

  /**
   * Conditionally sets the scroll pane border.
   *
   * @param border The border; if empty, the current border will not be changed.
   */
  public JScrollPaneBuilder border(final Optional<Border> border) {
    checkNotNull(border);

    border.ifPresent(this::border);
    return this;
  }

  /**
   * Sets the component to display in the scroll pane's viewport.
   *
   * @param view The component to display in the scroll pane's viewport.
   */
  public JScrollPaneBuilder view(final Component view) {
    checkNotNull(view);

    this.view = view;
    return this;
  }

  /**
   * Creates a new scroll pane using the builder's current state.
   *
   * @return A new scroll pane.
   * @throws IllegalStateException If {@code view} is unspecified.
   */
  public JScrollPane build() {
    checkState(view != null, "view must be specified");

    final JScrollPane scrollPane = new JScrollPane(view);

    if (border != null) {
      scrollPane.setBorder(border);
    }

    return scrollPane;
  }
}
