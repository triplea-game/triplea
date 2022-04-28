package games.strategy.triplea.ui;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.RuleComparator;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.util.UnitCategory;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.WrapLayout;

/** A Simple panel that displays a list of units. */
@Slf4j
public class SimpleUnitPanel extends JPanel {
  private static final long serialVersionUID = -3768796793775300770L;
  private final UiContext uiContext;
  private final Style style;

  public enum Style {
    LARGE_ICONS_COLUMN,
    SMALL_ICONS_WRAPPED_WITH_LABEL_WHEN_EMPTY
  }

  public SimpleUnitPanel(final UiContext uiContext) {
    this(uiContext, Style.LARGE_ICONS_COLUMN);
  }

  public SimpleUnitPanel(final UiContext uiContext, final Style style) {
    this.uiContext = uiContext;
    this.style = style;
    if (style == Style.SMALL_ICONS_WRAPPED_WITH_LABEL_WHEN_EMPTY) {
      setLayout(new WrapLayout());
    } else {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }
  }

  /**
   * Adds units to the panel based on the specified production rules.
   *
   * @param units a HashMap in the form ProductionRule -> number of units assumes that each
   *     production rule has 1 result, which is simple the number of units.
   */
  void setUnitsFromProductionRuleMap(
      final IntegerMap<ProductionRule> units, final GamePlayer player) {
    removeAll();

    final TreeSet<ProductionRule> productionRules = new TreeSet<>(new RuleComparator<>());
    productionRules.addAll(units.keySet());
    for (final ProductionRule productionRule : productionRules) {
      final int quantity = units.getInt(productionRule);
      for (final NamedAttachable resourceOrUnit : productionRule.getResults().keySet()) {
        addUnits(
            player,
            quantity * productionRule.getResults().getInt(resourceOrUnit),
            resourceOrUnit,
            false,
            false);
      }
    }
  }

  /**
   * Adds units to the panel based on the specified repair rules.
   *
   * @param units a HashMap in the form RepairRule -> number of units assumes that each repair rule
   *     has 1 result, which is simply the number of units.
   */
  public void setUnitsFromRepairRuleMap(
      final Map<Unit, IntegerMap<RepairRule>> units,
      final GamePlayer player,
      final GameState data) {
    removeAll();
    final Set<Unit> entries = units.keySet();
    for (final Unit unit : entries) {
      final IntegerMap<RepairRule> rules = units.get(unit);
      final TreeSet<RepairRule> repairRules = new TreeSet<>(new RuleComparator<>());
      repairRules.addAll(rules.keySet());
      for (final RepairRule repairRule : repairRules) {
        // check to see if the repair rule matches the damaged unit
        if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data.getProperties())
            && unit.getType().equals(repairRule.getAnyResultKey())) {
          addUnits(
              player,
              rules.getInt(repairRule),
              unit.getType(),
              Matches.unitHasTakenSomeBombingUnitDamage().test(unit),
              Matches.unitIsDisabled().test(unit));
        }
      }
    }
  }

  /**
   * Adds units to the panel based on the specified unit categories.
   *
   * @param categories a collection of UnitCategories.
   */
  public void setUnitsFromCategories(final Collection<UnitCategory> categories) {
    removeAll();
    for (final UnitCategory category : categories) {
      addUnits(
          category.getOwner(),
          category.getUnits().size(),
          category.getType(),
          category.hasDamageOrBombingUnitDamage(),
          category.getDisabled());
    }
  }

  private void addUnits(
      final GamePlayer player,
      final int quantity,
      final NamedAttachable unit,
      final boolean damaged,
      final boolean disabled) {
    final JLabel label = new JLabel();
    label.setText(" x " + quantity);
    if (unit instanceof UnitType) {
      final UnitType unitType = (UnitType) unit;

      final UnitImageFactory.ImageKey imageKey =
          UnitImageFactory.ImageKey.builder()
              .player(player)
              .type(unitType)
              .damaged(damaged)
              .disabled(disabled)
              .build();
      final Optional<ImageIcon> icon = uiContext.getUnitImageFactory().getIcon(imageKey);
      if (icon.isEmpty() && !uiContext.isShutDown()) {
        final String imageName = imageKey.getFullName();
        log.error("missing unit icon (won't be displayed): " + imageName + ", " + imageKey);
      }
      icon.ifPresent(label::setIcon);
      MapUnitTooltipManager.setUnitTooltip(label, unitType, player, quantity);
    } else if (unit instanceof Resource) {
      label.setIcon(
          style == Style.LARGE_ICONS_COLUMN
              ? uiContext.getResourceImageFactory().getLargeIcon(unit.getName())
              : uiContext.getResourceImageFactory().getIcon(unit.getName()));
    }
    add(label);
  }
}
