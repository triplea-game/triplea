package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.image.UnitImageFactory;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.JFrame;
import org.triplea.java.collections.IntegerMap;

class EditProductionPanel extends ProductionPanel {
  private static final long serialVersionUID = 5826523459539469173L;

  private EditProductionPanel(final UiContext uiContext) {
    super(uiContext);
  }

  static IntegerMap<ProductionRule> getProduction(
      final GamePlayer gamePlayer,
      final JFrame parent,
      final GameData data,
      final UiContext uiContext) {
    return new EditProductionPanel(uiContext)
        .show(gamePlayer, parent, data, false, new IntegerMap<>());
  }

  @Override
  protected void calculateLimits() {
    for (final Rule current : getRules()) {
      current.setMax(99);
    }
  }

  @Override
  protected void initRules(
      final GamePlayer player, final IntegerMap<ProductionRule> initialPurchase) {
    try (GameData.Unlocker ignored = this.data.acquireReadLock()) {
      gamePlayer = player;
      final Set<UnitType> unitsAllowed = new HashSet<>();
      if (player.getProductionFrontier() != null) {
        for (final ProductionRule productionRule : player.getProductionFrontier()) {
          final Rule rule = new Rule(productionRule, player);
          for (final Entry<NamedAttachable, Integer> entry :
              productionRule.getResults().entrySet()) {
            if (UnitType.class.isAssignableFrom(entry.getKey().getClass())) {
              unitsAllowed.add((UnitType) entry.getKey());
            }
          }
          final int initialQuantity = initialPurchase.getInt(productionRule);
          rule.setQuantity(initialQuantity);
          rules.add(rule);
        }
      }
      // this next part is purely to allow people to "add" neutral (null player) units to
      // territories.
      // This is because the null player does not have a production frontier, and we also do not
      // know what units we have
      // art for, so only use the units on a map.
      for (final Territory t : data.getMap()) {
        for (final Unit u : t.getUnitCollection()) {
          if (u.isOwnedBy(player)) {
            final UnitType ut = u.getType();
            if (!unitsAllowed.contains(ut)) {
              unitsAllowed.add(ut);
              final IntegerMap<NamedAttachable> result = new IntegerMap<>();
              result.add(ut, 1);
              final IntegerMap<Resource> cost = new IntegerMap<>();
              cost.add(data.getResourceList().getResourceOrThrow(Constants.PUS), 1);
              final ProductionRule newRule = new ProductionRule(ut.getName(), data, result, cost);
              final Rule rule = new Rule(newRule, player);
              rule.setQuantity(0);
              rules.add(rule);
            }
          }
        }
      }
      // now check if we have the art for anything that is left
      for (final UnitType ut : data.getUnitTypeList().getAllUnitTypes()) {
        if (!unitsAllowed.contains(ut)) {
          try {
            final UnitImageFactory imageFactory = uiContext.getUnitImageFactory();
            if (imageFactory != null) {
              if (imageFactory.hasImage(
                  UnitImageFactory.ImageKey.builder().player(player).type(ut).build())) {
                unitsAllowed.add(ut);
                final IntegerMap<NamedAttachable> result = new IntegerMap<>();
                result.add(ut, 1);
                final IntegerMap<Resource> cost = new IntegerMap<>();
                cost.add(data.getResourceList().getResourceOrThrow(Constants.PUS), 1);
                final ProductionRule newRule = new ProductionRule(ut.getName(), data, result, cost);
                final Rule rule = new Rule(newRule, player);
                rule.setQuantity(0);
                rules.add(rule);
              }
            }
          } catch (final Exception e) { // ignore
          }
        }
      }
    }
  }
}
