package games.strategy.engine.data;

import java.awt.Image;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.UiContext;
import lombok.extern.java.Log;

/**
 * A prototype for units.
 */
@Log
public class UnitType extends NamedAttachable {
  private static final long serialVersionUID = 4885339076798905247L;

  public UnitType(final String name, final GameData data) {
    super(name, data);
  }

  public List<Unit> create(final int quantity, final PlayerId owner) {
    return create(quantity, owner, false);
  }

  public List<Unit> create(final int quantity, final PlayerId owner, final boolean isTemp) {
    return create(quantity, owner, isTemp, 0, 0);
  }

  List<Unit> create(final int quantity, final PlayerId owner, final boolean isTemp, final int hitsTaken,
      final int bombingUnitDamage) {
    return IntStream.range(0, quantity)
        .mapToObj(i -> create(owner, isTemp, hitsTaken, bombingUnitDamage))
        .collect(Collectors.toList());
  }

  private Unit create(final PlayerId owner, final boolean isTemp, final int hitsTaken, final int bombingUnitDamage) {
    final Unit u = getData().getGameLoader().createUnit(this, owner, getData());
    u.setHits(hitsTaken);
    if (u instanceof TripleAUnit) {
      ((TripleAUnit) u).setUnitDamage(bombingUnitDamage);
    }
    if (!isTemp) {
      getData().getUnits().put(u);
    }
    return u;
  }

  public Unit create(final PlayerId owner) {
    return create(owner, false, 0, 0);
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof UnitType && ((UnitType) o).getName().equals(getName());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getName());
  }

  /**
   * Will return a key of NULL for any units which we do not have art for.
   */
  public static Map<PlayerId, List<UnitType>> getAllPlayerUnitsWithImages(final GameData data,
      final UiContext uiContext, final boolean forceIncludeNeutralPlayer) {
    final LinkedHashMap<PlayerId, List<UnitType>> unitTypes = new LinkedHashMap<>();
    data.acquireReadLock();
    try {
      for (final PlayerId p : data.getPlayerList().getPlayers()) {
        unitTypes.put(p, getPlayerUnitsWithImages(p, data, uiContext));
      }
      final Set<UnitType> unitsSoFar = new HashSet<>();
      for (final List<UnitType> l : unitTypes.values()) {
        unitsSoFar.addAll(l);
      }
      final Set<UnitType> all = data.getUnitTypeList().getAllUnitTypes();
      all.removeAll(unitsSoFar);
      if (forceIncludeNeutralPlayer || !all.isEmpty()) {
        unitTypes.put(PlayerId.NULL_PLAYERID, getPlayerUnitsWithImages(PlayerId.NULL_PLAYERID, data, uiContext));
        unitsSoFar.addAll(unitTypes.get(PlayerId.NULL_PLAYERID));
        all.removeAll(unitsSoFar);
        if (!all.isEmpty()) {
          unitTypes.put(null, new ArrayList<>(all));
        }
      }
    } finally {
      data.releaseReadLock();
    }
    return unitTypes;
  }

  private static List<UnitType> getPlayerUnitsWithImages(final PlayerId player, final GameData data,
      final UiContext uiContext) {
    final List<UnitType> unitTypes = new ArrayList<>();
    data.acquireReadLock();
    try {
      // add first based on current production ability
      if (player.getProductionFrontier() != null) {
        for (final ProductionRule productionRule : player.getProductionFrontier()) {
          for (final Entry<NamedAttachable, Integer> entry : productionRule.getResults().entrySet()) {
            if (UnitType.class.isAssignableFrom(entry.getKey().getClass())) {
              final UnitType ut = (UnitType) entry.getKey();
              if (!unitTypes.contains(ut)) {
                unitTypes.add(ut);
              }
            }
          }
        }
      }
      // this next part is purely to allow people to "add" neutral (null player) units to territories.
      // This is because the null player does not have a production frontier, and we also do not know what units we have
      // art for, so only
      // use the units on a map.
      for (final Territory t : data.getMap()) {
        for (final Unit u : t.getUnits()) {
          if (u.getOwner().equals(player)) {
            final UnitType ut = u.getType();
            if (!unitTypes.contains(ut)) {
              unitTypes.add(ut);
            }
          }
        }
      }
      // now check if we have the art for anything that is left
      for (final UnitType ut : data.getUnitTypeList().getAllUnitTypes()) {
        if (!unitTypes.contains(ut)) {
          try {
            final UnitImageFactory imageFactory = uiContext.getUnitImageFactory();
            if (imageFactory != null) {
              final Optional<Image> unitImage = imageFactory.getImage(ut, player, false, false);
              if (unitImage.isPresent()) {
                if (!unitTypes.contains(ut)) {
                  unitTypes.add(ut);
                }
              }
            }
          } catch (final Exception e) {
            log.log(Level.SEVERE, "Exception while drawing unit type: " + ut + ", ", e);
          }
        }
      }
    } finally {
      data.releaseReadLock();
    }
    return unitTypes;
  }
}
