/**
 * Created on 19.02.2012
 */
package games.strategy.engine.chat;

import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.framework.IGame;
import games.strategy.net.INode;
import games.strategy.triplea.ui.UIContext;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.SwingConstants;

public class PlayerChatRenderer extends DefaultListCellRenderer
{
	private final IGame m_game;
	private final UIContext m_uiContext;
	
	int m_maxIconCounter = 0;
	
	HashMap<String, List<Icon>> m_iconMap = new HashMap<String, List<Icon>>();
	
	public PlayerChatRenderer(final IGame game, final UIContext uiContext)
	{
		m_game = game;
		m_uiContext = uiContext;
		setIconMap();
	}
	
	@Override
	public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		super.getListCellRendererComponent(list, ((INode) value).getName(), index, isSelected, cellHasFocus);
		final List<Icon> icons = m_iconMap.get(value.toString());
		if (icons != null)
		{
			setHorizontalTextPosition(SwingConstants.LEFT);
			setIcon(new CompositeIcon(icons));
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
					icons.add(new ImageIcon(m_uiContext.getFlagImageFactory().getSmallFlag(playerList.getPlayerID(player))));
				}
				m_maxIconCounter = Math.max(m_maxIconCounter, icons.size());
				m_iconMap.put(playerNode.toString(), icons);
			}
		}
	}
	
	public int getMaxIconCounter()
	{
		return m_maxIconCounter;
	}
}
