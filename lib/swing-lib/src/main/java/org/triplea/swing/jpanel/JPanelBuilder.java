package org.triplea.swing.jpanel;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

/**
 * Example usage:<br>
 * <br>
 * Default layout is a flow layout: <code><pre>
 *   final JPanel panel = new JPanelBuilder()
 *       .addLabel("label")
 *       .add(new JButton("button"))
 *       .build();
 *  </pre></code><br>
 * <br>
 * Using a grid layout: <code><pre>
 *   final JPanel panel = new JPanelBuilder()
 *       .gridLayout()
 *       .rows(2)
 *       .columns(1)
 *       .add(new JLabel(""))
 *       .add(new JLabel(""))
 *       .build();
 * </pre></code><br>
 */
public class JPanelBuilder {
  private Float horizontalAlignment;
  private Border border;
  private Integer preferredHeight;

  /**
   * Constructs JPanel with all properties from JPanelBuilder, in effect this should be everything
   * minus components placed in their layout.
   */
  JPanel build() {
    final JPanel panel = new JPanel();
    panel.setOpaque(false);
    if (border != null) {
      panel.setBorder(border);
    }
    if (horizontalAlignment != null) {
      panel.setAlignmentX(horizontalAlignment);
    }

    if (preferredHeight != null) {
      panel.setPreferredSize(new Dimension(panel.getWidth(), preferredHeight));
    }
    return panel;
  }

  public JPanelBuilder border(final Border border) {
    this.border = border;
    return this;
  }

  /** Adds an empty border (padding) around the edges of the panel. */
  public JPanelBuilder border(final int size) {
    return border(size, size, size, size);
  }

  public JPanelBuilder border(final int top, final int left, final int bottom, final int right) {
    border = BorderFactory.createEmptyBorder(top, left, bottom, right);
    return this;
  }

  public JPanelBuilder borderEtched() {
    border = new EtchedBorder();
    return this;
  }

  public JPanelBuilder horizontalAlignmentCenter() {
    this.horizontalAlignment = JComponent.CENTER_ALIGNMENT;
    return this;
  }

  public JPanelBuilder height(final int height) {
    preferredHeight = height;
    return this;
  }

  public FlowLayoutBuilder add(final Component component) {
    return flowLayout().add(component);
  }

  public FlowLayoutBuilder flowLayout() {
    return new FlowLayoutBuilder(this);
  }

  public BorderLayoutBuilder borderLayout() {
    return new BorderLayoutBuilder(this);
  }

  public GridLayoutBuilder gridLayout(final int rows, final int columns) {
    return new GridLayoutBuilder(this, rows, columns);
  }

  public BoxLayoutBuilder boxLayoutHorizontal() {
    return new BoxLayoutBuilder(this, BoxLayoutBuilder.BoxLayoutOrientation.HORIZONTAL);
  }

  public BoxLayoutBuilder boxLayoutVertical() {
    return new BoxLayoutBuilder(this, BoxLayoutBuilder.BoxLayoutOrientation.VERTICAL);
  }

  public GridBagLayoutBuilder gridBagLayout() {
    return new GridBagLayoutBuilder(this);
  }
}
