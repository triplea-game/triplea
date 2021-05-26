package org.triplea.swing;

import static com.google.common.base.Preconditions.checkState;

import java.awt.Component;
import java.awt.Dimension;
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
  private Dimension preferredSize;
  private Dimension maxSize;

  private final Component view;

  public JScrollPaneBuilder(final Component view) {
    this.view = view;
  }

  /**
   * Sets the scroll pane border.
   *
   * @param border The border.
   */
  public JScrollPaneBuilder border(final Border border) {
    this.border = border;
    return this;
  }

  public JScrollPaneBuilder maxSize(final int width, final int height) {
    maxSize = new Dimension(width, height);
    return this;
  }

  public JScrollPaneBuilder preferredSize(final int width, final int height) {
    preferredSize = new Dimension(width, height);
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
    Optional.ofNullable(border).ifPresent(scrollPane::setBorder);
    Optional.ofNullable(maxSize).ifPresent(scrollPane::setMaximumSize);
    Optional.ofNullable(preferredSize).ifPresent(scrollPane::setPreferredSize);
    return scrollPane;
  }
}
