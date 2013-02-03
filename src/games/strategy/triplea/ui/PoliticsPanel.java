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
import games.strategy.engine.data.RelationshipTypeList;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.util.PlayerOrderComparator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

/**
 * This panel is fired by ActionButtons and controls the selection of a valid
 * political action to attempt.
 * 
 * @author Edwin van der Wal
 * 
 */
public class PoliticsPanel extends ActionPanel
{
	private static final long serialVersionUID = -4661479948450261578L;
	private final JLabel m_actionLabel = new JLabel();
	private JButton m_selectPoliticalActionButton = null;
	private JButton m_doneButton = null;
	private PoliticalActionAttachment m_choice = null;
	private final TripleAFrame m_parent;
	private boolean m_firstRun = true;
	
	protected List<PoliticalActionAttachment> m_validPoliticalActions = null;
	
	public PoliticsPanel(final GameData data, final MapPanel map, final TripleAFrame parent)
	{
		super(data, map);
		m_parent = parent;
	}
	
	@Override
	public String toString()
	{
		return "Politics Panel";
	}
	
	@Override
	public void display(final PlayerID id)
	{
		super.display(id);
		m_choice = null;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				removeAll();
				m_actionLabel.setText(id.getName() + " Politics");
				add(m_actionLabel);
				m_selectPoliticalActionButton = new JButton(SelectPoliticalActionAction);
				m_selectPoliticalActionButton.setEnabled(false);
				add(m_selectPoliticalActionButton);
				m_doneButton = new JButton(DontBotherAction);
				m_doneButton.setEnabled(false);
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						m_doneButton.requestFocusInWindow();
					}
				});
				add(m_doneButton);
			}
		});
	}
	
	/**
	 * waits till someone calls release() and then returns the political action
	 * chosen
	 * 
	 * @param firstRun
	 * 
	 * @return the choice of political action
	 */
	public PoliticalActionAttachment waitForPoliticalAction(final boolean firstRun, final IPoliticsDelegate iPoliticsDelegate)
	{
		m_firstRun = firstRun;
		// NEVER EVER ACCESS A DELEGATE OR BRIDGE FROM A UI!!!! (or the game won't work in multiplayer) (in other words, do not use the DelegateFinder in any way, or access local delegates, or pass the bridge)
		m_validPoliticalActions = new ArrayList<PoliticalActionAttachment>(iPoliticsDelegate.getValidActions()); // reset each time, we need to retest the conditions...
		Collections.sort(m_validPoliticalActions, new PoliticalActionComparator(getCurrentPlayer(), getData()));
		if (m_firstRun && m_validPoliticalActions.isEmpty())
		{
			return null; // No Valid political actions, do nothing
		}
		else
		{
			if (m_firstRun)
			{
				// play a sound for this phase
				ClipPlayer.play(SoundPath.CLIP_PHASE_POLITICS, getCurrentPlayer().getName());
			}
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					m_selectPoliticalActionButton.setEnabled(true);
					m_doneButton.setEnabled(true);
					// press the politics button for us.
					SelectPoliticalActionAction.actionPerformed(null);
				}
			});
		}
		waitForRelease();
		return m_choice;
	}
	
	/**
	 * Fires up a JDialog showing the political landscape and valid actions,
	 * choosing an action will release this model and trigger waitForRelease()
	 */
	private final Action SelectPoliticalActionAction = new AbstractAction("Do Politics...")
	{
		private static final long serialVersionUID = 3906101150281154032L;
		
		public void actionPerformed(final ActionEvent event)
		{
			final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
			final int availHeight = screenResolution.height - 96;
			final int availWidth = screenResolution.width - 30;
			final int availHeightOverview = (int) ((float) availHeight * 2 / 3);
			final int availHeightChoice = (int) ((float) availHeight / 3);
			// availHeighChoice -= (42 * getData().getPlayerList().getPlayers().size() + 46);
			final JDialog politicalChoiceDialog = new JDialog(m_parent, "Political Actions", true);
			final Insets insets = new Insets(1, 1, 1, 1);
			int row = 0;
			final JPanel politicalChoicePanel = new JPanel();
			politicalChoicePanel.setLayout(new GridBagLayout());
			final PoliticalStateOverview overview = new PoliticalStateOverview(getData(), getMap().getUIContext());
			final JScrollPane overviewScroll = new JScrollPane(overview);
			overviewScroll.setBorder(BorderFactory.createEmptyBorder());
			// add 26 to height when the actions are empty, because for some stupid reason java calculates the pack size wrong (again)...
			// add 20 to either when the opposite needs scroll bars, because that is how big scroll bars are..
			overviewScroll.setPreferredSize(new Dimension((overviewScroll.getPreferredSize().width > availWidth ? availWidth : (overviewScroll.getPreferredSize().width +
						(overviewScroll.getPreferredSize().height > availHeightOverview ? 20 : 0))),
						(overviewScroll.getPreferredSize().height > availHeightOverview ? availHeightOverview :
									(overviewScroll.getPreferredSize().height + (m_validPoliticalActions.isEmpty() ? 26 : 0)
												+ (overviewScroll.getPreferredSize().width > availWidth ? 20 : 0)))));
			// politicalChoicePanel.add(overviewScroll, new GridBagConstraints(0, row++, 4, 1, 1.0, 10.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
			// politicalChoicePanel.add(new JSeparator(JSeparator.HORIZONTAL), new GridBagConstraints(0, row++, 20, 1, 0.1, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
			final JScrollPane choiceScroll = new JScrollPane(PoliticalActionButtonPanel(politicalChoiceDialog));
			choiceScroll.setBorder(BorderFactory.createEmptyBorder());
			choiceScroll.setPreferredSize(new Dimension((choiceScroll.getPreferredSize().width > availWidth ? availWidth : (choiceScroll.getPreferredSize().width +
						(choiceScroll.getPreferredSize().height > availHeightChoice ? 20 : 0))),
						(choiceScroll.getPreferredSize().height > availHeightChoice ? availHeightChoice : (choiceScroll.getPreferredSize().height) +
									(choiceScroll.getPreferredSize().width > availWidth ? 20 : 0))));
			// politicalChoicePanel.add(choiceScroll, new GridBagConstraints(0, row++, 1, 1, 1.0, 11.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
			
			final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, overviewScroll, choiceScroll);
			splitPane.setOneTouchExpandable(true);
			splitPane.setDividerSize(8);
			politicalChoicePanel.add(splitPane, new GridBagConstraints(0, row++, 1, 1, 100.0, 100.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
			
			final JButton noActionButton = new JButton(new AbstractAction("No Actions")
			{
				private static final long serialVersionUID = -5979922310580413800L;
				
				public void actionPerformed(final ActionEvent arg0)
				{
					politicalChoiceDialog.setVisible(false);
				}
			});
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					noActionButton.requestFocusInWindow();
				}
			});
			politicalChoicePanel.add(noActionButton, new GridBagConstraints(0, row, 20, 1, 1.0, 1.0, GridBagConstraints.EAST, GridBagConstraints.NONE, insets, 0, 0));
			politicalChoiceDialog.add(politicalChoicePanel);
			politicalChoiceDialog.pack();
			politicalChoiceDialog.setLocationRelativeTo(m_parent);
			politicalChoiceDialog.setVisible(true);
			politicalChoiceDialog.dispose();
		}
	};
	
	private JPanel PoliticalActionButtonPanel(final JDialog parent)
	{
		final JPanel politicalActionButtonPanel = new JPanel();
		politicalActionButtonPanel.setLayout(new GridBagLayout());
		int row = 0;
		final Insets insets = new Insets(1, 1, 1, 1);
		for (final PoliticalActionAttachment paa : m_validPoliticalActions)
		{
			politicalActionButtonPanel.add(getOtherPlayerFlags(paa), new GridBagConstraints(0, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 0, 0));
			final JButton button = new JButton(getActionButtonText(paa));
			button.addActionListener(new ActionListener()
			{
				public void actionPerformed(final ActionEvent ae)
				{
					m_selectPoliticalActionButton.setEnabled(false);
					m_doneButton.setEnabled(false);
					m_validPoliticalActions = null;
					m_choice = paa;
					parent.setVisible(false);
					release();
				}
			});
			politicalActionButtonPanel.add(button, new GridBagConstraints(1, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
			politicalActionButtonPanel.add(getActionDescriptionLabel(paa), new GridBagConstraints(2, row, 1, 1, 5.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 0, 0));
			row++;
		}
		return politicalActionButtonPanel;
	}
	
	/**
	 * This will stop the politicsPhase
	 * 
	 */
	private final Action DontBotherAction = new AbstractAction("Done")
	{
		private static final long serialVersionUID = 5975405674090929150L;
		
		public void actionPerformed(final ActionEvent event)
		{
			if (!m_firstRun || youSureDoNothing())
			{
				m_choice = null;
				release();
			}
		}
		
		private boolean youSureDoNothing()
		{
			final int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(PoliticsPanel.this), "Are you sure you dont want to do anything?", "End Politics",
						JOptionPane.YES_NO_OPTION);
			return rVal == JOptionPane.YES_OPTION;
		}
	};
	
	/**
	 * Convenient method to get a JCompenent showing the flags involved in this
	 * action.
	 * 
	 * @param paa
	 *            the political action attachment to get the "otherflags" for
	 * @return a JComponent with the flags involved.
	 */
	private JPanel getOtherPlayerFlags(final PoliticalActionAttachment paa)
	{
		final JPanel panel = new JPanel();
		for (final PlayerID p : paa.getOtherPlayers())
		{
			panel.add(new JLabel(new ImageIcon(this.getMap().getUIContext().getFlagImageFactory().getFlag(p))));
		}
		return panel;
	}
	
	private String getActionButtonText(final PoliticalActionAttachment paa)
	{
		final String costString = paa.getCostPU() == 0 ? "" : "[" + paa.getCostPU() + " PU] ";
		return costString + PoliticsText.getInstance().getButtonText(paa.getText());
	}
	
	private JLabel getActionDescriptionLabel(final PoliticalActionAttachment paa)
	{
		final String chanceString = paa.toHit() == paa.diceSides() ? "" : "[" + paa.toHit() + "/" + paa.diceSides() + "] ";
		return new JLabel(chanceString + PoliticsText.getInstance().getDescription(paa.getText()));
	}
}


