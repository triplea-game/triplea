/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * UnitChooser.java
 * 
 * Created on December 3, 2001, 7:32 PM
 */
package games.strategy.triplea.ui;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.ui.Util;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;

/**
 * 
 * @author Tony Clayton
 * @version 1.0
 */
public class PlayerChooser extends JOptionPane
{
	private static final long serialVersionUID = -7272867474891641839L;
	private JList m_list;
	private final PlayerList m_players;
	private final PlayerID m_defaultPlayer;
	private final IUIContext m_uiContext;
	private final boolean m_allowNeutral;
	
	// private JOptionPane m_pane;
	
	/** Creates new PlayerChooser */
	public PlayerChooser(final PlayerList players, final IUIContext uiContext, final boolean allowNeutral)
	{
		this(players, null, uiContext, allowNeutral);
	}
	
	/** Creates new PlayerChooser */
	public PlayerChooser(final PlayerList players, final PlayerID defaultPlayer, final IUIContext uiContext, final boolean allowNeutral)
	{
		setMessageType(JOptionPane.PLAIN_MESSAGE);
		setOptionType(JOptionPane.OK_CANCEL_OPTION);
		setIcon(null);
		m_players = players;
		m_defaultPlayer = defaultPlayer;
		m_uiContext = uiContext;
		m_allowNeutral = allowNeutral;
		createComponents();
	}
	
	private void createComponents()
	{
		final Collection<PlayerID> players = new ArrayList<PlayerID>(m_players.getPlayers());
		if (m_allowNeutral)
			players.add(PlayerID.NULL_PLAYERID);
		m_list = new JList(players.toArray());
		m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_list.setSelectedValue(m_defaultPlayer, true);
		m_list.setFocusable(false);
		m_list.setCellRenderer(new PlayerChooserRenderer(m_players, m_uiContext));
		
		m_list.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(final MouseEvent evt)
			{
				if (evt.getClickCount() == 2)
				{
					// set OK_OPTION on DoubleClick, this fires a property change which causes the dialog to close()
					setValue(OK_OPTION);
				}
			}
		});
		
		setMessage(m_list);
	}
	
	/**
	 * Returns the selected player or null, or null if the dialog was closed
	 * 
	 * @return the player or null
	 */
	public PlayerID getSelected()
	{
		if (getValue() != null && getValue().equals(JOptionPane.OK_OPTION))
		{
			return (PlayerID) m_list.getSelectedValue();
		}
		return null;
	}
	
}


class PlayerChooserRenderer extends DefaultListCellRenderer
{
	private static final long serialVersionUID = -2185921124436293304L;
	private final IUIContext m_uiContext;
	
	PlayerChooserRenderer(final PlayerList players, final IUIContext uiContext)
	{
		m_uiContext = uiContext;
	}
	
	@Override
	public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		super.getListCellRendererComponent(list, ((PlayerID) value).getName(), index, isSelected, cellHasFocus);
		if (m_uiContext == null || (PlayerID) value == PlayerID.NULL_PLAYERID)
			setIcon(new ImageIcon(Util.createImage(32, 32, true)));
		else
			setIcon(new ImageIcon(m_uiContext.getFlagImageFactory().getFlag((PlayerID) value)));
		return this;
	}
}
