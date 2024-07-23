package games.strategy.triplea.ui;

import com.google.common.collect.Lists;
import games.strategy.engine.data.GameData;
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
import games.strategy.triplea.util.UnitSeparator;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.Setter;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.WrapLayout;

/** A simple panel that displays a list of units. */
public class SimpleUnitPanel extends JPanel {
  private static final long serialVersionUID = -3768796793775300770L;
  private final UiContext uiContext;
  private final Style style;
  @Setter private double scaleFactor = 1.0;
  @Setter private boolean showCountsForSingleUnits = true;

  public enum Style {
    LARGE_ICONS_COLUMN,
    SMALL_ICONS_ROW,
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
    } else if (style == Style.SMALL_ICONS_ROW) {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
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
    if (categories.isEmpty()) {
      return;
    }
    final GameData gameData = categories.iterator().next().getUnitAttachment().getData();
    final ArrayList<UnitCategory> unitCategories = Lists.newArrayList(categories);
    UnitSeparator.sortUnitCategories(unitCategories, gameData);
    for (final UnitCategory category : unitCategories) {
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
    if (showCountsForSingleUnits || quantity > 1) {
      label.setText("x " + quantity);
    }
    if (unit instanceof UnitType) {
      final UnitType unitType = (UnitType) unit;
      final UnitImageFactory.ImageKey imageKey =
          UnitImageFactory.ImageKey.builder()
              .player(player)
              .type(unitType)
              .damaged(damaged)
              .disabled(disabled)
              .build();
      ImageIcon icon = uiContext.getUnitImageFactory().getIcon(imageKey);
      label.setIcon(scaleIcon(icon, scaleFactor));
      MapUnitTooltipManager.setUnitTooltip(label, unitType, player, quantity, uiContext);
    } else if (unit instanceof Resource) {
      ImageIcon icon =
          style == Style.LARGE_ICONS_COLUMN
              ? uiContext.getResourceImageFactory().getLargeIcon(unit.getName())
              : uiContext.getResourceImageFactory().getIcon(unit.getName());
      label.setIcon(scaleIcon(icon, scaleFactor));
    }
    add(label);
    if (style == Style.SMALL_ICONS_ROW) {
      add(Box.createHorizontalStrut(8));
    }
  }

  private static ImageIcon scaleIcon(ImageIcon icon, double scaleFactor) {
    if (scaleFactor == 1.0) {
      return icon;
    }
    int width = Math.max(1, (int) (icon.getIconWidth() * scaleFactor));
    int height = Math.max(1, (int) (icon.getIconHeight() * scaleFactor));
    return new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
  }
}
