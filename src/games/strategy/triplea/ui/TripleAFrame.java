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
 * TripleAFrame.java
 *
 * Created on November 5, 2001, 1:32 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.URL;
import javax.swing.*;


import games.strategy.engine.data.*;
import games.strategy.engine.data.events.*;
import games.strategy.engine.framework.*;
import games.strategy.engine.gamePlayer.PlayerBridge;
import games.strategy.engine.message.*;
import games.strategy.engine.transcript.*;

import games.strategy.ui.*;
import games.strategy.util.*;

import games.strategy.triplea.*;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.image.*;
import games.strategy.triplea.delegate.message.*;




/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Main frame for the triple a game
 */
public class TripleAFrame extends JFrame
{	
	private final GameData m_data;
	private MapPanel m_mapPanel;
	private ImageScrollerSmallView m_smallView;
	private JLabel m_message = new JLabel("No selection");
	private ActionButtons m_actionButtons;
	//a set of TripleAPlayers
	private Set m_localPlayers;

	/** Creates new TripleAFrame */
    public TripleAFrame(IGame game, Set players) throws IOException
	{

		super("TripleA");

		
		this.m_data = game.getData();
		this.m_localPlayers = players;
		
		game.getTranscript().addTranscriptListener(m_transcriptListener);
		
		this.addWindowListener(WINDOW_LISTENER);
		
		createMenuBar();
		
		System.out.print("Loading unit images");
		long now = System.currentTimeMillis();
		UnitIconImageFactory.instance().load(this);
		System.out.println(" done:" + (((double) System.currentTimeMillis() - now) / 1000.0) + "s");;
		
		System.out.print("Loading flag images");
		now = System.currentTimeMillis();
		FlagIconImageFactory.instance().load(this);
		System.out.println(" done:" + (((double) System.currentTimeMillis() - now) / 1000.0) + "s");

		System.out.print("Loading maps");
		now = System.currentTimeMillis();
		MapImage.getInstance().loadMaps(m_data);
		
		Image small = MapImage.getInstance().getSmallMapImage();
		m_smallView = new ImageScrollerSmallView(small);
		
		Image large =  MapImage.getInstance().getLargeMapImage();
		m_mapPanel = new MapPanel(large,m_data, m_smallView);
		m_mapPanel.addMapSelectionListener(MAP_SELECTION_LISTENER);
		
		
		System.out.println(" done:" + (((double) System.currentTimeMillis() - now) / 1000.0) + "s");
		
		ImageScrollControl control = new ImageScrollControl(m_mapPanel, m_smallView);
		
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(m_message, BorderLayout.SOUTH);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(m_mapPanel, BorderLayout.CENTER);
		
		JPanel rightHandSide = new JPanel();
		rightHandSide.setLayout(new BorderLayout());
		rightHandSide.add(m_smallView, BorderLayout.NORTH);
		
		JTabbedPane tabs = new JTabbedPane();
		rightHandSide.add(tabs, BorderLayout.CENTER);
		
		m_actionButtons = new ActionButtons(m_data, m_mapPanel, this);
		tabs.addTab( "Actions", m_actionButtons);
		
		StatPanel stats = new StatPanel(m_data);
		tabs.addTab("Stats", stats);
		
		mainPanel.add(rightHandSide, BorderLayout.EAST);
		
		this.getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

	private void createMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		fileMenu.add( new AbstractAction("Exit")
			{
				public void actionPerformed(ActionEvent e)
				{
					System.exit(0);
				}
			}
		);
		
		JMenu helpMenu = new JMenu("Help");
		menuBar.add(helpMenu);
		helpMenu.add( new AbstractAction("About")
			{
				public void actionPerformed(ActionEvent e)
				{
					String text = "TripleA version:" +  games.strategy.engine.EngineVersion.VERSION.toString() + "\nhttp://sourceforge.net/projects/triplea";
					JOptionPane.showMessageDialog(TripleAFrame.this,text, "TripleA", JOptionPane.PLAIN_MESSAGE);
				}
			}
		);
		this.setJMenuBar(menuBar);
	}
	
	public static final WindowListener WINDOW_LISTENER = new WindowAdapter()
	{
		public void windowClosing(WindowEvent e) 
		{
			System.exit(0);
		}
	};	
	
