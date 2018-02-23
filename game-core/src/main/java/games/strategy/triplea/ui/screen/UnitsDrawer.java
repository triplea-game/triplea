package games.strategy.triplea.ui.screen;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.IDrawable;
import games.strategy.util.Tuple;

public class UnitsDrawer implements IDrawable {
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
  private static UnitFlagDrawMode drawUnitNationMode = UnitFlagDrawMode.NEXT_TO;

  public enum PreferenceKeys {
    DRAW_MODE, DRAWING_ENABLED
  }

  public static boolean enabledFlags = false;

  public enum UnitFlagDrawMode {
    BELOW, NEXT_TO
  }

  public UnitsDrawer(final int count, final String unitType, final String playerName, final Point placementPoint,
      final int damaged, final int bombingUnitDamage, final boolean disabled, final boolean overflow,
      final String territoryName, final UiContext uiContext) {
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
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {

    // If there are too many Units at one point a black line is drawn to make clear which units belong to where
    if (overflow) {
      graphics.setColor(Color.BLACK);
      graphics.fillRect(placementPoint.x - bounds.x - 2,
          (placementPoint.y - bounds.y) + uiContext.getUnitImageFactory().getUnitImageHeight(),
          uiContext.getUnitImageFactory().getUnitImageWidth() + 2, 3);
    }
    final UnitType type = data.getUnitTypeList().getUnitType(unitType);
    if (type == null) {
      throw new IllegalStateException("Type not found:" + unitType);
    }
    final PlayerID owner = data.getPlayerList().getPlayerId(playerName);
    final Optional<Image> img =
        uiContext.getUnitImageFactory().getImage(type, owner, (damaged > 0) || (bombingUnitDamage > 0), disabled);

    if (!img.isPresent()) {
      ClientLogger.logError("MISSING IMAGE (this unit or image will be invisible): " + type);
    }

    if (img.isPresent() && enabledFlags) {
      final int maxRange = new TripleAUnit(type, owner, data).getMaxMovementAllowed();
      switch (drawUnitNationMode) {
        case BELOW:
          // If unit is not in the "excluded list" it will get drawn
          if (maxRange != 0) {
            final Image flag = uiContext.getFlagImageFactory().getFlag(owner);
            final int xoffset = (img.get().getWidth(null) / 2) - (flag.getWidth(null) / 2);// centered flag in the middle
            final int yoffset = (img.get().getHeight(null) / 2) - (flag.getHeight(null) / 4)
                - 5;// centered flag in the middle moved it 1/2 - 5 down
            graphics.drawImage(flag, (placementPoint.x - bounds.x) + xoffset, (placementPoint.y - bounds.y) + yoffset,
                null);
          }
          drawUnit(graphics, img.get(), bounds);
          break;
        case NEXT_TO:
          drawUnit(graphics, img.get(), bounds);
          // If unit is not in the "excluded list" it will get drawn
          if (maxRange != 0) {
            final Image flag = uiContext.getFlagImageFactory().getSmallFlag(owner);
            final int xoffset = img.get().getWidth(null) - flag.getWidth(
                null);// If someone wants to put more effort in this, he could add an algorithm to calculate the real
            final int yoffset =
                img.get().getHeight(null) - flag.getHeight(null);// lower right corner - transparency/alpha channel etc.
            // currently the flag is drawn in the lower right corner of the image's bounds -> offsets on some unit
            // images
            // This Method draws the Flag in the lower right corner of the unit image. Since the position is the upper
            // left corner we have to move the picture up by the height and left by the width.
            graphics.drawImage(flag, (placementPoint.x - bounds.x) + xoffset, (placementPoint.y - bounds.y) + yoffset,
                null);
          }
          break;
        default:
          throw new AssertionError("unknown unit flag draw mode: " + drawUnitNationMode);
      }
    } else {
      if (img.isPresent()) {
        drawUnit(graphics, img.get(), bounds);
      }
    }
    // more then 1 unit of this category
    if (count != 1) {
      final int stackSize = mapData.getDefaultUnitsStackSize();
      if (stackSize > 0) { // Display more units as a stack
        for (int i = 1; (i < count) && (i < stackSize); i++) {
          if (img.isPresent()) {
            graphics.drawImage(img.get(), (placementPoint.x + (2 * i)) - bounds.x,
                placementPoint.y - (2 * i) - bounds.y,
                null);
          }
        }
        if (count > stackSize) {
          final Font font = MapImage.getPropertyMapFont();
          if (font.getSize() > 0) {
            graphics.setColor(MapImage.getPropertyUnitCountColor());
            graphics.setFont(font);
            graphics.drawString(String.valueOf(count), // draws how many units there are
                (placementPoint.x - bounds.x) + (2 * stackSize)
                    + ((uiContext.getUnitImageFactory().getUnitImageWidth() * 6) / 10),
                (placementPoint.y - (2 * stackSize) - bounds.y)
                    + ((uiContext.getUnitImageFactory().getUnitImageHeight() * 1) / 3));
          }
        }
      } else { // Display a white number at the bottom of the unit
        final Font font = MapImage.getPropertyMapFont();
        if (font.getSize() > 0) {
          graphics.setColor(MapImage.getPropertyUnitCountColor());
          graphics.setFont(font);
          graphics.drawString(String.valueOf(count), // draws how many units there are
              (placementPoint.x - bounds.x) + (uiContext.getUnitImageFactory().getUnitCounterOffsetWidth()),
              (placementPoint.y - bounds.y) + uiContext.getUnitImageFactory().getUnitCounterOffsetHeight());
        }
      }
    }
    displayHitDamage(bounds, graphics);
    // Display Factory Damage
    if (isDamageFromBombingDoneToUnitsInsteadOfTerritories(data) && Matches.unitTypeCanBeDamaged().test(type)) {
      displayFactoryDamage(bounds, graphics);
    }
  }

  private void displayFactoryDamage(final Rectangle bounds, final Graphics2D graphics) {
    final Font font = MapImage.getPropertyMapFont();
    if ((territoryName.length() != 0) && (font.getSize() > 0) && (bombingUnitDamage > 0)) {
      graphics.setColor(MapImage.getPropertyUnitFactoryDamageColor());
      graphics.setFont(font);
      graphics.drawString("" + bombingUnitDamage,
          (placementPoint.x - bounds.x) + (uiContext.getUnitImageFactory().getUnitImageWidth() / 4),
          (placementPoint.y - bounds.y) + (uiContext.getUnitImageFactory().getUnitImageHeight() / 4));
    }
  }

  /**
   * This draws the given image onto the given graphics object.
   */
  private void drawUnit(final Graphics2D graphics, final Image image, final Rectangle bounds) {
    graphics.drawImage(image, placementPoint.x - bounds.x, placementPoint.y - bounds.y, null);
  }

  private void displayHitDamage(final Rectangle bounds, final Graphics2D graphics) {
    final Font font = MapImage.getPropertyMapFont();
    if ((territoryName.length() != 0) && (font.getSize() > 0) && (damaged > 1)) {
      graphics.setColor(MapImage.getPropertyUnitHitDamageColor());
      graphics.setFont(font);
      graphics.drawString("" + damaged,
          (placementPoint.x - bounds.x) + ((uiContext.getUnitImageFactory().getUnitImageWidth() * 3) / 4),
          (placementPoint.y - bounds.y) + (uiContext.getUnitImageFactory().getUnitImageHeight() / 4));
    }
  }

  Tuple<Territory, List<Unit>> getUnits(final GameData data) {
    // note - it may be the case where the territory is being changed as a result
    // to a mouse click, and the map units haven't updated yet, so the unit count
    // from the territory wont match the units in count
    final Territory t = data.getMap().getTerritory(territoryName);
    final UnitType type = data.getUnitTypeList().getUnitType(unitType);
    final Predicate<Unit> selectedUnits = Matches.unitIsOfType(type)
        .and(Matches.unitIsOwnedBy(data.getPlayerList().getPlayerId(playerName)))
        .and((damaged > 0)
            ? Matches.unitHasTakenSomeDamage()
            : Matches.unitHasNotTakenAnyDamage())
        .and((bombingUnitDamage > 0)
            ? Matches.unitHasTakenSomeBombingUnitDamage()
            : Matches.unitHasNotTakenAnyBombingUnitDamage());
    return Tuple.of(t, t.getUnits().getMatches(selectedUnits));
  }

  @Override
  public int getLevel() {
    return UNITS_LEVEL;
  }

  @Override
  public String toString() {
    return "UnitsDrawer for " + count + " " + MyFormatter.pluralize(unitType) + " in  " + territoryName;
  }

  private static boolean isDamageFromBombingDoneToUnitsInsteadOfTerritories(final GameData data) {
    return Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data);
  }

  public static void setUnitFlagDrawMode(final UnitFlagDrawMode unitFlag, final Preferences prefs) {
    drawUnitNationMode = unitFlag;
    prefs.put(PreferenceKeys.DRAW_MODE.name(), unitFlag.toString());
  }
}
