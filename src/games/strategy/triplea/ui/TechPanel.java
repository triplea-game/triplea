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

/*
 * TechPanel.java
 *
 * Created on December 5, 2001, 7:04 PM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.*;
import games.strategy.triplea.delegate.dataObjects.TechRoll;
import games.strategy.ui.*;
import games.strategy.util.Util;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class TechPanel extends ActionPanel
{
    private JLabel m_actionLabel = new JLabel();
    private TechRoll m_techRoll;
    private int m_currTokens = 0;
    private int m_quantity;
    
    /** Creates new BattlePanel */
    public TechPanel(GameData data, MapPanel map)
    {
        super(data, map);
    }

    @Override
	public void display(final PlayerID id)
    {
        super.display(id);
        SwingUtilities.invokeLater(new Runnable()
        {
        
            @Override
			public void run()
            {
                removeAll();
                m_actionLabel.setText(id.getName() + " Tech Roll");
                add(m_actionLabel);
                
                if(isWW2V3TechModel())
                {
                    add(new JButton(GetTechTokenAction));
                    add(new JButton(JustRollTech));

                }
                else
                {
                    add(new JButton(GetTechRollsAction));
                    add(new JButton(DontBother));
                }
                
                getData().acquireReadLock();
                try
                {
                    getMap().centerOn(TerritoryAttachment.getCapital(id, getData()));
                }
                finally 
                {
                    getData().releaseReadLock();
                }
            }
        
        });

    }

    @Override
	public String toString()
    {
        return "TechPanel";
    }

    public TechRoll waitForTech()
    {
        if(getAvailableTechs().isEmpty())
            return null;
        
        waitForRelease();

        if (m_techRoll == null)
            return null;

        if (m_techRoll.getRolls() == 0)
            return null;

        return m_techRoll;
    }

    private List<TechAdvance> getAvailableTechs()
    {
        getData().acquireReadLock();
        try
        {
            Collection<TechAdvance> currentAdvances = TechTracker.getTechAdvances(getCurrentPlayer(),getData());
            Collection<TechAdvance> allAdvances = TechAdvance.getTechAdvances(getData(),getCurrentPlayer());
            return Util.difference(allAdvances, currentAdvances);
        }
        finally 
        {
            getData().releaseReadLock();
        }
    }
    
    private List<TechnologyFrontier> getAvailableCategories()
    {
        getData().acquireReadLock();
        try
        {
            Collection<TechnologyFrontier> currentAdvances = TechTracker.getTechCategories(getData(),getCurrentPlayer());
            Collection<TechnologyFrontier> allAdvances = TechAdvance.getTechCategories(getData(),getCurrentPlayer());
            return Util.difference(allAdvances, currentAdvances);
        }
        finally 
        {
            getData().releaseReadLock();
        }
    }
    

    private Action GetTechRollsAction = new AbstractAction("Roll Tech...")
    {
        @Override
		public void actionPerformed(ActionEvent event)
        {
            TechAdvance advance = null;
           
            if (isWW2V2() || isSelectableTechRoll())
            {
                List<TechAdvance> available = getAvailableTechs();
                if (available.isEmpty())
                {
                    JOptionPane.showMessageDialog(TechPanel.this, "No more available tech advances");
                    return;
                }

                JList list = new JList(new Vector<TechAdvance>(available));
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.add(list, BorderLayout.CENTER);
                panel.add(new JLabel("Select the tech you want to roll for"), BorderLayout.NORTH);
                list.setSelectedIndex(0);
                JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(TechPanel.this), panel, "Select advance", JOptionPane.PLAIN_MESSAGE);
                advance = (TechAdvance) list.getSelectedValue();
            }

            int PUs = getCurrentPlayer().getResources().getQuantity(Constants.PUS);
            String message = "Roll Tech";
            TechRollPanel techRollPanel = new TechRollPanel(PUs, getCurrentPlayer());
            int choice = JOptionPane.showConfirmDialog(getTopLevelAncestor(), techRollPanel, message, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null);
            if (choice != JOptionPane.OK_OPTION)
                return;

            int quantity = techRollPanel.getValue();
                        
            if (advance == null)
                m_techRoll = new TechRoll(null, quantity);            
            else {
            	TechnologyFrontier front = new TechnologyFrontier("",getData());
                front.addAdvance(advance);
            	m_techRoll = new TechRoll(front, quantity);
            }
            release();

        }
    };
    
    private Action DontBother = new AbstractAction("Done")
    {
        @Override
		public void actionPerformed(ActionEvent event)
        {
            m_techRoll = null;
            release();
            
        }
    };
    
    private Action GetTechTokenAction = new AbstractAction("Buy Tech Tokens...")
    {
        @Override
		public void actionPerformed(ActionEvent event)
        {            
            m_currTokens = getCurrentPlayer().getResources().getQuantity(Constants.TECH_TOKENS);
            //Notify user if there are no more techs to acheive
            List<TechnologyFrontier> techCategories = getAvailableCategories();
            
            //if (available.isEmpty())
            if (techCategories.isEmpty())
            {
                JOptionPane.showMessageDialog(TechPanel.this, "No more available tech advances");
                return;
            }
            
            TechnologyFrontier category = null;
            JList list = new JList(new Vector<TechnologyFrontier>(techCategories));
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.add(list, BorderLayout.CENTER);
            panel.add(new JLabel("Select which tech chart you want to roll for"), BorderLayout.NORTH);
            list.setSelectedIndex(0);
            JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(TechPanel.this), panel, "Select chart", JOptionPane.PLAIN_MESSAGE);
            category = (TechnologyFrontier) list.getSelectedValue();
            
            int PUs = getCurrentPlayer().getResources().getQuantity(Constants.PUS);
            
            String message = "Purchase Tech Tokens";
            TechTokenPanel techTokenPanel = new TechTokenPanel(PUs, m_currTokens, getCurrentPlayer());
            int choice = JOptionPane.showConfirmDialog(getTopLevelAncestor(), techTokenPanel, message, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null);
            if (choice != JOptionPane.OK_OPTION)
                return;

            m_quantity = techTokenPanel.getValue();
            
            m_currTokens += m_quantity;
            //getCurrentPlayer().getResources().addResource(getData().getResourceList().getResource(Constants.TECH_TOKENS), m_quantity);
                        
            m_techRoll = new TechRoll(category, m_currTokens, m_quantity);
            m_techRoll.setNewTokens(m_quantity);
            
            release();
        }
    };

    private Action JustRollTech = new AbstractAction("Done/Roll Current Tokens")
    {
        @Override
		public void actionPerformed(ActionEvent event)
        {
            m_currTokens = getCurrentPlayer().getResources().getQuantity(Constants.TECH_TOKENS);
            //If this player has tokens, roll them.
            if(m_currTokens > 0)
              {  
                 List<TechnologyFrontier> techCategories = getAvailableCategories();
                 
                 if (techCategories.isEmpty())
                 {
                   return;
                 }
                 
                 TechnologyFrontier category = null;
                 JList list = new JList(new Vector<TechnologyFrontier>(techCategories));
                 JPanel panel = new JPanel();
                 panel.setLayout(new BorderLayout());
                 panel.add(list, BorderLayout.CENTER);
                 panel.add(new JLabel("Select which tech chart you want to roll for"), BorderLayout.NORTH);
                 list.setSelectedIndex(0);
                 JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(TechPanel.this), panel, "Select chart", JOptionPane.PLAIN_MESSAGE);
                 category = (TechnologyFrontier) list.getSelectedValue();
                 m_techRoll = new TechRoll(category, m_currTokens);
              }
            else
                m_techRoll = null;
            release();            
        }
    };
    
}

