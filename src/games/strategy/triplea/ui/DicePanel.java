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


import java.awt.Dimension;
import javax.swing.*;

import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.message.BombingResults;
import games.strategy.triplea.image.DiceImageFactory;


public class DicePanel extends JPanel
{
  public DicePanel()
  {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  }

  public void clear()
  {
    removeAll();
  }

  public void setDiceRoll(BombingResults results)
  {
    removeAll();

    add(create(results.getDice(), -1, false));

    add(Box.createVerticalGlue());
    add(new JLabel("Cost:" + results.getCost()));

    invalidate();
  }

  public void setDiceRoll(DiceRoll diceRoll)
  {
    removeAll();
    for(int i = 1; i <= 6; i++)
    {

      int[] dice = diceRoll.getRolls(i);
      if(dice.length == 0)
        continue;

      add(new JLabel("Rolled at " + (i) + ":"));
      add(create(diceRoll.getRolls(i), i, diceRoll.getHitOnlyIfEquals()));
    }
    add(Box.createVerticalGlue());
    add(new JLabel("Total hits:" + diceRoll.getHits()));

    invalidate();
  }

  private JComponent create(int[] dice, int rollAt, boolean hitOnlyIfEquals)
  {
    JPanel dicePanel = new JPanel();
    dicePanel.setLayout(new BoxLayout(dicePanel, BoxLayout.X_AXIS));
    dicePanel.add(Box.createHorizontalStrut(20));
    for(int dieIndex = 0; dieIndex < dice.length; dieIndex++)
    {
      int roll = dice[dieIndex] + 1;
      boolean hit = hitOnlyIfEquals ? roll == rollAt : roll <= rollAt;
      dicePanel.add(new JLabel(DiceImageFactory.getInstance().getDieIcon(roll, hit)));
      dicePanel.add(Box.createHorizontalStrut(2));
    }
    JScrollPane scroll = new JScrollPane(dicePanel);
    scroll.setBorder(null);
    scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    //we're adding to a box layout, so to prevent the component from
    //grabbing extra space, set the max height.
    //allow room for a dice and a scrollbar
    scroll.setMinimumSize(new Dimension(scroll.getMinimumSize().width, DiceImageFactory.getInstance().DIE_HEIGHT + 17));
    scroll.setMaximumSize(new Dimension(scroll.getMaximumSize().width, DiceImageFactory.getInstance().DIE_HEIGHT + 17));
    scroll.setPreferredSize(new Dimension(scroll.getPreferredSize().width, DiceImageFactory.getInstance().DIE_HEIGHT + 17));

    return scroll;
  }

}
