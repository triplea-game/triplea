package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.TuvCostsCalculator;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.SwingComponents;

/** Panel showing full list of units for a player in a given battle simulation. */
public class PlayerUnitsPanel extends JPanel {
  private static final long serialVersionUID = -1206338960403314681L;
  private final GameData data;
  private final UiContext uiContext;
  private final boolean defender;
  private boolean isLandBattle = true;
  @Getter private List<UnitCategory> unitCategories = null;
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
  public void init(
      @Nonnull final GamePlayer panelPlayer,
      final List<Unit> units,
      final boolean isLandBattle,
      @Nullable final Territory territory) {
    this.isLandBattle = isLandBattle;
    unitPanels.clear();

    removeAll();
    final Predicate<UnitType> predicate;
    if (isLandBattle) {
      if (defender) {
        predicate = Matches.unitTypeIsNotSea();
      } else {
        predicate = Matches.unitTypeIsNotSea().or(Matches.unitTypeCanBombard(panelPlayer));
      }
    } else {
      predicate = Matches.unitTypeIsSeaOrAir();
    }
    final IntegerMap<UnitType> costs;
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      costs = new TuvCostsCalculator().getCostsForTuv(panelPlayer);
    }

    unitCategories = getAllUnitCategories(panelPlayer, units);
    UnitSeparator.sortUnitCategories(unitCategories, territory, panelPlayer);
    GamePlayer previousPlayer = null;
    JPanel panel = null;
    for (final UnitCategory category : unitCategories) {
      if (predicate.test(category.getType())) {
        if (!category.isOwnedBy(previousPlayer)) {
          panel = new JPanel();
          panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
          panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
          add(panel);
          previousPlayer = category.getOwner();
        }
        if (panel != null) {
          final var unitPanel = new UnitPanel(uiContext, category, costs);
          unitPanel.addChangeListener(this::notifyListeners);
          panel.add(unitPanel);
          unitPanels.add(unitPanel);
        }
      }
    }

    SwingComponents.redraw(this);
  }

  /**
   * Gets all unit type categories that can be in combat and pre-populates the categories with
   * {@code units}.
   *
   * @param panelPlayer player for this panel
   * @param units list of units to be populated
   */
  private List<UnitCategory> getAllUnitCategories(
      @Nonnull final GamePlayer panelPlayer, final List<Unit> units) {

    final List<UnitCategory> allUnitCategories = new ArrayList<>();

    // get list of relevant players for which the unit types need to be collected
    final Set<GamePlayer> players =
        units.stream()
            .map(Unit::getOwner)
            .filter(p -> !p.equals(panelPlayer))
            .collect(Collectors.toSet());
    players.add(panelPlayer);

    // Get unit types per player and add the respective unit category
    for (final GamePlayer player : players) {
      for (final UnitType unitType : getUnitTypes(player)) {
        final UnitCategory unitCategory = new UnitCategory(unitType, player);
        allUnitCategories.add(unitCategory);
      }
    }

    // Populate provided units into each category, add any remaining unit categories
    Collections.sort(allUnitCategories);
    final Set<UnitCategory> unitCategoriesWithUnits = UnitSeparator.categorize(units);
    for (final UnitCategory unitCategoryWithUnits : unitCategoriesWithUnits) {
      int categoryIndex = Collections.binarySearch(allUnitCategories, unitCategoryWithUnits);
      if (categoryIndex >= 0) { // key was found
        allUnitCategories.get(categoryIndex).getUnits().addAll(unitCategoryWithUnits.getUnits());
      } else {
        allUnitCategories.add(unitCategoryWithUnits);
      }
    }

    return allUnitCategories;
  }

  /**
   * Return all the unit types available for {@code gamePlayer}. A {@code UnitType} is available if
   * the unit can be produced/purchased or a respective unit is present on the map.
   *
   * @param gamePlayer Player for which the unit types should be collected
   */
  private Collection<UnitType> getUnitTypes(final GamePlayer gamePlayer) {
    final Collection<UnitType> unitTypes = new LinkedHashSet<>();

    final ProductionFrontier frontier = gamePlayer.getProductionFrontier();
    if (frontier != null) {
      unitTypes.addAll(frontier.getProducibleUnitTypes());
    }

    // Get any  player's unit on the map and collect the unit type
    data.getUnits().getUnits().stream()
        .filter(u -> gamePlayer.equals(u.getOwner()))
        .forEach(u -> unitTypes.add(u.getType()));

    // Filter out anything like factories, or units that have no combat ability AND cannot be taken
    // casualty
    return CollectionUtils.getMatches(
        unitTypes,
        Matches.unitTypeCanBeInBattle(
            !defender,
            isLandBattle,
            gamePlayer,
            1,
            BattleCalculatorPanel.hasMaxRounds(isLandBattle, data),
            false,
            List.of()));
  }

  /**
   * Returns true if any unit panel number is not zero (any units 'added' to the panel), false if
   * all are zero.
   */
  boolean hasNoneZeroUnitEntries() {
    return unitPanels.stream().anyMatch(unitPanel -> unitPanel.getCount() != 0);
  }

  public void addChangeListener(final Runnable listener) {
    listeners.add(listener);
  }

  private void notifyListeners() {
    listeners.forEach(Runnable::run);
  }
}
