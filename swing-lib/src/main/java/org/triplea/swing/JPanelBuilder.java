package org.triplea.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.triplea.swing.GridBagHelper.Anchor;
import org.triplea.swing.GridBagHelper.Fill;

import com.google.common.base.Preconditions;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Example usage:.
 * <code><pre>
 *   final JPanel panel = JPanelBuilder.builder()
 *       .gridLayout(2, 1)
 *       .add(new JLabel("")
 *       .add(new JLabel("")
 *       .build();
 * </pre></code>
 */
public class JPanelBuilder {

  @Value
  private static class PanelComponent {
    @Nonnull
    private final Component component;
    @Nonnull
    private final PanelProperties panelProperties;
  }

  private final Collection<PanelComponent> panelComponents = new ArrayList<>();
  private Float horizontalAlignment;
  private Border border;
  private LayoutManager layout;
  private BoxLayoutType boxLayoutType = null;
  private boolean useGridBagHelper = false;
  private int gridBagHelperColumns;
  private Integer preferredHeight;

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

    panel.setOpaque(false);

    if (border != null) {
      panel.setBorder(border);
    }

    if (layout == null && boxLayoutType != null) {
      final int boxDirection = boxLayoutType == BoxLayoutType.HORIZONTAL ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS;
      layout = new BoxLayout(panel, boxDirection);
    } else if (layout == null) {
      layout = new FlowLayout();
    }

    panel.setLayout(layout);

    GridBagHelper gridBagHelper = null;
    if (useGridBagHelper) {
      gridBagHelper = new GridBagHelper(panel, gridBagHelperColumns);
    }

    for (final PanelComponent panelComponent : panelComponents) {
      final PanelProperties panelProperties = panelComponent.getPanelProperties();

      if (panelProperties.borderLayoutPosition == BorderLayoutPosition.DEFAULT) {
        if (gridBagHelper != null) {

          gridBagHelper.add(
              panelComponent.getComponent(),
              panelProperties.columnSpan,
              panelProperties.anchor,
              panelProperties.fill);

        } else {
          panel.add(panelComponent.getComponent());
        }
      } else {
        switch (panelProperties.borderLayoutPosition) {
          case CENTER:
            panel.add(panelComponent.getComponent(), BorderLayout.CENTER);
            break;
          case SOUTH:
            panel.add(panelComponent.getComponent(), BorderLayout.SOUTH);
            break;
          case NORTH:
            panel.add(panelComponent.getComponent(), BorderLayout.NORTH);
            break;
          case WEST:
            panel.add(panelComponent.getComponent(), BorderLayout.WEST);
            break;
          case EAST:
            panel.add(panelComponent.getComponent(), BorderLayout.EAST);
            break;
          default:
            Preconditions.checkState(layout != null);
            panel.add(panelComponent.getComponent());
            break;
        }
      }
    }
    if (horizontalAlignment != null) {
      panel.setAlignmentX(horizontalAlignment);
    }

    if (preferredHeight != null) {
      panel.setPreferredSize(new Dimension(panel.getWidth(), preferredHeight));
    }
    return panel;
  }

  /**
   * Sets the current layout manager to a GridBag. This helper method will do most of the work
   * of the gridbag for you, but the number of columns in the grid needs to be specified. After
   * this components can be added as normal, rows will wrap as needed when enough components are added.
   *
   * @param gridBagHelperColumns The number of columns to be created before components will wrap to a new row.
   */
  public JPanelBuilder gridBagLayout(final int gridBagHelperColumns) {
    this.useGridBagHelper = true;
    this.gridBagHelperColumns = gridBagHelperColumns;
    return this;
  }

  public JPanelBuilder horizontalBoxLayout() {
    boxLayoutType = BoxLayoutType.HORIZONTAL;
    return this;
  }

  public JPanelBuilder verticalBoxLayout() {
    boxLayoutType = BoxLayoutType.VERTICAL;
    return this;
  }

  /**
   * Toggles a border layout, adds a given component to the 'north' portion of the border layout.
   */
  public JPanelBuilder addNorth(final JComponent child) {
    layout = new BorderLayout();
    Preconditions.checkNotNull(child);
    panelComponents.add(new PanelComponent(child, new PanelProperties(BorderLayoutPosition.NORTH)));
    return this;
  }

  /**
   * Toggles a border layout, adds a given component to the 'east' portion of the border layout.
   */
  public JPanelBuilder addEast(final JComponent child) {
    layout = new BorderLayout();
    Preconditions.checkNotNull(child);
    panelComponents.add(new PanelComponent(child, new PanelProperties(BorderLayoutPosition.EAST)));
    return this;
  }

  /**
   * Toggles a border layout, adds a given component to the 'west' portion of the border layout.
   */
  public JPanelBuilder addWest(final JComponent child) {
    layout = new BorderLayout();
    Preconditions.checkNotNull(child);
    panelComponents.add(new PanelComponent(child, new PanelProperties(BorderLayoutPosition.WEST)));
    return this;
  }

  /**
   * Toggles a border layout, adds a given component to the 'center' portion of the border layout.
   */
  public JPanelBuilder addCenter(final JComponent child) {
    layout = new BorderLayout();
    panelComponents.add(new PanelComponent(child, new PanelProperties(BorderLayoutPosition.CENTER)));
    return this;
  }

  /**
   * Specify a grid layout with a given number of rows and columns.
   *
   * @param rows First parameter for 'new GridLayout'
   * @param columns Second parameter for 'new GridLayout'
   */
  public JPanelBuilder gridLayout(final int rows, final int columns) {
    layout = new GridLayout(rows, columns);
    return this;
  }

  public JPanelBuilder border(final Border border) {
    this.border = border;
    return this;
  }

  public JPanelBuilder borderEmpty() {
    return borderEmpty(0);
  }

  public JPanelBuilder borderEmpty(final int borderWidth) {
    return borderEmpty(borderWidth, borderWidth, borderWidth, borderWidth);
  }

  public JPanelBuilder borderEmpty(final int top, final int left, final int bottom, final int right) {
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

  public JPanelBuilder flowLayout() {
    return flowLayout(FlowLayoutJustification.DEFAULT);
  }

  public JPanelBuilder flowLayout(final FlowLayoutJustification flowLayoutDirection) {
    layout = flowLayoutDirection.newFlowLayout();
    return this;
  }

  /**
   * Adds {@code component} to the panel and ensures it will be left-justified in the final layout. Primarily for use
   * with vertical box layouts.
   */
  public JPanelBuilder addLeftJustified(final Component component) {
    final Box box = Box.createHorizontalBox();
    box.add(component);
    box.add(Box.createHorizontalGlue());
    return add(box);
  }

  public JPanelBuilder add(final Component component) {
    Preconditions.checkNotNull(component);
    panelComponents.add(new PanelComponent(component, new PanelProperties(BorderLayoutPosition.DEFAULT)));
    return this;
  }

  public JPanelBuilder add(final Component component, final GridBagHelper.Anchor anchor,
      final GridBagHelper.Fill fill) {
    Preconditions.checkNotNull(component);
    panelComponents.add(new PanelComponent(component, new PanelProperties(anchor, fill)));
    return this;
  }

  /**
   * Adds a given component to the southern portion of a border layout.
   */
  public JPanelBuilder addSouth(final JComponent child) {
    layout = new BorderLayout();
    Preconditions.checkNotNull(child);
    panelComponents.add(new PanelComponent(child, new PanelProperties(BorderLayoutPosition.SOUTH)));
    return this;
  }

  public JPanelBuilder addLabel(final String text) {
    add(new JLabel(text));
    return this;
  }

  /**
   * use this when you want an empty space component that will take up extra space. For example, with a gridbag layout
   * with 2 columns, if you have 2 components, the second will be stretched by default to fill all available space
   * to the right. This right hand component would then resize with the window. If on the other hand a 3 column
   * grid bag were used and the last element were a horizontal glue, then the 2nd component would then have a fixed
   * size.
   */
  public JPanelBuilder addHorizontalGlue() {
    add(Box.createHorizontalGlue());
    return this;
  }

  public JPanelBuilder addVerticalStrut(final int strutSize) {
    add(Box.createVerticalStrut(strutSize));
    return this;
  }

  public JPanelBuilder addHorizontalStrut(final int strutSize) {
    add(Box.createHorizontalStrut(strutSize));
    return this;
  }

  public JPanelBuilder borderLayout() {
    layout = new BorderLayout();
    return this;
  }


  public JPanelBuilder preferredHeight(final int height) {
    preferredHeight = height;
    return this;
  }

  /**
   * BoxLayout needs a reference to the panel component that is using the layout, so we cannot create the layout
   * until after we create the component. Thus we use a flag to create it, rather than creating and storing
   * the layout manager directly as we do for the other layouts such as gridLayout.
   */
  private enum BoxLayoutType {
    NONE, HORIZONTAL, VERTICAL
  }

  /**
   * Swing border layout locations.
   */
  public enum BorderLayoutPosition {
    DEFAULT, CENTER, SOUTH, NORTH, WEST, EAST
  }


  /**
   * Type-safe alias for magic values in {@code FlowLayout}.
   */
  @AllArgsConstructor
  public enum FlowLayoutJustification {
    DEFAULT(FlowLayout.CENTER),

    CENTER(FlowLayout.CENTER),

    LEFT(FlowLayout.LEFT),

    RIGHT(FlowLayout.RIGHT);

    private final int align;

    FlowLayout newFlowLayout() {
      return new FlowLayout(align);
    }
  }


  /**
   * Struct-like class for the various properties and styles that can be applied to a panel.
   */
  private static final class PanelProperties {
    BorderLayoutPosition borderLayoutPosition = BorderLayoutPosition.DEFAULT;
    GridBagHelper.ColumnSpan columnSpan = GridBagHelper.ColumnSpan.of(1);
    GridBagHelper.Fill fill = Fill.NONE;
    GridBagHelper.Anchor anchor = Anchor.WEST;


    PanelProperties(final BorderLayoutPosition borderLayoutPosition) {
      this.borderLayoutPosition = borderLayoutPosition;
    }

    PanelProperties(final Anchor anchor, final Fill fill) {
      this.anchor = anchor;
      this.fill = fill;
    }
  }
}
