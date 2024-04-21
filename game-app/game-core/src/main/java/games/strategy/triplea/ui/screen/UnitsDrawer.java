package games.strategy.triplea.ui.screen;

import static games.strategy.triplea.image.UnitImageFactory.ImageKey;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.AbstractDrawable;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;
import java.util.function.Predicate;
import lombok.Getter;

/**
 * Draws units for the associated territory.
 *
 * <p>If all units cannot be drawn within the territory bounds, they will be drawn in a single
 * horizontal row, overflowing to the right of the territory. A solid black line, rooted at the
 * territory's default placement point, will be drawn under all units in this case.
 */
public class UnitsDrawer extends AbstractDrawable {
  private final int count;
  private final String unitType;
  private final String playerName;
  @Getter private final Point placementPoint;
  private final int damaged;
  private final int bombingUnitDamage;
  private final boolean disabled;
  private final boolean overflow;
  private final String territoryName;
  private final UiContext uiContext;

  /** Identifies the location where a nation flag is drawn relative to a unit. */
  public enum UnitFlagDrawMode {
    NONE,
    SMALL_FLAG,
    LARGE_FLAG,
  }

  public UnitsDrawer(
      final int count,
      final String unitType,
      final String playerName,
      final Point placementPoint,
      final int damaged,
      final int bombingUnitDamage,
      final boolean disabled,
      final boolean overflow,
      final String territoryName,
      final UiContext uiContext) {
    this.count = count;
    this.unitType = unitType;
    this.playerName = playerName;
    this.placementPoint = placementPoint;
    this.damaged = damaged;
    this.bombingUnitDamage = bombingUnitDamage;
    this.disabled = disabled;
    this.overflow = overflow;
    this.territoryName = territoryName;
    this.uiContext = uiContext;
  }

  public Rectangle getPlacementRectangle() {
    UnitImageFactory factory = uiContext.getUnitImageFactory();
    return new Rectangle(
        placementPoint.x,
        placementPoint.y,
        factory.getUnitImageWidth(),
        factory.getUnitImageHeight());
  }

  public String getPlayer() {
    return playerName;
  }

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    // If there are too many Units at one point a black line is drawn to make clear which units
    // belong to where
    final var factory = uiContext.getUnitImageFactory();
    if (overflow) {
      graphics.setColor(Color.BLACK);
      graphics.fillRect(
          placementPoint.x - bounds.x - 2,
          placementPoint.y - bounds.y + factory.getUnitImageHeight(),
          factory.getUnitImageWidth() + 2,
          3);
    }
    final UnitType type = data.getUnitTypeList().getUnitType(unitType);
    if (type == null) {
      throw new IllegalStateException("Type not found:" + unitType);
    }
    final GamePlayer owner = data.getPlayerList().getPlayerId(playerName);
    final boolean damagedImage = damaged > 0 || bombingUnitDamage > 0;

    final var imageKey =
        ImageKey.builder()
            .type(type)
            .player(owner)
            .damaged(damagedImage)
            .disabled(disabled)
            .build();

    final Image img = factory.getImage(imageKey);
    final int maxRange = new Unit(type, owner, data).getMaxMovementAllowed();

    final UnitFlagDrawMode drawMode =
        ClientSetting.unitFlagDrawMode.getValue().orElse(UnitFlagDrawMode.NONE);

    if (drawMode == UnitFlagDrawMode.LARGE_FLAG) {
      // If unit is not in the "excluded list" it will get drawn
      if (maxRange != 0) {
        final Image flag = uiContext.getFlagImageFactory().getFlag(owner);
        final int xOffset = img.getWidth(null) / 2 - flag.getWidth(null) / 2;
        final int yOffset = img.getHeight(null) / 2 - flag.getHeight(null) / 4 - 5;
        graphics.drawImage(
            flag,
            (placementPoint.x - bounds.x) + xOffset,
            (placementPoint.y - bounds.y) + yOffset,
            null);
      }
      drawUnit(graphics, img, bounds);
    } else if (drawMode == UnitFlagDrawMode.SMALL_FLAG) {
      drawUnit(graphics, img, bounds);
      // If unit is not in the "excluded list" it will get drawn
      if (maxRange != 0) {
        final Image flag = uiContext.getFlagImageFactory().getSmallFlag(owner);
        final int xOffset = img.getWidth(null) - flag.getWidth(null);
        final int yOffset = img.getHeight(null) - flag.getHeight(null);
        // This Method draws the Flag in the lower right corner of the unit image. Since the
        // position is the upper left corner we have to move the picture up by the height and
        // left by the width.
        graphics.drawImage(
            flag,
            (placementPoint.x - bounds.x) + xOffset,
            (placementPoint.y - bounds.y) + yOffset,
            null);
      }
    } else {
      drawUnit(graphics, img, bounds);
    }