class PoliticalActionComparator implements Comparator<PoliticalActionAttachment>
{
	private final GameData m_data;
	private final PlayerID m_player;
	
	public PoliticalActionComparator(final PlayerID currentPlayer, final GameData data)
	{
		m_data = data;
		m_player = currentPlayer;
	}
	
	public int compare(final PoliticalActionAttachment paa1, final PoliticalActionAttachment paa2)
	{
		if (paa1.equals(paa2))
			return 0;
		final String[] paa1RelationChange = paa1.getRelationshipChange().iterator().next().split(":");
		final String[] paa2RelationChange = paa2.getRelationshipChange().iterator().next().split(":");
		final RelationshipTypeList relationshipTypeList;
		m_data.acquireReadLock();
		try
		{
			relationshipTypeList = m_data.getRelationshipTypeList();
		} finally
		{
			m_data.releaseReadLock();
		}
		final RelationshipType paa1NewType = relationshipTypeList.getRelationshipType(paa1RelationChange[2]);
		final RelationshipType paa2NewType = relationshipTypeList.getRelationshipType(paa2RelationChange[2]);
		// sort by player
		final PlayerID paa1p1 = m_data.getPlayerList().getPlayerID(paa1RelationChange[0]);
		final PlayerID paa1p2 = m_data.getPlayerList().getPlayerID(paa1RelationChange[1]);
		final PlayerID paa2p1 = m_data.getPlayerList().getPlayerID(paa2RelationChange[0]);
		final PlayerID paa2p2 = m_data.getPlayerList().getPlayerID(paa2RelationChange[1]);
		final PlayerID paa1OtherPlayer = (m_player.equals(paa1p1) ? paa1p2 : paa1p1);
		final PlayerID paa2OtherPlayer = (m_player.equals(paa2p1) ? paa2p2 : paa2p1);
		if (!paa1OtherPlayer.equals(paa2OtherPlayer))
		{
			final int order = new PlayerOrderComparator(m_data).compare(paa1OtherPlayer, paa2OtherPlayer);
			if (order != 0)
				return order;
		}
		// sort by achetype
		if (!paa1NewType.equals(paa2NewType))
		{
			if (paa1NewType.getRelationshipTypeAttachment().isWar() && !paa2NewType.getRelationshipTypeAttachment().isWar())
				return -1;
			if (!paa1NewType.getRelationshipTypeAttachment().isWar() && paa2NewType.getRelationshipTypeAttachment().isWar())
				return 1;
			if (paa1NewType.getRelationshipTypeAttachment().isNeutral() && paa2NewType.getRelationshipTypeAttachment().isAllied())
				return -1;
			if (paa1NewType.getRelationshipTypeAttachment().isAllied() && paa2NewType.getRelationshipTypeAttachment().isNeutral())
				return 1;
		}
		// sort by name of new relationship type
		if (!paa1NewType.getName().equals(paa2NewType.getName()))
		{
			return paa1NewType.getName().compareTo(paa2NewType.getName());
		}
		return 0;
	}
}
