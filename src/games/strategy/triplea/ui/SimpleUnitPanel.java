package games.strategy.triplea.ui;

import javax.swing.*;
import java.util.*;
import games.strategy.util.IntegerMap;
import games.strategy.triplea.image.UnitIconImageFactory;
import games.strategy.engine.data.*;

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

    Iterator iter = units.keySet().iterator();
    while(iter.hasNext())
    {
      ProductionRule productionRule = (ProductionRule) iter.next();
      JLabel label = new JLabel();
      label.setText(" x " + units.getInt(productionRule));
      UnitType unit = (UnitType) productionRule.getResults().keySet().iterator().next();
      label.setIcon(UnitIconImageFactory.instance().getIcon(unit, player, data));
      add(label);
    }
  }
}