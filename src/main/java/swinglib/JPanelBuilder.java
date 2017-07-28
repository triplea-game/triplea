package swinglib;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.apache.commons.math3.util.Pair;

/**
 * Example usage:
 * <code><pre>
 *   final JPanel panel = JPanelBuilder.builder()
 *       .gridLayout(2, 1)
 *       .add(new JLabel("")
 *       .add(new JLabel("")
 *       .build();
 * </pre></code>
 */
public class JPanelBuilder {

  private final Collection<Pair<Component, BorderLayoutPosition>> components = new ArrayList<>();
  private Layout layout = Layout.DEFAULT;
  private int gridRows;
  private int gridColumns;
  private BorderType borderType;
  private int borderWidth;
  private Float xAlignment;

  private JPanelBuilder() {}

  public static JPanelBuilder builder() {
    return new JPanelBuilder();
  }

  /**
   * Constructs a Swing JPanel using current builder values.
   * Values that must be set: (requires no values to be set)
   */
  public JPanel build() {
    final JPanel panel = new JPanel();
    if (borderType != null) {
      switch (borderType) {
        case EMPTY:
        default:
          panel.setBorder(new EmptyBorder(borderWidth, borderWidth, borderWidth, borderWidth));
          break;
      }
    }
    switch (layout) {
      case GRID:
        panel.setLayout(new GridLayout(gridRows, gridColumns));
        break;
      case GRID_BAG:
        panel.setLayout(new GridBagLayout());
        break;
      case BOX_LAYOUT_HORIZONTAL:
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        break;
      case BOX_LAYOUT_VERTICAL:
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        break;
      case DEFAULT:
        panel.setLayout(new BorderLayout());
        break;
    }
    for (final Pair<Component, BorderLayoutPosition> child : components) {
      if (child.getSecond() == BorderLayoutPosition.DEFAULT) {
        panel.add(child.getFirst());
      } else {
        switch (child.getSecond()) {
          case CENTER:
            panel.add(child.getFirst(), BorderLayout.CENTER);
            break;
          case SOUTH:
            panel.add(child.getFirst(), BorderLayout.SOUTH);
            break;
          case NORTH:
            panel.add(child.getFirst(), BorderLayout.NORTH);
            break;
          case WEST:
            panel.add(child.getFirst(), BorderLayout.WEST);
            break;
          case EAST:
            panel.add(child.getFirst(), BorderLayout.EAST);
            break;
          default:
            panel.add(child.getFirst());
            break;
        }
      }
    }
    if(xAlignment != null) {
      panel.setAlignmentX(xAlignment);
    }
    return panel;
  }

  public JPanelBuilder gridBagLayout() {
    layout = Layout.GRID_BAG;
    return this;
  }

  public JPanelBuilder horizontalBoxLayout() {
    layout = Layout.BOX_LAYOUT_HORIZONTAL;
    return this;
  }

  public JPanelBuilder verticalBoxLayout() {
    layout = Layout.BOX_LAYOUT_VERTICAL;
    return this;
  }

  public JPanelBuilder addNorth(final Component child) {
    components.add(new Pair<>(child, BorderLayoutPosition.NORTH));
    return this;
  }

  public JPanelBuilder addSouth(final Component child) {
    components.add(new Pair<>(child, BorderLayoutPosition.SOUTH));
    return this;
  }

  public JPanelBuilder addEast(final Component child) {
    components.add(new Pair<>(child, BorderLayoutPosition.EAST));
    return this;
  }

  public JPanelBuilder addWest(final Component child) {
    components.add(new Pair<>(child, BorderLayoutPosition.WEST));
    return this;
  }

  public JPanelBuilder addCenter(final Component child) {
    components.add(new Pair<>(child, BorderLayoutPosition.CENTER));
    return this;
  }


  public JPanelBuilder add(final Component component) {
    components.add(new Pair<>(component, BorderLayoutPosition.DEFAULT));
    return this;
  }

  /**
   * Specify a grid layout with a given number of rows and columns.
   * 
   * @param rows First parameter for 'new GridLayout'
   * @param columns Second parameter for 'new GridLayout'
   */
  public JPanelBuilder gridLayout(final int rows, final int columns) {
    layout = Layout.GRID;
    this.gridRows = rows;
    this.gridColumns = columns;
    return this;
  }

  public JPanelBuilder border(final BorderType borderType) {
    this.borderType = borderType;
    return this;
  }

  public JPanelBuilder borderWidth(final int borderWidth) {
    this.borderWidth = borderWidth;
    return this;
  }

  public JPanelBuilder xAlignmentCenter() {
    this.xAlignment = JComponent.CENTER_ALIGNMENT;
    return this;
  }

  private enum Layout {
    DEFAULT, GRID, GRID_BAG, BOX_LAYOUT_HORIZONTAL, BOX_LAYOUT_VERTICAL
  }

  public enum BorderLayoutPosition {
    DEFAULT, CENTER, SOUTH, NORTH, WEST, EAST
  }

  public enum BorderType {
    EMPTY
  }
}