class TechRollPanel extends JPanel
{
    int m_PUs;
    PlayerID m_player;
    JLabel m_left = new JLabel();
    ScrollableTextField m_textField;

    TechRollPanel(int PUs, PlayerID player)
    {
        setLayout(new GridBagLayout());
        m_PUs = PUs;
        m_player = player;
        JLabel title = new JLabel("Select the number of tech rolls:");
        title.setBorder(new javax.swing.border.EmptyBorder(5, 5, 5, 5));
        m_textField = new ScrollableTextField(0, PUs / TechTracker.getTechCost(player));
        m_textField.addChangeListener(m_listener);
        JLabel costLabel = new JLabel("x5");
        setLabel(PUs);
        int space = 0;
        add(title, new GridBagConstraints(0, 0, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, space, space), 0, 0));
        add(m_textField, new GridBagConstraints(0, 1, 1, 1, 0.5, 1, GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(8, 10, space, space), 0, 0));
        add(costLabel, new GridBagConstraints(1, 1, 1, 1, 0.5, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 5, space, 2), 0, 0));
        add(m_left, new GridBagConstraints(0, 2, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, space, space), 0, 0));
    }

    private void setLabel(int PUs)
    {
        m_left.setText("Left to spend:" + PUs);
    }

    private ScrollableTextFieldListener m_listener = new ScrollableTextFieldListener()
    {
        @Override
		public void changedValue(ScrollableTextField stf)
        {
        	setLabel(m_PUs - (TechTracker.getTechCost(m_player) * m_textField.getValue()));
        }
    };

    public int getValue()
    {
        return m_textField.getValue();
    }
}

class TechTokenPanel extends JPanel
{
    int m_PUs;
    PlayerID m_player;
    JLabel m_left = new JLabel();
    JLabel m_right = new JLabel();
    ScrollableTextField m_textField;

    TechTokenPanel(int PUs, int currTokens, PlayerID player)
    {
        setLayout(new GridBagLayout());
        m_PUs = PUs;
        m_player = player;
        JLabel title = new JLabel("Select the number of tech tokens to purchase:");
        title.setBorder(new javax.swing.border.EmptyBorder(5, 5, 5, 5));
        m_textField = new ScrollableTextField(0, PUs / TechTracker.getTechCost(m_player));
        m_textField.addChangeListener(m_listener);
        JLabel costLabel = new JLabel("x"+TechTracker.getTechCost(m_player));
        setLabel(PUs);
        setTokens(currTokens);
        int space = 0;
        add(title, new GridBagConstraints(0, 0, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, space, space), 0, 0));
        add(m_textField, new GridBagConstraints(0, 1, 1, 1, 0.5, 1, GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(8, 10, space, space), 0, 0));
        add(costLabel, new GridBagConstraints(1, 1, 1, 1, 0.5, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 5, space, 2), 0, 0));
        add(m_left, new GridBagConstraints(0, 2, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, space, space), 0, 0));
        add(m_right, new GridBagConstraints(0, 2, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 120, space, space), 0, 0));
    }

    private void setLabel(int PUs)
    {
        m_left.setText("Left to spend:" + PUs);
    }

    private void setTokens(int tokens)
    {
        m_right.setText("Current token count:" + tokens);
    }
    
    private ScrollableTextFieldListener m_listener = new ScrollableTextFieldListener()
    {
        @Override
		public void changedValue(ScrollableTextField stf)
        {
        	setLabel(m_PUs - (TechTracker.getTechCost(m_player) * m_textField.getValue()));
        }
    };

    public int getValue()
    {
        return m_textField.getValue();
    }

}
