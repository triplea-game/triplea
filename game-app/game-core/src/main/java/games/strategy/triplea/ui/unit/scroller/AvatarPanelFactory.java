package games.strategy.triplea.ui.unit.scroller;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.image.UnitImageFactory.ImageKey;
import games.strategy.triplea.ui.panels.map.MapPanel;
import games.strategy.triplea.ui.screen.UnitsDrawer;
import games.strategy.triplea.util.UnitCategory;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Comparator;
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
   * Max rendering width is so that the unit scroller image does not stretch too wide. On top of
   * that, once an image has been rendered, the minimum size of the right hand action panels will be
   * equal to the rendering width.
   */
  private static final int MAX_RENDERING_WIDTH = 300;

  /**
   * Add some extra height to the unit avatar image. This gives us some padding on the top and
   * bottom of the avatar image and helps the unit count rendering at the bottom of the avatar image
   * from being cut-off (test this with territories containing multiple subs on NWO).
   */
  private static final int EXTRA_DRAW_HEIGHT = 12;

  /**
   * This value nudges the rendering of units down a bit. If a unit image uses the full image draw
   * height, the top part can appear cut off or just really close to the maximum upper limit. This
   * value moves the unit 'down' a bit when rendering it (test this with battleships on NWO).
   */
  private static final int UNIT_DRAW_ADDED_VERTICAL_TRANSLATION = 7;

  /**
   * This value nudges the rendering of the unit count down a bit. Since we shift the rendering of
   * unit images down, this does not look good for infantry where instead of the unit count being
   * drawn between their feet, it is drawn squarely between their legs (which looks funny). So we
   * translate the rendering to be lower by a bit. (test this with territories containing multiple
   * infantry).
   */
  private static final int UNIT_COUNT_ADDED_VERTICAL_TRANSLATION = 4;

  private final UnitImageFactory unitImageFactory;

  AvatarPanelFactory(final MapPanel mapPanel) {
    unitImageFactory = mapPanel.getUiContext().getUnitImageFactory().withScaleFactor(1.0);
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

  private Image createEmptyUnitStackImage(final int renderingWidth) {
    return new BufferedImage(
        renderingWidth,
        unitImageFactory.getUnitImageHeight() + EXTRA_DRAW_HEIGHT,
        BufferedImage.TYPE_INT_ARGB);
  }

  private static Image createUnitStackImage(
      final UnitImageFactory unitImageFactory,
      final GamePlayer player,
      final List<Unit> units,
      final int renderingWidth) {

    Preconditions.checkArgument(!units.isEmpty());

    final var unitsToDraw = UnitScrollerModel.getUniqueUnitCategories(player, units);

    final var dimension =
        unitImageFactory.getImageDimensions(
            ImageKey.builder().type(unitsToDraw.get(0).getType()).player(player).build());

    final var combinedImage =
        new BufferedImage(
            renderingWidth,
            unitImageFactory.getUnitImageHeight() + EXTRA_DRAW_HEIGHT,
            BufferedImage.TYPE_INT_ARGB);

    final Graphics2D graphics = (Graphics2D) combinedImage.getGraphics();

    final List<Point> drawLocations =
        AvatarCoordinateCalculator.builder()
            .unitImageWidth(dimension.width)
            .unitImageHeight(dimension.height)
            .unitImageCount(unitsToDraw.size())
            .renderingWidth(renderingWidth)
            .renderingHeight(unitImageFactory.getUnitImageHeight())
            .build()
            .computeDrawCoordinates();

    Postconditions.assertState(
        drawLocations.size() == unitsToDraw.size(),
        String.format(
            "Draw location count (%s) should have matched units draw size (%s)",
            drawLocations.size(), unitsToDraw.size()));

    unitsToDraw.sort(unitRenderingOrder(player));
    for (int i = 0; i < drawLocations.size(); i++) {
      final var unitToDraw = unitsToDraw.get(i);
      final var imageToDraw = unitImageFactory.getImage(ImageKey.of(unitToDraw));

      final Point drawLocation = drawLocations.get(i);
      graphics.drawImage(
          imageToDraw, drawLocation.x, drawLocation.y + UNIT_DRAW_ADDED_VERTICAL_TRANSLATION, null);

      final int unitCount = countUnit(unitsToDraw.get(i).getType(), units);
      if (unitCount > 1) {
        UnitsDrawer.drawOutlinedText(
            graphics,
            String.valueOf(unitCount),
            drawLocation.x + unitImageFactory.getUnitCounterOffsetWidth(),
            drawLocation.y
                + unitImageFactory.getUnitCounterOffsetHeight()
                + UNIT_COUNT_ADDED_VERTICAL_TRANSLATION,
            MapImage.getPropertyUnitCountColor(),
            MapImage.getPropertyUnitCountOutline());
      }
    }
    return combinedImage;
  }

  private static Comparator<UnitCategory> unitRenderingOrder(final GamePlayer currentPlayer) {
    final Comparator<UnitCategory> isAir =
        Comparator.comparing(unitCategory -> unitCategory.getUnitAttachment().getIsAir());
    final Comparator<UnitCategory> isSea =
        Comparator.comparing(unitCategory -> unitCategory.getUnitAttachment().getIsSea());
    final Comparator<UnitCategory> unitAttackPower =
        Comparator.comparingInt(
            unitCategory -> unitCategory.getUnitAttachment().getAttack(currentPlayer));
    final Comparator<UnitCategory> unitName =
        Comparator.comparing(unitCategory -> unitCategory.getType().getName());

    return isAir //
        .thenComparing(isSea)
        .thenComparing(unitAttackPower)
        .thenComparing(unitName)
        .reversed();
  }

  private static int countUnit(final UnitType unitType, final Collection<Unit> units) {
    return units.stream() //
        .map(Unit::getType)
        .mapToInt(type -> type.equals(unitType) ? 1 : 0)
        .sum();
  }
}
