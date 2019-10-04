package org.triplea.swing;

import com.google.common.base.Preconditions;
import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JSplitPane;

/**
 * Relatively simple type-safe builder API for constructing a JTabbedPane. <br>
 * Example usage:
 *
 * <pre>
 * <code>
 *   JSplitPane splitPane = JSplitPaneBuilder.builder()
 *      .addLeft(new JLabel("left component")
 *      .addRight(new JLabel("right component");
 *      .build();
 * </code>
 * </pre>
 *
 * <br>
 * Note the builder sets the 'horizontal' or 'vertical' configuration of the split panel
 * automatically. To do the top/bottom version, eg:
 *
 * <pre>
 * <code>
 *   JSplitPane splitPane = JSplitPaneBuilder.builder()
 *      .addTop(new JLabel("top component")
 *      .addBottom(new JLabel("bottom component");
 *      .build();
 * </code>
 * </pre>
 */
public final class JSplitPaneBuilder {

  private int dividerLocation = 150;

  private boolean extraSpaceToTopAndLeft = false;
  private Component left;
  private Component right;

  private Component top;
  private Component bottom;

  private JSplitPaneBuilder() {}

  public static JSplitPaneBuilder builder() {
    return new JSplitPaneBuilder();
  }

  /** Builds the swing component. */
  public JSplitPane build() {
    final JSplitPane pane = new JSplitPane();
    pane.setDividerLocation(dividerLocation);
    pane.setResizeWeight(extraSpaceToTopAndLeft ? 1.0 : 0.0);

    if (left != null) {
      Preconditions.checkNotNull(right);
      Preconditions.checkState(top == null);
      Preconditions.checkState(bottom == null);

      pane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
      pane.setLeftComponent(left);
      pane.setRightComponent(right);
    } else {
      Preconditions.checkNotNull(top);
      Preconditions.checkNotNull(bottom);
      Preconditions.checkState(right == null);

      pane.setOrientation(JSplitPane.VERTICAL_SPLIT);
      pane.setTopComponent(top);
      pane.setBottomComponent(bottom);
    }

    return pane;
  }

  public JSplitPaneBuilder dividerLocation(final int dividerLocation) {
    this.dividerLocation = dividerLocation;
    return this;
  }

  public JSplitPaneBuilder addLeft(final Component component) {
    this.left = component;
    return this;
  }

  /**
   * Adds a component to the right hand side of the split pane. TODO: maybe we may want a
   * 'add-first' and 'add-second' method so that the client does not have to worry about calling
   * addRight vs addBottom, they can call 'addSecond'
   */
  public JSplitPaneBuilder addRight(final Component component) {
    this.right = component;
    return this;
  }

  /**
   * Overrides default behavior and allocates extra window space to the left or top panels in the
   * split pane.
   */
  public JSplitPaneBuilder giveExtraSpaceToTopAndLeftPanes() {
    this.extraSpaceToTopAndLeft = true;
    return this;
  }

  /**
   * Sets spacing flag back to default, extra window space will be given to the bottom or right
   * panes.
   */
  public JSplitPaneBuilder giveExtraSpaceToBottomAndRightPanes() {
    this.extraSpaceToTopAndLeft = false;
    return this;
  }

  public JSplitPaneBuilder addTop(final JComponent top) {
    this.top = top;
    return this;
  }

  public JSplitPaneBuilder addBottom(final JComponent bottom) {
    this.bottom = bottom;
    return this;
  }
}
