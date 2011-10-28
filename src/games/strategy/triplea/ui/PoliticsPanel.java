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
import games.strategy.triplea.attatchments.PoliticalActionAttachment;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

/**
 * This panel is fired by ActionButtons and controls the selection of a valid
 * political action to attempt.
 * 
 * @author Edwin van der Wal
 * 
 */
@SuppressWarnings("serial")
public class PoliticsPanel extends ActionPanel
{
	
	private final JLabel m_actionLabel = new JLabel();
	private PoliticalActionAttachment m_choice = null;
	private final TripleAFrame m_parent;
	
	public PoliticsPanel(GameData data, MapPanel map, TripleAFrame parent)
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
		SwingUtilities.invokeLater(new Runnable()
		{
			
			public void run()
			{
				removeAll();
				m_actionLabel.setText(id.getName() + " Politics");
				add(m_actionLabel);
				
				add(new JButton(SelectPoliticalActionAction));
				add(new JButton(DontBotherAction));
			}
			
		});
	}
	
	/**
	 * waits till someone calls release() and then returns the political action
	 * chosen
	 * 
	 * @return the choice of political action
	 */
	public PoliticalActionAttachment waitForPoliticalAction()
	{
		
		if (PoliticalActionAttachment.getValidActions(getCurrentPlayer()).isEmpty())
		{
			return null; // No Valid political actions, do nothing
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
		
		public void actionPerformed(ActionEvent event)
		{
			final JDialog politicalChoiceDialog = new JDialog(m_parent, true);
			Insets insets = new Insets(1, 1, 1, 1);
			int row = 0;
			JPanel politicalChoicePanel = new JPanel();
			politicalChoicePanel.setLayout(new GridBagLayout());
			PoliticalStateOverview overview = new PoliticalStateOverview(getData(), getMap().getUIContext());
			politicalChoicePanel.add(overview, new GridBagConstraints(0, row++, 4, 1, 1.0, 20.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
			politicalChoicePanel.add(new JSeparator(JSeparator.HORIZONTAL), new GridBagConstraints(0, row++, 20, 1, 0.1, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
			
			for (final PoliticalActionAttachment paa : PoliticalActionAttachment.getValidActions(getCurrentPlayer()))
			{
				politicalChoicePanel.add(getOtherPlayerFlags(paa), new GridBagConstraints(0, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 0, 0));
				JButton button = new JButton(PoliticsText.getInstance().getButtonText(paa.getText()));
				button.addActionListener(new ActionListener()
				{
					
					public void actionPerformed(ActionEvent ae)
					{
						m_choice = paa;
						politicalChoiceDialog.setVisible(false);
						release();
					}
					
				});
				politicalChoicePanel.add(button, new GridBagConstraints(1, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
				politicalChoicePanel.add(new JLabel(PoliticsText.getInstance().getDescription(paa.getText())), new GridBagConstraints(2, row, 1, 1, 5.0, 1.0, GridBagConstraints.WEST,
							GridBagConstraints.BOTH, insets, 0, 0));
				row++;
			}
			
			politicalChoicePanel.add(new JButton(new AbstractAction("No Actions")
			{
				
				public void actionPerformed(ActionEvent arg0)
				{
					politicalChoiceDialog.setVisible(false);
				}
				
			}), new GridBagConstraints(0, row, 20, 1, 1.0, 1.0, GridBagConstraints.EAST, GridBagConstraints.NONE, insets, 0, 0));
			
			politicalChoiceDialog.add(politicalChoicePanel);
			Dimension d = politicalChoiceDialog.getPreferredSize();
			politicalChoiceDialog.setSize(new Dimension(d.width + 20, d.height + 50));
			politicalChoiceDialog.setVisible(true);
		}
	};
	
	/**
	 * This will stop the politicsPhase
	 * 
	 */
	private final Action DontBotherAction = new AbstractAction("Done")
	{
		
		public void actionPerformed(ActionEvent event)
		{
			
			m_choice = null;
			release();
			
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
	private JPanel getOtherPlayerFlags(PoliticalActionAttachment paa)
	{
		JPanel panel = new JPanel();
		for (PlayerID p : paa.getOtherPlayers())
		{
			panel.add(new JLabel(new ImageIcon(this.getMap().getUIContext().getFlagImageFactory().getFlag(p))));
		}
		return panel;
	}
}
