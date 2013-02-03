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
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.triplea.Constants;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 * A panel that shows the current political state, this has no other
 * functionality then a view on the current politics.
 * 
 * @author Edwin van der Wal
 * 
 */
public class PoliticalStateOverview extends JPanel
{
	private static final long serialVersionUID = -8445782272897831080L;
	private final UIContext m_uic;
	private final GameData m_data;
	public final static String LABEL_SELF = "----";
	
	/**
	 * Constructs this panel
	 * 
	 * @param data
	 *            gamedata to get the info from
	 * @param uic
	 *            uicontext to use to show this panel.
	 */
	public PoliticalStateOverview(final GameData data, final UIContext uic)
	{
		m_uic = uic;
		m_data = data;
		drawPoliticsUI();
	}
	
	/**
	 * does the actual adding of elements to this panel.
	 */
	private void drawPoliticsUI()
	{
		this.setLayout(new GridBagLayout());
		// draw horizontal labels
		int currentCell = 1;
		final Insets insets = new Insets(5, 2, 5, 2);
		for (final PlayerID p : m_data.getPlayerList())
		{
			this.add(getPlayerLabel(p), new GridBagConstraints(currentCell++, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
		}
		// draw vertical labels and dividers
		currentCell = 1;
		for (final PlayerID p : m_data.getPlayerList())
		{
			this.add(new JSeparator(), new GridBagConstraints(0, currentCell++, 20, 1, 0.1, 0.1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
			this.add(getPlayerLabel(p), new GridBagConstraints(0, currentCell++, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 0, 0));
		}
		// draw cells
		int x = 1;
		int y = 2;
		for (final PlayerID pVertical : m_data.getPlayerList())
		{
			for (final PlayerID pHorizontal : m_data.getPlayerList())
			{
				if (pHorizontal.equals(pVertical))
				{
					this.add(new JLabel(PoliticalStateOverview.LABEL_SELF), new GridBagConstraints(x++, y, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 0, 0));
				}
				else
				{
					this.add(getRelationshipLabel(pVertical, pHorizontal), new GridBagConstraints(x++, y, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
				}
			}
			y = y + 2;
			x = 1;
		}
	}
	
	/**
	 * Gets a label showing the coloured relationshipName between these two
	 * players.
	 * 
	 * @param player1
	 * @param player2
	 * @return
	 */
	private JPanel getRelationshipLabel(final PlayerID player1, final PlayerID player2)
	{
		final RelationshipType relType;
		m_data.acquireReadLock();
		try
		{
			relType = m_data.getRelationshipTracker().getRelationshipType(player1, player2);
		} finally
		{
			m_data.releaseReadLock();
		}
		final JLabel relationshipLabel = new JLabel(relType.getName());
		final JPanel relationshipLabelPanel = new JPanel();
		relationshipLabelPanel.add(relationshipLabel);
		relationshipLabelPanel.setBackground(getRelationshipTypeColor(relType));
		return relationshipLabelPanel;
	}
	
	/**
	 * returns a color to represent the relationship
	 * 
	 * @param relType
	 *            which relationship to get the color for
	 * @return the color to represent this relationship
	 */
	private Color getRelationshipTypeColor(final RelationshipType relType)
	{
		final String archeType = relType.getRelationshipTypeAttachment().getArcheType();
		if (archeType.equals(Constants.RELATIONSHIP_ARCHETYPE_ALLIED))
			return Color.green;
		if (archeType.equals(Constants.RELATIONSHIP_ARCHETYPE_NEUTRAL))
			return Color.lightGray;
		if (archeType.equals(Constants.RELATIONSHIP_ARCHETYPE_WAR))
			return Color.red;
		throw new IllegalStateException("PoliticsUI: RelationshipType: " + relType.getName() + " can only be of archeType Allied, Neutral or War");
	}
	
	/**
	 * Gets a label showing the flag + name of this player
	 * 
	 * @param player
	 *            the player to get the label for
	 * @return the label representing this player
	 */
	protected JLabel getPlayerLabel(final PlayerID player)
	{
		return new JLabel(player.getName(), new ImageIcon(m_uic.getFlagImageFactory().getFlag(player)), JLabel.LEFT);
	}
	
	/**
	 * Redraw this panel (because of changed politics)
	 * 
	 */
	public void redrawPolitics()
	{
		this.removeAll();
		this.drawPoliticsUI();
		this.updateUI();
	}
}