    // more than 1 unit of this category
    if (count != 1) {
      final int stackSize = mapData.getDefaultUnitsStackSize();
      if (stackSize > 0) { // Display more units as a stack
        for (int i = 1; i < count && i < stackSize; i++) {
          graphics.drawImage(
              img, placementPoint.x + 2 * i - bounds.x, placementPoint.y - 2 * i - bounds.y, null);
        }
        if (count > stackSize) {
          final String s = String.valueOf(count);

          drawOutlinedText(
              graphics,
              s,
              placementPoint.x - bounds.x + 2 * stackSize + factory.getUnitImageWidth() * 6 / 10,
              placementPoint.y - 2 * stackSize - bounds.y + factory.getUnitImageHeight() / 3,
              MapImage.getPropertyUnitCountColor(),
              MapImage.getPropertyUnitCountOutline());
        }
      } else { // Display a white number at the bottom of the unit
        final String s = String.valueOf(count);
        drawOutlinedText(
            graphics,
            s,
            placementPoint.x - bounds.x + factory.getUnitCounterOffsetWidth(),
            placementPoint.y - bounds.y + factory.getUnitCounterOffsetHeight(),
            MapImage.getPropertyUnitCountColor(),
            MapImage.getPropertyUnitCountOutline());
      }
    }
    displayHitDamage(bounds, graphics);
    // Display Factory Damage
    if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data.getProperties())
        && Matches.unitTypeCanBeDamaged().test(type)) {
      displayFactoryDamage(bounds, graphics);
    }
  }

  /** This draws the given image onto the given graphics object. */
  private void drawUnit(final Graphics2D graphics, final Image image, final Rectangle bounds) {
    graphics.drawImage(image, placementPoint.x - bounds.x, placementPoint.y - bounds.y, null);

    // draw unit icons in top right corner
    final List<Image> unitIcons =
        uiContext.getUnitIconImageFactory().getImages(playerName, unitType);
    for (final Image unitIcon : unitIcons) {
      final int xOffset = image.getWidth(null) - unitIcon.getWidth(null);
      graphics.drawImage(
          unitIcon, (placementPoint.x - bounds.x) + xOffset, (placementPoint.y - bounds.y), null);
    }
  }

  private void displayHitDamage(final Rectangle bounds, final Graphics2D graphics) {
    if (!territoryName.isEmpty() && damaged > 1) {
      final String s = String.valueOf(damaged);
      final var factory = uiContext.getUnitImageFactory();
      drawOutlinedText(
          graphics,
          s,
          placementPoint.x - bounds.x + factory.getUnitImageWidth() * 3 / 4,
          placementPoint.y - bounds.y + factory.getUnitImageHeight() / 4,
          MapImage.getPropertyUnitHitDamageColor(),
          MapImage.getPropertyUnitHitDamageOutline());
    }
  }

  private void displayFactoryDamage(final Rectangle bounds, final Graphics2D graphics) {
    if (!territoryName.isEmpty() && bombingUnitDamage > 0) {
      final String s = String.valueOf(bombingUnitDamage);
      final var factory = uiContext.getUnitImageFactory();
      drawOutlinedText(
          graphics,
          s,
          placementPoint.x - bounds.x + factory.getUnitImageWidth() / 4,
          placementPoint.y - bounds.y + factory.getUnitImageHeight() / 4,
          MapImage.getPropertyUnitFactoryDamageColor(),
          MapImage.getPropertyUnitFactoryDamageOutline());
    }
  }

  public static void drawOutlinedText(
      final Graphics2D graphics,
      final String s,
      final int x,
      final int y,
      final Color textColor,
      final Color outlineColor) {
    final var font = MapImage.getPropertyMapFont();
    if (font.getSize() > 0) {
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setFont(font);
      graphics.setColor(outlineColor);
      graphics.drawString(s, x - 1, y - 1);
      graphics.drawString(s, x - 1, y + 1);
      graphics.drawString(s, x + 1, y + 1);
      graphics.drawString(s, x + 1, y - 1);
      graphics.setColor(textColor);
      graphics.drawString(s, x, y);
    }
  }

  List<Unit> getUnits(final GameState data) {
    // note - it may be the case where the territory is being changed as a result to a mouse click,
    // and the map units haven't updated yet, so the unit count from the territory won't match the
    // units in count
    final Territory t = data.getMap().getTerritory(territoryName);
    final UnitType type = data.getUnitTypeList().getUnitType(unitType);
    final Predicate<Unit> selectedUnits =
        Matches.unitIsOfType(type)
            .and(Matches.unitIsOwnedBy(data.getPlayerList().getPlayerId(playerName)))
            .and(
                damaged > 0 ? Matches.unitHasTakenSomeDamage() : Matches.unitHasNotTakenAnyDamage())
            .and(
                bombingUnitDamage > 0
                    ? Matches.unitHasTakenSomeBombingUnitDamage()
                    : Matches.unitHasNotTakenAnyBombingUnitDamage());
    return t.getMatches(selectedUnits);
  }

  public Territory getTerritory(GameData data) {
    return data.getMap().getTerritory(territoryName);
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.UNITS_LEVEL;
  }
}
