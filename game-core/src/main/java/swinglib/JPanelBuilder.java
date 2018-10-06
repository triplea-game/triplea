package swinglib;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.apache.commons.math3.util.Pair;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import lombok.AllArgsConstructor;
import swinglib.GridBagHelper.Anchor;
import swinglib.GridBagHelper.ColumnSpan;
import swinglib.GridBagHelper.Fill;

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

  private final Collection<Pair<Component, PanelProperties>> components = new ArrayList<>();
  private Float horizontalAlignment;
  private Border border;
  private LayoutManager layout;
  private BoxLayoutType boxLayoutType = null;
  private boolean useGridBagHelper = false;
  private int gridBagHelperColumns;
  private boolean flowLayoutWrapper = false;
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

    for (final Pair<Component, PanelProperties> child : components) {
      Preconditions.checkNotNull(child.getFirst());
      Preconditions.checkNotNull(child.getSecond());


      final PanelProperties panelProperties = child.getSecond();

      if (panelProperties.borderLayoutPosition == BorderLayoutPosition.DEFAULT) {
        if (gridBagHelper != null) {

          gridBagHelper.add(
              child.getFirst(),
              panelProperties.columnSpan,
              panelProperties.anchor,
              panelProperties.fill);

        } else {
          panel.add(child.getFirst());
        }
      } else {
        switch (panelProperties.borderLayoutPosition) {
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
            Preconditions.checkState(layout != null);
            panel.add(child.getFirst());
            break;
        }
      }
    }
    if (horizontalAlignment != null) {
      panel.setAlignmentX(horizontalAlignment);
    }

    if (flowLayoutWrapper) {
      return JPanelBuilder.builder()
          .flowLayout()
          .add(panel)
          .build();
    }

    if (preferredHeight != null) {
      panel.setPreferredSize(new Dimension(panel.getWidth(), preferredHeight));
    }
    return panel;
  }

  /**
   * Sets the current layout manager to a GridBag. This helper method will do most of the work
   * of the gridbag for you, but the number of columns in the grid needs to be specified. After
   * this components can be added as normal, rows will wrap as needed when enough components are
   * added.
   *
   * @param gridBagHelperColumns The number of columns to be created before components will wrap
   *        to a new row.
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
    components.add(new Pair<>(child, new PanelProperties(BorderLayoutPosition.NORTH)));
    return this;
  }

  /**
   * Toggles a border layout, adds a given component to the 'east' portion of the border layout.
   */
  public JPanelBuilder addEast(final JComponent child) {
    layout = new BorderLayout();
    Preconditions.checkNotNull(child);
    components.add(new Pair<>(child, new PanelProperties(BorderLayoutPosition.EAST)));
    return this;
  }

  /**
   * Toggles a border layout, adds a given component to the 'west' portion of the border layout.
   */
  public JPanelBuilder addWest(final JComponent child) {
    layout = new BorderLayout();
    Preconditions.checkNotNull(child);
    components.add(new Pair<>(child, new PanelProperties(BorderLayoutPosition.WEST)));
    return this;
  }

  /**
   * Toggles a border layout, adds a given component to the 'center' portion of the border layout.
   */
  public JPanelBuilder addCenter(final JComponent child) {
    layout = new BorderLayout();
    components.add(new Pair<>(child, new PanelProperties(BorderLayoutPosition.CENTER)));
    return this;
  }

  /**
   * Adds a center component with padding.
   */
  public JPanelBuilder addCenter(final JComponent child, final Padding padding) {
    return addCenter(
        JPanelBuilder.builder()
            .verticalBoxLayout()
            .addVerticalStrut(padding.value)
            .add(JPanelBuilder.builder()
                .flowLayout()
                .addHorizontalStrut(padding.value)
                .add(child)
                .addHorizontalStrut(padding.value)
                .build())
            .addVerticalStrut(padding.value)
            .build());
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

  /**
   * Adds a 'titled' border to the current panel.
   */
  public JPanelBuilder borderTitled(final String title) {
    Preconditions.checkArgument(!Strings.nullToEmpty(title).isEmpty());
    border = new TitledBorder(title);
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

  public JPanelBuilder addEach(final Iterable<? extends Component> components) {
    components.forEach(this::add);
    return this;
  }

  /**
   * For each component supplied as a parameter, adds each one.
   */
  public JPanelBuilder addEach(final Component... component) {
    Preconditions.checkArgument(component.length > 0);
    Arrays.asList(component).forEach(this::add);
    return this;
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public JPanelBuilder addIf(final Optional<Component> component) {
    component.ifPresent(this::add);
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
    components.add(new Pair<>(component, new PanelProperties(BorderLayoutPosition.DEFAULT)));
    return this;
  }

  /**
   * An 'add' method to be used with the grid bag layout, can specify how many columns the component should span.
   */
  public JPanelBuilder add(final Component component, final GridBagHelper.ColumnSpan columnSpan) {
    components.add(new Pair<>(component, new PanelProperties(columnSpan)));
    return this;
  }


  public JPanelBuilder add(final Component component, final GridBagHelper.Anchor anchor,
      final GridBagHelper.Fill fill) {
    components.add(new Pair<>(component, new PanelProperties(anchor, fill)));
    return this;
  }


  public JPanelBuilder flowLayoutWrapper() {
    flowLayoutWrapper = true;
    return this;
  }

  /**
   * Assumes a border layout, adds a given component to the southern portion of the border layout
   * if the boolean parameter is true.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public JPanelBuilder addSouthIf(final Optional<JComponent> component) {
    component.ifPresent(this::addSouth);
    return this;
  }

  /**
   * Adds a given component to the southern portion of a border layout.
   */
  public JPanelBuilder addSouth(final JComponent child) {
    layout = new BorderLayout();
    Preconditions.checkNotNull(child);
    components.add(new Pair<>(child, new PanelProperties(BorderLayoutPosition.SOUTH)));
    return this;
  }

  public JPanelBuilder addLabel(final String text) {
    add(new JLabel(text));
    return this;
  }

  /**
   * Use this to fill up extra vertical space to avoid components stetching to fill up that space.
   */
  public JPanelBuilder addVerticalGlue() {
    add(JPanelBuilder.builder()
        .add(Box.createVerticalGlue())
        .build());
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

  public JPanelBuilder addHtmlLabel(final String s) {
    return addLabel("<html>" + s + "</html>");
  }

  public JPanelBuilder addEmptyLabel() {
    return addLabel("");
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

    PanelProperties(final ColumnSpan columnSpan) {
      this.columnSpan = columnSpan;
    }

    PanelProperties(final Anchor anchor, final Fill fill) {
      this.anchor = anchor;
      this.fill = fill;
    }
  }

  /**
   * Class that represents simple top/bottom + left/right padding.
   */
  public static class Padding {
    private final int value;

    private Padding(final int value) {
      this.value = value;
    }

    public static Padding of(final int value) {
      return new Padding(value);
    }
  }
}
