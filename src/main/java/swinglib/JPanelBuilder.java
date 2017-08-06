package swinglib;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.apache.commons.math3.util.Pair;

import com.google.common.base.Preconditions;

import games.strategy.debug.ClientLogger;
import games.strategy.triplea.ResourceLoader;

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
  private Float xAlignment;
  private String backgroundImage;
  private Border border;
  private LayoutManager layout;
  private BoxLayoutType boxLayoutType = null;

  private boolean useGridBagHelper = false;
  private int gridBagHelperColumns;


  private JPanelBuilder() {}

  public static JPanelBuilder builder() {
    return new JPanelBuilder();
  }

  /**
   * Constructs a Swing JPanel using current builder values.
   * Values that must be set: (requires no values to be set)
   */
  public JPanel build() {

    JPanel panel = new JPanel();

    panel.setOpaque(false);

    if (backgroundImage != null) {
      final URL backgroundImageUrl = ResourceLoader.getGameEngineAssetLoader().getResource(backgroundImage);

      try {
        final BufferedImage bufferedImaged = ImageIO.read(backgroundImageUrl);
        panel = new JPanel() {
          private static final long serialVersionUID = -5926048824903223767L;

          @Override
          protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            g.drawImage(bufferedImaged, 50, 200, null);
          }
        };
      } catch (final IOException e) {
        ClientLogger.logError("Could not load image: " + backgroundImage, e);
      }
    }


    if (border != null) {
      panel.setBorder(border);
    }

    if (layout != null) {
    } else if (boxLayoutType != null) {
      final int boxDirection = boxLayoutType == BoxLayoutType.HORIZONTAL ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS;
      layout = new BoxLayout(panel, boxDirection);
    } else {
      layout =new BorderLayout();
    }

    panel.setLayout(layout);

    GridBagHelper gridBagHelper = null;
    if (useGridBagHelper) {
      gridBagHelper = new GridBagHelper(panel, gridBagHelperColumns);
    }

    for (final Pair<Component, BorderLayoutPosition> child : components) {
      if (child.getSecond() == BorderLayoutPosition.DEFAULT) {
        if (gridBagHelper != null) {
          gridBagHelper.add(child.getFirst());
        } else {
          panel.add(child.getFirst());
        }
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
            Preconditions.checkState(layout != null);
            panel.add(child.getFirst());
            break;
        }
      }
    }
    if (xAlignment != null) {
      panel.setAlignmentX(xAlignment);
    }

    return panel;
  }

  public JPanelBuilder gridBagLayout() {
    layout = new GridBagLayout();
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

  public JPanelBuilder addWestIf(final boolean condition, final Supplier<Component> componentSupplier) {
    if(condition) {
      addWest(componentSupplier.get());
    }
    return this;
  }

  public JPanelBuilder addCenterIf(final boolean condition, final Supplier<Component> componentSupplier) {
    if(condition) {
      addCenter(componentSupplier.get());
    }
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
    layout = new GridLayout(rows, columns);
    return this;
  }

  public JPanelBuilder borderEmpty(final int borderWidth) {
    border = new EmptyBorder(borderWidth, borderWidth, borderWidth, borderWidth);
    return this;
  }

  public JPanelBuilder borderEtched() {
    border = new EtchedBorder();
    return this;
  }

  public JPanelBuilder horizontalAlignmentCenter() {
    this.xAlignment = JComponent.CENTER_ALIGNMENT;
    return this;
  }

  public JPanelBuilder backgroundImage(final String imagePath) {
    this.backgroundImage = imagePath;
    return this;
  }

  public JPanelBuilder flowLayout() {
    layout = new FlowLayout();
    return this;
  }

  public JPanelBuilder addEach(final List<? extends Component> components) {
    components.forEach(this::add);
    return this;
  }

  public JPanelBuilder addEach(final Component component, final Component other) {
    add(component);
    add(other);
    return this;
  }

  public <T> JPanelBuilder addEach(final Iterable<? extends T> components,
      final Function<T, Component> componentFunction) {
    components.forEach(value -> this.add(componentFunction.apply(value)));
    return this;
  }

  public JPanelBuilder withGridBagHelper(final int gridBagHelperColumns) {
    this.useGridBagHelper = true;
    this.gridBagHelperColumns = gridBagHelperColumns;
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

  public enum BorderLayoutPosition {
    DEFAULT, CENTER, SOUTH, NORTH, WEST, EAST
  }

  public enum BorderType {
    EMPTY, ETCHED
  }
}
