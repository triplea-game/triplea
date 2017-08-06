package swinglib;

import java.awt.Component;

import javax.swing.JSplitPane;

public final class JSplitPaneBuilder {


  private int dividerLocation = 150;


  private Component left;
  private Component right;


  private JSplitPaneBuilder() {

  }

  public JSplitPane build() {
    final JSplitPane pane = new JSplitPane();
    pane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);

    pane.setDividerLocation(dividerLocation);

    if (left != null) {
      pane.setLeftComponent(left);
    }

    if (right != null) {
      pane.setRightComponent(right);
    }

    return pane;
  }


  public static JSplitPaneBuilder builder() {
    return new JSplitPaneBuilder();
  }


  public JSplitPaneBuilder dividerLocation(final int dividerLocation) {
    this.dividerLocation = dividerLocation;
    return this;
  }

  public JSplitPaneBuilder addLeft(final Component component) {
    this.left = component;
    return this;
  }

  public JSplitPaneBuilder addRight(final Component component) {
    this.right = component;
    return this;
  }


}
