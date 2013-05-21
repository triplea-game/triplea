/**
 * Created on 19.02.2012
 */
package games.strategy.engine.chat;

import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.framework.IGame;
import games.strategy.net.INode;
import games.strategy.triplea.ui.IUIContext;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.SwingConstants;

public class PlayerChatRenderer extends DefaultListCellRenderer
{
	private static final long serialVersionUID = -8195565028281374498L;
	private final IGame m_game;
	private final IUIContext m_uiContext;
	
	int m_maxIconCounter = 0;
	
	HashMap<String, List<Icon>> m_iconMap = new HashMap<String, List<Icon>>();
	HashMap<String, Set<String>> m_playerMap = new HashMap<String, Set<String>>();
	
	public PlayerChatRenderer(final IGame game, final IUIContext uiContext)
	{
		m_game = game;
		m_uiContext = uiContext;
		setIconMap();
	}
	
	@Override
	public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		final List<Icon> icons = m_iconMap.get(value.toString());
		if (icons != null)
		{
			super.getListCellRendererComponent(list, ((INode) value).getName(), index, isSelected, cellHasFocus);
			setHorizontalTextPosition(SwingConstants.LEFT);
			setIcon(new CompositeIcon(icons));
		}
		else
		{
			final StringBuilder sb = new StringBuilder(((INode) value).getName());
			final Set<String> players = m_playerMap.get(value.toString());
			if (players != null && !players.isEmpty())
			{
				sb.append(" (");
				final Iterator<String> iter = players.iterator();
				while (iter.hasNext())
				{
					sb.append(iter.next());
					if (iter.hasNext())
						sb.append(", ");
				}
				sb.append(")");
			}
			super.getListCellRendererComponent(list, sb.toString(), index, isSelected, cellHasFocus);
		}
		return this;
	}
	
	private void setIconMap()
	{
		final PlayerManager playerManager = m_game.getPlayerManager();
		
		PlayerList playerList;
		m_game.getData().acquireReadLock();
		try
		{
			playerList = m_game.getData().getPlayerList();
		} finally
		{
			m_game.getData().releaseReadLock();
		}
		for (final INode playerNode : new HashSet<INode>(playerManager.getPlayerMapping().values())) // new HashSet removes duplicates
		{
			final Set<String> players = playerManager.getPlayedBy(playerNode);
			if (players.size() > 0)
			{
				final List<Icon> icons = new ArrayList<Icon>(players.size());
				for (final String player : players)
				{
					if (m_uiContext != null && m_uiContext.getFlagImageFactory() != null)
						icons.add(new ImageIcon(m_uiContext.getFlagImageFactory().getSmallFlag(playerList.getPlayerID(player))));
				}
				m_maxIconCounter = Math.max(m_maxIconCounter, icons.size());
				m_playerMap.put(playerNode.toString(), players);
				if (m_uiContext == null)
					m_iconMap.put(playerNode.toString(), null);
				else
					m_iconMap.put(playerNode.toString(), icons);
			}
		}
	}
	
	public int getMaxIconCounter()
	{
		return m_maxIconCounter;
	}
}
