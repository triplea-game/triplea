package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.TuvCostsCalculator;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** Panel showing full list of units for a player in a given battle simulation. */
public class PlayerUnitsPanel extends JPanel {
  private static final long serialVersionUID = -1206338960403314681L;
  private final GameData data;
  private final UiContext uiContext;
  private final boolean defender;
  private boolean isLand = true;
  @Getter private List<UnitCategory> categories = null;
  private final List<Runnable> listeners = new ArrayList<>();
  private final List<UnitPanel> unitPanels = new ArrayList<>();

  public PlayerUnitsPanel(final GameData data, final UiContext uiContext, final boolean defender) {
    this.data = data;
    this.uiContext = uiContext;
    this.defender = defender;
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
  }

  public void clear() {
    unitPanels.forEach(p -> p.setCount(0));
  }

  public List<Unit> getUnits() {
    return unitPanels.stream()
        .map(UnitPanel::getUnits)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  /** Sets up components to an initial state. */
  public void init(final GamePlayer gamePlayer, final List<Unit> units, final boolean land) {
    isLand = land;
    unitPanels.clear();
    categories = new ArrayList<>(categorize(gamePlayer, units));

    categories.sort(
        (c1, c2) -> {
          if (!c1.isOwnedBy(c2.getOwner())) {
            if (c1.isOwnedBy(gamePlayer)) {
              return -1;
            } else if (c2.isOwnedBy(gamePlayer)) {
              return 1;
            } else {
              return c1.getOwner().getName().compareTo(c2.getOwner().getName());
            }
          }
          final UnitType ut1 = c1.getType();
          final UnitType ut2 = c2.getType();
          final UnitAttachment u1 = ut1.getUnitAttachment();
          final UnitAttachment u2 = ut2.getUnitAttachment();
          // For land battles, sort by land, air, can't combat move (AA), bombarding
          if (land) {
            if (u1.getIsSea() != u2.getIsSea()) {
              return u1.getIsSea() ? 1 : -1;
            }
            final boolean u1CanNotCombatMove =
                Matches.unitTypeCanNotMoveDuringCombatMove().test(ut1)
                    || !Matches.unitTypeCanMove(gamePlayer).test(ut1);
            final boolean u2CanNotCombatMove =
                Matches.unitTypeCanNotMoveDuringCombatMove().test(ut2)
                    || !Matches.unitTypeCanMove(gamePlayer).test(ut2);
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
        });

    removeAll();
    final Predicate<UnitType> predicate;
    if (land) {
      if (defender) {
        predicate = Matches.unitTypeIsNotSea();
      } else {
        predicate = Matches.unitTypeIsNotSea().or(Matches.unitTypeCanBombard(gamePlayer));
      }
    } else {
      predicate = Matches.unitTypeIsSeaOrAir();
    }
    final IntegerMap<UnitType> costs;
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      costs = new TuvCostsCalculator().getCostsForTuv(gamePlayer);
    }

    GamePlayer previousPlayer = null;
    JPanel panel = null;
    for (final UnitCategory category : categories) {
      if (predicate.test(category.getType())) {
        if (!category.isOwnedBy(previousPlayer)) {
          panel = new JPanel();
          panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
          panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
          add(panel);
          previousPlayer = category.getOwner();
        }
        final var unitPanel = new UnitPanel(uiContext, category, costs);
        unitPanel.addChangeListener(this::notifyListeners);
        panel.add(unitPanel);
        unitPanels.add(unitPanel);
      }
    }

    // TODO: probably do not need to do this much revalidation.
    invalidate();
    validate();
    revalidate();
    getParent().invalidate();
  }

  /**
   * Get all unit type categories that can be in combat first in the order of the player's
   * production frontier and then any unit types the player owns on the map. Then populate the list
   * of units into the categories.
   */
  private Set<UnitCategory> categorize(final GamePlayer gamePlayer, final List<Unit> units) {

    // Get player unit types from production frontier and unit types on the map
    final Set<UnitCategory> categories = new LinkedHashSet<>();
    for (final UnitType t : getUnitTypes(gamePlayer)) {
      final UnitCategory category = new UnitCategory(t, gamePlayer);
      categories.add(category);
    }

    // Get unit owner unit types from production frontier and unit types on the map
    for (final GamePlayer player :
        units.stream()
            .map(Unit::getOwner)
            .filter(p -> !p.equals(gamePlayer))
            .collect(Collectors.toSet())) {
      for (final UnitType t : getUnitTypes(player)) {
        final UnitCategory category = new UnitCategory(t, player);
        categories.add(category);
      }
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
   * Return all the unit types available for the given player. A unit type is available if the unit
   * can be purchased or if a player has one on the map.
   */
  private Collection<UnitType> getUnitTypes(final GamePlayer player) {
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
      for (final Unit u : t.getUnitCollection()) {
        if (u.isOwnedBy(player)) {
          unitTypes.add(u.getType());
        }
      }
    }

    // Filter out anything like factories, or units that have no combat ability AND cannot be taken
    // casualty
    unitTypes =
        CollectionUtils.getMatches(
            unitTypes,
            Matches.unitTypeCanBeInBattle(
                !defender,
                isLand,
                player,
                1,
                BattleCalculatorPanel.hasMaxRounds(isLand, data),
                false,
                List.of()));

    return unitTypes;
  }

  /**
   * Returns true if all of the unit panel numbers are zero, false if there are any units 'added' to
   * the panel.
   */
  boolean isEmpty() {
    return unitPanels.stream().allMatch(unitPanel -> unitPanel.getCount() == 0);
  }

  public void addChangeListener(final Runnable listener) {
    listeners.add(listener);
  }

  private void notifyListeners() {
    listeners.forEach(Runnable::run);
  }
}
