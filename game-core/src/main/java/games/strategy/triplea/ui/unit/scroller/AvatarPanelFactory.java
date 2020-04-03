package games.strategy.triplea.ui.unit.scroller;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.triplea.java.Postconditions;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Draws the unit avatar panel for the unit scroller. The avatar panel contains representative
 * images of the units contained in the 'current' territory.
 */
class AvatarPanelFactory {
  /**
   * Set to be about the maximum height of unit images. Note, unit images can be scaled up and down.
   */
  private static final int PANEL_HEIGHT = 70;

  /**
   * Max rendering width is so that the unit scroller image does not stretch too wide. On top of
   * that, once an image has been rendered, the minimum size of the right hand action panels will be
   * equal to the rendering width.
   */
  private static final int MAX_RENDERING_WIDTH = 300;

  private final UnitImageFactory unitImageFactory;

  AvatarPanelFactory(final MapPanel mapPanel) {
    unitImageFactory = mapPanel.getUiContext().getUnitImageFactory();
  }

  /**
   * Draws the unit 'avatar' image and returns it on a panel.
   *
   * @param units The units to be drawn.
   * @param currentPlayer The players whose turn it is.
   * @param panelWidth How much horizontal space we have for drawing.
   * @return A panel containing a drawing of the unique images for each unit type.
   */
  JPanel buildPanel(final List<Unit> units, final GamePlayer currentPlayer, final int panelWidth) {
    final int renderingWidth = Math.min(panelWidth, MAX_RENDERING_WIDTH);

    final Icon unitIcon =
        units.isEmpty()
            ? new ImageIcon(createEmptyUnitStackImage(renderingWidth))
            : new ImageIcon(
                createUnitStackImage(unitImageFactory, currentPlayer, units, renderingWidth));

    return new JPanelBuilder() //
        .borderLayout()
        .addCenter(new JLabel(unitIcon, SwingConstants.CENTER))
        .build();
  }

  private static Image createEmptyUnitStackImage(final int renderingWidth) {
    return new BufferedImage(renderingWidth, PANEL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
  }

  private static Image createUnitStackImage(
      final UnitImageFactory unitImageFactory,
      final GamePlayer player,
      final List<Unit> units,
      final int renderingWidth) {

    Preconditions.checkArgument(!units.isEmpty());

    final var unitsToDraw = UnitScrollerModel.getUniqueUnitCategories(player, units);

    final var dimension = unitImageFactory.getImageDimensions(unitsToDraw.get(0).getType(), player);

    final var combinedImage =
        new BufferedImage(renderingWidth, PANEL_HEIGHT, BufferedImage.TYPE_INT_ARGB);

    final var graphics = combinedImage.getGraphics();

    final List<Point> drawLocations =
        AvatarCoordinateCalculator.builder()
            .unitImageWidth(dimension.width)
            .unitImageHeight(dimension.height)
            .unitImageCount(unitsToDraw.size())
            .renderingWidth(renderingWidth)
            .renderingHeight(PANEL_HEIGHT)
            .build()
            .computeDrawCoordinates();

    Postconditions.assertState(
        drawLocations.size() == unitsToDraw.size(),
        String.format(
            "Draw location count (%s) should have matched units draw size (%s)",
            drawLocations.size(), unitsToDraw.size()));

    for (int i = 0; i < drawLocations.size(); i++) {
      final var imageToDraw = unitImageFactory.getImage(unitsToDraw.get(i));
      final Point drawLocation = drawLocations.get(i);
      graphics.drawImage(imageToDraw, drawLocation.x, drawLocation.y, null);
    }
    return combinedImage;
  }
}
