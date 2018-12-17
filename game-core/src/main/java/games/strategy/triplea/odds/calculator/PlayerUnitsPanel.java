package games.strategy.triplea.odds.calculator;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

/**
 * Panel showing full list of units for a player in a given battle simulation.
 */
public class PlayerUnitsPanel extends JPanel {
  private static final long serialVersionUID = -1206338960403314681L;
  private final GameData data;
  private final UiContext uiContext;
  private final boolean defender;
  private boolean isLand = true;
  private List<UnitCategory> categories = null;
  private final List<Runnable> listeners = new ArrayList<>();

  public PlayerUnitsPanel(final GameData data, final UiContext uiContext, final boolean defender) {
    this.data = data;
    this.uiContext = uiContext;
    this.defender = defender;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  }

  public void clear() {
    for (final Component c : getComponents()) {
      final UnitPanel panel = (UnitPanel) c;
      panel.setCount(0);
    }
  }

  public List<Unit> getUnits() {
    final List<Unit> allUnits = new ArrayList<>();
    for (final Component c : getComponents()) {
      final UnitPanel panel = (UnitPanel) c;
      allUnits.addAll(panel.getUnits());
    }
    return allUnits;
  }

  public List<UnitCategory> getCategories() {
    return categories;
  }

  /**
   * Sets up components to an initial state.
   */
  public void init(final PlayerId id, final List<Unit> units, final boolean land) {
    isLand = land;
    categories = new ArrayList<>(categorize(id, units));
    categories.sort(Comparator.comparing(UnitCategory::getType, (ut1, ut2) -> {
      final UnitAttachment u1 = UnitAttachment.get(ut1);
      final UnitAttachment u2 = UnitAttachment.get(ut2);
      // For land battles, sort by land, air, can't combat move (AA), bombarding
      if (land) {
        if (u1.getIsSea() != u2.getIsSea()) {
          return u1.getIsSea() ? 1 : -1;
        }
        final boolean u1CanNotCombatMove =
            Matches.unitTypeCanNotMoveDuringCombatMove().test(ut1) || !Matches.unitTypeCanMove(id).test(ut1);
        final boolean u2CanNotCombatMove =
            Matches.unitTypeCanNotMoveDuringCombatMove().test(ut2) || !Matches.unitTypeCanMove(id).test(ut2);
        if (u1CanNotCombatMove != u2CanNotCombatMove) {
          return u1CanNotCombatMove ? 1 : -1;
        }
        if (u1.getIsAir() != u2.getIsAir()) {
          return u1.getIsAir() ? 1 : -1;
        }
      } else {
        if (u1.getIsSea() != u2.getIsSea()) {
          return u1.getIsSea() ? -1 : 1;
        }
      }
      return u1.getName().compareTo(u2.getName());
    }));
    removeAll();
    final Predicate<UnitType> predicate;
    if (land) {
      if (defender) {
        predicate = Matches.unitTypeIsNotSea();
      } else {
        predicate = Matches.unitTypeIsNotSea().or(Matches.unitTypeCanBombard(id));
      }
    } else {
      predicate = Matches.unitTypeIsSeaOrAir();
    }
    final IntegerMap<UnitType> costs;
    try {
      data.acquireReadLock();
      costs = TuvUtils.getCostsForTuv(id, data);
    } finally {
      data.releaseReadLock();
    }
    for (final UnitCategory category : categories) {
      if (predicate.test(category.getType())) {
        final UnitPanel upanel = new UnitPanel(uiContext, category, costs);
        upanel.addChangeListener(this::notifyListeners);
        add(upanel);
      }
    }
    // TODO: probably do not need to do this much revalidation.
    invalidate();
    validate();
    revalidate();
    getParent().invalidate();
  }

  /**
   * Get all unit type categories that can be in combat first in the order of the player's production frontier and then
   * any unit types the player owns on the map. Then populate the list of units into the categories.
   */
  private Set<UnitCategory> categorize(final PlayerId id, final List<Unit> units) {

    // Get all unit types from production frontier and player unit types on the map
    final Set<UnitCategory> categories = new LinkedHashSet<>();
    for (final UnitType t : getUnitTypes(id)) {
      final UnitCategory category = new UnitCategory(t, id);
      categories.add(category);
    }

    // Populate units into each category then add any remaining categories (damaged units, etc)
    final Set<UnitCategory> unitCategories = UnitSeparator.categorize(units);
    for (final UnitCategory category : categories) {
      for (final UnitCategory unitCategory : unitCategories) {
        if (category.equals(unitCategory)) {
          category.getUnits().addAll(unitCategory.getUnits());
        }
      }
    }
    categories.addAll(unitCategories);

    return categories;
  }

  /**
   * Return all the unit types available for the given player. A unit type is
   * available if the unit can be purchased or if a player has one on the map.
   */
  private Collection<UnitType> getUnitTypes(final PlayerId player) {
    Collection<UnitType> unitTypes = new LinkedHashSet<>();
    final ProductionFrontier frontier = player.getProductionFrontier();
    if (frontier != null) {
      for (final ProductionRule rule : frontier) {
        for (final NamedAttachable type : rule.getResults().keySet()) {
          if (type instanceof UnitType) {
            unitTypes.add((UnitType) type);
          }
        }
      }
    }
    for (final Territory t : data.getMap()) {
      for (final Unit u : t.getUnits()) {
        if (u.getOwner().equals(player)) {
          unitTypes.add(u.getType());
        }
      }
    }

    // Filter out anything like factories, or units that have no combat ability AND cannot be taken casualty
    unitTypes = CollectionUtils.getMatches(unitTypes,
        Matches.unitTypeCanBeInBattle(!defender, isLand, player, 1, false, false, false));

    return unitTypes;
  }

  public void addChangeListener(final Runnable listener) {
    listeners.add(listener);
  }

  private void notifyListeners() {
    listeners.forEach(Runnable::run);
  }
}
