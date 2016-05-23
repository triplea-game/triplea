package games.strategy.triplea.ui;

import java.awt.Image;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.swing.JFrame;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.util.IntegerMap;

public class EditProductionPanel extends ProductionPanel {
  private static final long serialVersionUID = 5826523459539469173L;

  public static IntegerMap<ProductionRule> getProduction(final PlayerID id, final JFrame parent, final GameData data,
      final IUIContext uiContext) {
    return new EditProductionPanel(uiContext).show(id, parent, data, false, new IntegerMap<>());
  }

  /** Creates new ProductionPanel */
  private EditProductionPanel(final IUIContext uiContext) {
    super(uiContext);
  }

  @Override
  protected void calculateLimits() {
    for (final Rule current : getRules()) {
      current.setMax(99);
    }
  }

  @Override
  protected void initRules(final PlayerID player, final GameData data,
      final IntegerMap<ProductionRule> initialPurchase) {
    m_data.acquireReadLock();
    try {
      m_id = player;
      final Set<UnitType> unitsAllowed = new HashSet<>();
      if (player.getProductionFrontier() != null) {
        for (final ProductionRule productionRule : player.getProductionFrontier()) {
          final Rule rule = new Rule(productionRule, player);
          for (final Entry<NamedAttachable, Integer> entry : productionRule.getResults().entrySet()) {
            if (UnitType.class.isAssignableFrom(entry.getKey().getClass())) {
              unitsAllowed.add((UnitType) entry.getKey());
            }
          }
          final int initialQuantity = initialPurchase.getInt(productionRule);
          rule.setQuantity(initialQuantity);
          m_rules.add(rule);
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
            if (!unitsAllowed.contains(ut)) {
              unitsAllowed.add(ut);
              final IntegerMap<NamedAttachable> result = new IntegerMap<>();
              result.add(ut, 1);
              final IntegerMap<Resource> cost = new IntegerMap<>();
              cost.add(data.getResourceList().getResource(Constants.PUS), 1);
              final ProductionRule newRule = new ProductionRule(ut.getName(), data, result, cost);
              final Rule rule = new Rule(newRule, player);
              rule.setQuantity(0);
              m_rules.add(rule);
            }
          }
        }
      }
      // now check if we have the art for anything that is left
      for (final UnitType ut : data.getUnitTypeList().getAllUnitTypes()) {
        if (!unitsAllowed.contains(ut)) {
          try {
            final UnitImageFactory imageFactory = m_uiContext.getUnitImageFactory();
            if (imageFactory != null) {
              final Optional<Image> unitImage = imageFactory.getImage(ut, player, data, false, false);
              if (unitImage.isPresent()) {
                unitsAllowed.add(ut);
                final IntegerMap<NamedAttachable> result = new IntegerMap<>();
                result.add(ut, 1);
                final IntegerMap<Resource> cost = new IntegerMap<>();
                cost.add(data.getResourceList().getResource(Constants.PUS), 1);
                final ProductionRule newRule = new ProductionRule(ut.getName(), data, result, cost);
                final Rule rule = new Rule(newRule, player);
                rule.setQuantity(0);
                m_rules.add(rule);
              }
            }
          } catch (final Exception e) { // ignore
          }
        }
      }
    } finally {
      m_data.releaseReadLock();
    }
  }
}
