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


import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.Die.DieType;

import java.awt.Dimension;
import java.awt.event.*;

import javax.swing.*;
import java.util.*;


public class DiceChooser extends JPanel
{
    
  private final UIContext m_uiContext;  
  private JPanel m_dicePanel; 
  private int[] m_random;
  private int m_diceCount = 0;
  private int m_numRolls = 0;
  private int m_hitAt = 0;
  private boolean m_hitOnlyIfEquals = false;
  private Collection<JButton> m_buttons; 
  private JButton m_undoButton;
  private JLabel m_diceCountLabel;
    
  public DiceChooser(UIContext uiContext, int numRolls, int hitAt, boolean hitOnlyIfEquals)
  {
    m_uiContext = uiContext;
    m_numRolls = numRolls;
    m_hitAt = hitAt; 
    m_hitOnlyIfEquals = hitOnlyIfEquals;
    m_buttons = new ArrayList<JButton>(Constants.MAX_DICE);
    m_random = new int[numRolls];
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    createComponents();
  }

  public void clear()
  {
    m_dicePanel.removeAll();
    for (int i=0; i<m_diceCount; i++)
        m_random[i] = 0;
    m_diceCount = 0;
  }

  public int[] getDice()
  {
      if (m_diceCount < m_numRolls)
          return null;
      return m_random;
  }

  private void addDie(int roll)
  {
      boolean hit = (roll == m_hitAt || (!m_hitOnlyIfEquals && (m_hitAt > 0) && roll > m_hitAt));
      DieType dieType = hit ? DieType.HIT : DieType.MISS;
      m_dicePanel.add(new JLabel(m_uiContext.getDiceImageFactory().getDieIcon(roll, dieType)));
      m_dicePanel.add(Box.createHorizontalStrut(2));
      m_random[m_diceCount++] = roll - 1;
      updateDiceCount();

      validate();
      invalidate();
      repaint();
  }

  private void removeLastDie()
  {
      // remove the strut and the component
      int lastIndex = m_dicePanel.getComponentCount() - 1;
      m_dicePanel.remove(lastIndex);
      m_dicePanel.remove(lastIndex - 1);
      m_diceCount--;
      updateDiceCount();

      validate();
      invalidate();
      repaint();
  }

  private void updateDiceCount()
  {
      boolean showButtons = (m_diceCount < m_numRolls );

      Iterator<JButton> buttonIter = m_buttons.iterator();
      while (buttonIter.hasNext())
      {
          JButton button = buttonIter.next();
          button.setEnabled(showButtons);
      }

      m_undoButton.setEnabled((m_diceCount > 0));
      m_diceCountLabel.setText("Dice remaining: "+(m_numRolls - m_diceCount));
  }

  private void createComponents()
  {
    JPanel diceButtonPanel = new JPanel();
    diceButtonPanel.setLayout(new BoxLayout(diceButtonPanel, BoxLayout.X_AXIS));
    diceButtonPanel.add(Box.createHorizontalStrut(40));
    for(int roll = 1; roll <= Constants.MAX_DICE; roll++)
    {
      boolean hit = (roll == m_hitAt || (!m_hitOnlyIfEquals && (m_hitAt > 0) && roll > m_hitAt));

      diceButtonPanel.add(Box.createHorizontalStrut(4));

      final int dieNum = roll;
      DieType dieType = hit ? DieType.HIT : DieType.MISS;
      JButton button = new JButton(new AbstractAction(null, m_uiContext.getDiceImageFactory().getDieIcon(roll, dieType))
          {
              public void actionPerformed(ActionEvent event)
                {
                    addDie(dieNum);
                }
          });
      m_buttons.add(button);
      button.setPreferredSize(new Dimension(m_uiContext.getDiceImageFactory().DIE_WIDTH+4, m_uiContext.getDiceImageFactory().DIE_HEIGHT+4));
      diceButtonPanel.add(button);
    }

    diceButtonPanel.add(Box.createHorizontalStrut(4));
    m_undoButton = new JButton(new AbstractAction("Undo")
      {
          public void actionPerformed(ActionEvent event)
            {
                removeLastDie();
            }
      });
    diceButtonPanel.add(m_undoButton);
    diceButtonPanel.add(Box.createHorizontalStrut(40));

    m_diceCountLabel = new JLabel("Dice remaining:   ");
    JPanel labelPanel = new JPanel();
    labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
    labelPanel.add(m_diceCountLabel);

    m_dicePanel = new JPanel();
    m_dicePanel.setBorder(BorderFactory.createLoweredBevelBorder());
    m_dicePanel.setLayout(new BoxLayout(m_dicePanel, BoxLayout.X_AXIS));

    JScrollPane scroll = new JScrollPane(m_dicePanel);
    scroll.setBorder(null);
    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    //we're adding to a box layout, so to prevent the component from
    //grabbing extra space, set the max height.
    //allow room for a dice and a scrollbar
    scroll.setMinimumSize(new Dimension(scroll.getMinimumSize().width, m_uiContext.getDiceImageFactory().DIE_HEIGHT + 17));
    scroll.setMaximumSize(new Dimension(scroll.getMaximumSize().width, m_uiContext.getDiceImageFactory().DIE_HEIGHT + 17));
    scroll.setPreferredSize(new Dimension(scroll.getPreferredSize().width, m_uiContext.getDiceImageFactory().DIE_HEIGHT + 17));
    add(scroll);
    add(Box.createVerticalStrut(8));
    add(labelPanel);
    add(Box.createVerticalStrut(8));
    add(diceButtonPanel);
    updateDiceCount();
  }

}
