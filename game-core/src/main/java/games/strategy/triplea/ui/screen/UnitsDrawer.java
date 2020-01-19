package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.AbstractDrawable;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.extern.java.Log;
import org.triplea.util.Tuple;

/**
 * Draws units for the associated territory.
 *
 * <p>If all units cannot be drawn within the territory bounds, they will be drawn in a single
 * horizontal row, overflowing to the right of the territory. A solid black line, rooted at the
 * territory's default placement point, will be drawn under all units in this case.
 */
@Log
public class UnitsDrawer extends AbstractDrawable {
  private final int count;
  private final String unitType;
  private final String playerName;
  private final Point placementPoint;
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
    LARGE_FLAG;

    public UnitFlagDrawMode nextDrawMode() {
      final var values = values();
      return values[(ordinal() + 1) % values.length];
    }
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

  public Point getPlacementPoint() {
    return placementPoint;
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
    if (overflow) {
      graphics.setColor(Color.BLACK);
      graphics.fillRect(
          placementPoint.x - bounds.x - 2,
          placementPoint.y - bounds.y + uiContext.getUnitImageFactory().getUnitImageHeight(),
          uiContext.getUnitImageFactory().getUnitImageWidth() + 2,
          3);
    }
    final UnitType type = data.getUnitTypeList().getUnitType(unitType);
    if (type == null) {
      throw new IllegalStateException("Type not found:" + unitType);
    }
    final GamePlayer owner = data.getPlayerList().getPlayerId(playerName);
    final Optional<Image> img =
        uiContext
            .getUnitImageFactory()
            .getImage(type, owner, damaged > 0 || bombingUnitDamage > 0, disabled);

    if (img.isEmpty() && !uiContext.isShutDown()) {
      log.severe(
          "MISSING UNIT IMAGE (won't be displayed): "
              + type.getName()
              + " owned by "
              + owner.getName()
              + " in "
              + territoryName);
    }

    final int maxRange = new TripleAUnit(type, owner, data).getMaxMovementAllowed();

    final UnitFlagDrawMode drawMode =
        ClientSetting.unitFlagDrawMode.getValue().orElse(UnitFlagDrawMode.NONE);
    if (img.isPresent()) {
      if (drawMode == UnitFlagDrawMode.LARGE_FLAG) {
        // If unit is not in the "excluded list" it will get drawn
        if (maxRange != 0) {
          final Image flag = uiContext.getFlagImageFactory().getFlag(owner);
          final int xoffset = img.get().getWidth(null) / 2 - flag.getWidth(null) / 2;
          final int yoffset = img.get().getHeight(null) / 2 - flag.getHeight(null) / 4 - 5;
          graphics.drawImage(
              flag,
              (placementPoint.x - bounds.x) + xoffset,
              (placementPoint.y - bounds.y) + yoffset,
              null);
        }
        drawUnit(graphics, img.get(), bounds, data);
      } else if (drawMode == UnitFlagDrawMode.SMALL_FLAG) {
        drawUnit(graphics, img.get(), bounds, data);
        // If unit is not in the "excluded list" it will get drawn
        if (maxRange != 0) {
          final Image flag = uiContext.getFlagImageFactory().getSmallFlag(owner);
          final int xoffset = img.get().getWidth(null) - flag.getWidth(null);
          final int yoffset = img.get().getHeight(null) - flag.getHeight(null);
          // This Method draws the Flag in the lower right corner of the unit image. Since the
          // position is the upper
          // left corner we have to move the picture up by the height and left by the width.
          graphics.drawImage(
              flag,
              (placementPoint.x - bounds.x) + xoffset,
              (placementPoint.y - bounds.y) + yoffset,
              null);
        }
      } else {
        drawUnit(graphics, img.get(), bounds, data);
      }
    }

    // more then 1 unit of this category
    if (count != 1) {
      final int stackSize = mapData.getDefaultUnitsStackSize();
      if (stackSize > 0) { // Display more units as a stack
        for (int i = 1; i < count && i < stackSize; i++) {
          if (img.isPresent()) {
            graphics.drawImage(
                img.get(),
                placementPoint.x + 2 * i - bounds.x,
                placementPoint.y - 2 * i - bounds.y,
                null);
          }
        }
        if (count > stackSize) {
          final String s = String.valueOf(count);
          final int x =
              placementPoint.x
                  - bounds.x
                  + 2 * stackSize
                  + uiContext.getUnitImageFactory().getUnitImageWidth() * 6 / 10;
          final int y =
              placementPoint.y
                  - 2 * stackSize
                  - bounds.y
                  + uiContext.getUnitImageFactory().getUnitImageHeight() / 3;
          drawOutlinedText(
              graphics,
              s,
              x,
              y,
              MapImage.getPropertyMapFont(),
              MapImage.getPropertyUnitCountColor(),
              MapImage.getPropertyUnitCountOutline());
        }
      } else { // Display a white number at the bottom of the unit
        final String s = String.valueOf(count);
        final int x =
            placementPoint.x
                - bounds.x
                + uiContext.getUnitImageFactory().getUnitCounterOffsetWidth();
        final int y =
            placementPoint.y
                - bounds.y
                + uiContext.getUnitImageFactory().getUnitCounterOffsetHeight();
        drawOutlinedText(
            graphics,
            s,
            x,
            y,
            MapImage.getPropertyMapFont(),
            MapImage.getPropertyUnitCountColor(),
            MapImage.getPropertyUnitCountOutline());
      }
    }
    displayHitDamage(bounds, graphics);
    // Display Factory Damage
    if (isDamageFromBombingDoneToUnitsInsteadOfTerritories(data)
        && Matches.unitTypeCanBeDamaged().test(type)) {
      displayFactoryDamage(bounds, graphics);
    }
  }

