/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


package games.strategy.triplea.ui;

import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.UnitTypeComparator;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.util.IntegerMap;

import java.util.*;

import javax.swing.*;

/**
 *
 * A Simple panel that displays a list of units.
 *
 */

public class SimpleUnitPanel extends JPanel
{
  private final UIContext m_uiContext;  
    
  public SimpleUnitPanel(UIContext uiContext)
  {
    m_uiContext = uiContext;  
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  }

  /**
   *
   * @param units a HashMap in the form ProductionRule -> number of units
   * assumes that each production rule has 1 result, which is simple the number of units
   */
  public void setUnitsFromProductionRuleMap(IntegerMap<ProductionRule> units, PlayerID player, GameData data)
  {
    removeAll();


    TreeSet<ProductionRule> productionRules = new TreeSet<ProductionRule>(productionRuleComparator);
    productionRules.addAll(units.keySet());
    Iterator<ProductionRule> iter = productionRules.iterator();
    while (iter.hasNext())
    {
      ProductionRule productionRule = iter.next();

      int quantity = units.getInt(productionRule);

      UnitType unit = (UnitType) productionRule.getResults().keySet().
        iterator().next();
      boolean damaged = false;

      addUnits(player, data, quantity, unit, damaged);

    }
  }

  /**
   *
   * @param categories a collection of UnitCategories
   */
  public void setUnitsFromCategories(Collection categories, GameData data)
  {
    removeAll();

    Iterator iter = categories.iterator();
    while (iter.hasNext())
    {
      UnitCategory category = (UnitCategory) iter.next();
      addUnits(category.getOwner(), data, category.getUnits().size(), category.getType(), category.getDamaged());
    }
  }

  private void addUnits(PlayerID player, GameData data, int quantity, UnitType unit, boolean damaged)
  {
    JLabel label = new JLabel();
    label.setText(" x " + quantity);
    label.setIcon(m_uiContext.getUnitImageFactory().getIcon(unit, player,
        data, damaged));
    add(label);
  }

  Comparator<ProductionRule> productionRuleComparator = new Comparator<ProductionRule>()
  {
      UnitTypeComparator utc = new UnitTypeComparator();

      public int compare(ProductionRule o1, ProductionRule o2)
      {
          UnitType u1 = (UnitType)  o1.getResults().keySet().iterator().next();
          UnitType u2 = (UnitType)  o2.getResults().keySet().iterator().next();
          return utc.compare(u1, u2);
      }
  };

  Comparator<RepairRule> repairRuleComparator = new Comparator<RepairRule>()
  {
      UnitTypeComparator utc = new UnitTypeComparator();

      public int compare(RepairRule o1, RepairRule o2)
      {
          UnitType u1 = (UnitType)  o1.getResults().keySet().iterator().next();
          UnitType u2 = (UnitType)  o2.getResults().keySet().iterator().next();
          return utc.compare(u1, u2);
      }
  };
  
}
