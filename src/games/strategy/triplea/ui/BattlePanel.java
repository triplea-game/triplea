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
 * BattlePanel.java
 *
 * Created on December 4, 2001, 7:00 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.data.events.*;

import games.strategy.triplea.delegate.message.*;

/**
 *
 * UI for fighting battles.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class BattlePanel extends ActionPanel
{	
	private JLabel m_actionLabel = new JLabel();
	private FightBattleMessage m_fightBattleMessage;
	private DefaultListModel m_listModel = new DefaultListModel();
	private JList m_list = new JList(m_listModel);
	private MyListSelectionModel m_listSelectionModel = new MyListSelectionModel();
	private TripleAFrame m_parent;
	
	private static Font BOLD;
	static
	{
		Map atts = new HashMap();
		atts.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
		BOLD = new Font(atts);
	}
	
	/** Creates new BattlePanel */
    public BattlePanel(GameData data, MapPanel map, TripleAFrame parent) 
	{
		super(data, map);
		m_list.setBackground(this.getBackground());
		m_list.setSelectionModel(m_listSelectionModel);
		m_parent = parent;
    }
	
	public void display(PlayerID id, Collection battles, Collection bombing)
	{
		super.display(id);
		removeAll();
		m_actionLabel.setText(id.getName() + " battle");
		add(m_actionLabel);
		Iterator iter = battles.iterator();
		while(iter.hasNext() )
		{
			Action action = new FightBattleAction((Territory) iter.next(), false);
			add(new JButton(action));
		}
		
		iter = bombing.iterator();
		while(iter.hasNext() )
		{
			Action action = new FightBattleAction((Territory) iter.next(), true);
			add(new JButton(action));
		}
		SwingUtilities.invokeLater(REFRESH);
	}
	
	public Message battleInfo(BattleInfoMessage msg)
	{
		if(!m_parent.playing(msg.getDontNotify()))
		{
			setStep(msg);
			String ok = "OK";
			String[] options =  {ok};
			JOptionPane.showOptionDialog(getTopLevelAncestor(), msg.getMessage(), msg.getShortMessage(), JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, ok);
		}
		return null;
	}
	
	public Message listBattle(BattleStepMessage msg)
	{
		removeAll();
		
		if(msg.getSteps().isEmpty())
		{
			SwingUtilities.invokeLater(REFRESH);
			return null;
		}
		
		getMap().centerOn(msg.getTerritory());
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		JTextArea text = new JTextArea();
		
		text.setFont(BOLD);
		text.setEditable(false);
		text.setBackground(this.getBackground());
		text.setText(msg.getTitle());
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		panel.add(text, BorderLayout.NORTH);
		
		m_listModel.removeAllElements();

		
		Iterator iter = msg.getSteps().iterator();
		while(iter.hasNext())
		{
			m_listModel.addElement(iter.next());
		}
		m_listSelectionModel.hiddenSetSelectionInterval(0);
		panel.add(m_list, BorderLayout.CENTER);
		add(panel);
		SwingUtilities.invokeLater(REFRESH);
		
		return null;
	}

	public FightBattleMessage waitForBattleSelection()
	{
		try
		{
			synchronized(getLock())
			{
				getLock().wait();
			}
		} catch(InterruptedException ie)
		{
			waitForBattleSelection();
		}
		
		if(m_fightBattleMessage != null)
			getMap().centerOn(m_fightBattleMessage.getTerritory());
		
		return m_fightBattleMessage;
	}
	
	/**
	 * Walks through and pause at each list item.
	 */
	private void walkStep(final int start, final int stop)
	{
		if(start < 0 || stop < 0 || stop >= m_listModel.getSize())
			throw new IllegalStateException("Illegal start and stop.  start:" + start + " stop:" + stop);
		
		Object lock = new Object();
		
		int current = start;
		while(current != stop)
		{
			if(current == 0)
				pause();
			
			current++;
			if( current >= m_listModel.getSize())
			{
				current = 0;
			}
			
			final int set = current;
			Runnable r = new Runnable()
			{
				public void run()
				{	
					m_listSelectionModel.hiddenSetSelectionInterval(set);
				}
			};
			
			try
			{
				SwingUtilities.invokeAndWait(r);
				pause();
			} catch(InterruptedException ie) 
			{
			} catch(java.lang.reflect.InvocationTargetException ioe)
			{
				ioe.printStackTrace();
				throw new RuntimeException(ioe.getMessage());
			}
		}
	}
	
	private void pause()
	{
		Object lock = new Object();
		try
		{
			synchronized(lock)
			{
				lock.wait(850);
			}
		} catch(InterruptedException ie) 
		{
		}
	}
	
	private void setStep(BattleMessage msg)
	{
		if(msg.getStep() != null)
		{
			int newIndex = m_listModel.lastIndexOf(msg.getStep());
			int currentIndex = m_list.getSelectedIndex();
			if(newIndex != -1)
				walkStep(currentIndex,newIndex );
		}
	}
	
	public SelectCasualtyMessage getCasualties(PlayerID player, SelectCasualtyQueryMessage msg)
	{
		setStep(msg);
		
		boolean plural = msg. getCount() > 1;
		UnitChooser chooser = new UnitChooser(msg.getSelectFrom(), msg.getDependent());
		
		String messageText = msg.getMessage() + " " + player.getName() + " select " + msg.getCount() + (plural ? " casualties" :" casualty") + ".";
		chooser.setTitle(messageText);
		chooser.setMax(msg.getCount());
		String[] options = {"OK"};
		int option = JOptionPane.showOptionDialog( getRootPane(), chooser, player.getName() + " select casualties", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
		Collection choosen = chooser.getSelected(false);
		SelectCasualtyMessage response = new SelectCasualtyMessage(choosen);
		return response;
	}

	public Message battleStringMessage(BattleStringMessage message)
	{
		setStep(message);
		JOptionPane.showMessageDialog(getRootPane(), message.getMessage(), message.getMessage(), JOptionPane.PLAIN_MESSAGE);
		return null;
	}
	
	public RetreatMessage getRetreat(RetreatQueryMessage rqm)
	{
		setStep(rqm);
		
		String message = rqm.getMessage();
		String ok = "Retreat";
		String cancel ="Cancel";
		String[] options ={ok, cancel};
		int choice = JOptionPane.showOptionDialog(getTopLevelAncestor(), message, "Retreat?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, cancel);
		boolean retreat = (choice == 0);
		if(!retreat)
			return null;
				
		RetreatComponent comp = new RetreatComponent(rqm);
		int option = JOptionPane.showConfirmDialog( getTopLevelAncestor(), comp,rqm.getMessage(), JOptionPane.OK_CANCEL_OPTION);
		if(option == JOptionPane.OK_OPTION)
		{
			if(comp.getSelection() != null)
				return new RetreatMessage(comp.getSelection());
		}

		return null;	
	}

	private class RetreatComponent extends JPanel
	{
		RetreatQueryMessage m_query;
		JList m_list;
		
		RetreatComponent(RetreatQueryMessage rqm)
		{
			this.setLayout(new BorderLayout());
			
			JLabel label = new JLabel("Retreat to...");
			add(label, BorderLayout.NORTH);
			
			m_query = rqm;
			Vector listElements = new Vector(rqm.getTerritories());
			
			m_list = new JList(listElements);
			m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane scroll = new JScrollPane(m_list);
			add(scroll, BorderLayout.CENTER);
			m_listSelectionModel.hiddenSetSelectionInterval(0);
		}
		
		public Territory getSelection()
		{
			return (Territory) m_list.getSelectedValue();
		}
	}

	class FightBattleAction extends AbstractAction
	{
		Territory m_territory;
		boolean m_bomb;
		
		FightBattleAction(Territory battleSite, boolean bomb)
		{
			super( (bomb ? "Bombing raid in " :  "Battle in ") + battleSite.getName());
			m_territory = battleSite;
			m_bomb = bomb;
		}
		
		public void actionPerformed(ActionEvent actionEvent) 
		{
			m_fightBattleMessage = new FightBattleMessage(m_territory, m_bomb);
			synchronized(getLock())
			{
				getLock().notify();
			}
		}		
	}
	
	public String toString()
	{
		return "BattlePanel";
	}	
}

/**
 * Doesnt allow the user to change the selection, 
 * must be done through hiddenSetSelectionInterval.
 */
class MyListSelectionModel extends DefaultListSelectionModel
{
	public void setSelectionInterval(int index0, int index1)
	{
	}

	public void hiddenSetSelectionInterval(int index)
	{
		super.setSelectionInterval(index, index);
	}
}