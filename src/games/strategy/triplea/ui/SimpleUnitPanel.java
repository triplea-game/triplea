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

import javax.swing.*;
import java.util.*;
import games.strategy.util.IntegerMap;
import games.strategy.triplea.image.UnitIconImageFactory;
import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.*;

/**
 *
 * A Simple panel that displays a list of units.
 *
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class SimpleUnitPanel extends JPanel
{
  public SimpleUnitPanel()
  {

  }

  /**
   *
   * @param units a HashMap in the form ProductionRule -> number of units
   * assumes that each production rule has 1 result, which is simple the number of units
   */
  public void setUnitsFromProductionRuleMap(IntegerMap units, PlayerID player, GameData data)
  {
    removeAll();
    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

    TreeSet productionRules = new TreeSet(productionRuleComparator);
    productionRules.addAll(units.keySet());
    Iterator iter =productionRules.iterator();
    while (iter.hasNext())
    {
        ProductionRule productionRule = (ProductionRule) iter.next();
        JLabel label = new JLabel();
        label.setText(" x " + units.getInt(productionRule));
        UnitType unit = (UnitType) productionRule.getResults().keySet().
            iterator().next();
        label.setIcon(UnitIconImageFactory.instance().getIcon(unit, player,
            data, false));
        add(label);
    }
}

  Comparator productionRuleComparator = new Comparator()
  {
      UnitTypeComparator utc = new UnitTypeComparator();

      public int compare(Object o1, Object o2)
      {
          UnitType u1 = (UnitType)  ((ProductionRule) o1).getResults().keySet().iterator().next();
          UnitType u2 = (UnitType)  ((ProductionRule) o2).getResults().keySet().iterator().next();
          return utc.compare(u1, u2);
      }
  };
}
