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


import games.strategy.triplea.delegate.*;

import java.awt.Dimension;
import java.util.*;

import javax.swing.*;


public class DicePanel extends JPanel
{
    
  private final UIContext m_uiContext;  
    
  public DicePanel(UIContext uiContext)
  {
    m_uiContext = uiContext;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  }

  public void clear()
  {
    removeAll();
  }

  public void setDiceRollForBombing(int[] random, int cost)
  {
    removeAll();

    List<Die> dice = new ArrayList<Die>(random.length);
    for(int i =0; i < random.length; i++)
    {
        dice.add(new Die(random[i]));
    }
    
    add(create(dice, -1));

    add(Box.createVerticalGlue());
    add(new JLabel("Cost:" + cost));

    invalidate();
  }

  public void setDiceRoll(final DiceRoll diceRoll)
  {
    if(!SwingUtilities.isEventDispatchThread() )  
    {

        SwingUtilities.invokeLater(
        new Runnable()        
        {
            public void run()
            {
                setDiceRoll(diceRoll);
            }
        }
        
        );
    
    return;
    }
      
      
    removeAll();
    for(int i = 1; i <= 6; i++)
    {

      List<Die> dice = diceRoll.getRolls(i);
      if(dice.isEmpty())
        continue;

      add(new JLabel("Rolled at " + (i) + ":"));
      add(create(diceRoll.getRolls(i), i));
      
    }
    
    add(Box.createVerticalGlue());
    add(new JLabel("Total hits:" + diceRoll.getHits()));

    validate();
    invalidate();
    repaint();
  }

  private JComponent create(List<Die> dice, int rollAt)
  {
    JPanel dicePanel = new JPanel();
    dicePanel.setLayout(new BoxLayout(dicePanel, BoxLayout.X_AXIS));
    dicePanel.add(Box.createHorizontalStrut(20));
    for(Die die : dice)
    {
      int roll = die.getValue() + 1;
      boolean hit = die.isHit();
      dicePanel.add(new JLabel(m_uiContext.getDiceImageFactory().getDieIcon(roll, hit)));
      dicePanel.add(Box.createHorizontalStrut(2));
    }
    JScrollPane scroll = new JScrollPane(dicePanel);
    scroll.setBorder(null);
    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    //we're adding to a box layout, so to prevent the component from
    //grabbing extra space, set the max height.
    //allow room for a dice and a scrollbar
    scroll.setMinimumSize(new Dimension(scroll.getMinimumSize().width, m_uiContext.getDiceImageFactory().DIE_HEIGHT + 17));
    scroll.setMaximumSize(new Dimension(scroll.getMaximumSize().width, m_uiContext.getDiceImageFactory().DIE_HEIGHT + 17));
    scroll.setPreferredSize(new Dimension(scroll.getPreferredSize().width, m_uiContext.getDiceImageFactory().DIE_HEIGHT + 17));

    return scroll;
  }

}
