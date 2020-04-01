package games.strategy.triplea.ui.unit.scroller;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Draws the unit avatar panel for the unit scroller. The avatar panel contains representative
 * images of the units contained in the 'current' territory.
 */
class AvatarPanelFactory {
  static final int MAX_UNITS_IN_AVATAR_STACK = 3;

  private static final int TYPICAL_UNIT_IMAGE_SIZE = 40;

  /** Height difference between overlapping images. */
  private static final int HEIGHT_OFFSET = 12;

  private static final int WIDTH_OFFSET = 12;

  private final UnitImageFactory unitImageFactory;

  AvatarPanelFactory(final MapPanel mapPanel) {
    unitImageFactory = mapPanel.getUiContext().getUnitImageFactory();
  }

  JPanel buildPanel(final List<Unit> units, final GamePlayer currentPlayer) {
    final Icon unitIcon =
        units.isEmpty()
            ? new ImageIcon(createEmptyUnitStackImage())
            : new ImageIcon(createUnitStackImage(unitImageFactory, currentPlayer, units));

    return new JPanelBuilder() //
        .borderLayout()
        .addCenter(new JLabel(unitIcon, SwingConstants.CENTER))
        .build();
  }

  private static Image createEmptyUnitStackImage() {
    return new BufferedImage(
        TYPICAL_UNIT_IMAGE_SIZE + (MAX_UNITS_IN_AVATAR_STACK * WIDTH_OFFSET),
        TYPICAL_UNIT_IMAGE_SIZE + (MAX_UNITS_IN_AVATAR_STACK * HEIGHT_OFFSET),
        BufferedImage.TYPE_INT_ARGB);
  }

  private static Image createUnitStackImage(
      final UnitImageFactory unitImageFactory, final GamePlayer player, final List<Unit> units) {

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