	public final MapSelectionListener  MAP_SELECTION_LISTENER = new MapSelectionListener ()
	{
		Territory in;
		
		public void territorySelected(Territory territory, MouseEvent me)
		{}
		
		public void mouseEntered(Territory territory)
		{
			in = territory;
			refresh();
		}
		
		void refresh()
		{
			StringBuffer buf = new StringBuffer();
			buf.append(in == null ? "none" : in.getName());
			if(in != null)
			{
				TerritoryAttatchment ta = TerritoryAttatchment.get(in);
				if(ta != null)
				{
					int production = ta.getProduction();
					if(production > 0)
						buf.append(" production:" + production);
				}
			}
			m_message.setText(buf.toString());
		}
	};
	
	public IntegerMap getProduction(PlayerID player)
	{
		m_actionButtons.changeToProduce(player);
		return m_actionButtons.waitForPurchase();
	}

	public MoveMessage getMove(PlayerID player, PlayerBridge bridge)
	{
		m_actionButtons.changeToMove(player);
		return m_actionButtons.waitForMove(bridge);
	}
	
	public PlaceMessage getPlace(PlayerID player)
	{
		m_actionButtons.changeToPlace(player);
		return m_actionButtons.waitForPlace();
	}
	
	public Message listBattle(BattleStepMessage msg)
	{
		return m_actionButtons.listBattle(msg);
	}

	public FightBattleMessage getBattle(PlayerID player, Collection battles, Collection bombingRaids)
	{
		m_actionButtons.changeToBattle(player, battles, bombingRaids);
		return m_actionButtons.waitForBattleSelection();
	}
	
	public SelectCasualtyMessage getCasualties(PlayerID player, SelectCasualtyQueryMessage msg)
	{
		return m_actionButtons.getCasualties(player, msg);
	}

	public Message battleStringMessage(BattleStringMessage message)
	{
		return m_actionButtons.battleStringMessage(message);
	}
	
	public RetreatMessage getRetreat(RetreatQueryMessage rqm)
	{
		return m_actionButtons.getRetreat(rqm);
	}
	
	public Message battleInfo(BattleInfoMessage msg)
	{
		return m_actionButtons.battleInfo(msg);
	}
	
	public void notifyError(String message)
	{
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	public void notifyMessage(String message)
	{
		JOptionPane.showMessageDialog(this, message, message, JOptionPane.PLAIN_MESSAGE);
	}

	public boolean getOKToLetAirDie(String message)
	{
		String ok = "Kill air";
		String cancel = "Keep moving";
		String[] options = {cancel,ok};
		int choice = JOptionPane.showOptionDialog(this, message, "Air cannot land", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, cancel);
		return choice == 1;
	}
	
	public boolean getOK(String message)
	{
		int choice = JOptionPane.showConfirmDialog(this, message, message, JOptionPane.OK_CANCEL_OPTION);
		return choice == JOptionPane.OK_OPTION;
	}
	
	public boolean getStrategicBombingRaid(StrategicBombQuery query)
	{
		String message = "Bomb in " + query.getLocation().getName();
		String bomb = "Bomb";
		String normal = "Attack";
		String[] choices = {bomb, normal};
		int choice = JOptionPane.showOptionDialog(this, message, "Bomb?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, choices, bomb);
		return choice == 0;
	}
	
	public IntegerMessage getTechRolls(PlayerID id)
	{
		m_actionButtons.changeToTech(id);
		return m_actionButtons.waitForTech();
	}
	
	public TerritoryMessage getRocketAttack(Collection territories)
	{	
		JList list = new JList(new Vector(territories));
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		JScrollPane scroll = new JScrollPane(list);
		String[] options = {"OK"};
		JOptionPane.showOptionDialog(this, scroll, "Select rocket attack", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
		Territory selected = (Territory) list.getSelectedValue();
		return new TerritoryMessage(selected);
	}	

	public boolean playing(PlayerID id)
	{
		if(id == null)
			return false;
		
		Iterator iter = m_localPlayers.iterator();
		while(iter.hasNext())
		{
			TripleAPlayer player = (TripleAPlayer) iter.next();
			if(player.getID().equals(id))
				return true;
		}
		return false;
	}
	
	private ITranscriptListener m_transcriptListener = new ITranscriptListener()
	{
		public void messageRecieved(TranscriptMessage msg)
		{
			if(msg.getChannel() == TranscriptMessage.PRIORITY_CHANNEL)
			{
				JOptionPane.showMessageDialog(TripleAFrame.this, msg.getMessage(), "Game Over", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	};

}
