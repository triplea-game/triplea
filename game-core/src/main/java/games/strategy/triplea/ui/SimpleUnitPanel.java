package games.strategy.triplea.ui;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitTypeComparator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.util.IntegerMap;

/**
 * A Simple panel that displays a list of units.
 */
public class SimpleUnitPanel extends JPanel {
  private static final long serialVersionUID = -3768796793775300770L;
  private final UiContext uiContext;

  public SimpleUnitPanel(final UiContext uiContext) {
    this.uiContext = uiContext;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  }

  /**
   * @param units
   *        a HashMap in the form ProductionRule -> number of units
   *        assumes that each production rule has 1 result, which is simple the number of units.
   */
  void setUnitsFromProductionRuleMap(final IntegerMap<ProductionRule> units, final PlayerID player) {
    removeAll();
    final TreeSet<ProductionRule> productionRules = new TreeSet<>(productionRuleComparator);
    productionRules.addAll(units.keySet());
    for (final ProductionRule productionRule : productionRules) {
      final int quantity = units.getInt(productionRule);
      for (final NamedAttachable resourceOrUnit : productionRule.getResults().keySet()) {
        addUnits(player, quantity * productionRule.getResults().getInt(resourceOrUnit), resourceOrUnit, false, false);
      }
    }
  }

  /**
   * @param units
   *        a HashMap in the form RepairRule -> number of units
   *        assumes that each repair rule has 1 result, which is simply the number of units.
   */
  public void setUnitsFromRepairRuleMap(final HashMap<Unit, IntegerMap<RepairRule>> units, final PlayerID player,
      final GameData data) {
    removeAll();
    final Set<Unit> entries = units.keySet();
    for (final Unit unit : entries) {
      final IntegerMap<RepairRule> rules = units.get(unit);
      final TreeSet<RepairRule> repairRules = new TreeSet<>(repairRuleComparator);
      repairRules.addAll(rules.keySet());
      for (final RepairRule repairRule : repairRules) {
        final int quantity = rules.getInt(repairRule);
        if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
          // check to see if the repair rule matches the damaged unit
          if (unit.getType().equals((repairRule.getResults().keySet().iterator().next()))) {
            addUnits(player, quantity, unit.getType(), Matches.unitHasTakenSomeBombingUnitDamage().test(unit),
                Matches.unitIsDisabled().test(unit));
          }
        }
      }
    }
  }

  /**
   * @param categories
   *        a collection of UnitCategories.
   */
  public void setUnitsFromCategories(final Collection<UnitCategory> categories) {
    removeAll();
    for (final UnitCategory category : categories) {
      addUnits(category.getOwner(), category.getUnits().size(), category.getType(),
          category.hasDamageOrBombingUnitDamage(), category.getDisabled());
    }
  }

  private void addUnits(final PlayerID player, final int quantity, final NamedAttachable unit,
      final boolean damaged, final boolean disabled) {
    final JLabel label = new JLabel();
    label.setText(" x " + quantity);
    if (unit instanceof UnitType) {
      final Optional<ImageIcon> icon =
          uiContext.getUnitImageFactory().getIcon((UnitType) unit, player, damaged, disabled);
      if (icon.isPresent()) {
        label.setIcon(icon.get());
      }
    } else if (unit instanceof Resource) {
      label.setIcon(uiContext.getResourceImageFactory().getIcon((Resource) unit, true));
    }
    add(label);
  }

  Comparator<ProductionRule> productionRuleComparator = new Comparator<ProductionRule>() {
    UnitTypeComparator utc = new UnitTypeComparator();

    @Override
    public int compare(final ProductionRule o1, final ProductionRule o2) {
      if ((o1.getResults().size() == 1) && (o2.getResults().size() == 1)) {
        final NamedAttachable n1 = o1.getResults().keySet().iterator().next();
        final NamedAttachable n2 = o2.getResults().keySet().iterator().next();
        if (n1 instanceof UnitType) {
          final UnitType u1 = (UnitType) n1;
          if (n2 instanceof UnitType) {
            final UnitType u2 = (UnitType) n2;
            return utc.compare(u1, u2);
          } else if (n2 instanceof Resource) {
            // final Resource r2 = (Resource) n2;
            return -1;
          }
          return n1.getName().compareTo(n2.getName());
        } else if (n1 instanceof Resource) {
          final Resource r1 = (Resource) n1;
          if (n2 instanceof UnitType) {
            // final UnitType u2 = (UnitType) n2;
            return 1;
          } else if (n2 instanceof Resource) {
            final Resource r2 = (Resource) n2;
            return r1.getName().compareTo(r2.getName());
          } else {
            return n1.getName().compareTo(n2.getName());
          }
        }
        return n1.getName().compareTo(n2.getName());
      }
      if (o1.getResults().size() > o2.getResults().size()) {
        return -1;
      } else if (o1.getResults().size() < o2.getResults().size()) {
        return 1;
      }
      return o1.getName().compareTo(o2.getName());
    }
  };
  Comparator<RepairRule> repairRuleComparator = new Comparator<RepairRule>() {
    UnitTypeComparator utc = new UnitTypeComparator();

    @Override
    public int compare(final RepairRule o1, final RepairRule o2) {
      if ((o1.getResults().size() == 1) && (o2.getResults().size() == 1)) {
        final NamedAttachable n1 = o1.getResults().keySet().iterator().next();
        final NamedAttachable n2 = o2.getResults().keySet().iterator().next();
        if (n1 instanceof UnitType) {
          final UnitType u1 = (UnitType) n1;
          if (n2 instanceof UnitType) {
            final UnitType u2 = (UnitType) n2;
            return utc.compare(u1, u2);
          } else if (n2 instanceof Resource) {
            // final Resource r2 = (Resource) n2;
            return -1;
          }
          return n1.getName().compareTo(n2.getName());
        } else if (n1 instanceof Resource) {
          final Resource r1 = (Resource) n1;
          if (n2 instanceof UnitType) {
            // final UnitType u2 = (UnitType) n2;
            return 1;
          } else if (n2 instanceof Resource) {
            final Resource r2 = (Resource) n2;
            return r1.getName().compareTo(r2.getName());
          } else {
            return n1.getName().compareTo(n2.getName());
          }
        }
        return n1.getName().compareTo(n2.getName());
      }
      if (o1.getResults().size() > o2.getResults().size()) {
        return -1;
      } else if (o1.getResults().size() < o2.getResults().size()) {
        return 1;
      }
      return o1.getName().compareTo(o2.getName());
    }
  };
}