  /** This draws the given image onto the given graphics object. */
  private void drawUnit(
      final Graphics2D graphics, final Image image, final Rectangle bounds, final GameData data) {
    graphics.drawImage(image, placementPoint.x - bounds.x, placementPoint.y - bounds.y, null);

    // draw unit icons in top right corner
    final List<Image> unitIcons =
        uiContext.getUnitIconImageFactory().getImages(playerName, unitType, data);
    for (final Image unitIcon : unitIcons) {
      final int xoffset = image.getWidth(null) - unitIcon.getWidth(null);
      graphics.drawImage(
          unitIcon, (placementPoint.x - bounds.x) + xoffset, (placementPoint.y - bounds.y), null);
    }
  }

  private void displayHitDamage(final Rectangle bounds, final Graphics2D graphics) {
    if (territoryName.length() != 0 && damaged > 1) {
      final String s = String.valueOf(damaged);
      final int x =
          placementPoint.x - bounds.x + uiContext.getUnitImageFactory().getUnitImageWidth() * 3 / 4;
      final int y =
          placementPoint.y - bounds.y + uiContext.getUnitImageFactory().getUnitImageHeight() / 4;
      drawOutlinedText(
          graphics,
          s,
          x,
          y,
          MapImage.getPropertyMapFont(),
          MapImage.getPropertyUnitHitDamageColor(),
          MapImage.getPropertyUnitHitDamageOutline());
    }
  }

  private void displayFactoryDamage(final Rectangle bounds, final Graphics2D graphics) {
    if (territoryName.length() != 0 && bombingUnitDamage > 0) {
      final String s = String.valueOf(bombingUnitDamage);
      final int x =
          placementPoint.x - bounds.x + uiContext.getUnitImageFactory().getUnitImageWidth() / 4;
      final int y =
          placementPoint.y - bounds.y + uiContext.getUnitImageFactory().getUnitImageHeight() / 4;
      drawOutlinedText(
          graphics,
          s,
          x,
          y,
          MapImage.getPropertyMapFont(),
          MapImage.getPropertyUnitFactoryDamageColor(),
          MapImage.getPropertyUnitFactoryDamageOutline());
    }
  }

  private static void drawOutlinedText(
      final Graphics2D graphics,
      final String s,
      final int x,
      final int y,
      final Font font,
      final Color textColor,
      final Color outlineColor) {
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

  Tuple<Territory, List<Unit>> getUnits(final GameData data) {
    // note - it may be the case where the territory is being changed as a result to a mouse click,
    // and the map units
    // haven't updated yet, so the unit count from the territory wont match the units in count
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
    return Tuple.of(t, t.getUnitCollection().getMatches(selectedUnits));
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.UNITS_LEVEL;
  }

  @Override
  public String toString() {
    return "UnitsDrawer for "
        + count
        + " "
        + MyFormatter.pluralize(unitType)
        + " in  "
        + territoryName;
  }

  private static boolean isDamageFromBombingDoneToUnitsInsteadOfTerritories(final GameData data) {
    return Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data);
  }
}
