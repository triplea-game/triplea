package games.strategy.triplea.ui.unit.scroller;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.image.FlagIconImageFactory;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.MapPanel;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.ui.OverlayIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Draws the unit avatar panel for the unit scroller. The avatar panel contains representative
 * images of the units contained in the 'current' territory.
 */
class AvatarPanelFactory {
  static final int MAX_UNITS_IN_AVATAR_STACK = 4;

  private static final int TYPICAL_UNIT_IMAGE_SIZE = 40;

  /** Height difference between overlapping images. */
  private static final int HEIGHT_OFFSET = 10;

  private static final int WIDTH_OFFSET = 15;

  private final UnitImageFactory unitImageFactory;
  private final FlagIconImageFactory flagIconImageFactory;

  AvatarPanelFactory(final MapPanel mapPanel) {
    unitImageFactory = mapPanel.getUiContext().getUnitImageFactory();
    flagIconImageFactory = mapPanel.getUiContext().getFlagImageFactory();
  }

  JLabel buildPanel(final List<Unit> units, final PlayerId currentPlayer) {
    final Icon flaggedUnitIcon;
    if (units.isEmpty()) {
      flaggedUnitIcon = new ImageIcon(createEmptyUnitStackImage());
    } else {
      final Unit firstUnit = units.iterator().next();
      final UnitCategory unitCategory = new UnitCategory(firstUnit.getType(), currentPlayer);
      final ImageIcon unitIcon =
          new ImageIcon(createUnitStackImage(unitImageFactory, currentPlayer, units));
      final ImageIcon flagIcon =
          new ImageIcon(flagIconImageFactory.getSmallFlag(unitCategory.getOwner()));
      // overlay flag onto upper-right of icon
      flaggedUnitIcon =
          new OverlayIcon(
              unitIcon, flagIcon, unitIcon.getIconWidth() - (flagIcon.getIconWidth() / 2), 0);
    }
    final JLabel unitImage =
        new JLabel(" x" + units.size(), flaggedUnitIcon, SwingConstants.CENTER);
    unitImage.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    return unitImage;
  }

  private static Image createEmptyUnitStackImage() {
    return new BufferedImage(
        TYPICAL_UNIT_IMAGE_SIZE + (MAX_UNITS_IN_AVATAR_STACK * WIDTH_OFFSET),
        TYPICAL_UNIT_IMAGE_SIZE + (MAX_UNITS_IN_AVATAR_STACK * HEIGHT_OFFSET),
        BufferedImage.TYPE_INT_ARGB);
  }

  private static Image createUnitStackImage(
      final UnitImageFactory unitImageFactory, final PlayerId player, final List<Unit> units) {

    Preconditions.checkArgument(!units.isEmpty());

    final var unitsToDraw = UnitScrollerModel.getUniqueUnitCategories(player, units);

    final var dimension = unitImageFactory.getImageDimensions(unitsToDraw.get(0).getType(), player);

    final var combinedImage =
        new BufferedImage(
            dimension.width + (MAX_UNITS_IN_AVATAR_STACK * WIDTH_OFFSET),
            dimension.height + (MAX_UNITS_IN_AVATAR_STACK * HEIGHT_OFFSET),
            BufferedImage.TYPE_INT_ARGB);

    final var graphics = combinedImage.getGraphics();
    for (int i = 0; i < unitsToDraw.size(); i++) {
      final int x = i * WIDTH_OFFSET;
      final int y = i * HEIGHT_OFFSET;
      final ImageObserver observer = null;
      graphics.drawImage(unitImageFactory.getImage(unitsToDraw.get(i)), x, y, observer);
    }
    return combinedImage;
  }
}
